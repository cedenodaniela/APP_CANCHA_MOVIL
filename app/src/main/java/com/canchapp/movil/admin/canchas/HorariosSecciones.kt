package com.canchapp.movil.admin.canchas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.canchapp.movil.R
import com.canchapp.movil.admin.canchas.data.BloqueoDto
import com.canchapp.movil.admin.canchas.data.DiaDto
import com.canchapp.movil.admin.canchas.data.TarifaDto
import com.canchapp.movil.core.red.MensajeError
import com.canchapp.movil.ui.componentes.Aviso
import com.canchapp.movil.ui.componentes.TipoAviso
import com.canchapp.movil.ui.componentes.textoDe
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Etiquetas para las pruebas de pantalla. */
object EtiquetasHorarios {
    const val GUARDAR_HORARIO = "horario_guardar"
    const val NUEVA_TARIFA = "tarifas_nueva"
    const val GUARDAR_TARIFA = "tarifas_guardar"
    const val NUEVO_BLOQUEO = "bloqueos_nuevo"
    const val GUARDAR_BLOQUEO = "bloqueos_guardar"
    fun diaAbierto(dia: String) = "horario_abierto_$dia"
}

private val FORMATO_FECHA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale.forLanguageTag("es-EC"))

/** Nombre del día para mostrar («Miércoles»), a partir del nombre de la API («MIERCOLES»). */
@Composable
private fun nombreDia(dia: String): String = stringArrayResource(R.array.dias_semana)[DIAS_API.indexOf(dia).coerceAtLeast(0)]

/** «Todos los días», o los días cortos separados por comas («Lun, Mar, Vie»). */
@Composable
private fun resumenDias(dias: List<String>): String {
    if (dias.size == 7) return stringResource(R.string.todos_los_dias)
    val cortos = stringArrayResource(R.array.dias_semana_cortos)
    return DIAS_API.filter { it in dias }.joinToString(", ") { cortos[DIAS_API.indexOf(it)] }
}

/** Elige una hora de [HORAS_DEL_DIA] con un menú, sin teclado. */
@Composable
fun SelectorHora(
    etiqueta: String,
    valor: String?,
    alElegir: (String) -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
) {
    var abierto by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(
            onClick = { abierto = true },
            enabled = habilitado,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        ) {
            Text("$etiqueta ${valor ?: "--:--"}")
        }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            HORAS_DEL_DIA.forEach { hora ->
                DropdownMenuItem(text = { Text(hora) }, onClick = {
                    alElegir(hora)
                    abierto = false
                })
            }
        }
    }
}

@Composable
private fun Tarjeta(contenido: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) { Box(Modifier.padding(12.dp)) { contenido() } }
}

@Composable
private fun Ayuda(texto: String) {
    Text(texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun mostrarCargaOError(cargando: Boolean, error: MensajeError?, alReintentar: () -> Unit): Boolean {
    when {
        cargando -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        error != null -> Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Aviso(textoDe(error))
            OutlinedButton(onClick = alReintentar, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.accion_reintentar))
            }
        }
        else -> return false
    }
    return true
}

// ---------------------------------------------------------------- Horario

/** Sección «Horario» de la pantalla 9 (RF-05). */
@Composable
fun HorarioSeccion(
    estado: EstadoHorario,
    alEditarDia: (dia: String, cambio: DiaDto.() -> DiaDto) -> Unit,
    alGuardar: () -> Unit,
    alReintentar: () -> Unit,
) {
    if (mostrarCargaOError(estado.cargando, estado.errorCarga, alReintentar)) return
    val habilitado = !estado.guardando
    LazyColumn(
        modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Ayuda(stringResource(R.string.ayuda_horario)) }
        items(estado.dias, key = { it.dia }) { dia ->
            val nombre = nombreDia(dia.dia)
            Tarjeta {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                stringResource(if (dia.abierto) R.string.estado_abierto else R.string.estado_cerrado),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        val descripcion = stringResource(
                            if (dia.abierto) R.string.descripcion_cerrar_dia else R.string.descripcion_abrir_dia, nombre.lowercase(),
                        )
                        Switch(
                            checked = dia.abierto,
                            onCheckedChange = { abierto -> alEditarDia(dia.dia) { copy(abierto = abierto) } },
                            enabled = habilitado,
                            modifier = Modifier
                                .testTag(EtiquetasHorarios.diaAbierto(dia.dia))
                                .semantics { contentDescription = descripcion },
                        )
                    }
                    if (dia.abierto) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SelectorHora(stringResource(R.string.campo_apertura), dia.apertura,
                                { h -> alEditarDia(dia.dia) { copy(apertura = h) } }, Modifier.weight(1f), habilitado)
                            SelectorHora(stringResource(R.string.campo_cierre), dia.cierre,
                                { h -> alEditarDia(dia.dia) { copy(cierre = h) } }, Modifier.weight(1f), habilitado)
                        }
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                estado.error?.let { Aviso(textoDe(it)) }
                if (estado.guardado) Aviso(stringResource(R.string.horario_guardado), tipo = TipoAviso.EXITO)
                BotonGuardar(stringResource(R.string.accion_guardar_horario), estado.guardando, alGuardar, EtiquetasHorarios.GUARDAR_HORARIO)
            }
        }
    }
}

