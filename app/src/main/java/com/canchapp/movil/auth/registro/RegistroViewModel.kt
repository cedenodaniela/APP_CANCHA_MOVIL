package com.canchapp.movil.auth.registro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canchapp.movil.auth.componentes.esCorreoValido
import com.canchapp.movil.auth.componentes.normalizarCorreo
import com.canchapp.movil.auth.data.AuthRepository
import com.canchapp.movil.auth.data.SolicitudRegistro
import com.canchapp.movil.core.red.MensajeError
import com.canchapp.movil.core.red.aMensajeError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Campos del formulario que pueden tener error. */
enum class CampoRegistro { NOMBRE, APELLIDO, EMAIL, PASSWORD, CONFIRMACION }

/**
 * Estado de la pantalla de registro.
 *
 * @param cuentaCreada correo de la cuenta recién creada; la pantalla lo usa una vez para volver al login
 */
data class EstadoRegistro(
    val nombre: String = "",
    val apellido: String = "",
    val email: String = "",
    val telefono: String = "",
    val password: String = "",
    val confirmacion: String = "",
    val passwordVisible: Boolean = false,
    val errores: Set<CampoRegistro> = emptySet(),
    val cargando: Boolean = false,
    val error: MensajeError? = null,
    val cuentaCreada: String? = null,
)

/**
 * Pantalla 1 · Registro de clientes (DERCAS §8.2, RF-01).
 *
 * Revisa el formulario antes de enviarlo para avisar al instante, pero quien decide es el backend:
 * si rechaza el registro (por ejemplo, «el correo ya está registrado»), se muestra su mensaje tal cual.
 * El registro crea solo cuentas de CLIENTE; los administradores se crean desde el panel web (RF-22).
 */
@HiltViewModel
class RegistroViewModel @Inject constructor(
    private val auth: AuthRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoRegistro())
    val estado: StateFlow<EstadoRegistro> = _estado.asStateFlow()

    fun alCambiarNombre(valor: String) = cambiar(CampoRegistro.NOMBRE) { copy(nombre = valor) }
    fun alCambiarApellido(valor: String) = cambiar(CampoRegistro.APELLIDO) { copy(apellido = valor) }
    fun alCambiarEmail(valor: String) = cambiar(CampoRegistro.EMAIL) { copy(email = valor) }
    fun alCambiarTelefono(valor: String) = cambiar(null) { copy(telefono = valor) }
    fun alCambiarPassword(valor: String) = cambiar(CampoRegistro.PASSWORD) { copy(password = valor) }
    fun alCambiarConfirmacion(valor: String) = cambiar(CampoRegistro.CONFIRMACION) { copy(confirmacion = valor) }

    fun alternarPasswordVisible() = _estado.update { it.copy(passwordVisible = !it.passwordVisible) }

    /** La pantalla ya volvió al login con la cuenta creada. */
    fun cuentaCreadaMostrada() = _estado.update { it.copy(cuentaCreada = null) }

    fun registrar() {
        val actual = _estado.value
        if (actual.cargando) return

        val limpio = actual.copy(
            nombre = actual.nombre.trim(),
            apellido = actual.apellido.trim(),
            email = normalizarCorreo(actual.email),
            telefono = actual.telefono.trim(),
        )
        val errores = buildSet {
            if (limpio.nombre.isEmpty()) add(CampoRegistro.NOMBRE)
            if (limpio.apellido.isEmpty()) add(CampoRegistro.APELLIDO)
            if (!esCorreoValido(limpio.email)) add(CampoRegistro.EMAIL)
            if (limpio.password.length < LONGITUD_MINIMA_CONTRASENA) add(CampoRegistro.PASSWORD)
            if (limpio.confirmacion != limpio.password) add(CampoRegistro.CONFIRMACION)
        }
        _estado.value = limpio.copy(errores = errores, error = null)
        if (errores.isNotEmpty()) return

        _estado.update { it.copy(cargando = true) }
        viewModelScope.launch {
            val solicitud = SolicitudRegistro(
                nombre = limpio.nombre,
                apellido = limpio.apellido,
                email = limpio.email,
                telefono = limpio.telefono.ifEmpty { null },
                password = limpio.password,
            )
            auth.registrar(solicitud)
                .onSuccess {
                    _estado.update { it.copy(cargando = false, password = "", confirmacion = "", cuentaCreada = limpio.email) }
                }
                .onFailure { falla ->
                    _estado.update { it.copy(cargando = false, error = falla.aMensajeError()) }
                }
        }
    }

    /** Aplica el cambio y quita el error del campo editado y el aviso general. */
    private fun cambiar(campo: CampoRegistro?, cambio: EstadoRegistro.() -> EstadoRegistro) =
        _estado.update { it.cambio().copy(errores = it.errores - setOfNotNull(campo), error = null) }

    companion object {
        /**
         * Mínimo que exige el backend (`Usuario.LONGITUD_MINIMA_CONTRASENA`). Se revisa aquí solo para
         * avisar antes de enviar; si el backend cambia el mínimo, su mensaje es el que manda.
         */
        const val LONGITUD_MINIMA_CONTRASENA = 8
    }
}
