package com.canchapp.movil.core.red

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor

/**
 * Solo en debug: escribe en Logcat las solicitudes y respuestas de la API.
 *
 * - Oculta siempre el encabezado `Authorization`, para que el token no aparezca en los registros.
 * - En las rutas con credenciales no escribe el cuerpo: `/auth/…` (login y recuperación, con la
 *   contraseña y el token) y el registro `POST /usuarios`. Un endpoint nuevo que reciba o devuelva
 *   credenciales se agrega a [llevaCredenciales].
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
            val registro = if (llevaCredenciales(chain.request())) sinCuerpo else conCuerpo
            registro.intercept(chain)
        }
    }

    private fun llevaCredenciales(solicitud: Request): Boolean {
        val ruta = solicitud.url.encodedPath
        return "/auth/" in ruta || (solicitud.method == "POST" && ruta.trimEnd('/').endsWith("/usuarios"))
    }
}
