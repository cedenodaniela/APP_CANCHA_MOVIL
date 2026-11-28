package com.canchapp.movil.admin.canchas

import com.canchapp.movil.admin.canchas.data.BloqueoDto
import com.canchapp.movil.admin.canchas.data.CanchaDto
import com.canchapp.movil.admin.canchas.data.CanchasRepository
import com.canchapp.movil.admin.canchas.data.DiaDto
import com.canchapp.movil.admin.canchas.data.HorarioDto
import com.canchapp.movil.admin.canchas.data.HorariosRepository
import com.canchapp.movil.admin.canchas.data.SolicitudBloqueo
import com.canchapp.movil.admin.canchas.data.SolicitudTarifa
import com.canchapp.movil.admin.canchas.data.TarifaDto
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.time.LocalDate

/** RF-05 · Horario, tarifas y bloqueos en la pantalla 9. */
@OptIn(ExperimentalCoroutinesApi::class)
class HorariosViewModelsTest {

    private val despachador = StandardTestDispatcher()
    private val horarios = mockk<HorariosRepository>()
    private val canchas = mockk<CanchasRepository>()

    @Before
    fun preparar() = Dispatchers.setMain(despachador)

    @After
    fun limpiar() = Dispatchers.resetMain()

    private fun rechazo(codigo: Int, cuerpo: String) =
        HttpException(Response.error<Any>(codigo, cuerpo.toResponseBody("application/json".toMediaType())))

    private val semana = DIAS_API.map { DiaDto(it, true, "10:00", "24:00") }

    @Test
    fun `dado un dia cerrado que se abre, cuando se edita, entonces se le proponen las horas del DERCAS y se guarda la semana`() =
        runTest(despachador) {
            coEvery { horarios.horario() } returns Result.success(HorarioDto(semana.map { if (it.dia == "DOMINGO") DiaDto("DOMINGO", false) else it }))
            val enviado = slot<HorarioDto>()
            coEvery { horarios.guardarHorario(capture(enviado)) } answers { Result.success(enviado.captured) }
            val vm = HorarioViewModel(horarios)
            vm.cargar()
            advanceUntilIdle()

            vm.editarDia("DOMINGO") { copy(abierto = true) }
            vm.editarDia("LUNES") { copy(abierto = false) }
            vm.guardar()
            advanceUntilIdle()

            val domingo = enviado.captured.dias.first { it.dia == "DOMINGO" }
            val lunes = enviado.captured.dias.first { it.dia == "LUNES" }
            assertEquals(DiaDto("DOMINGO", true, "10:00", "24:00"), domingo)
            assertEquals(DiaDto("LUNES", false, null, null), lunes)
            assertTrue(vm.estado.value.guardado)
        }

    @Test
    fun `dado una tarifa que se cruza, cuando se guarda, entonces el formulario sigue abierto con el mensaje del backend`() =
        runTest(despachador) {
            coEvery { horarios.tarifas() } returns Result.success(emptyList())
            coEvery { horarios.guardarTarifa(null, any()) } returns Result.failure(
                rechazo(409, """{"codigo":"TARIFA_SUPERPUESTA","mensaje":"Se cruza con la tarifa de 10:00 a 18:00."}"""),
            )
            val vm = TarifasViewModel(horarios)

            vm.nueva()
            vm.editar { copy(dias = setOf("VIERNES", "LUNES"), inicio = "17:00", fin = "20:00", precio = "22.5") }
            vm.guardar()
            advanceUntilIdle()

            assertEquals(
                MensajeError.DelServidor("TARIFA_SUPERPUESTA", "Se cruza con la tarifa de 10:00 a 18:00."),
                vm.estado.value.error,
            )
            assertTrue(vm.estado.value.formulario != null)
            coVerify { horarios.guardarTarifa(null, SolicitudTarifa(listOf("LUNES", "VIERNES"), "17:00", "20:00", "22.5")) }
        }

