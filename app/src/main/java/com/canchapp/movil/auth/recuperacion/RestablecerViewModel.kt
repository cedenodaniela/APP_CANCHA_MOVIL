package com.canchapp.movil.auth.recuperacion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canchapp.movil.auth.data.AuthRepository
import com.canchapp.movil.auth.registro.RegistroViewModel.Companion.LONGITUD_MINIMA_CONTRASENA
import com.canchapp.movil.core.red.MensajeError
import com.canchapp.movil.core.red.aMensajeError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Campos de la contraseña nueva que pueden tener error. */
enum class CampoRestablecer { PASSWORD, CONFIRMACION }

/**
 * Estado de la pantalla de contraseña nueva.
 *
 * @param restablecida la contraseña ya cambió; la pantalla vuelve una vez al login
 */
data class EstadoRestablecer(
    val password: String = "",
    val confirmacion: String = "",
    val passwordVisible: Boolean = false,
    val errores: Set<CampoRestablecer> = emptySet(),
    val cargando: Boolean = false,
    val error: MensajeError? = null,
    val restablecida: Boolean = false,
)

/**
 * Pantalla 1 · Recuperación de contraseña, segundo paso (RF-02).
 *
 * Se abre desde el enlace del correo, que trae el token. Revisa las mismas reglas del registro
 * para avisar antes de enviar; si el enlace venció o ya se usó, lo dice el backend.
 */
@HiltViewModel
class RestablecerViewModel @Inject constructor(
    private val auth: AuthRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoRestablecer())
    val estado: StateFlow<EstadoRestablecer> = _estado.asStateFlow()

    fun alCambiarPassword(valor: String) = cambiar(CampoRestablecer.PASSWORD) { copy(password = valor) }
    fun alCambiarConfirmacion(valor: String) = cambiar(CampoRestablecer.CONFIRMACION) { copy(confirmacion = valor) }
    fun alternarPasswordVisible() = _estado.update { it.copy(passwordVisible = !it.passwordVisible) }

    /** La pantalla ya volvió al login. */
    fun restablecidaMostrada() = _estado.update { it.copy(restablecida = false) }

    /** @param token el del enlace del correo */
    fun guardar(token: String) {
        val actual = _estado.value
        if (actual.cargando) return

        val errores = buildSet {
            if (actual.password.length < LONGITUD_MINIMA_CONTRASENA) add(CampoRestablecer.PASSWORD)
            if (actual.confirmacion != actual.password) add(CampoRestablecer.CONFIRMACION)
        }
        _estado.update { it.copy(errores = errores, error = null) }
        if (errores.isNotEmpty()) return

        _estado.update { it.copy(cargando = true) }
        viewModelScope.launch {
            auth.restablecerContrasena(token, actual.password)
                .onSuccess {
                    _estado.update { it.copy(cargando = false, password = "", confirmacion = "", restablecida = true) }
                }
                .onFailure { falla -> _estado.update { it.copy(cargando = false, error = falla.aMensajeError()) } }
        }
    }

    private fun cambiar(campo: CampoRestablecer, cambio: EstadoRestablecer.() -> EstadoRestablecer) =
        _estado.update { it.cambio().copy(errores = it.errores - campo, error = null) }
}
