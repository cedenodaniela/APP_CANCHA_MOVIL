package com.canchapp.movil.admin.canchas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.canchapp.movil.core.red.aMensajeError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** Horas que se pueden elegir: de 00:00 a 24:00, cada 30 minutos, como las acepta el backend (RN-01). */
val HORAS_DEL_DIA: List<String> = (0..48).map { "%02d:%02d".format(it / 2, (it % 2) * 30) }

/** Días tal como los nombra la API, de lunes a domingo. */
val DIAS_API = listOf("LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO", "DOMINGO")

/** Código con el que el backend pide confirmar una acción que afecta reservas activas. */
private const val RESERVAS_ACTIVAS = "RESERVAS_ACTIVAS"

// ---------------------------------------------------------------- Horario

data class EstadoHorario(
    val cargando: Boolean = true,
    val errorCarga: MensajeError? = null,
    val dias: List<DiaDto> = emptyList(),
    val guardando: Boolean = false,
    val error: MensajeError? = null,
    val guardado: Boolean = false,
)

/** Pantalla 9 · Horario de atención del complejo por día de la semana (RF-05). */
@HiltViewModel
class HorarioViewModel @Inject constructor(
    private val horarios: HorariosRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoHorario())
    val estado: StateFlow<EstadoHorario> = _estado.asStateFlow()

    fun cargar() {
        _estado.update { it.copy(cargando = it.dias.isEmpty(), errorCarga = null) }
        viewModelScope.launch {
            horarios.horario()
                .onSuccess { h -> _estado.update { it.copy(cargando = false, dias = h.dias) } }
                .onFailure { f -> _estado.update { it.copy(cargando = false, errorCarga = f.aMensajeError()) } }
        }
    }

    /** Cambia un día. Al abrir un día cerrado se le proponen las 10:00 a 24:00 del DERCAS (RN-02b). */
    fun editarDia(dia: String, cambio: DiaDto.() -> DiaDto) = _estado.update { e ->
        e.copy(
            dias = e.dias.map { d ->
                if (d.dia != dia) d else d.cambio().let {
                    if (it.abierto && (it.apertura == null || it.cierre == null)) it.copy(apertura = "10:00", cierre = "24:00") else it
                }
            },
            error = null,
            guardado = false,
        )
    }

    fun guardar() {
        val actual = _estado.value
        if (actual.guardando || actual.cargando) return
        _estado.update { it.copy(guardando = true, error = null, guardado = false) }
        viewModelScope.launch {
            val dias = actual.dias.map { if (it.abierto) it else it.copy(apertura = null, cierre = null) }
            horarios.guardarHorario(HorarioDto(dias))
                .onSuccess { h -> _estado.update { it.copy(guardando = false, dias = h.dias, guardado = true) } }
                .onFailure { f -> _estado.update { it.copy(guardando = false, error = f.aMensajeError()) } }
        }
    }
}

// ---------------------------------------------------------------- Tarifas

/** Formulario de una tarifa. El precio se escribe como texto («22.50»). */
data class FormularioTarifa(
    val id: Long? = null,
    val dias: Set<String> = DIAS_API.toSet(),
    val inicio: String = "10:00",
    val fin: String = "18:00",
    val precio: String = "",
)

data class EstadoTarifas(
    val cargando: Boolean = true,
    val errorCarga: MensajeError? = null,
    val tarifas: List<TarifaDto> = emptyList(),
    val formulario: FormularioTarifa? = null,
    val guardando: Boolean = false,
    val error: MensajeError? = null,
    val porEliminar: TarifaDto? = null,
)

/** Pantalla 9 · Tarifas por franja (RF-05). Que dos tarifas no se crucen lo decide el backend. */
@HiltViewModel
class TarifasViewModel @Inject constructor(
    private val horarios: HorariosRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoTarifas())
    val estado: StateFlow<EstadoTarifas> = _estado.asStateFlow()

    fun cargar() {
        _estado.update { it.copy(cargando = it.tarifas.isEmpty(), errorCarga = null) }
        viewModelScope.launch {
            horarios.tarifas()
                .onSuccess { t -> _estado.update { it.copy(cargando = false, tarifas = t) } }
                .onFailure { f -> _estado.update { it.copy(cargando = false, errorCarga = f.aMensajeError()) } }
        }
    }

    fun nueva() = _estado.update { it.copy(formulario = FormularioTarifa(), error = null) }

    fun editarTarifa(t: TarifaDto) = _estado.update {
        it.copy(formulario = FormularioTarifa(t.id, t.dias.toSet(), t.inicio, t.fin, t.precioHora), error = null)
    }

    fun cerrarFormulario() = _estado.update { it.copy(formulario = null, error = null) }

    fun editar(cambio: FormularioTarifa.() -> FormularioTarifa) =
        _estado.update { e -> e.copy(formulario = e.formulario?.cambio(), error = null) }

    fun guardar() {
        val f = _estado.value.formulario ?: return
        if (_estado.value.guardando) return
        _estado.update { it.copy(guardando = true, error = null) }
        viewModelScope.launch {
            val solicitud = SolicitudTarifa(
                dias = DIAS_API.filter { it in f.dias },
                inicio = f.inicio,
                fin = f.fin,
                precioHora = f.precio.trim().ifEmpty { null },
            )
            horarios.guardarTarifa(f.id, solicitud)
                .onSuccess {
                    _estado.update { it.copy(guardando = false, formulario = null) }
                    cargar()
                }
                .onFailure { falla -> _estado.update { it.copy(guardando = false, error = falla.aMensajeError()) } }
        }
    }

    fun pedirEliminar(t: TarifaDto) = _estado.update { it.copy(porEliminar = t) }

    fun cancelarEliminar() = _estado.update { it.copy(porEliminar = null) }

    fun confirmarEliminar() {
        val t = _estado.value.porEliminar ?: return
        _estado.update { it.copy(porEliminar = null) }
        viewModelScope.launch {
            horarios.eliminarTarifa(t.id)
                .onSuccess { cargar() }
                .onFailure { f -> _estado.update { it.copy(errorCarga = null, error = f.aMensajeError()) } }
        }
    }

    companion object {
        /** Ayuda de escritura para el precio: dígitos y hasta 2 decimales. No es una regla. */
        fun soloPrecio(texto: String): String {
            val limpio = texto.replace(',', '.').filter { it.isDigit() || it == '.' }
            val partes = limpio.split('.')
            return if (partes.size == 1) partes[0].take(6) else partes[0].take(6) + "." + partes[1].take(2)
        }
    }
}

