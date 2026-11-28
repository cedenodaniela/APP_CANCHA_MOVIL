package com.canchapp.movil.admin.ajustes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.canchapp.movil.R
import com.canchapp.movil.admin.ajustes.AjustesViewModel.Companion.soloDigitos
import com.canchapp.movil.core.red.MensajeError
import com.canchapp.movil.ui.componentes.Aviso
import com.canchapp.movil.ui.componentes.BotonMenu
import com.canchapp.movil.ui.componentes.TipoAviso
import com.canchapp.movil.ui.componentes.textoDe
import com.canchapp.movil.ui.theme.CanchAppTheme

/** Etiquetas para las pruebas de pantalla. */
object EtiquetasAjustes {
    const val NOMBRE = "ajustes_nombre"
    const val NUMERO_CUENTA = "ajustes_numero_cuenta"
    const val DIAS_ANTICIPACION = "ajustes_dias_anticipacion"
    const val LIMITAR = "ajustes_limitar"
    const val GUARDAR = "ajustes_guardar"
    const val REINTENTAR = "ajustes_reintentar"
}

/** Pantalla 10 · Ajustes del complejo (RF-03), conectada a su ViewModel. */
@Composable
fun AjustesScreen(alAbrirMenu: () -> Unit, viewModel: AjustesViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    AjustesContenido(
        estado = estado,
        acciones = AccionesAjustes(
            alAbrirMenu = alAbrirMenu,
            alReintentar = viewModel::cargar,
            alEditar = viewModel::editar,
            alGuardar = viewModel::guardar,
        ),
    )
}

/** Lo que la persona puede hacer en los ajustes. */
data class AccionesAjustes(
    val alAbrirMenu: () -> Unit = {},
    val alReintentar: () -> Unit = {},
    val alEditar: (FormularioAjustes.() -> FormularioAjustes) -> Unit = {},
    val alGuardar: () -> Unit = {},
)