@Composable
private fun BotonGuardar(texto: String, guardando: Boolean, alPulsar: () -> Unit, etiquetaPrueba: String) {
    Button(
        onClick = alPulsar,
        enabled = !guardando,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .testTag(etiquetaPrueba),
    ) {
        if (guardando) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.guardando_cambios))
        } else {
            Text(texto)
        }
    }
}

// ---------------------------------------------------------------- Tarifas

/** Lo que la persona puede hacer en la sección de tarifas. */
data class AccionesTarifas(
    val alReintentar: () -> Unit = {},
    val alNueva: () -> Unit = {},
    val alEditar: (TarifaDto) -> Unit = {},
    val alCambiarFormulario: (FormularioTarifa.() -> FormularioTarifa) -> Unit = {},
    val alGuardar: () -> Unit = {},
    val alCerrar: () -> Unit = {},
    val alPedirEliminar: (TarifaDto) -> Unit = {},
    val alConfirmarEliminar: () -> Unit = {},
    val alCancelarEliminar: () -> Unit = {},
)

/** Sección «Tarifas» de la pantalla 9 (RF-05). */
@Composable
fun TarifasSeccion(estado: EstadoTarifas, acciones: AccionesTarifas) {
    if (mostrarCargaOError(estado.cargando, estado.errorCarga, acciones.alReintentar)) return
    LazyColumn(
        modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Ayuda(stringResource(R.string.ayuda_tarifas)) }
        if (estado.formulario == null) estado.error?.let { item { Aviso(textoDe(it)) } }
        if (estado.tarifas.isEmpty()) item { Ayuda(stringResource(R.string.sin_tarifas)) }
        items(estado.tarifas, key = { it.id }) { t ->
            Tarjeta {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.precio_por_hora, t.precioHora),
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.rango_horas, t.inicio, t.fin), style = MaterialTheme.typography.bodyMedium)
                        Text(resumenDias(t.dias), style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { acciones.alEditar(t) }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.accion_editar))
                    }
                    TextButton(onClick = { acciones.alPedirEliminar(t) }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.accion_eliminar))
                    }
                }
            }
        }
        item {
            Button(
                onClick = acciones.alNueva,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag(EtiquetasHorarios.NUEVA_TARIFA),
            ) { Text(stringResource(R.string.accion_nueva_tarifa)) }
        }
    }

    estado.formulario?.let { FormularioTarifaDialogo(it, estado.guardando, estado.error, acciones) }
    estado.porEliminar?.let {
        Confirmar(
            titulo = stringResource(R.string.titulo_eliminar_tarifa),
            texto = stringResource(R.string.texto_eliminar_tarifa),
            accion = stringResource(R.string.accion_eliminar),
            alConfirmar = acciones.alConfirmarEliminar,
            alCancelar = acciones.alCancelarEliminar,
        )
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun FormularioTarifaDialogo(f: FormularioTarifa, guardando: Boolean, error: MensajeError?, acciones: AccionesTarifas) {
    val cortos = stringArrayResource(R.array.dias_semana_cortos)
    AlertDialog(
        onDismissRequest = acciones.alCerrar,
        title = { Text(stringResource(if (f.id == null) R.string.titulo_nueva_tarifa else R.string.titulo_editar_tarifa)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.campo_dias), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DIAS_API.forEachIndexed { i, dia ->
                        FilterChip(
                            selected = dia in f.dias,
                            onClick = { acciones.alCambiarFormulario { copy(dias = if (dia in dias) dias - dia else dias + dia) } },
                            label = { Text(cortos[i]) },
                            enabled = !guardando,
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelectorHora(stringResource(R.string.campo_desde), f.inicio,
                        { h -> acciones.alCambiarFormulario { copy(inicio = h) } }, Modifier.weight(1f), !guardando)
                    SelectorHora(stringResource(R.string.campo_hasta), f.fin,
                        { h -> acciones.alCambiarFormulario { copy(fin = h) } }, Modifier.weight(1f), !guardando)
                }
                OutlinedTextField(
                    value = f.precio,
                    onValueChange = { v -> acciones.alCambiarFormulario { copy(precio = TarifasViewModel.soloPrecio(v)) } },
                    label = { Text(stringResource(R.string.campo_precio_hora)) },
                    singleLine = true,
                    enabled = !guardando,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let { Aviso(textoDe(it)) }
            }
        },
        confirmButton = {
            TextButton(onClick = acciones.alGuardar, enabled = !guardando,
                modifier = Modifier.testTag(EtiquetasHorarios.GUARDAR_TARIFA)) {
                Text(stringResource(if (guardando) R.string.guardando_cambios else R.string.accion_guardar))
            }
        },
        dismissButton = { TextButton(onClick = acciones.alCerrar) { Text(stringResource(R.string.accion_cancelar)) } },
    )
}

// ---------------------------------------------------------------- Bloqueos

/** Lo que la persona puede hacer en la sección de bloqueos. */
data class AccionesBloqueos(
    val alReintentar: () -> Unit = {},
    val alNuevo: () -> Unit = {},
    val alCambiarFormulario: (FormularioBloqueo.() -> FormularioBloqueo) -> Unit = {},
    val alGuardar: () -> Unit = {},
    val alCerrar: () -> Unit = {},
    val alConfirmar: () -> Unit = {},
    val alCancelarConfirmacion: () -> Unit = {},
    val alPedirEliminar: (BloqueoDto) -> Unit = {},
    val alConfirmarEliminar: () -> Unit = {},
    val alCancelarEliminar: () -> Unit = {},
)

/** Sección «Bloqueos» de la pantalla 9 (RF-05). */
@Composable
fun BloqueosSeccion(estado: EstadoBloqueos, acciones: AccionesBloqueos, hoy: LocalDate) {
    if (mostrarCargaOError(estado.cargando, estado.errorCarga, acciones.alReintentar)) return
    LazyColumn(
        modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Ayuda(stringResource(R.string.ayuda_bloqueos)) }
        if (estado.formulario == null) estado.error?.let { item { Aviso(textoDe(it)) } }
        if (estado.bloqueos.isEmpty()) item { Ayuda(stringResource(R.string.sin_bloqueos)) }
        items(estado.bloqueos, key = { it.id }) { b ->
            Tarjeta {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        val fecha = runCatching { LocalDate.parse(b.fecha).format(FORMATO_FECHA) }.getOrDefault(b.fecha)
                        Text(fecha.replaceFirstChar { it.titlecase(Locale.forLanguageTag("es-EC")) },
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.rango_horas, b.inicio, b.fin), style = MaterialTheme.typography.bodyMedium)
                        Text(estado.nombreCancha(b.canchaId) ?: stringResource(R.string.todas_las_canchas),
                            style = MaterialTheme.typography.bodyMedium)
                        Text(b.motivo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { acciones.alPedirEliminar(b) }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.accion_quitar))
                    }
                }
            }
        }
        item {
            Button(
                onClick = acciones.alNuevo,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag(EtiquetasHorarios.NUEVO_BLOQUEO),
            ) { Text(stringResource(R.string.accion_nuevo_bloqueo)) }
        }
    }

    estado.formulario?.let { FormularioBloqueoDialogo(it, estado, acciones, hoy) }
    estado.confirmacion?.let { mensaje ->
        Confirmar(stringResource(R.string.titulo_confirmar_bloqueo), mensaje, stringResource(R.string.accion_bloquear),
            acciones.alConfirmar, acciones.alCancelarConfirmacion)
    }
    estado.porEliminar?.let {
        Confirmar(stringResource(R.string.titulo_quitar_bloqueo), stringResource(R.string.texto_quitar_bloqueo),
            stringResource(R.string.accion_quitar), acciones.alConfirmarEliminar, acciones.alCancelarEliminar)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormularioBloqueoDialogo(f: FormularioBloqueo, estado: EstadoBloqueos, acciones: AccionesBloqueos, hoy: LocalDate) {
    var eligiendoFecha by rememberSaveable { mutableStateOf(false) }
    var eligiendoCancha by remember { mutableStateOf(false) }
    val guardando = estado.guardando

    AlertDialog(
        onDismissRequest = acciones.alCerrar,
        title = { Text(stringResource(R.string.titulo_nuevo_bloqueo)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { eligiendoFecha = true }, enabled = !guardando,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.campo_fecha) + ": " + (f.fecha?.format(FORMATO_FECHA) ?: "--"))
                }
                Box {
                    OutlinedButton(onClick = { eligiendoCancha = true }, enabled = !guardando,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.campo_cancha) + ": " +
                            (estado.nombreCancha(f.canchaId) ?: stringResource(R.string.todas_las_canchas)))
                    }
                    DropdownMenu(expanded = eligiendoCancha, onDismissRequest = { eligiendoCancha = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.todas_las_canchas)) }, onClick = {
                            acciones.alCambiarFormulario { copy(canchaId = null) }
                            eligiendoCancha = false
                        })
                        estado.canchas.forEach { c ->
                            DropdownMenuItem(text = { Text(c.nombre) }, onClick = {
                                acciones.alCambiarFormulario { copy(canchaId = c.id) }
                                eligiendoCancha = false
                            })
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelectorHora(stringResource(R.string.campo_desde), f.inicio,
                        { h -> acciones.alCambiarFormulario { copy(inicio = h) } }, Modifier.weight(1f), !guardando)
                    SelectorHora(stringResource(R.string.campo_hasta), f.fin,
                        { h -> acciones.alCambiarFormulario { copy(fin = h) } }, Modifier.weight(1f), !guardando)
                }
                OutlinedTextField(
                    value = f.motivo,
                    onValueChange = { v -> acciones.alCambiarFormulario { copy(motivo = v.take(200)) } },
                    label = { Text(stringResource(R.string.campo_motivo)) },
                    placeholder = { Text(stringResource(R.string.ejemplo_motivo)) },
                    enabled = !guardando,
                    modifier = Modifier.fillMaxWidth(),
                )
                estado.error?.let { Aviso(textoDe(it)) }
            }
        },
        confirmButton = {
            TextButton(onClick = acciones.alGuardar, enabled = !guardando,
                modifier = Modifier.testTag(EtiquetasHorarios.GUARDAR_BLOQUEO)) {
                Text(stringResource(if (guardando) R.string.guardando_cambios else R.string.accion_bloquear))
            }
        },
        dismissButton = { TextButton(onClick = acciones.alCerrar) { Text(stringResource(R.string.accion_cancelar)) } },
    )

    if (eligiendoFecha) {
        val hoyMillis = hoy.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        val selector = rememberDatePickerState(
            initialSelectedDateMillis = f.fecha?.atStartOfDay()?.toInstant(ZoneOffset.UTC)?.toEpochMilli() ?: hoyMillis,
            // Ayuda de escritura: no se ofrecen fechas pasadas. Igual las rechaza el backend.
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= hoyMillis
            },
        )
        DatePickerDialog(
            onDismissRequest = { eligiendoFecha = false },
            confirmButton = {
                TextButton(onClick = {
                    selector.selectedDateMillis?.let { millis ->
                        val fecha = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        acciones.alCambiarFormulario { copy(fecha = fecha) }
                    }
                    eligiendoFecha = false
                }) { Text(stringResource(R.string.accion_aceptar)) }
            },
            dismissButton = { TextButton(onClick = { eligiendoFecha = false }) { Text(stringResource(R.string.accion_cancelar)) } },
        ) { DatePicker(state = selector) }
    }
}

@Composable
private fun Confirmar(titulo: String, texto: String, accion: String, alConfirmar: () -> Unit, alCancelar: () -> Unit) {
    AlertDialog(
        onDismissRequest = alCancelar,
        title = { Text(titulo) },
        text = { Text(texto) },
        confirmButton = { TextButton(onClick = alConfirmar) { Text(accion) } },
        dismissButton = { TextButton(onClick = alCancelar) { Text(stringResource(R.string.accion_cancelar)) } },
    )
}
