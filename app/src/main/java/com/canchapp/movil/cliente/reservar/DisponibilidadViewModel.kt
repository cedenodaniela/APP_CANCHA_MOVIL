package com.canchapp.movil.cliente.reservar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canchapp.movil.admin.canchas.data.CanchaDto
import com.canchapp.movil.admin.canchas.data.CanchasRepository
import com.canchapp.movil.cliente.reservar.data.BloqueDisponibleDto
import com.canchapp.movil.cliente.reservar.data.DisponibilidadRepository
import com.canchapp.movil.core.red.MensajeError
import com.canchapp.movil.core.red.aMensajeError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/** Zona del complejo para saber «hoy»: la del backend, no la del celular (AGENTS.md §6). */
val ZONA_COMPLEJO: ZoneId = ZoneId.of("America/Guayaquil")

/** Estado de la pantalla de disponibilidad del cliente (RF-06). */
data class EstadoDisponibilidad(
    val cargandoCanchas: Boolean = true,
    val errorCanchas: MensajeError? = null,
    val canchas: List<CanchaDto> = emptyList(),
    val cargando: Boolean = false,
    val error: MensajeError? = null,
    val fecha: LocalDate? = null,
    val canchaId: Long? = null,
    val bloques: List<BloqueDisponibleDto> = emptyList(),
    val bloqueSeleccionado: BloqueDisponibleDto? = null,
)

/**
 * Pantalla 2 · Disponibilidad y reserva del cliente (RF-06, RF-07).
 *
 * Consulta los bloques de 30 minutos que manda el backend para una fecha y una cancha, y muestra
 * su estado (libre u ocupado) y su precio. La UI no calcula nada: los datos salen del backend.
 */
@HiltViewModel
class DisponibilidadViewModel @Inject constructor(
    private val disponibilidad: DisponibilidadRepository,
    private val canchas: CanchasRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoDisponibilidad())
    val estado: StateFlow<EstadoDisponibilidad> = _estado.asStateFlow()

    init {
        cargarCanchas()
    }

    /** Carga las canchas para el selector y, con la primera activa, la disponibilidad del día. */
    fun cargarCanchas() {
        _estado.update { it.copy(cargandoCanchas = it.canchas.isEmpty(), errorCanchas = null) }
        viewModelScope.launch {
            canchas.listar()
                .onSuccess { lista ->
                    val activas = lista.filter { it.activa }
                    _estado.update { e -> e.copy(cargandoCanchas = false, canchas = activas, errorCanchas = null) }
                    val seleccion = _estado.value.canchaId ?: activas.firstOrNull()?.id
                    _estado.update { it.copy(canchaId = seleccion) }
                    cargarDisponibilidad(LocalDate.now(ZONA_COMPLEJO), seleccion)
                }
                .onFailure { falla -> _estado.update { it.copy(cargandoCanchas = false, errorCanchas = falla.aMensajeError()) } }
        }
    }

    /** Consulta los bloques del día para la cancha elegida (o de las activas, si es `null`). */
    fun cargarDisponibilidad(fecha: LocalDate, canchaId: Long?) {
        _estado.update {
            it.copy(cargando = true, error = null, fecha = fecha, bloques = emptyList(), bloqueSeleccionado = null)
        }
        viewModelScope.launch {
            disponibilidad.disponibilidad(canchaId, fecha)
                .onSuccess { bloques -> _estado.update { it.copy(cargando = false, bloques = bloques) } }
                .onFailure { falla -> _estado.update { it.copy(cargando = false, error = falla.aMensajeError()) } }
        }
    }

    /** Reintenta la última consulta de disponibilidad (p. ej. después de un error del backend). */
    fun reintentarDisponibilidad() = cargarDisponibilidad(_estado.value.fecha ?: LocalDate.now(ZONA_COMPLEJO), _estado.value.canchaId)

    /** Elige la cancha del selector; si cambia, se consulta su disponibilidad. */
    fun cambiarCancha(id: Long?) {
        val actual = _estado.value
        if (actual.canchaId == id) return
        _estado.update { it.copy(canchaId = id, bloques = emptyList(), bloqueSeleccionado = null, error = null) }
        cargarDisponibilidad(actual.fecha ?: LocalDate.now(ZONA_COMPLEJO), id)
    }

    /** Elige la fecha; si cambia, se consulta la disponibilidad de ese día. */
    fun cambiarFecha(fecha: LocalDate) {
        val actual = _estado.value
        if (actual.fecha == fecha) return
        _estado.update { it.copy(fecha = fecha, bloques = emptyList(), bloqueSeleccionado = null, error = null) }
        cargarDisponibilidad(fecha, actual.canchaId)
    }

    /** Marca un bloque como seleccionado para la reserva; `null` lo desmarca. */
    fun seleccionarBloque(bloque: BloqueDisponibleDto?) = _estado.update { it.copy(bloqueSeleccionado = bloque) }
}
