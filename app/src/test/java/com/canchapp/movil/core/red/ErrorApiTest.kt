package com.canchapp.movil.core.red

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.net.ConnectException

class ErrorApiTest {

    private fun httpError(codigo: Int, cuerpo: String) = HttpException(
        Response.error<Any>(codigo, cuerpo.toResponseBody("application/json".toMediaType())),
    )

    @Test
    fun `dado un error con cuerpo uniforme, cuando se traduce, entonces muestra el mensaje del backend`() {
        val error = httpError(401, """{"codigo":"CREDENCIALES_INVALIDAS","mensaje":"Correo o contraseña incorrectos"}""")

        assertEquals(
            MensajeError.DelServidor("CREDENCIALES_INVALIDAS", "Correo o contraseña incorrectos"),
            error.aMensajeError(),
        )
    }

    @Test
    fun `dado un error sin el cuerpo esperado, cuando se traduce, entonces es inesperado`() {
        assertEquals(MensajeError.Inesperado, httpError(500, "<html>error</html>").aMensajeError())
    }

    @Test
    fun `dado que no hay conexión, cuando se traduce, entonces lo indica`() {
        assertEquals(MensajeError.SinConexion, ConnectException("sin red").aMensajeError())
    }
}
