package com.canchapp.movil.auth.recuperacion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canchapp.movil.auth.componentes.esCorreoValido
import com.canchapp.movil.auth.componentes.normalizarCorreo
import com.canchapp.movil.auth.data.AuthRepository
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
 * Estado de la pantalla «¿Olvidaste tu contraseña?».
 *
 * @param enviado mensaje del backend después de pedir el enlace; es el mismo exista o no la cuenta
 */
data class EstadoRecuperar(
    val email: String = "",
    val emailInvalido: Boolean = false,
    val cargando: Boolean = false,
    val error: MensajeError? = null,
    val enviado: String? = null,
)

/**
 * Pantalla 1 · Recuperación de contraseña, primer paso (RF-02).
 *
 * Solo pide el correo. El backend responde siempre lo mismo, exista o no la cuenta, y esta
 * pantalla muestra ese mensaje tal cual: la app tampoco revela qué correos están registrados.
 */
@HiltViewModel
class RecuperarViewModel @Inject constructor(
    private val auth: AuthRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoRecuperar())
    val estado: StateFlow<EstadoRecuperar> = _estado.asStateFlow()

    /** Al cambiar el correo se puede volver a pedir el enlace. */
    fun alCambiarEmail(email: String) =
        _estado.update { it.copy(email = email, emailInvalido = false, error = null, enviado = null) }

    /** Deja el correo del login escrito, si la persona ya lo había puesto. */
    fun usarCorreo(email: String) {
        if (_estado.value.email.isEmpty()) _estado.update { it.copy(email = email) }
    }

    fun enviar() {
        val actual = _estado.value
        if (actual.cargando) return

        val email = normalizarCorreo(actual.email)
        val emailInvalido = !esCorreoValido(email)
        _estado.update { it.copy(email = email, emailInvalido = emailInvalido, error = null, enviado = null) }
        if (emailInvalido) return

        _estado.update { it.copy(cargando = true) }
        viewModelScope.launch {
            auth.solicitarRecuperacion(email)
                .onSuccess { mensaje -> _estado.update { it.copy(cargando = false, enviado = mensaje) } }
                .onFailure { falla -> _estado.update { it.copy(cargando = false, error = falla.aMensajeError()) } }
        }
    }
}