    @Test
    fun `dado una tarifa valida, cuando se guarda, entonces se cierra el formulario y se recarga la lista`() = runTest(despachador) {
        val guardada = TarifaDto(3, DIAS_API, "10:00", "18:00", "21.00")
        coEvery { horarios.tarifas() } returns Result.success(emptyList()) andThen Result.success(listOf(guardada))
        coEvery { horarios.guardarTarifa(3, any()) } returns Result.success(guardada)
        val vm = TarifasViewModel(horarios)
        vm.cargar()
        advanceUntilIdle()

        vm.editarTarifa(guardada.copy(precioHora = "20.00"))
        vm.editar { copy(precio = "21.00") }
        vm.guardar()
        advanceUntilIdle()

        assertNull(vm.estado.value.formulario)
        assertEquals(listOf(guardada), vm.estado.value.tarifas)
    }

    @Test
    fun `dado un precio escrito con coma o con muchos decimales, cuando se escribe, entonces queda con punto y 2 decimales`() {
        assertEquals("22.50", TarifasViewModel.soloPrecio("22,505"))
        assertEquals("20", TarifasViewModel.soloPrecio("20$"))
    }

    @Test
    fun `dado un horario con reservas activas, cuando bloquea, entonces pide confirmar y luego envia confirmar en true`() =
        runTest(despachador) {
            coEvery { canchas.listar() } returns Result.success(listOf(CanchaDto(1, "Cancha 1", "Sintético", 30, 50, true)))
            coEvery { horarios.bloqueos() } returns Result.success(emptyList())
            coEvery { horarios.crearBloqueo(match { !it.confirmar }) } returns Result.failure(
                rechazo(409, """{"codigo":"RESERVAS_ACTIVAS","mensaje":"Ese horario tiene 2 reservas activas."}"""),
            )
            coEvery { horarios.crearBloqueo(match { it.confirmar }) } returns
                Result.success(BloqueoDto(5, 1, "2026-12-01", "10:00", "12:00", "Mantenimiento"))
            val vm = BloqueosViewModel(horarios, canchas)
            vm.cargar()
            advanceUntilIdle()

            vm.nuevo(LocalDate.of(2026, 12, 1))
            vm.editar { copy(canchaId = 1, motivo = " Mantenimiento ") }
            vm.guardar()
            advanceUntilIdle()
            assertEquals("Ese horario tiene 2 reservas activas.", vm.estado.value.confirmacion)
            assertEquals("Cancha 1", vm.estado.value.nombreCancha(1))

            vm.confirmar()
            advanceUntilIdle()

            assertNull(vm.estado.value.confirmacion)
            assertNull(vm.estado.value.formulario)
            coVerify {
                horarios.crearBloqueo(SolicitudBloqueo(1, "2026-12-01", "10:00", "12:00", "Mantenimiento", confirmar = true))
            }
        }

    @Test
    fun `dado un bloqueo por quitar, cuando se cancela, entonces no se borra`() = runTest(despachador) {
        val bloqueo = BloqueoDto(5, null, "2026-12-01", "10:00", "12:00", "Feriado")
        coEvery { canchas.listar() } returns Result.success(emptyList())
        coEvery { horarios.bloqueos() } returns Result.success(listOf(bloqueo))
        val vm = BloqueosViewModel(horarios, canchas)
        vm.cargar()
        advanceUntilIdle()

        vm.pedirEliminar(bloqueo)
        vm.cancelarEliminar()
        advanceUntilIdle()

        assertNull(vm.estado.value.porEliminar)
        coVerify(exactly = 0) { horarios.eliminarBloqueo(any()) }
    }

    @Test
    fun `dadas las horas que se ofrecen, cuando se listan, entonces van de 00 00 a 24 00 cada media hora`() {
        assertEquals(49, HORAS_DEL_DIA.size)
        assertEquals("17:30", HORAS_DEL_DIA[35])
        assertEquals("24:00", HORAS_DEL_DIA.last())
    }
}
