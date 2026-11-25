package com.canchapp.movil.core.red

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

/**
 * Solo en debug: escribe en Logcat las solicitudes y respuestas de la API.
 *
 * - Oculta siempre el encabezado `Authorization`, para que el token no aparezca en los registros.
 * - En las rutas de autenticación (`/auth/…`) no escribe el cuerpo: ahí viajan la contraseña
 *   (login, registro, recuperación) y el token de la respuesta. Cualquier endpoint nuevo que reciba
 *   o devuelva credenciales debe vivir bajo `/auth/`.
 */
object RegistroHttp {

    fun agregar(builder: OkHttpClient.Builder) {
        builder.addInterceptor(crear())
    }

    /** @param destino dónde se escribe; las pruebas pasan uno propio para revisar lo escrito */
    internal fun crear(destino: HttpLoggingInterceptor.Logger = HttpLoggingInterceptor.Logger.DEFAULT): Interceptor {
        val conCuerpo = HttpLoggingInterceptor(destino).apply {
            level = HttpLoggingInterceptor.Level.BODY
            redactHeader("Authorization")
        }
        val sinCuerpo = HttpLoggingInterceptor(destino).apply {
            level = HttpLoggingInterceptor.Level.HEADERS
            redactHeader("Authorization")
        }
        return Interceptor { chain ->
            val registro = if ("/auth/" in chain.request().url.encodedPath) sinCuerpo else conCuerpo
            registro.intercept(chain)
        }
    }
}
