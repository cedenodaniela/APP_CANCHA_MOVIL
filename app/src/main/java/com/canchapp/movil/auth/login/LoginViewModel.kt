package com.canchapp.movil.auth.login

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
 * Estado de la pantalla de inicio de sesión.
 *
 * Los errores de cada campo solo aparecen después de intentar ingresar, para no regañar
 * mientras la persona escribe.
 *
 * @param cuentaCreada la persona acaba de registrarse: se le confirma y se deja su correo escrito
 * @param contrasenaRestablecida la persona acaba de crear una contraseña nueva con el enlace del correo (RF-02)
 */
data class EstadoLogin(
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val emailInvalido: Boolean = false,
    val passwordVacia: Boolean = false,
    val cargando: Boolean = false,
    val error: MensajeError? = null,
    val cuentaCreada: Boolean = false,
    val contrasenaRestablecida: Boolean = false,
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

    /** Al volver del registro: confirma la cuenta creada y deja el correo escrito. */
    fun mostrarCuentaCreada(email: String) =
        _estado.update { EstadoLogin(email = email, cuentaCreada = true) }

    /** Al volver de la contraseña nueva (RF-02): confirma el cambio. */
    fun mostrarContrasenaRestablecida() =
        _estado.update { EstadoLogin(email = it.email, contrasenaRestablecida = true) }

    fun iniciarSesion() {
        val actual = _estado.value
        if (actual.cargando) return

        val email = normalizarCorreo(actual.email)
        val emailInvalido = !esCorreoValido(email)
        val passwordVacia = actual.password.isEmpty()
        _estado.update {
            it.copy(email = email, emailInvalido = emailInvalido, passwordVacia = passwordVacia, error = null, cuentaCreada = false, contrasenaRestablecida = false)
        }
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
}
