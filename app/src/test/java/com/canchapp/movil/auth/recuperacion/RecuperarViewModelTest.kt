package com.canchapp.movil.auth.recuperacion

import com.canchapp.movil.auth.data.AuthRepository
import com.canchapp.movil.core.red.MensajeError
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
import java.io.IOException

/** RF-02 · Pedir el enlace de recuperación. */
@OptIn(ExperimentalCoroutinesApi::class)
class RecuperarViewModelTest {

    private val despachador = StandardTestDispatcher()
    private val auth = mockk<AuthRepository>()
    private lateinit var viewModel: RecuperarViewModel

    @Before
    fun preparar() {
        Dispatchers.setMain(despachador)
        viewModel = RecuperarViewModel(auth)
    }

    @After
    fun limpiar() = Dispatchers.resetMain()

    @Test
    fun `dado un correo invalido, cuando envia, entonces lo marca y no llama al backend`() = runTest(despachador) {
        viewModel.alCambiarEmail("ana.correo.com")

        viewModel.enviar()
        advanceUntilIdle()

        assertTrue(viewModel.estado.value.emailInvalido)
        coVerify(exactly = 0) { auth.solicitarRecuperacion(any()) }
    }

    @Test
    fun `dado un correo con espacios, cuando envia, entonces lo normaliza y muestra el mensaje del backend`() = runTest(despachador) {
        coEvery { auth.solicitarRecuperacion("ana@correo.com") } returns Result.success("Si el correo está registrado, te enviamos un enlace.")
        viewModel.alCambiarEmail(" Ana@Correo.com ")

        viewModel.enviar()
        assertTrue(viewModel.estado.value.cargando)
        advanceUntilIdle()

        val estado = viewModel.estado.value
        assertFalse(estado.cargando)
        assertEquals("ana@correo.com", estado.email)
        assertEquals("Si el correo está registrado, te enviamos un enlace.", estado.enviado)
    }

    @Test
    fun `dado que no hay conexion, cuando envia, entonces lo avisa`() = runTest(despachador) {
        coEvery { auth.solicitarRecuperacion(any()) } returns Result.failure(IOException())
        viewModel.alCambiarEmail("ana@correo.com")

        viewModel.enviar()
        advanceUntilIdle()

        assertEquals(MensajeError.SinConexion, viewModel.estado.value.error)
        assertNull(viewModel.estado.value.enviado)
    }

    @Test
    fun `dado el correo del login, cuando abre la pantalla, entonces lo deja escrito sin pisar lo que escribio`() {
        viewModel.usarCorreo("ana@correo.com")
        assertEquals("ana@correo.com", viewModel.estado.value.email)

        viewModel.alCambiarEmail("otra@correo.com")
        viewModel.usarCorreo("ana@correo.com")
        assertEquals("otra@correo.com", viewModel.estado.value.email)
    }
}
