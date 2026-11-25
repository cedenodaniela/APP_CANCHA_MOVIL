package com.canchapp.movil.core.red

import com.canchapp.movil.core.sesion.FuenteSesion
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InterceptorAutenticacionTest {

    private class SesionFalsa(var token: String?) : FuenteSesion {
        var cerrada = false
        override fun tokenActual() = token
        override suspend fun cerrarSesion() {
            cerrada = true
            token = null
        }
    }

    private lateinit var api: MockWebServer

    @Before
    fun iniciar() {
        api = MockWebServer()
        api.start()
    }

    @After
    fun detener() {
        api.close()
    }

    private fun cliente(sesion: FuenteSesion) = OkHttpClient.Builder()
        .addInterceptor(InterceptorAutenticacion(sesion, api.url("/api/v1/")))
        .build()

    private fun llamar(sesion: FuenteSesion, codigo: Int = 200) {
        api.enqueue(MockResponse.Builder().code(codigo).build())
        cliente(sesion).newCall(Request.Builder().url(api.url("/api/v1/reservas")).build()).execute().close()
    }

    @Test
    fun `dada una sesión, cuando llama a la API, entonces envía el token`() {
        llamar(SesionFalsa("abc"))

        assertEquals("Bearer abc", api.takeRequest().headers["Authorization"])
    }

    @Test
    fun `dado que no hay sesión, cuando llama a la API, entonces no envía token`() {
        llamar(SesionFalsa(null))

        assertNull(api.takeRequest().headers["Authorization"])
    }

    @Test
    fun `dado un token vencido, cuando la API responde 401, entonces cierra la sesión`() {
        val sesion = SesionFalsa("vencido")

        llamar(sesion, codigo = 401)

        assertTrue(sesion.cerrada)
    }

    @Test
    fun `dado un error distinto de 401, cuando la API responde, entonces la sesión sigue abierta`() {
        val sesion = SesionFalsa("abc")

        llamar(sesion, codigo = 403)

        assertFalse(sesion.cerrada)
    }

    @Test
    fun `dado otro servidor, cuando se le llama, entonces no recibe el token`() {
        val otro = MockWebServer().apply { start() }
        try {
            otro.enqueue(MockResponse.Builder().code(200).build())
            cliente(SesionFalsa("abc")).newCall(Request.Builder().url(otro.url("/recurso")).build()).execute().close()

            assertNull(otro.takeRequest().headers["Authorization"])
        } finally {
            otro.close()
        }
    }
}
