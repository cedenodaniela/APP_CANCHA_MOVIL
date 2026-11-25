package com.canchapp.movil.core.red

import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** RNF-02 · El registro de depuración nunca escribe contraseñas ni tokens. */
class RegistroHttpTest {

    private val servidor = MockWebServer()
    private val lineas = mutableListOf<String>()

    @Before
    fun preparar() = servidor.start()

    @After
    fun cerrar() = servidor.close()

    private fun enviar(ruta: String, cuerpo: String, respuesta: String): String {
        servidor.enqueue(MockResponse.Builder().code(200).body(respuesta).build())
        val cliente = OkHttpClient.Builder()
            .addInterceptor(RegistroHttp.crear { linea -> lineas += linea })
            .build()
        cliente.newCall(
            Request.Builder()
                .url(servidor.url(ruta))
                .header("Authorization", "Bearer token-secreto")
                .post(cuerpo.toRequestBody("application/json".toMediaType()))
                .build(),
        ).execute().use { it.body.string() }
        return lineas.joinToString("\n")
    }

    @Test
    fun `dado un login, cuando se registra, entonces no aparecen la contrasena ni el token`() {
        val registro = enviar("/api/v1/auth/login", """{"password":"clave-secreta"}""", """{"token":"jwt-secreto"}""")

        assertTrue(registro.contains("/api/v1/auth/login"))
        assertFalse(registro.contains("clave-secreta"))
        assertFalse(registro.contains("jwt-secreto"))
        assertFalse(registro.contains("token-secreto"))
    }

    @Test
    fun `dado otra ruta, cuando se registra, entonces muestra el cuerpo pero no el token`() {
        val registro = enviar("/api/v1/reservas", """{"canchaId":1}""", """{"id":9}""")

        assertTrue(registro.contains("\"canchaId\":1"))
        assertFalse(registro.contains("token-secreto"))
    }
}
