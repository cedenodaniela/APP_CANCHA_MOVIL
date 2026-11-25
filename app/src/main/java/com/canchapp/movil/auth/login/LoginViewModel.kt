package com.canchapp.movil.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
 * Estado de la pantalla de inicio de sesión.
 *
 * Los errores de cada campo solo aparecen después de intentar ingresar, para no regañar
 * mientras la persona escribe.
 */
data class EstadoLogin(
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val emailInvalido: Boolean = false,
    val passwordVacia: Boolean = false,
    val cargando: Boolean = false,
    val error: MensajeError? = null,
)

/**
 * Pantalla 1 · Inicio de sesión (DERCAS §8.2, RF-01).
 *
 * Solo valida el formato del formulario; si las credenciales son correctas lo decide el backend,
 * y su mensaje («Correo o contraseña incorrectos») se muestra tal cual, sin revelar cuál de los dos falló.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val auth: AuthRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoLogin())
    val estado: StateFlow<EstadoLogin> = _estado.asStateFlow()

    fun alCambiarEmail(email: String) =
        _estado.update { it.copy(email = email, emailInvalido = false, error = null) }

    fun alCambiarPassword(password: String) =
        _estado.update { it.copy(password = password, passwordVacia = false, error = null) }

    fun alternarPasswordVisible() =
        _estado.update { it.copy(passwordVisible = !it.passwordVisible) }

    fun iniciarSesion() {
        val actual = _estado.value
        if (actual.cargando) return

        // El autocompletado del teclado suele dejar un espacio al final del correo.
        val email = actual.email.trim().lowercase()
        val emailInvalido = !esCorreoValido(email)
        val passwordVacia = actual.password.isEmpty()
        _estado.update { it.copy(email = email, emailInvalido = emailInvalido, passwordVacia = passwordVacia, error = null) }
        if (emailInvalido || passwordVacia) return

        _estado.update { it.copy(cargando = true) }
        viewModelScope.launch {
            auth.iniciarSesion(email, actual.password)
                .onSuccess {
                    // La navegación reacciona a la sesión guardada; aquí solo se limpia la contraseña.
                    _estado.update { it.copy(password = "", cargando = false) }
                }
                .onFailure { falla ->
                    _estado.update { it.copy(cargando = false, error = falla.aMensajeError()) }
                }
        }
    }

    private companion object {
        /** Forma básica de un correo; si existe la cuenta lo decide el backend. */
        val FORMATO_CORREO = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

        fun esCorreoValido(email: String) = FORMATO_CORREO.matches(email)
    }
}
