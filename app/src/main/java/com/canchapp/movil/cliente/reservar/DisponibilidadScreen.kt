package com.canchapp.movil.cliente.reservar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.canchapp.movil.R
import com.canchapp.movil.admin.canchas.data.CanchaDto
import com.canchapp.movil.cliente.reservar.data.BloqueDisponibleDto
import com.canchapp.movil.core.red.MensajeError
import com.canchapp.movil.ui.componentes.Aviso
import com.canchapp.movil.ui.componentes.textoDe
import com.canchapp.movil.ui.theme.Azul
import com.canchapp.movil.ui.theme.CanchAppTheme
import com.canchapp.movil.ui.theme.Rojo
import com.canchapp.movil.ui.theme.Verde
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Etiquetas para las pruebas de pantalla. */
object EtiquetasDisponibilidad {
    const val CANCHA = "disponibilidad_cancha"
    const val FECHA = "disponibilidad_fecha"
    const val BLOQUE = "disponibilidad_bloque"
    const val REINTENTAR = "disponibilidad_reintentar"
    const val SALIR = "disponibilidad_salir"
}

/** Lo que la persona puede hacer en la pantalla de disponibilidad. */
data class AccionesDisponibilidad(
    val alSalir: () -> Unit = { },
    val alCambiarCancha: (Long?) -> Unit = { },
    val alCambiarFecha: (LocalDate) -> Unit = { },
    val alSeleccionarBloque: (BloqueDisponibleDto) -> Unit = { },
    val alReintentarCanchas: () -> Unit = { },
    val alReintentarDisponibilidad: () -> Unit = { },
)

/** Formato con el que se muestra la fecha consultada («27/11/2026»). */
private val FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("es-EC"))

/**
 * Pantalla 2 · Disponibilidad y reserva del cliente (RF-06, RF-07).
 *
 * Muestra el selector de fecha y de cancha, y la grilla de bloques con el estado y el precio que
 * manda el backend. La UI no calcula nada: solo muestra y deja elegir.
 */
@Composable
fun DisponibilidadScreen(
    alSalir: () -> Unit = {},
    viewModel: DisponibilidadViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    DisponibilidadContenido(
        estado = estado,
        acciones = AccionesDisponibilidad(
            alSalir = alSalir,
            alCambiarCancha = viewModel::cambiarCancha,
            alCambiarFecha = viewModel::cambiarFecha,
            alSeleccionarBloque = viewModel::seleccionarBloque,
            alReintentarCanchas = viewModel::cargarCanchas,
            alReintentarDisponibilidad = viewModel::reintentarDisponibilidad,
        ),
    )
}

