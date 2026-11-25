package com.canchapp.movil.auth.login

import com.canchapp.movil.auth.data.AuthRepository
import com.canchapp.movil.core.red.MensajeError
import com.canchapp.movil.core.sesion.Rol
import com.canchapp.movil.core.sesion.UsuarioSesion
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.ConnectException

/** RF-01 · Pantalla de inicio de sesión. */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val despachador = StandardTestDispatcher()
    private val auth = mockk<AuthRepository>()
    private lateinit var viewModel: LoginViewModel

    @Before
    fun preparar() {
        Dispatchers.setMain(despachador)
        viewModel = LoginViewModel(auth)
    }

    @After
    fun limpiar() = Dispatchers.resetMain()

    @Test
    fun `dado un correo mal escrito, cuando ingresa, entonces marca el campo y no llama al backend`() = runTest(despachador) {
        viewModel.alCambiarEmail("ana.correo.com")
        viewModel.alCambiarPassword("secreta")

        viewModel.iniciarSesion()
        advanceUntilIdle()

        assertTrue(viewModel.estado.value.emailInvalido)
        assertFalse(viewModel.estado.value.passwordVacia)
        coVerify(exactly = 0) { auth.iniciarSesion(any(), any()) }
    }

    @Test
    fun `dado la contrasena vacia, cuando ingresa, entonces marca el campo`() = runTest(despachador) {
        viewModel.alCambiarEmail("ana@correo.com")

        viewModel.iniciarSesion()

        assertTrue(viewModel.estado.value.passwordVacia)
        coVerify(exactly = 0) { auth.iniciarSesion(any(), any()) }
    }

    @Test
    fun `dado un correo con espacios y mayusculas, cuando ingresa, entonces lo envia limpio`() = runTest(despachador) {
        coEvery { auth.iniciarSesion(any(), any()) } returns Result.success(UsuarioSesion(1, "Ana", Rol.CLIENTE))
        viewModel.alCambiarEmail("  Ana@Correo.com ")
        viewModel.alCambiarPassword("secreta")

        viewModel.iniciarSesion()
        assertTrue(viewModel.estado.value.cargando)
        advanceUntilIdle()

        coVerify { auth.iniciarSesion("ana@correo.com", "secreta") }
        val estado = viewModel.estado.value
        assertFalse(estado.cargando)
        assertEquals("", estado.password)
        assertNull(estado.error)
    }

    @Test
    fun `dado que el backend rechaza, cuando ingresa, entonces muestra su mensaje`() = runTest(despachador) {
        coEvery { auth.iniciarSesion(any(), any()) } returns Result.failure(ConnectException("sin red"))
        viewModel.alCambiarEmail("ana@correo.com")
        viewModel.alCambiarPassword("secreta")

        viewModel.iniciarSesion()
        advanceUntilIdle()

        assertFalse(viewModel.estado.value.cargando)
        assertEquals(MensajeError.SinConexion, viewModel.estado.value.error)
    }

    @Test
    fun `dado un error mostrado, cuando corrige el correo, entonces el error desaparece`() = runTest(despachador) {
        coEvery { auth.iniciarSesion(any(), any()) } returns Result.failure(ConnectException("sin red"))
        viewModel.alCambiarEmail("ana@correo.com")
        viewModel.alCambiarPassword("secreta")
        viewModel.iniciarSesion()
        advanceUntilIdle()

        viewModel.alCambiarEmail("otra@correo.com")

        assertNull(viewModel.estado.value.error)
    }

    @Test
    fun `dado que viene del registro, cuando se muestra, entonces confirma la cuenta y deja el correo`() = runTest(despachador) {
        viewModel.mostrarCuentaCreada("ana@correo.com")

        assertTrue(viewModel.estado.value.cuentaCreada)
        assertEquals("ana@correo.com", viewModel.estado.value.email)
    }
}
