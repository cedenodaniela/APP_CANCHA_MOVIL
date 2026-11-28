package com.canchapp.movil.ui.componentes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.canchapp.movil.R
import com.canchapp.movil.ui.theme.CanchAppTheme

/** Etiquetas para las pruebas de pantalla. */
object EtiquetasPantallaPendiente {
    const val SALIR = "pendiente_salir"
}

/**
 * Destino provisional mientras no existe la pantalla real de un RF.
 * Permite navegar por roles desde el primer día sin inventar funcionalidad.
 *
 * @param requerimiento RF que construirá la pantalla real, por ejemplo «RF-06 y RF-07»
 * @param alAbrirMenu si no es nulo, muestra el ícono ☰ del menú del administrador
 * @param alSalir si no es nulo, muestra el botón «Salir», que pregunta antes de cerrar la sesión
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPendiente(
    titulo: String,
    requerimiento: String,
    alAbrirMenu: (() -> Unit)? = null,
    alSalir: (() -> Unit)? = null,
) {
    var confirmandoSalida by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titulo) },
                navigationIcon = { alAbrirMenu?.let { BotonMenu(it) } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { relleno ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.pendiente_rf, requerimiento), style = MaterialTheme.typography.bodyLarge)
            if (alSalir != null) {
                // Área táctil de al menos 48 dp (RNF-05).
                Button(
                    onClick = { confirmandoSalida = true },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag(EtiquetasPantallaPendiente.SALIR),
                ) {
                    Text(stringResource(R.string.accion_salir))
                }
            }
        }
    }

    if (confirmandoSalida && alSalir != null) {
        DialogoCerrarSesion(
            alConfirmar = {
                confirmandoSalida = false
                alSalir()
            },
            alCancelar = { confirmandoSalida = false },
        )
    }
}

@Preview
@Composable
private fun PantallaPendientePreview() {
    CanchAppTheme {
        PantallaPendiente(titulo = "Reservar", requerimiento = "RF-06 y RF-07", alSalir = {})
    }
}
