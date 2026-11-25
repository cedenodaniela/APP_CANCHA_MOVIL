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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/** RF-02 · Crear la contraseña nueva con el enlace del correo. */
@OptIn(ExperimentalCoroutinesApi::class)
class RestablecerViewModelTest {

    private val despachador = StandardTestDispatcher()
    private val auth = mockk<AuthRepository>()
    private lateinit var viewModel: RestablecerViewModel

    @Before
    fun preparar() {
        Dispatchers.setMain(despachador)
        viewModel = RestablecerViewModel(auth)
    }

    @After
    fun limpiar() = Dispatchers.resetMain()

    @Test
    fun `dado una contrasena corta y una confirmacion distinta, cuando guarda, entonces marca ambos campos`() = runTest(despachador) {
        viewModel.alCambiarPassword("1234567")
        viewModel.alCambiarConfirmacion("otra")

        viewModel.guardar("token")
        advanceUntilIdle()

        assertEquals(setOf(CampoRestablecer.PASSWORD, CampoRestablecer.CONFIRMACION), viewModel.estado.value.errores)
        coVerify(exactly = 0) { auth.restablecerContrasena(any(), any()) }
    }

    @Test
    fun `dado una contrasena valida, cuando guarda, entonces envia el token y vuelve al login`() = runTest(despachador) {
        coEvery { auth.restablecerContrasena("token-del-correo", "nueva-clave") } returns Result.success(Unit)
        viewModel.alCambiarPassword("nueva-clave")
        viewModel.alCambiarConfirmacion("nueva-clave")

        viewModel.guardar("token-del-correo")
        assertTrue(viewModel.estado.value.cargando)
        advanceUntilIdle()

        val estado = viewModel.estado.value
        assertTrue(estado.restablecida)
        assertFalse(estado.cargando)
        assertEquals("", estado.password)
        assertEquals("", estado.confirmacion)

        viewModel.restablecidaMostrada()
        assertFalse(viewModel.estado.value.restablecida)
    }

    @Test
    fun `dado un enlace vencido, cuando guarda, entonces muestra el mensaje del backend`() = runTest(despachador) {
        val vencido = HttpException(
            Response.error<Any>(
                400,
                """{"codigo":"ENLACE_INVALIDO","mensaje":"El enlace no es válido o ya venció. Solicita uno nuevo."}"""
                    .toResponseBody("application/json".toMediaType()),
            ),
        )
        coEvery { auth.restablecerContrasena(any(), any()) } returns Result.failure(vencido)
        viewModel.alCambiarPassword("nueva-clave")
        viewModel.alCambiarConfirmacion("nueva-clave")

        viewModel.guardar("viejo")
        advanceUntilIdle()

        assertEquals(
            MensajeError.DelServidor("ENLACE_INVALIDO", "El enlace no es válido o ya venció. Solicita uno nuevo."),
            viewModel.estado.value.error,
        )
        assertFalse(viewModel.estado.value.restablecida)
    }

    @Test
    fun `dado un campo con error, cuando lo corrige, entonces solo ese error desaparece`() = runTest(despachador) {
        viewModel.alCambiarConfirmacion("otra")
        viewModel.guardar("token")
        assertEquals(setOf(CampoRestablecer.PASSWORD, CampoRestablecer.CONFIRMACION), viewModel.estado.value.errores)

        viewModel.alCambiarPassword("nueva-clave")

        assertEquals(setOf(CampoRestablecer.CONFIRMACION), viewModel.estado.value.errores)
    }
}
