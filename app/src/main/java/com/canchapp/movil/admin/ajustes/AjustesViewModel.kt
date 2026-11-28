package com.canchapp.movil.admin.ajustes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canchapp.movil.admin.ajustes.data.ComplejoDto
import com.canchapp.movil.admin.ajustes.data.ConfiguracionDto
import com.canchapp.movil.admin.ajustes.data.ConfiguracionRepository
import com.canchapp.movil.admin.ajustes.data.DatosBancariosDto
import com.canchapp.movil.admin.ajustes.data.PoliticaDto
import com.canchapp.movil.core.red.MensajeError
import com.canchapp.movil.core.red.aMensajeError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Tipos de cuenta que acepta el backend. */
enum class TipoCuenta { AHORROS, CORRIENTE }

/**
 * Lo que se edita en la pantalla. Los números se guardan como texto mientras se escriben:
 * un campo vacío se envía vacío y el backend explica qué falta.
 */
data class FormularioAjustes(
    val nombre: String = "",
    val direccion: String = "",
    val telefono: String = "",
    val banco: String = "",
    val tipoCuenta: TipoCuenta = TipoCuenta.AHORROS,
    val numeroCuenta: String = "",
    val titular: String = "",
    val minutosExpiracion: String = "",
    val horasPlazoCancelacion: String = "",
    val maxReservasEfectivo: String = "",
    val diasAnticipacion: String = "",
    val limitarTrasInasistencia: Boolean = false,
)

/**
 * Estado de la pantalla de ajustes.
 *
 * @param errorCarga no se pudo leer la configuración; se ofrece reintentar
 * @param guardado el último cambio se guardó bien; se confirma hasta la siguiente edición
 */
data class EstadoAjustes(
    val cargando: Boolean = true,
    val errorCarga: MensajeError? = null,
    val formulario: FormularioAjustes = FormularioAjustes(),
    val guardando: Boolean = false,
    val error: MensajeError? = null,
    val guardado: Boolean = false,
)

/**
 * Pantalla 10 · Configuración del complejo (RF-03), solo para el administrador.
 *
 * Carga la configuración, deja editar los datos del complejo, la cuenta bancaria y las políticas,
 * y la guarda completa. Los rangos válidos los decide el backend: si rechaza un valor, se muestra
 * su mensaje («Los días de anticipación deben ser al menos 1.»).
 */
@HiltViewModel
class AjustesViewModel @Inject constructor(
    private val configuracion: ConfiguracionRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoAjustes())
    val estado: StateFlow<EstadoAjustes> = _estado.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        _estado.update { it.copy(cargando = true, errorCarga = null) }
        viewModelScope.launch {
            configuracion.consultar()
                .onSuccess { dto -> _estado.update { EstadoAjustes(cargando = false, formulario = dto.aFormulario()) } }
                .onFailure { falla -> _estado.update { it.copy(cargando = false, errorCarga = falla.aMensajeError()) } }
        }
    }

    /** Aplica un cambio del formulario y quita los avisos del guardado anterior. */
    fun editar(cambio: FormularioAjustes.() -> FormularioAjustes) =
        _estado.update { it.copy(formulario = it.formulario.cambio(), error = null, guardado = false) }

    fun guardar() {
        val actual = _estado.value
        if (actual.guardando || actual.cargando) return

        _estado.update { it.copy(guardando = true, error = null, guardado = false) }
        viewModelScope.launch {
            configuracion.guardar(actual.formulario.aDto())
                .onSuccess { dto ->
                    _estado.update { it.copy(guardando = false, guardado = true, formulario = dto.aFormulario()) }
                }
                .onFailure { falla -> _estado.update { it.copy(guardando = false, error = falla.aMensajeError()) } }
        }
    }

    companion object {
        /** Ayuda de escritura para los campos numéricos: solo deja dígitos. No es una regla. */
        fun soloDigitos(texto: String): String = texto.filter(Char::isDigit).take(MAXIMO_DIGITOS)

        private const val MAXIMO_DIGITOS = 4
    }
}

private fun ConfiguracionDto.aFormulario() = FormularioAjustes(
    nombre = complejo.nombre.orEmpty(),
    direccion = complejo.direccion.orEmpty(),
    telefono = complejo.telefono.orEmpty(),
    banco = datosBancarios.banco.orEmpty(),
    tipoCuenta = TipoCuenta.entries.firstOrNull { it.name == datosBancarios.tipoCuenta } ?: TipoCuenta.AHORROS,
    numeroCuenta = datosBancarios.numeroCuenta.orEmpty(),
    titular = datosBancarios.titular.orEmpty(),
    minutosExpiracion = politica.minutosExpiracion?.toString().orEmpty(),
    horasPlazoCancelacion = politica.horasPlazoCancelacion?.toString().orEmpty(),
    maxReservasEfectivo = politica.maxReservasEfectivo?.toString().orEmpty(),
    diasAnticipacion = politica.diasAnticipacion?.toString().orEmpty(),
    limitarTrasInasistencia = politica.limitarTrasInasistencia ?: false,
)

private fun FormularioAjustes.aDto() = ConfiguracionDto(
    complejo = ComplejoDto(nombre.trim(), direccion.trim(), telefono.trim()),
    datosBancarios = DatosBancariosDto(banco.trim(), tipoCuenta.name, numeroCuenta.trim(), titular.trim()),
    politica = PoliticaDto(
        minutosExpiracion = minutosExpiracion.toIntOrNull(),
        horasPlazoCancelacion = horasPlazoCancelacion.toIntOrNull(),
        maxReservasEfectivo = maxReservasEfectivo.toIntOrNull(),
        diasAnticipacion = diasAnticipacion.toIntOrNull(),
        limitarTrasInasistencia = limitarTrasInasistencia,
    ),
)
