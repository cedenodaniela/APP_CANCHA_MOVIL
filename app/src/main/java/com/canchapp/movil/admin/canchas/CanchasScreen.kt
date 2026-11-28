package com.canchapp.movil.admin.canchas

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.canchapp.movil.R
import com.canchapp.movil.admin.canchas.data.CanchaDto
import com.canchapp.movil.core.red.MensajeError
import com.canchapp.movil.ui.componentes.Aviso
import com.canchapp.movil.ui.componentes.textoDe
import com.canchapp.movil.ui.theme.CanchAppTheme
import com.canchapp.movil.ui.theme.Verde
import java.time.LocalDate
import java.time.ZoneId

/** Pestañas de la pantalla 9: canchas (RF-04), y horario, tarifas y bloqueos (RF-05). */
enum class PestanaCanchas(@StringRes val titulo: Int) {
    CANCHAS(R.string.pestana_canchas),
    HORARIO(R.string.pestana_horario),
    TARIFAS(R.string.pestana_tarifas),
    BLOQUEOS(R.string.pestana_bloqueos),
}

/** Zona del complejo: «hoy» para los bloqueos es el de Calceta, no el del celular. */
private val ZONA_COMPLEJO: ZoneId = ZoneId.of("America/Guayaquil")

/** Etiquetas para las pruebas de pantalla. */
object EtiquetasCanchas {
    fun pestana(p: PestanaCanchas) = "canchas_pestana_${p.name}"
    const val NUEVA = "canchas_nueva"
    const val REINTENTAR = "canchas_reintentar"
    fun estado(id: Long) = "canchas_estado_$id"
    fun tarjeta(id: Long) = "canchas_tarjeta_$id"
}

/** Pantalla 9 · Canchas del administrador (RF-04), conectada a su ViewModel. */
@Composable
fun CanchasScreen(
    alVolver: () -> Unit,
    alNueva: () -> Unit,
    alEditar: (id: Long) -> Unit,
    viewModel: CanchasViewModel = hiltViewModel(),
    horarioVm: HorarioViewModel = hiltViewModel(),
    tarifasVm: TarifasViewModel = hiltViewModel(),
    bloqueosVm: BloqueosViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    var pestana by rememberSaveable { mutableStateOf(PestanaCanchas.CANCHAS) }

    // Cada vez que se abre una pestaña (o se vuelve del formulario), se actualiza.
    LaunchedEffect(pestana) {
        when (pestana) {
            PestanaCanchas.CANCHAS -> viewModel.cargar()
            PestanaCanchas.HORARIO -> horarioVm.cargar()
            PestanaCanchas.TARIFAS -> tarifasVm.cargar()
            PestanaCanchas.BLOQUEOS -> bloqueosVm.cargar()
        }
    }

    CanchasContenido(
        pestana = pestana,
        alCambiarPestana = { pestana = it },
        otraPestana = { actual ->
            when (actual) {
                PestanaCanchas.HORARIO -> {
                    val e by horarioVm.estado.collectAsStateWithLifecycle()
                    HorarioSeccion(e, horarioVm::editarDia, horarioVm::guardar, horarioVm::cargar)
                }
                PestanaCanchas.TARIFAS -> {
                    val e by tarifasVm.estado.collectAsStateWithLifecycle()
                    TarifasSeccion(
                        e,
                        AccionesTarifas(
                            alReintentar = tarifasVm::cargar,
                            alNueva = tarifasVm::nueva,
                            alEditar = tarifasVm::editarTarifa,
                            alCambiarFormulario = tarifasVm::editar,
                            alGuardar = tarifasVm::guardar,
                            alCerrar = tarifasVm::cerrarFormulario,
                            alPedirEliminar = tarifasVm::pedirEliminar,
                            alConfirmarEliminar = tarifasVm::confirmarEliminar,
                            alCancelarEliminar = tarifasVm::cancelarEliminar,
                        ),
                    )
                }
                PestanaCanchas.BLOQUEOS -> {
                    val e by bloqueosVm.estado.collectAsStateWithLifecycle()
                    val hoy = LocalDate.now(ZONA_COMPLEJO)
                    BloqueosSeccion(
                        e,
                        AccionesBloqueos(
                            alReintentar = bloqueosVm::cargar,
                            alNuevo = { bloqueosVm.nuevo(hoy) },
                            alCambiarFormulario = bloqueosVm::editar,
                            alGuardar = bloqueosVm::guardar,
                            alCerrar = bloqueosVm::cerrarFormulario,
                            alConfirmar = bloqueosVm::confirmar,
                            alCancelarConfirmacion = bloqueosVm::cancelarConfirmacion,
                            alPedirEliminar = bloqueosVm::pedirEliminar,
                            alConfirmarEliminar = bloqueosVm::confirmarEliminar,
                            alCancelarEliminar = bloqueosVm::cancelarEliminar,
                        ),
                        hoy,
                    )
                }
                PestanaCanchas.CANCHAS -> Unit
            }
        },
        estado = estado,
        acciones = AccionesCanchas(
            alVolver = alVolver,
            alNueva = alNueva,
            alEditar = alEditar,
            alReintentar = viewModel::cargar,
            alCambiarEstado = viewModel::cambiarEstado,
            alConfirmarDesactivar = viewModel::confirmarDesactivar,
            alCancelarConfirmacion = viewModel::cancelarConfirmacion,
        ),
    )
}

