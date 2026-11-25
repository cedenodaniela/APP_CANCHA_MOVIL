package com.canchapp.movil.auth.registro

import com.canchapp.movil.auth.data.AuthRepository
import com.canchapp.movil.auth.data.SolicitudRegistro
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/** RF-01 · Registro de clientes. */
@OptIn(ExperimentalCoroutinesApi::class)
class RegistroViewModelTest {

    private val despachador = StandardTestDispatcher()
    private val auth = mockk<AuthRepository>()
    private lateinit var viewModel: RegistroViewModel

    @Before
    fun preparar() {
        Dispatchers.setMain(despachador)
        viewModel = RegistroViewModel(auth)
    }

    @After
    fun limpiar() = Dispatchers.resetMain()

    private fun llenar(
        nombre: String = "Ana",
        apellido: String = "Mora",
        email: String = "ana@correo.com",
        telefono: String = "",
        password: String = "secreta123",
        confirmacion: String = password,
    ) {
        viewModel.alCambiarNombre(nombre)
        viewModel.alCambiarApellido(apellido)
        viewModel.alCambiarEmail(email)
        viewModel.alCambiarTelefono(telefono)
        viewModel.alCambiarPassword(password)
        viewModel.alCambiarConfirmacion(confirmacion)
    }

    @Test
    fun `dado un formulario vacio, cuando se registra, entonces marca cada campo obligatorio y no llama al backend`() = runTest(despachador) {
        viewModel.registrar()
        advanceUntilIdle()

        assertEquals(
            setOf(CampoRegistro.NOMBRE, CampoRegistro.APELLIDO, CampoRegistro.EMAIL, CampoRegistro.PASSWORD),
            viewModel.estado.value.errores,
        )
        coVerify(exactly = 0) { auth.registrar(any()) }
    }

    @Test
    fun `dado una contrasena corta y una confirmacion distinta, cuando se registra, entonces marca ambos campos`() = runTest(despachador) {
        llenar(password = "1234567", confirmacion = "otra")

        viewModel.registrar()

        assertEquals(setOf(CampoRegistro.PASSWORD, CampoRegistro.CONFIRMACION), viewModel.estado.value.errores)
        coVerify(exactly = 0) { auth.registrar(any()) }
    }

    @Test
    fun `dado datos validos con espacios, cuando se registra, entonces envia los datos limpios y vuelve al login`() = runTest(despachador) {
        coEvery { auth.registrar(any()) } returns Result.success(Unit)
        llenar(nombre = " Ana ", apellido = "Mora ", email = " Ana@Correo.com", telefono = " ")

        viewModel.registrar()
        assertTrue(viewModel.estado.value.cargando)
        advanceUntilIdle()

        coVerify { auth.registrar(SolicitudRegistro("Ana", "Mora", "ana@correo.com", null, "secreta123")) }
        val estado = viewModel.estado.value
        assertFalse(estado.cargando)
        assertEquals("ana@correo.com", estado.cuentaCreada)
        assertEquals("", estado.password)
        assertEquals("", estado.confirmacion)
    }

    @Test
    fun `dado un correo ya registrado, cuando se registra, entonces muestra el mensaje del backend`() = runTest(despachador) {
        val conflicto = HttpException(
            Response.error<Any>(
                409,
                """{"codigo":"CORREO_YA_REGISTRADO","mensaje":"Ya existe una cuenta con ese correo."}"""
                    .toResponseBody("application/json".toMediaType()),
            ),
        )
        coEvery { auth.registrar(any()) } returns Result.failure(conflicto)
        llenar()

        viewModel.registrar()
        advanceUntilIdle()

        assertEquals(
            MensajeError.DelServidor("CORREO_YA_REGISTRADO", "Ya existe una cuenta con ese correo."),
            viewModel.estado.value.error,
        )
        assertNull(viewModel.estado.value.cuentaCreada)
    }

    @Test
    fun `dado un campo con error, cuando lo corrige, entonces solo ese error desaparece`() = runTest(despachador) {
        viewModel.registrar()

        viewModel.alCambiarNombre("Ana")

        assertFalse(CampoRegistro.NOMBRE in viewModel.estado.value.errores)
        assertTrue(CampoRegistro.APELLIDO in viewModel.estado.value.errores)
    }

    @Test
    fun `dado la cuenta creada, cuando la pantalla vuelve al login, entonces no se repite`() = runTest(despachador) {
        coEvery { auth.registrar(any()) } returns Result.success(Unit)
        llenar()
        viewModel.registrar()
        advanceUntilIdle()

        viewModel.cuentaCreadaMostrada()

        assertNull(viewModel.estado.value.cuentaCreada)
    }
}
