package com.canchapp.movil.cliente.reservar

import com.canchapp.movil.admin.canchas.data.CanchaDto
import com.canchapp.movil.admin.canchas.data.CanchasRepository
import com.canchapp.movil.cliente.reservar.data.BloqueDisponibleDto
import com.canchapp.movil.cliente.reservar.data.DisponibilidadRepository
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.time.LocalDate

/** RF-06 · Disponibilidad del cliente: el ViewModel consulta y muestra lo que manda el backend. */
@OptIn(ExperimentalCoroutinesApi::class)
class DisponibilidadViewModelTest {

    private val despachador = StandardTestDispatcher()
    private val repositorioDisponibilidad = mockk<DisponibilidadRepository>()
    private val repositorioCanchas = mockk<CanchasRepository>()

    @Before
    fun preparar() = Dispatchers.setMain(despachador)

    @After
    fun limpiar() = Dispatchers.resetMain()

    private fun crear(): DisponibilidadViewModel = DisponibilidadViewModel(repositorioDisponibilidad, repositorioCanchas)

    @Test
    fun `dado la cancha activa, cuando entra la pantalla, entonces consulta su disponibilidad para el día de hoy`() =
        runTest(despachador) {
            coEvery { repositorioCanchas.listar() } returns Result.success(listOf(CanchaDto(1, "Cancha 1", "Sintético", 30, 50, activa = true)))
            coEvery { repositorioDisponibilidad.disponibilidad(any(), any()) } returns Result.success(
                listOf(BloqueDisponibleDto(1, "2026-11-28", "10:00", "10:30", false, "20.00")),
            )

            val viewModel = crear()
            assertTrue(viewModel.estado.value.cargandoCanchas)
            advanceUntilIdle()

            assertFalse(viewModel.estado.value.cargandoCanchas)
            assertEquals(1L, viewModel.estado.value.canchaId)
            assertNotNull(viewModel.estado.value.fecha)
            assertFalse(viewModel.estado.value.cargando)
            assertEquals(1, viewModel.estado.value.bloques.size)
        }

    @Test
    fun `dado dos canchas activas, cuando se cambia la cancha, entonces consulta la disponibilidad de esa`() =
        runTest(despachador) {
            coEvery { repositorioCanchas.listar() } returns Result.success(
                listOf(CanchaDto(1, "Cancha 1", "Sintético", 30, 50, activa = true), CanchaDto(2, "Cancha 2", "Sintético", 30, 50, activa = true)),
            )
            coEvery { repositorioDisponibilidad.disponibilidad(any(), any()) } returns Result.success(emptyList())

            val viewModel = crear()
            advanceUntilIdle()
            assertEquals(1L, viewModel.estado.value.canchaId)

            viewModel.cambiarCancha(2L)
            advanceUntilIdle()

            coVerify { repositorioDisponibilidad.disponibilidad(2L, any()) }
            assertEquals(2L, viewModel.estado.value.canchaId)
        }

    @Test
    fun `dado un error del backend, cuando se cambia la fecha, entonces muestra su mensaje`() =
        runTest(despachador) {
            coEvery { repositorioCanchas.listar() } returns Result.success(listOf(CanchaDto(1, "Cancha 1", "Sintético", 30, 50, activa = true)))
            val falla = HttpException(
                Response.error<Any>(
                    400,
                    """{"codigo":"RESERVA_INVALIDA","mensaje":"Solo se puede consultar disponibilidad de hoy hasta 3 días después."}"""
                        .toResponseBody("application/json".toMediaType()),
                ),
            )
            coEvery { repositorioDisponibilidad.disponibilidad(any(), any()) } returns Result.failure(falla)

            val viewModel = crear()
            advanceUntilIdle()
            assertNull(viewModel.estado.value.error)

            viewModel.cambiarFecha(LocalDate.now().plusDays(1))
            advanceUntilIdle()

            assertEquals(
                MensajeError.DelServidor("RESERVA_INVALIDA", "Solo se puede consultar disponibilidad de hoy hasta 3 días después."),
                viewModel.estado.value.error,
            )
        }

    @Test
    fun `dado un bloque del backend, cuando se selecciona, entonces se muestra como seleccionado`() =
        runTest(despachador) {
            coEvery { repositorioCanchas.listar() } returns Result.success(listOf(CanchaDto(1, "Cancha 1", "Sintético", 30, 50, activa = true)))
            coEvery { repositorioDisponibilidad.disponibilidad(any(), any()) } returns Result.success(
                listOf(BloqueDisponibleDto(1, "2026-11-28", "10:00", "10:30", false, "20.00")),
            )

            val viewModel = crear()
            advanceUntilIdle()
            val bloque = viewModel.estado.value.bloques.first()

            viewModel.seleccionarBloque(bloque)
            assertEquals(bloque, viewModel.estado.value.bloqueSeleccionado)

            viewModel.seleccionarBloque(null)
            assertNull(viewModel.estado.value.bloqueSeleccionado)
        }
}
