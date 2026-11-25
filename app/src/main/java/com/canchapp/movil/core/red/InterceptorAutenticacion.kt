package com.canchapp.movil.core.red

import com.canchapp.movil.core.sesion.FuenteSesion
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Agrega `Authorization: Bearer <token>` a cada llamada a la API del backend (app_cancha/AGENTS.md §6).
 *
 * Si el backend responde 401 con una sesión abierta (token vencido o revocado), cierra la sesión:
 * la navegación observa la sesión y vuelve al inicio de sesión. A otros servidores nunca envía el token.
 */
class InterceptorAutenticacion(
    private val fuenteSesion: FuenteSesion,
    private val urlApi: HttpUrl,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val solicitud = chain.request()
        val token = fuenteSesion.tokenActual()
        val esApi = solicitud.url.host == urlApi.host && solicitud.url.port == urlApi.port

        if (token == null || !esApi) {
            return chain.proceed(solicitud)
        }

        val respuesta = chain.proceed(solicitud.newBuilder().header("Authorization", "Bearer $token").build())
        if (respuesta.code == 401) {
            runBlocking { fuenteSesion.cerrarSesion() }
        }
        return respuesta
    }
}
