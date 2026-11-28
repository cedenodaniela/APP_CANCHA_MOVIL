package com.canchapp.movil.admin.canchas

import com.canchapp.movil.admin.canchas.data.CanchaDto
import com.canchapp.movil.admin.canchas.data.CanchasRepository
import com.canchapp.movil.core.red.MensajeError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
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
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/** RF-04 · Lista de canchas del administrador. */
@OptIn(ExperimentalCoroutinesApi::class)
class CanchasViewModelTest {

    private val despachador = StandardTestDispatcher()
    private val repositorio = mockk<CanchasRepository>()
    private val cancha1 = CanchaDto(1, "Cancha 1", "Sintético", 30, 50, activa = true, fotoVersion = 99)
    private val cancha2 = CanchaDto(2, "Cancha 2", "Sintético", 30, 50, activa = false)

    @Before
    fun preparar() {
        Dispatchers.setMain(despachador)
        every { repositorio.urlFoto(any()) } answers { firstArg<CanchaDto>().fotoVersion?.let { "foto-$it" } }
    }

    @After
    fun limpiar() = Dispatchers.resetMain()

    private fun conflicto() = HttpException(
        Response.error<Any>(
            409,
            """{"codigo":"RESERVAS_ACTIVAS","mensaje":"La cancha tiene 2 reservas activas. Confirma si quieres desactivarla de todos modos."}"""
                .toResponseBody("application/json".toMediaType()),
        ),
    )

    @Test
    fun `dado las canchas del backend, cuando carga, entonces las muestra con su foto`() = runTest(despachador) {
        coEvery { repositorio.listar() } returns Result.success(listOf(cancha1, cancha2))
        val viewModel = CanchasViewModel(repositorio)

        viewModel.cargar()
        advanceUntilIdle()

        val estado = viewModel.estado.value
        assertFalse(estado.cargando)
        assertEquals(2, estado.canchas.size)
        assertEquals("foto-99", estado.canchas[0].fotoUrl)
        assertNull(estado.canchas[1].fotoUrl)
    }

    @Test
    fun `dado que no hay conexion, cuando carga por primera vez, entonces ofrece reintentar`() = runTest(despachador) {
        coEvery { repositorio.listar() } returns Result.failure(IOException())
        val viewModel = CanchasViewModel(repositorio)

        viewModel.cargar()
        advanceUntilIdle()

        assertEquals(MensajeError.SinConexion, viewModel.estado.value.errorCarga)
    }

    @Test
    fun `dado una cancha con reservas activas, cuando la desactiva, entonces pide confirmar con el mensaje del backend`() = runTest(despachador) {
        coEvery { repositorio.listar() } returns Result.success(listOf(cancha1))
        coEvery { repositorio.cambiarEstado(1, false, false) } returns Result.failure(conflicto())
        coEvery { repositorio.cambiarEstado(1, false, true) } returns Result.success(cancha1.copy(activa = false))
        val viewModel = CanchasViewModel(repositorio)
        viewModel.cargar()
        advanceUntilIdle()

        viewModel.cambiarEstado(1, false)
        advanceUntilIdle()

        val pendiente = viewModel.estado.value.confirmacion
        assertEquals("Cancha 1", pendiente?.nombre)
        assertEquals("La cancha tiene 2 reservas activas. Confirma si quieres desactivarla de todos modos.", pendiente?.mensaje)
        assertEquals(true, viewModel.estado.value.canchas[0].cancha.activa)

        viewModel.confirmarDesactivar()
        advanceUntilIdle()

        assertNull(viewModel.estado.value.confirmacion)
        assertEquals(false, viewModel.estado.value.canchas[0].cancha.activa)
        coVerify(exactly = 1) { repositorio.cambiarEstado(1, false, true) }
    }

    @Test
    fun `dado la confirmacion pendiente, cuando cancela, entonces no cambia nada`() = runTest(despachador) {
        coEvery { repositorio.listar() } returns Result.success(listOf(cancha1))
        coEvery { repositorio.cambiarEstado(1, false, false) } returns Result.failure(conflicto())
        val viewModel = CanchasViewModel(repositorio)
        viewModel.cargar()
        advanceUntilIdle()
        viewModel.cambiarEstado(1, false)
        advanceUntilIdle()

        viewModel.cancelarConfirmacion()

        assertNull(viewModel.estado.value.confirmacion)
        coVerify(exactly = 0) { repositorio.cambiarEstado(1, false, true) }
    }

    @Test
    fun `dado una cancha inactiva, cuando la activa, entonces se actualiza en la lista`() = runTest(despachador) {
        coEvery { repositorio.listar() } returns Result.success(listOf(cancha2))
        coEvery { repositorio.cambiarEstado(2, true, false) } returns Result.success(cancha2.copy(activa = true))
        val viewModel = CanchasViewModel(repositorio)
        viewModel.cargar()
        advanceUntilIdle()

        viewModel.cambiarEstado(2, true)
        advanceUntilIdle()

        assertEquals(true, viewModel.estado.value.canchas[0].cancha.activa)
        assertEquals(emptySet<Long>(), viewModel.estado.value.cambiando)
    }
}
