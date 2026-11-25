package com.canchapp.movil.auth.data

import com.canchapp.movil.core.red.MensajeError
import com.canchapp.movil.core.red.aMensajeError
import com.canchapp.movil.core.sesion.Rol
import com.canchapp.movil.core.sesion.Sesion
import com.canchapp.movil.core.sesion.SesionRepository
import com.canchapp.movil.core.sesion.UsuarioSesion
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create

/** RF-01 · Inicio de sesión contra el contrato de `POST /api/v1/auth/login`. */
class AuthRepositoryTest {

    private val servidor = MockWebServer()
    private val sesiones = mockk<SesionRepository>()
    private lateinit var repositorio: AuthRepository

    @Before
    fun preparar() {
        servidor.start()
        coEvery { sesiones.guardar(any()) } returns Unit
        val json = Json { ignoreUnknownKeys = true }
        val api = Retrofit.Builder()
            .baseUrl(servidor.url("/api/v1/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create<AuthApi>()
        repositorio = AuthRepository(api, sesiones)
    }

    @After
    fun cerrar() = servidor.close()

    @Test
    fun `dado credenciales validas, cuando inicia sesion, entonces guarda el token y el usuario`() = runTest {
        servidor.enqueue(
            MockResponse.Builder()
                .code(200)
                .body("""{"token":"jwt-123","expiraEn":"2026-11-25T20:00:00Z","usuario":{"id":7,"nombre":"Ana","rol":"CLIENTE"}}""")
                .build(),
        )

        val resultado = repositorio.iniciarSesion("ana@correo.com", "secreta")

        val usuario = UsuarioSesion(7, "Ana", Rol.CLIENTE)
        assertEquals(usuario, resultado.getOrNull())
        coVerify { sesiones.guardar(Sesion("jwt-123", usuario)) }

        val solicitud = servidor.takeRequest()
        assertEquals("POST", solicitud.method)
        assertEquals("/api/v1/auth/login", solicitud.url.encodedPath)
        assertEquals("""{"email":"ana@correo.com","password":"secreta"}""", solicitud.body?.utf8())
    }

    @Test
    fun `dado credenciales incorrectas, cuando inicia sesion, entonces devuelve el mensaje del backend y no guarda nada`() = runTest {
        servidor.enqueue(
            MockResponse.Builder()
                .code(401)
                .body("""{"codigo":"CREDENCIALES_INVALIDAS","mensaje":"Correo o contraseña incorrectos"}""")
                .build(),
        )

        val resultado = repositorio.iniciarSesion("ana@correo.com", "mala")

        assertTrue(resultado.isFailure)
        assertEquals(
            MensajeError.DelServidor("CREDENCIALES_INVALIDAS", "Correo o contraseña incorrectos"),
            resultado.exceptionOrNull()!!.aMensajeError(),
        )
        coVerify(exactly = 0) { sesiones.guardar(any()) }
    }
}
