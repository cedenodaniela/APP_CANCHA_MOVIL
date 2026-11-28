package com.canchapp.movil.admin.canchas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canchapp.movil.admin.canchas.data.CanchaDto
import com.canchapp.movil.admin.canchas.data.CanchasRepository
import com.canchapp.movil.core.red.MensajeError
import com.canchapp.movil.core.red.aMensajeError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Una cancha de la lista, con la dirección de su foto si tiene. */
data class CanchaItem(val cancha: CanchaDto, val fotoUrl: String?)

/**
 * El backend avisó que la cancha tiene reservas activas: se le muestra su mensaje (dice cuántas)
 * y se espera que confirme antes de desactivarla (RF-04).
 */
data class ConfirmacionDesactivar(val id: Long, val nombre: String, val mensaje: String)

data class EstadoCanchas(
    val cargando: Boolean = true,
    val errorCarga: MensajeError? = null,
    val canchas: List<CanchaItem> = emptyList(),
    val cambiando: Set<Long> = emptySet(),
    val error: MensajeError? = null,
    val confirmacion: ConfirmacionDesactivar? = null,
)

/**
 * Pantalla 9 · Canchas del administrador (RF-04): lista con su estado y el interruptor para
 * activarlas o desactivarlas.
 */
@HiltViewModel
class CanchasViewModel @Inject constructor(
    private val canchas: CanchasRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoCanchas())
    val estado: StateFlow<EstadoCanchas> = _estado.asStateFlow()

    /** Se llama cada vez que la pantalla aparece, para ver los cambios hechos en el formulario. */
    fun cargar() {
        _estado.update { it.copy(cargando = it.canchas.isEmpty(), errorCarga = null) }
        viewModelScope.launch {
            canchas.listar()
                .onSuccess { lista -> _estado.update { it.copy(cargando = false, canchas = lista.map(::aItem)) } }
                .onFailure { falla ->
                    _estado.update {
                        if (it.canchas.isEmpty()) it.copy(cargando = false, errorCarga = falla.aMensajeError())
                        else it.copy(cargando = false, error = falla.aMensajeError())
                    }
                }
        }
    }

    fun cambiarEstado(id: Long, activa: Boolean) = enviarEstado(id, activa, confirmar = false)

    /** La persona vio cuántas reservas activas hay y quiere desactivarla igual. */
    fun confirmarDesactivar() {
        val pendiente = _estado.value.confirmacion ?: return
        _estado.update { it.copy(confirmacion = null) }
        enviarEstado(pendiente.id, activa = false, confirmar = true)
    }

    fun cancelarConfirmacion() = _estado.update { it.copy(confirmacion = null) }

    fun errorMostrado() = _estado.update { it.copy(error = null) }

    private fun enviarEstado(id: Long, activa: Boolean, confirmar: Boolean) {
        if (id in _estado.value.cambiando) return
        _estado.update { it.copy(cambiando = it.cambiando + id, error = null) }
        viewModelScope.launch {
            canchas.cambiarEstado(id, activa, confirmar)
                .onSuccess { cambiada ->
                    _estado.update { e ->
                        e.copy(
                            cambiando = e.cambiando - id,
                            canchas = e.canchas.map { if (it.cancha.id == id) aItem(cambiada) else it },
                        )
                    }
                }
                .onFailure { falla ->
                    val mensaje = falla.aMensajeError()
                    _estado.update { e ->
                        val nombre = e.canchas.firstOrNull { it.cancha.id == id }?.cancha?.nombre.orEmpty()
                        if (mensaje is MensajeError.DelServidor && mensaje.codigo == RESERVAS_ACTIVAS) {
                            e.copy(cambiando = e.cambiando - id, confirmacion = ConfirmacionDesactivar(id, nombre, mensaje.texto))
                        } else {
                            e.copy(cambiando = e.cambiando - id, error = mensaje)
                        }
                    }
                }
        }
    }

    private fun aItem(cancha: CanchaDto) = CanchaItem(cancha, canchas.urlFoto(cancha))

    private companion object {
        /** Código con el que el backend pide confirmar la desactivación. */
        const val RESERVAS_ACTIVAS = "RESERVAS_ACTIVAS"
    }
}