/** Disponibilidad sin estado propio: se prueba y se previsualiza con cualquier [EstadoDisponibilidad]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisponibilidadContenido(
    estado: EstadoDisponibilidad,
    acciones: AccionesDisponibilidad,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.titulo_disponibilidad)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
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
                estado.errorCanchas != null -> ErrorDeCarga(estado.errorCanchas, acciones.alReintentarCanchas)
                estado.cargandoCanchas || estado.cargando -> Cargando()
                estado.error != null -> ErrorDeCarga(estado.error, acciones.alReintentarDisponibilidad)
                else -> Disponible(estado, acciones)
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
        Text(stringResource(R.string.cargando_disponibilidad), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ErrorDeCarga(error: MensajeError, alReintentar: () -> Unit) {
    Column(
        modifier = Modifier
            .widthIn(max = 560.dp)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Aviso(textoDe(error))
        OutlinedButton(
            onClick = alReintentar,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag(EtiquetasDisponibilidad.REINTENTAR),
        ) {
            Text(stringResource(R.string.accion_reintentar))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Disponible(
    estado: EstadoDisponibilidad,
    acciones: AccionesDisponibilidad,
) {
    val hoy = LocalDate.now(ZONA_COMPLEJO)
    var eligiendoFecha by rememberSaveable { mutableStateOf(false) }
    var eligiendoCancha by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedButton(
            onClick = { eligiendoFecha = true },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag(EtiquetasDisponibilidad.FECHA),
        ) {
            Text(stringResource(R.string.selector_fecha) + ": " + (estado.fecha?.format(FORMATO_FECHA) ?: ""))
        }

        SelectorCancha(
            estado = estado,
            eligiendoCancha = eligiendoCancha,
            setEligiendoCancha = { eligiendoCancha = it },
            acciones = acciones,
        )

        if (estado.error != null) Aviso(textoDe(estado.error))

        Text(
            stringResource(R.string.selector_cancha) + ": " + (nombreCancha(estado, estado.canchaId) ?: stringResource(R.string.todas_las_canchas)),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )

        if (estado.bloques.isEmpty()) {
            Text(stringResource(R.string.sin_disponibilidad), style = MaterialTheme.typography.bodyLarge)
        } else {
            estado.bloques.groupBy { it.canchaId }.forEach { grupo ->
                val canchaId = grupo.key
                val bloques = grupo.value.sortedBy { it.inicio }
                Text(
                    "${nombreCancha(estado, canchaId) ?: stringResource(R.string.todas_las_canchas)} · ${stringResource(R.string.selector_fecha)} ${estado.fecha?.format(FORMATO_FECHA)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    bloques.forEach { bloque ->
                        FilaBloque(
                            bloque = bloque,
                            seleccionado = estado.bloqueSeleccionado == bloque,
                            etiquetaPrueba = EtiquetasDisponibilidad.BLOQUE + "_" + bloque.canchaId + "_" + bloque.inicio,
                            alSeleccionar = acciones.alSeleccionarBloque,
                        )
                    }
                }
            }
        }

        Button(
            onClick = acciones.alSalir,
            enabled = !estado.cargando,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .testTag(EtiquetasDisponibilidad.SALIR),
        ) {
            Text(stringResource(R.string.accion_salir))
        }
    }

    if (eligiendoFecha) {
        val hoyMillis = hoy.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        val selector = rememberDatePickerState(
            initialSelectedDateMillis = estado.fecha?.atStartOfDay()?.toInstant(ZoneOffset.UTC)?.toEpochMilli() ?: hoyMillis,
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
                        acciones.alCambiarFecha(fecha)
                    }
                    eligiendoFecha = false
                }) { Text(stringResource(R.string.accion_aceptar)) }
            },
            dismissButton = { TextButton(onClick = { eligiendoFecha = false }) { Text(stringResource(R.string.accion_cancelar)) } },
        ) { DatePicker(state = selector) }
    }
}

@Composable
private fun SelectorCancha(
    estado: EstadoDisponibilidad,
    eligiendoCancha: Boolean,
    setEligiendoCancha: (Boolean) -> Unit,
    acciones: AccionesDisponibilidad,
) {
    Box {
        OutlinedButton(
            onClick = { setEligiendoCancha(true) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag(EtiquetasDisponibilidad.CANCHA),
        ) {
            Text(
                stringResource(R.string.selector_cancha) + ": " +
                    (estado.canchas.firstOrNull()?.nombre ?: stringResource(R.string.todas_las_canchas)),
            )
        }
        DropdownMenu(expanded = eligiendoCancha, onDismissRequest = { setEligiendoCancha(false) }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.todas_las_canchas)) }, onClick = {
                acciones.alCambiarCancha(null)
                setEligiendoCancha(false)
            })
            estado.canchas.forEach { c ->
                DropdownMenuItem(text = { Text(c.nombre) }, onClick = {
                    acciones.alCambiarCancha(c.id)
                    setEligiendoCancha(false)
                })
            }
        }
    }
}

/**
 * Una fila de bloque: color y texto con el estado (libre u ocupado) y el precio que manda el backend.
 * Solo se puede seleccionar un bloque libre; el backend es quien decide qué puede reservarse (RF-07).
 */
@Composable
private fun FilaBloque(
    bloque: BloqueDisponibleDto,
    seleccionado: Boolean,
    etiquetaPrueba: String,
    alSeleccionar: (BloqueDisponibleDto) -> Unit,
) {
    val color = when {
        bloque.ocupado -> Rojo
        seleccionado -> Azul
        else -> Verde
    }
    val colorContenido = when {
        bloque.ocupado -> MaterialTheme.colorScheme.onError
        seleccionado -> MaterialTheme.colorScheme.onSecondary
        else -> MaterialTheme.colorScheme.onPrimary
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color,
        contentColor = colorContenido,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable { if (!bloque.ocupado) alSeleccionar(bloque) }
            .testTag(etiquetaPrueba),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                "${bloque.inicio} - ${bloque.fin}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                stringResource(if (bloque.ocupado) R.string.ocupado else R.string.libre) + " · $${bloque.precio}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/** Nombre de la cancha para mostrar, o «Todas las canchas» cuando no hay una (id en `null`). */
private fun nombreCancha(estado: EstadoDisponibilidad, canchaId: Long?): String? =
    canchaId?.let { id -> estado.canchas.firstOrNull { it.id == id }?.nombre }

@Preview(showSystemUi = true)
@Composable
private fun DisponibilidadPreview() {
    CanchAppTheme {
        DisponibilidadContenido(
            estado = EstadoDisponibilidad(
                cargandoCanchas = false,
                canchas = listOf(CanchaDto(1, "Cancha 1", "Sintético", 30, 50, activa = true)),
                fecha = LocalDate.now(),
                bloques = listOf(BloqueDisponibleDto(1, "2026-11-28", "10:00", "10:30", false, "20.00")),
            ),
            acciones = AccionesDisponibilidad(),
        )
    }
}
