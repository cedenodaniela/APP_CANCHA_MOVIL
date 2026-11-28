package com.canchapp.movil.admin.canchas

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.canchapp.movil.R
import com.canchapp.movil.core.red.MensajeError
import com.canchapp.movil.ui.componentes.Aviso
import com.canchapp.movil.ui.componentes.textoDe
import com.canchapp.movil.ui.theme.CanchAppTheme
import java.io.File

/** Etiquetas para las pruebas de pantalla. */
object EtiquetasFormularioCancha {
    const val NOMBRE = "cancha_nombre"
    const val ANCHO = "cancha_ancho"
    const val LARGO = "cancha_largo"
    const val CAMARA = "cancha_camara"
    const val GALERIA = "cancha_galeria"
    const val GUARDAR = "cancha_guardar"
}

/**
 * Pantalla 9 · Alta y edición de una cancha (RF-04), conectada a su ViewModel.
 *
 * @param id la cancha a editar, o `null` para crear una
 * @param alGuardada se llama una vez, cuando todo se guardó, para volver a la lista
 */
@Composable
fun CanchaFormularioScreen(
    id: Long?,
    alVolver: () -> Unit,
    alGuardada: () -> Unit,
    viewModel: CanchaFormularioViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val contexto = LocalContext.current

    LaunchedEffect(id) { viewModel.iniciar(id) }
    LaunchedEffect(estado.guardada) { if (estado.guardada) alGuardada() }

    // Galería: el Photo Picker del sistema no pide permisos.
    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(viewModel::elegirFoto)
    }
    // Cámara: guarda en un archivo de la app; no hace falta el permiso CAMERA.
    var fotoCamara by rememberSaveable { mutableStateOf<Uri?>(null) }
    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { tomada ->
        if (tomada) fotoCamara?.let(viewModel::elegirFoto)
    }

    CanchaFormularioContenido(
        estado = estado,
        acciones = AccionesFormularioCancha(
            alVolver = alVolver,
            alReintentar = { id?.let(viewModel::cargar) },
            alCambiarNombre = viewModel::alCambiarNombre,
            alCambiarTipoCesped = viewModel::alCambiarTipoCesped,
            alCambiarAncho = viewModel::alCambiarAncho,
            alCambiarLargo = viewModel::alCambiarLargo,
            alTomarFoto = {
                val destino = archivoParaFoto(contexto)
                fotoCamara = destino
                camara.launch(destino)
            },
            alElegirGaleria = {
                galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            alGuardar = viewModel::guardar,
        ),
    )
}

