package com.canchapp.movil.admin.canchas

import android.net.Uri
import com.canchapp.movil.admin.canchas.data.CanchaDto
import com.canchapp.movil.admin.canchas.data.CanchasRepository
import com.canchapp.movil.admin.canchas.data.PreparadorFoto
import com.canchapp.movil.admin.canchas.data.SolicitudCancha
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/** RF-04 · Alta y edición de una cancha. */
@OptIn(ExperimentalCoroutinesApi::class)
class CanchaFormularioViewModelTest {

    private val despachador = StandardTestDispatcher()
    private val repositorio = mockk<CanchasRepository>()
    private val preparador = mockk<PreparadorFoto>()
    private val foto = mockk<Uri>()
    private val jpg = byteArrayOf(1, 2, 3)

    @Before
    fun preparar() {
        Dispatchers.setMain(despachador)
        every { repositorio.urlFoto(any()) } answers { firstArg<CanchaDto>().fotoVersion?.let { "foto-$it" } }
    }

    @After
    fun limpiar() = Dispatchers.resetMain()

    private fun crear() = CanchaFormularioViewModel(repositorio, preparador)

    private fun CanchaFormularioViewModel.llenar() {
        alCambiarNombre(" Cancha 3 ")
        alCambiarAncho("30")
        alCambiarLargo("5a0")
    }

    @Test
    fun `dado una cancha nueva sin foto, cuando guarda, entonces la crea con los datos limpios`() = runTest(despachador) {
        coEvery { repositorio.crear(any()) } returns Result.success(CanchaDto(3, "Cancha 3", "Sintético", 30, 50, true))
        val viewModel = crear()
        viewModel.iniciar(null)
        viewModel.llenar()

        viewModel.guardar()
        advanceUntilIdle()

        coVerify { repositorio.crear(SolicitudCancha("Cancha 3", "Sintético", 30, 50)) }
        assertTrue(viewModel.estado.value.guardada)
        coVerify(exactly = 0) { repositorio.subirFoto(any(), any()) }
    }

    @Test
    fun `dado una foto elegida, cuando guarda una cancha nueva, entonces la crea y luego sube la foto`() = runTest(despachador) {
        coEvery { repositorio.crear(any()) } returns Result.success(CanchaDto(3, "Cancha 3", "Sintético", 30, 50, true))
        coEvery { preparador.preparar(foto) } returns jpg
        coEvery { repositorio.subirFoto(3, jpg) } returns Result.success(CanchaDto(3, "Cancha 3", "Sintético", 30, 50, true, 7))
        val viewModel = crear()
        viewModel.iniciar(null)
        viewModel.llenar()
        viewModel.elegirFoto(foto)

        viewModel.guardar()
        advanceUntilIdle()

        val estado = viewModel.estado.value
        assertTrue(estado.guardada)
        assertEquals("foto-7", estado.fotoActual)
        assertNull(estado.fotoNueva)
    }

    @Test
    fun `dado que la foto falla despues de crear, cuando reintenta, entonces edita la misma cancha sin crear otra`() = runTest(despachador) {
        val creada = CanchaDto(3, "Cancha 3", "Sintético", 30, 50, true)
        coEvery { repositorio.crear(any()) } returns Result.success(creada)
        coEvery { repositorio.editar(3, any()) } returns Result.success(creada)
        coEvery { preparador.preparar(foto) } returns jpg
        coEvery { repositorio.subirFoto(3, jpg) } returns Result.failure(IOException()) andThen Result.success(creada.copy(fotoVersion = 1))
        val viewModel = crear()
        viewModel.iniciar(null)
        viewModel.llenar()
        viewModel.elegirFoto(foto)

        viewModel.guardar()
        advanceUntilIdle()
        assertEquals(MensajeError.SinConexion, viewModel.estado.value.error)
        assertEquals(3L, viewModel.estado.value.id)

        viewModel.guardar()
        advanceUntilIdle()

        coVerify(exactly = 1) { repositorio.crear(any()) }
        coVerify(exactly = 1) { repositorio.editar(3, any()) }
        assertTrue(viewModel.estado.value.guardada)
    }

    @Test
    fun `dado un nombre repetido, cuando guarda, entonces muestra el mensaje del backend`() = runTest(despachador) {
        val repetido = HttpException(
            Response.error<Any>(
                409,
                """{"codigo":"CANCHA_REPETIDA","mensaje":"Ya existe una cancha con ese nombre."}"""
                    .toResponseBody("application/json".toMediaType()),
            ),
        )
        coEvery { repositorio.crear(any()) } returns Result.failure(repetido)
        val viewModel = crear()
        viewModel.iniciar(null)
        viewModel.llenar()

        viewModel.guardar()
        advanceUntilIdle()

        assertEquals(MensajeError.DelServidor("CANCHA_REPETIDA", "Ya existe una cancha con ese nombre."), viewModel.estado.value.error)
        assertFalse(viewModel.estado.value.guardada)
    }

    @Test
    fun `dado una foto que no se puede leer, cuando guarda, entonces avisa y no la sube`() = runTest(despachador) {
        coEvery { repositorio.crear(any()) } returns Result.success(CanchaDto(3, "Cancha 3", "Sintético", 30, 50, true))
        coEvery { preparador.preparar(foto) } returns null
        val viewModel = crear()
        viewModel.iniciar(null)
        viewModel.llenar()
        viewModel.elegirFoto(foto)

        viewModel.guardar()
        advanceUntilIdle()

        assertTrue(viewModel.estado.value.fotoIlegible)
        assertFalse(viewModel.estado.value.guardada)
        coVerify(exactly = 0) { repositorio.subirFoto(any(), any()) }
    }

    @Test
    fun `dado una cancha existente, cuando abre el formulario, entonces carga sus datos una sola vez`() = runTest(despachador) {
        coEvery { repositorio.obtener(1) } returns Result.success(CanchaDto(1, "Cancha 1", "Sintético", 30, 50, true, 5))
        val viewModel = crear()

        viewModel.iniciar(1)
        viewModel.iniciar(1)
        advanceUntilIdle()

        val estado = viewModel.estado.value
        assertFalse(estado.esNueva)
        assertEquals("Cancha 1", estado.nombre)
        assertEquals("50", estado.largo)
        assertEquals("foto-5", estado.fotoActual)
        coVerify(exactly = 1) { repositorio.obtener(1) }
    }
}