// ---------------------------------------------------------------- Bloqueos

/** Formulario de un bloqueo. [canchaId] `null` bloquea todas las canchas. */
data class FormularioBloqueo(
    val canchaId: Long? = null,
    val fecha: LocalDate? = null,
    val inicio: String = "10:00",
    val fin: String = "12:00",
    val motivo: String = "",
)

/**
 * @param confirmacion mensaje del backend que dice cuántas reservas activas hay en ese horario;
 *                     el bloqueo se guarda solo si la persona confirma
 */
data class EstadoBloqueos(
    val cargando: Boolean = true,
    val errorCarga: MensajeError? = null,
    val bloqueos: List<BloqueoDto> = emptyList(),
    val canchas: List<CanchaDto> = emptyList(),
    val formulario: FormularioBloqueo? = null,
    val guardando: Boolean = false,
    val error: MensajeError? = null,
    val confirmacion: String? = null,
    val porEliminar: BloqueoDto? = null,
) {
    fun nombreCancha(id: Long?): String? = id?.let { canchaId -> canchas.firstOrNull { it.id == canchaId }?.nombre }
}

/** Pantalla 9 · Bloqueos de horario (RF-05). */
@HiltViewModel
class BloqueosViewModel @Inject constructor(
    private val horarios: HorariosRepository,
    private val canchas: CanchasRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoBloqueos())
    val estado: StateFlow<EstadoBloqueos> = _estado.asStateFlow()

    fun cargar() {
        _estado.update { it.copy(cargando = it.bloqueos.isEmpty() && it.canchas.isEmpty(), errorCarga = null) }
        viewModelScope.launch {
            val lista = canchas.listar()
            horarios.bloqueos()
                .onSuccess { b ->
                    _estado.update { it.copy(cargando = false, bloqueos = b, canchas = lista.getOrDefault(it.canchas)) }
                }
                .onFailure { f -> _estado.update { it.copy(cargando = false, errorCarga = f.aMensajeError()) } }
        }
    }

    /** @param hoy la fecha de hoy, que se propone en el formulario */
    fun nuevo(hoy: LocalDate) = _estado.update { it.copy(formulario = FormularioBloqueo(fecha = hoy), error = null) }

    fun cerrarFormulario() = _estado.update { it.copy(formulario = null, error = null, confirmacion = null) }

    fun editar(cambio: FormularioBloqueo.() -> FormularioBloqueo) =
        _estado.update { e -> e.copy(formulario = e.formulario?.cambio(), error = null) }

    fun guardar() = enviar(confirmar = false)

    /** La persona vio cuántas reservas activas hay y quiere bloquear igual. */
    fun confirmar() {
        _estado.update { it.copy(confirmacion = null) }
        enviar(confirmar = true)
    }

    fun cancelarConfirmacion() = _estado.update { it.copy(confirmacion = null) }

    private fun enviar(confirmar: Boolean) {
        val f = _estado.value.formulario ?: return
        if (_estado.value.guardando) return
        _estado.update { it.copy(guardando = true, error = null) }
        viewModelScope.launch {
            val solicitud = SolicitudBloqueo(f.canchaId, f.fecha?.toString(), f.inicio, f.fin, f.motivo.trim(), confirmar)
            horarios.crearBloqueo(solicitud)
                .onSuccess {
                    _estado.update { it.copy(guardando = false, formulario = null) }
                    cargar()
                }
                .onFailure { falla ->
                    val mensaje = falla.aMensajeError()
                    _estado.update {
                        if (mensaje is MensajeError.DelServidor && mensaje.codigo == RESERVAS_ACTIVAS) {
                            it.copy(guardando = false, confirmacion = mensaje.texto)
                        } else {
                            it.copy(guardando = false, error = mensaje)
                        }
                    }
                }
        }
    }

    fun pedirEliminar(b: BloqueoDto) = _estado.update { it.copy(porEliminar = b) }

    fun cancelarEliminar() = _estado.update { it.copy(porEliminar = null) }

    fun confirmarEliminar() {
        val b = _estado.value.porEliminar ?: return
        _estado.update { it.copy(porEliminar = null) }
        viewModelScope.launch {
            horarios.eliminarBloqueo(b.id)
                .onSuccess { cargar() }
                .onFailure { f -> _estado.update { it.copy(error = f.aMensajeError()) } }
        }
    }
}
