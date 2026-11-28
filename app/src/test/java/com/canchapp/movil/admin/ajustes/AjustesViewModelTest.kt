package com.canchapp.movil.admin.ajustes

import com.canchapp.movil.admin.ajustes.data.ComplejoDto
import com.canchapp.movil.admin.ajustes.data.ConfiguracionDto
import com.canchapp.movil.admin.ajustes.data.ConfiguracionRepository
import com.canchapp.movil.admin.ajustes.data.DatosBancariosDto
import com.canchapp.movil.admin.ajustes.data.PoliticaDto
import com.canchapp.movil.core.red.MensajeError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
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
import java.io.IOException

/** RF-03 · Ajustes del complejo. */
@OptIn(ExperimentalCoroutinesApi::class)
class AjustesViewModelTest {

    private val despachador = StandardTestDispatcher()
    private val repositorio = mockk<ConfiguracionRepository>()

    private val guardada = ConfiguracionDto(
        complejo = ComplejoDto("Complejo Deportivo Calceta", "Calceta, Manabí", "0969908865"),
        datosBancarios = DatosBancariosDto("Banco Pichincha", "AHORROS", "2201234567", "Complejo Deportivo Calceta"),
        politica = PoliticaDto(15, 3, 1, 3, true),
    )

    @Before
    fun preparar() = Dispatchers.setMain(despachador)

    @After
    fun limpiar() = Dispatchers.resetMain()

    private fun crear(): AjustesViewModel = AjustesViewModel(repositorio)

    @Test
    fun `dado la configuracion del backend, cuando abre la pantalla, entonces la muestra para editar`() = runTest(despachador) {
        coEvery { repositorio.consultar() } returns Result.success(guardada)

        val viewModel = crear()
        assertTrue(viewModel.estado.value.cargando)
        advanceUntilIdle()

        val f = viewModel.estado.value.formulario
        assertFalse(viewModel.estado.value.cargando)
        assertEquals("Complejo Deportivo Calceta", f.nombre)
        assertEquals(TipoCuenta.AHORROS, f.tipoCuenta)
        assertEquals("15", f.minutosExpiracion)
        assertEquals("3", f.diasAnticipacion)
        assertTrue(f.limitarTrasInasistencia)
    }

    @Test
    fun `dado que no hay conexion, cuando abre la pantalla, entonces ofrece reintentar`() = runTest(despachador) {
        coEvery { repositorio.consultar() } returns Result.failure(IOException()) andThen Result.success(guardada)

        val viewModel = crear()
        advanceUntilIdle()
        assertEquals(MensajeError.SinConexion, viewModel.estado.value.errorCarga)

        viewModel.cargar()
        advanceUntilIdle()
        assertNull(viewModel.estado.value.errorCarga)
        assertEquals("Banco Pichincha", viewModel.estado.value.formulario.banco)
    }

    @Test
    fun `dado cambios validos, cuando guarda, entonces envia la configuracion completa y lo confirma`() = runTest(despachador) {
        coEvery { repositorio.consultar() } returns Result.success(guardada)
        val enviada = slot<ConfiguracionDto>()
        coEvery { repositorio.guardar(capture(enviada)) } answers { Result.success(enviada.captured) }
        val viewModel = crear()
        advanceUntilIdle()

        viewModel.editar { copy(diasAnticipacion = "5", tipoCuenta = TipoCuenta.CORRIENTE, banco = " Banco Guayaquil ") }
        viewModel.guardar()
        assertTrue(viewModel.estado.value.guardando)
        advanceUntilIdle()

        assertEquals(5, enviada.captured.politica.diasAnticipacion)
        assertEquals("CORRIENTE", enviada.captured.datosBancarios.tipoCuenta)
        assertEquals("Banco Guayaquil", enviada.captured.datosBancarios.banco)
        assertTrue(viewModel.estado.value.guardado)
        assertFalse(viewModel.estado.value.guardando)
    }

    @Test
    fun `dado un valor que el backend rechaza, cuando guarda, entonces muestra su mensaje`() = runTest(despachador) {
        coEvery { repositorio.consultar() } returns Result.success(guardada)
        val rechazo = HttpException(
            Response.error<Any>(
                400,
                """{"codigo":"CONFIGURACION_INVALIDA","mensaje":"Los días de anticipación deben ser al menos 1."}"""
                    .toResponseBody("application/json".toMediaType()),
            ),
        )
        coEvery { repositorio.guardar(any()) } returns Result.failure(rechazo)
        val viewModel = crear()
        advanceUntilIdle()

        viewModel.editar { copy(diasAnticipacion = "0") }
        viewModel.guardar()
        advanceUntilIdle()

        assertEquals(
            MensajeError.DelServidor("CONFIGURACION_INVALIDA", "Los días de anticipación deben ser al menos 1."),
            viewModel.estado.value.error,
        )
        assertFalse(viewModel.estado.value.guardado)
    }

    @Test
    fun `dado un numero vacio, cuando guarda, entonces lo envia vacio para que el backend explique`() = runTest(despachador) {
        coEvery { repositorio.consultar() } returns Result.success(guardada)
        val enviada = slot<ConfiguracionDto>()
        coEvery { repositorio.guardar(capture(enviada)) } returns Result.success(guardada)
        val viewModel = crear()
        advanceUntilIdle()

        viewModel.editar { copy(minutosExpiracion = "") }
        viewModel.guardar()
        advanceUntilIdle()

        assertNull(enviada.captured.politica.minutosExpiracion)
    }

    @Test
    fun `dado un aviso de guardado, cuando edita otra vez, entonces el aviso desaparece`() = runTest(despachador) {
        coEvery { repositorio.consultar() } returns Result.success(guardada)
        coEvery { repositorio.guardar(any()) } returns Result.success(guardada)
        val viewModel = crear()
        advanceUntilIdle()
        viewModel.guardar()
        advanceUntilIdle()

        viewModel.editar { copy(telefono = "0999999999") }

        assertFalse(viewModel.estado.value.guardado)
        coVerify(exactly = 1) { repositorio.guardar(any()) }
    }

    @Test
    fun `dado texto en un campo numerico, cuando se escribe, entonces solo quedan digitos`() {
        assertEquals("15", AjustesViewModel.soloDigitos("1a5-"))
        assertEquals("1234", AjustesViewModel.soloDigitos("123456"))
    }
}
