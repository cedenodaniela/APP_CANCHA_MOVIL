package com.canchapp.movil.admin.canchas

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canchapp.movil.admin.canchas.data.CanchaDto
import com.canchapp.movil.admin.canchas.data.CanchasRepository
import com.canchapp.movil.admin.canchas.data.PreparadorFoto
import com.canchapp.movil.admin.canchas.data.SolicitudCancha
import com.canchapp.movil.core.red.MensajeError
import com.canchapp.movil.core.red.aMensajeError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Estado del formulario de una cancha.
 *
 * @param id `null` mientras la cancha es nueva
 * @param fotoActual dirección de la foto guardada en el backend
 * @param fotoNueva foto elegida en la cámara o la galería, que se sube al guardar
 * @param fotoIlegible la foto elegida no se pudo leer como imagen
 * @param guardada todo se guardó; la pantalla vuelve a la lista
 */
data class EstadoFormularioCancha(
    val id: Long? = null,
    val cargando: Boolean = false,
    val errorCarga: MensajeError? = null,
    val nombre: String = "",
    val tipoCesped: String = TIPO_CESPED_INICIAL,
    val ancho: String = "",
    val largo: String = "",
    val fotoActual: String? = null,
    val fotoNueva: Uri? = null,
    val guardando: Boolean = false,
    val error: MensajeError? = null,
    val fotoIlegible: Boolean = false,
    val guardada: Boolean = false,
) {
    val esNueva: Boolean get() = id == null

    companion object {
        /** Las canchas del complejo son de césped sintético (DERCAS §4.1). Solo es el valor inicial. */
        const val TIPO_CESPED_INICIAL = "Sintético"
    }
}

/**
 * Pantalla 9 · Alta y edición de una cancha (RF-04).
 *
 * Guarda los datos y, si se eligió una foto, la sube después. Si la cancha se creó pero la foto
 * falló, el formulario queda en modo edición para reintentar sin crearla dos veces.
 */
@HiltViewModel
class CanchaFormularioViewModel @Inject constructor(
    private val canchas: CanchasRepository,
    private val preparador: PreparadorFoto,
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoFormularioCancha())
    val estado: StateFlow<EstadoFormularioCancha> = _estado.asStateFlow()

    private var iniciado = false

    /** @param id la cancha a editar, o `null` para una nueva. Solo actúa la primera vez. */
    fun iniciar(id: Long?) {
        if (iniciado) return
        iniciado = true
        if (id != null) cargar(id)
    }

    fun cargar(id: Long) {
        _estado.update { it.copy(id = id, cargando = true, errorCarga = null) }
        viewModelScope.launch {
            canchas.obtener(id)
                .onSuccess { cancha -> _estado.update { it.conCancha(cancha).copy(cargando = false) } }
                .onFailure { falla -> _estado.update { it.copy(cargando = false, errorCarga = falla.aMensajeError()) } }
        }
    }

    fun alCambiarNombre(valor: String) = editar { copy(nombre = valor) }
    fun alCambiarTipoCesped(valor: String) = editar { copy(tipoCesped = valor) }
    fun alCambiarAncho(valor: String) = editar { copy(ancho = soloDigitos(valor)) }
    fun alCambiarLargo(valor: String) = editar { copy(largo = soloDigitos(valor)) }
    fun elegirFoto(uri: Uri) = editar { copy(fotoNueva = uri) }

    fun guardar() {
        val actual = _estado.value
        if (actual.guardando || actual.cargando) return

        _estado.update { it.copy(guardando = true, error = null, fotoIlegible = false) }
        viewModelScope.launch {
            val solicitud = SolicitudCancha(
                nombre = actual.nombre.trim(),
                tipoCesped = actual.tipoCesped.trim(),
                anchoMetros = actual.ancho.toIntOrNull(),
                largoMetros = actual.largo.toIntOrNull(),
            )
            val datos = if (actual.id == null) canchas.crear(solicitud) else canchas.editar(actual.id, solicitud)
            val guardada = datos.getOrElse { falla ->
                _estado.update { it.copy(guardando = false, error = falla.aMensajeError()) }
                return@launch
            }
            // Desde aquí la cancha existe: un reintento la edita, no la crea otra vez.
            _estado.update { it.conCancha(guardada) }

            val foto = actual.fotoNueva
            if (foto == null) {
                _estado.update { it.copy(guardando = false, guardada = true) }
                return@launch
            }
            val jpg = preparador.preparar(foto)
            if (jpg == null) {
                _estado.update { it.copy(guardando = false, fotoIlegible = true) }
                return@launch
            }
            canchas.subirFoto(guardada.id, jpg)
                .onSuccess { conFoto ->
                    _estado.update { it.conCancha(conFoto).copy(guardando = false, fotoNueva = null, guardada = true) }
                }
                .onFailure { falla -> _estado.update { it.copy(guardando = false, error = falla.aMensajeError()) } }
        }
    }

    private fun editar(cambio: EstadoFormularioCancha.() -> EstadoFormularioCancha) =
        _estado.update { it.cambio().copy(error = null, fotoIlegible = false) }

    private fun EstadoFormularioCancha.conCancha(cancha: CanchaDto) = copy(
        id = cancha.id,
        nombre = cancha.nombre,
        tipoCesped = cancha.tipoCesped,
        ancho = cancha.anchoMetros.toString(),
        largo = cancha.largoMetros.toString(),
        fotoActual = canchas.urlFoto(cancha),
    )

    companion object {
        /** Ayuda de escritura para las medidas: solo dígitos. No es una regla. */
        fun soloDigitos(texto: String): String = texto.filter(Char::isDigit).take(4)
    }
}