/** Archivo temporal para la cámara, en la carpeta que comparte el FileProvider (`res/xml/rutas_archivos.xml`). */
private fun archivoParaFoto(contexto: Context): Uri {
    val carpeta = File(contexto.cacheDir, "fotos").apply { mkdirs() }
    val archivo = File(carpeta, "cancha_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(contexto, "${contexto.packageName}.archivos", archivo)
}

/** Lo que la persona puede hacer en el formulario de la cancha. */
data class AccionesFormularioCancha(
    val alVolver: () -> Unit = {},
    val alReintentar: () -> Unit = {},
    val alCambiarNombre: (String) -> Unit = {},
    val alCambiarTipoCesped: (String) -> Unit = {},
    val alCambiarAncho: (String) -> Unit = {},
    val alCambiarLargo: (String) -> Unit = {},
    val alTomarFoto: () -> Unit = {},
    val alElegirGaleria: () -> Unit = {},
    val alGuardar: () -> Unit = {},
)

/** Formulario sin estado propio: se prueba y se previsualiza con cualquier [EstadoFormularioCancha]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanchaFormularioContenido(estado: EstadoFormularioCancha, acciones: AccionesFormularioCancha) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(if (estado.esNueva) R.string.titulo_nueva_cancha else R.string.titulo_editar_cancha))
                },
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
    ) { relleno ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(relleno),
            contentAlignment = Alignment.TopCenter,
        ) {
            when {
                estado.cargando -> CircularProgressIndicator(Modifier.padding(32.dp))
                estado.errorCarga != null -> Column(
                    Modifier
                        .widthIn(max = 560.dp)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Aviso(textoDe(estado.errorCarga))
                    OutlinedButton(onClick = acciones.alReintentar, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.accion_reintentar))
                    }
                }
                else -> Formulario(estado, acciones)
            }
        }
    }
}

@Composable
private fun Formulario(estado: EstadoFormularioCancha, acciones: AccionesFormularioCancha) {
    val habilitado = !estado.guardando
    Column(
        modifier = Modifier
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.seccion_foto), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        FotoCancha(
            url = estado.fotoNueva ?: estado.fotoActual,
            nombre = estado.nombre,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
        )
        Text(
            stringResource(R.string.ayuda_foto_cancha),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = acciones.alTomarFoto,
                enabled = habilitado,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag(EtiquetasFormularioCancha.CAMARA),
            ) {
                Icon(painterResource(R.drawable.ic_camara), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.accion_tomar_foto))
            }
            OutlinedButton(
                onClick = acciones.alElegirGaleria,
                enabled = habilitado,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag(EtiquetasFormularioCancha.GALERIA),
            ) {
                Icon(painterResource(R.drawable.ic_galeria), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.accion_elegir_galeria))
            }
        }

        Campo(estado.nombre, acciones.alCambiarNombre, R.string.campo_nombre_cancha, habilitado,
            Modifier.fillMaxWidth(), EtiquetasFormularioCancha.NOMBRE, mayusculas = true)
        Campo(estado.tipoCesped, acciones.alCambiarTipoCesped, R.string.campo_tipo_cesped, habilitado,
            Modifier.fillMaxWidth(), mayusculas = true)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Campo(estado.ancho, acciones.alCambiarAncho, R.string.campo_ancho, habilitado,
                Modifier.weight(1f), EtiquetasFormularioCancha.ANCHO, numero = true)
            Campo(estado.largo, acciones.alCambiarLargo, R.string.campo_largo, habilitado,
                Modifier.weight(1f), EtiquetasFormularioCancha.LARGO, numero = true)
        }

        if (estado.fotoIlegible) Aviso(stringResource(R.string.foto_ilegible))
        estado.error?.let { Aviso(textoDe(it)) }

        Button(
            onClick = acciones.alGuardar,
            enabled = habilitado,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .testTag(EtiquetasFormularioCancha.GUARDAR),
        ) {
            if (estado.guardando) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.guardando_cambios))
            } else {
                Text(stringResource(R.string.accion_guardar_cancha))
            }
        }
    }
}

@Composable
private fun Campo(
    valor: String,
    alCambiar: (String) -> Unit,
    etiqueta: Int,
    habilitado: Boolean,
    modifier: Modifier = Modifier,
    etiquetaPrueba: String? = null,
    numero: Boolean = false,
    mayusculas: Boolean = false,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(stringResource(etiqueta)) },
        enabled = habilitado,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (numero) KeyboardType.Number else KeyboardType.Text,
            capitalization = if (mayusculas) KeyboardCapitalization.Sentences else KeyboardCapitalization.None,
            imeAction = ImeAction.Next,
        ),
        modifier = modifier.then(if (etiquetaPrueba != null) Modifier.testTag(etiquetaPrueba) else Modifier),
    )
}

@Preview(showSystemUi = true)
@Composable
private fun CanchaFormularioPreview() {
    CanchAppTheme {
        CanchaFormularioContenido(
            EstadoFormularioCancha(
                id = 1,
                nombre = "Cancha 1",
                ancho = "30",
                largo = "50",
                error = MensajeError.DelServidor("CANCHA_REPETIDA", "Ya existe una cancha con ese nombre."),
            ),
            AccionesFormularioCancha(),
        )
    }
}