/** Ajustes sin estado propio: se prueban y se previsualizan con cualquier [EstadoAjustes]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjustesContenido(estado: EstadoAjustes, acciones: AccionesAjustes) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.titulo_ajustes)) },
                navigationIcon = {
                    BotonMenu(acciones.alAbrirMenu)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { relleno ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno),
            contentAlignment = Alignment.TopCenter,
        ) {
            when {
                estado.cargando -> Cargando()
                estado.errorCarga != null -> ErrorDeCarga(estado.errorCarga, acciones.alReintentar)
                else -> Formulario(estado, acciones)
            }
        }
    }
}

@Composable
private fun Cargando() {
    Column(
        modifier = Modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CircularProgressIndicator()
        Text(stringResource(R.string.cargando_ajustes), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ErrorDeCarga(error: MensajeError, alReintentar: () -> Unit) {
    Column(
        modifier = Modifier
            .widthIn(max = 560.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Aviso(textoDe(error))
        OutlinedButton(
            onClick = alReintentar,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag(EtiquetasAjustes.REINTENTAR),
        ) {
            Text(stringResource(R.string.accion_reintentar))
        }
    }
}

@Composable
private fun Formulario(estado: EstadoAjustes, acciones: AccionesAjustes) {
    val f = estado.formulario
    val habilitado = !estado.guardando
    val editar = acciones.alEditar

    Column(
        modifier = Modifier
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Seccion(stringResource(R.string.seccion_complejo)) {
            Campo(f.nombre, { v -> editar { copy(nombre = v) } }, R.string.campo_nombre_complejo, habilitado,
                etiquetaPrueba = EtiquetasAjustes.NOMBRE, mayusculas = true)
            Campo(f.direccion, { v -> editar { copy(direccion = v) } }, R.string.campo_direccion, habilitado, mayusculas = true)
            Campo(f.telefono, { v -> editar { copy(telefono = v) } }, R.string.campo_telefono_complejo, habilitado,
                tipoTeclado = KeyboardType.Phone)
        }

        Seccion(stringResource(R.string.seccion_datos_bancarios), stringResource(R.string.ayuda_datos_bancarios)) {
            Campo(f.banco, { v -> editar { copy(banco = v) } }, R.string.campo_banco, habilitado, mayusculas = true)
            Text(stringResource(R.string.campo_tipo_cuenta), style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                TipoCuenta.entries.forEachIndexed { i, tipo ->
                    SegmentedButton(
                        selected = f.tipoCuenta == tipo,
                        onClick = { editar { copy(tipoCuenta = tipo) } },
                        enabled = habilitado,
                        shape = SegmentedButtonDefaults.itemShape(i, TipoCuenta.entries.size),
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(
                            stringResource(
                                if (tipo == TipoCuenta.AHORROS) R.string.tipo_cuenta_ahorros else R.string.tipo_cuenta_corriente,
                            ),
                        )
                    }
                }
            }
            Campo(f.numeroCuenta, { v -> editar { copy(numeroCuenta = v) } }, R.string.campo_numero_cuenta, habilitado,
                etiquetaPrueba = EtiquetasAjustes.NUMERO_CUENTA, tipoTeclado = KeyboardType.Number)
            Campo(f.titular, { v -> editar { copy(titular = v) } }, R.string.campo_titular, habilitado, mayusculas = true)
        }

        Seccion(stringResource(R.string.seccion_politicas), stringResource(R.string.ayuda_politicas)) {
            CampoNumero(f.minutosExpiracion, { v -> editar { copy(minutosExpiracion = v) } },
                R.string.campo_minutos_expiracion, R.string.ayuda_minutos_expiracion, habilitado)
            CampoNumero(f.horasPlazoCancelacion, { v -> editar { copy(horasPlazoCancelacion = v) } },
                R.string.campo_horas_cancelacion, R.string.ayuda_horas_cancelacion, habilitado)
            CampoNumero(f.maxReservasEfectivo, { v -> editar { copy(maxReservasEfectivo = v) } },
                R.string.campo_max_efectivo, R.string.ayuda_max_efectivo, habilitado)
            CampoNumero(f.diasAnticipacion, { v -> editar { copy(diasAnticipacion = v) } },
                R.string.campo_dias_anticipacion, R.string.ayuda_dias_anticipacion, habilitado,
                etiquetaPrueba = EtiquetasAjustes.DIAS_ANTICIPACION)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.campo_limitar_inasistencia), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.ayuda_limitar_inasistencia),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = f.limitarTrasInasistencia,
                    onCheckedChange = { v -> editar { copy(limitarTrasInasistencia = v) } },
                    enabled = habilitado,
                    modifier = Modifier.testTag(EtiquetasAjustes.LIMITAR),
                )
            }
        }

        // El aviso queda junto al botón, donde la persona está mirando al guardar.
        estado.error?.let { Aviso(textoDe(it)) }
        if (estado.guardado) Aviso(stringResource(R.string.cambios_guardados), tipo = TipoAviso.EXITO)

        Button(
            onClick = acciones.alGuardar,
            enabled = habilitado,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .testTag(EtiquetasAjustes.GUARDAR),
        ) {
            if (estado.guardando) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.guardando_cambios))
            } else {
                Text(stringResource(R.string.accion_guardar_cambios))
            }
        }
    }
}

@Composable
private fun Seccion(titulo: String, ayuda: String? = null, contenido: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                titulo,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() },
            )
            ayuda?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            contenido()
        }
    }
}

@Composable
private fun Campo(
    valor: String,
    alCambiar: (String) -> Unit,
    etiqueta: Int,
    habilitado: Boolean,
    etiquetaPrueba: String? = null,
    tipoTeclado: KeyboardType = KeyboardType.Text,
    mayusculas: Boolean = false,
    ayuda: Int? = null,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(stringResource(etiqueta)) },
        supportingText = ayuda?.let { { Text(stringResource(it)) } },
        enabled = habilitado,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = tipoTeclado,
            capitalization = if (mayusculas) KeyboardCapitalization.Sentences else KeyboardCapitalization.None,
            imeAction = ImeAction.Next,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (etiquetaPrueba != null) Modifier.testTag(etiquetaPrueba) else Modifier),
    )
}

@Composable
private fun CampoNumero(
    valor: String,
    alCambiar: (String) -> Unit,
    etiqueta: Int,
    ayuda: Int,
    habilitado: Boolean,
    etiquetaPrueba: String? = null,
) = Campo(
    valor = valor,
    alCambiar = { alCambiar(soloDigitos(it)) },
    etiqueta = etiqueta,
    habilitado = habilitado,
    etiquetaPrueba = etiquetaPrueba,
    tipoTeclado = KeyboardType.Number,
    ayuda = ayuda,
)

private val FORMULARIO_EJEMPLO = FormularioAjustes(
    nombre = "Complejo Deportivo Calceta",
    direccion = "Calceta, Manabí",
    telefono = "0969908865",
    banco = "Banco Pichincha",
    numeroCuenta = "2201234567",
    titular = "Complejo Deportivo Calceta",
    minutosExpiracion = "15",
    horasPlazoCancelacion = "3",
    maxReservasEfectivo = "1",
    diasAnticipacion = "3",
    limitarTrasInasistencia = true,
)

@Preview(showSystemUi = true)
@Composable
private fun AjustesPreview() {
    CanchAppTheme {
        AjustesContenido(EstadoAjustes(cargando = false, formulario = FORMULARIO_EJEMPLO), AccionesAjustes())
    }
}

@Preview(showSystemUi = true)
@Composable
private fun AjustesConErrorPreview() {
    CanchAppTheme {
        AjustesContenido(
            EstadoAjustes(
                cargando = false,
                formulario = FORMULARIO_EJEMPLO.copy(diasAnticipacion = "0"),
                error = MensajeError.DelServidor("CONFIGURACION_INVALIDA", "Los días de anticipación deben ser al menos 1."),
            ),
            AccionesAjustes(),
        )
    }
}