/** Lo que la persona puede hacer en la lista de canchas. */
data class AccionesCanchas(
    val alVolver: () -> Unit = {},
    val alNueva: () -> Unit = {},
    val alEditar: (Long) -> Unit = {},
    val alReintentar: () -> Unit = {},
    val alCambiarEstado: (id: Long, activa: Boolean) -> Unit = { _, _ -> },
    val alConfirmarDesactivar: () -> Unit = {},
    val alCancelarConfirmacion: () -> Unit = {},
)

/** Lista de canchas sin estado propio: se prueba y se previsualiza con cualquier [EstadoCanchas]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanchasContenido(
    estado: EstadoCanchas,
    acciones: AccionesCanchas,
    pestana: PestanaCanchas = PestanaCanchas.CANCHAS,
    alCambiarPestana: (PestanaCanchas) -> Unit = {},
    otraPestana: @Composable (PestanaCanchas) -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.titulo_canchas)) },
                navigationIcon = {
                    IconButton(onClick = acciones.alVolver) {
                        Icon(painterResource(R.drawable.ic_atras), contentDescription = stringResource(R.string.accion_volver))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        floatingActionButton = {
            if (pestana == PestanaCanchas.CANCHAS) ExtendedFloatingActionButton(
                onClick = acciones.alNueva,
                icon = { Icon(painterResource(R.drawable.ic_agregar), contentDescription = null) },
                text = { Text(stringResource(R.string.accion_nueva_cancha)) },
                modifier = Modifier.testTag(EtiquetasCanchas.NUEVA),
            )
        },
    ) { relleno ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(relleno),
        ) {
        PrimaryTabRow(selectedTabIndex = pestana.ordinal) {
            PestanaCanchas.entries.forEach { p ->
                Tab(
                    selected = pestana == p,
                    onClick = { alCambiarPestana(p) },
                    text = { Text(stringResource(p.titulo)) },
                    modifier = Modifier.testTag(EtiquetasCanchas.pestana(p)),
                )
            }
        }
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            when {
                pestana != PestanaCanchas.CANCHAS -> otraPestana(pestana)
                estado.cargando -> Column(
                    Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.cargando_canchas))
                }
                estado.errorCarga != null -> Column(
                    Modifier
                        .widthIn(max = 560.dp)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Aviso(textoDe(estado.errorCarga))
                    OutlinedButton(
                        onClick = acciones.alReintentar,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag(EtiquetasCanchas.REINTENTAR),
                    ) { Text(stringResource(R.string.accion_reintentar)) }
                }
                else -> Lista(estado, acciones)
            }
        }
        }
    }

    estado.confirmacion?.let { pendiente ->
        AlertDialog(
            onDismissRequest = acciones.alCancelarConfirmacion,
            title = { Text(stringResource(R.string.titulo_confirmar_desactivar, pendiente.nombre)) },
            text = { Text(pendiente.mensaje) },
            confirmButton = {
                TextButton(onClick = acciones.alConfirmarDesactivar) { Text(stringResource(R.string.accion_desactivar)) }
            },
            dismissButton = {
                TextButton(onClick = acciones.alCancelarConfirmacion) { Text(stringResource(R.string.accion_cancelar)) }
            },
        )
    }
}

@Composable
private fun Lista(estado: EstadoCanchas, acciones: AccionesCanchas) {
    LazyColumn(
        modifier = Modifier
            .widthIn(max = 640.dp)
            .fillMaxWidth(),
        // Espacio abajo para que el botón flotante no tape la última cancha.
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        estado.error?.let { error -> item { Aviso(textoDe(error)) } }
        if (estado.canchas.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.sin_canchas),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(estado.canchas, key = { it.cancha.id }) { item ->
            TarjetaCancha(item, cambiando = item.cancha.id in estado.cambiando, acciones = acciones)
        }
    }
}

@Composable
private fun TarjetaCancha(item: CanchaItem, cambiando: Boolean, acciones: AccionesCanchas) {
    val cancha = item.cancha
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { acciones.alEditar(cancha.id) }
            .testTag(EtiquetasCanchas.tarjeta(cancha.id)),
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FotoCancha(item.fotoUrl, cancha.nombre, Modifier.size(width = 96.dp, height = 72.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(cancha.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(R.string.detalle_cancha, cancha.tipoCesped, cancha.anchoMetros, cancha.largoMetros),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                EtiquetaEstado(cancha.activa)
            }
            val descripcion = stringResource(
                if (cancha.activa) R.string.descripcion_desactivar_cancha else R.string.descripcion_activar_cancha,
                cancha.nombre,
            )
            if (cambiando) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Switch(
                    checked = cancha.activa,
                    onCheckedChange = { acciones.alCambiarEstado(cancha.id, it) },
                    modifier = Modifier
                        .testTag(EtiquetasCanchas.estado(cancha.id))
                        .semantics { contentDescription = descripcion },
                )
            }
        }
    }
}

/** Estado con texto y color, nunca solo color (AGENTS.md §6). */
@Composable
private fun EtiquetaEstado(activa: Boolean) {
    val color = if (activa) Verde else MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        stringResource(if (activa) R.string.estado_activa else R.string.estado_inactiva),
        style = MaterialTheme.typography.labelLarge,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

/** Foto de la cancha, o un recuadro con un ícono si todavía no tiene. */
@Composable
fun FotoCancha(url: Any?, nombre: String, modifier: Modifier = Modifier) {
    val forma = RoundedCornerShape(12.dp)
    if (url == null) {
        Box(
            modifier
                .clip(forma)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(R.drawable.ic_imagen),
                contentDescription = stringResource(R.string.sin_foto),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        AsyncImage(
            model = url,
            contentDescription = stringResource(R.string.descripcion_foto_cancha, nombre),
            contentScale = ContentScale.Crop,
            modifier = modifier
                .clip(forma)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
    }
}

@Preview(showSystemUi = true)
@Composable
private fun CanchasPreview() {
    CanchAppTheme {
        CanchasContenido(
            EstadoCanchas(
                cargando = false,
                canchas = listOf(
                    CanchaItem(CanchaDto(1, "Cancha 1", "Sintético", 30, 50, true), null),
                    CanchaItem(CanchaDto(2, "Cancha 2", "Sintético", 30, 50, false), null),
                ),
                error = MensajeError.SinConexion,
            ),
            AccionesCanchas(),
        )
    }
}
