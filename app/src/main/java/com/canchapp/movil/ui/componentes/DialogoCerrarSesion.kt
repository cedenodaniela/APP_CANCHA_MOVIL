package com.canchapp.movil.ui.componentes

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.canchapp.movil.R
import com.canchapp.movil.ui.theme.CanchAppTheme

/** Etiquetas para las pruebas de pantalla. */
object EtiquetasCerrarSesion {
    const val CONFIRMAR = "cerrar_sesion_si"
    const val CANCELAR = "cerrar_sesion_no"
}

/**
 * Pregunta antes de cerrar la sesión, para que un toque sin querer en «Salir» no obligue a
 * escribir otra vez el correo y la contraseña.
 */
@Composable
fun DialogoCerrarSesion(alConfirmar: () -> Unit, alCancelar: () -> Unit) {
    AlertDialog(
        onDismissRequest = alCancelar,
        title = { Text(stringResource(R.string.titulo_cerrar_sesion)) },
        text = { Text(stringResource(R.string.texto_cerrar_sesion)) },
        confirmButton = {
            TextButton(onClick = alConfirmar, modifier = Modifier.testTag(EtiquetasCerrarSesion.CONFIRMAR)) {
                Text(stringResource(R.string.accion_si_salir))
            }
        },
        dismissButton = {
            TextButton(onClick = alCancelar, modifier = Modifier.testTag(EtiquetasCerrarSesion.CANCELAR)) {
                Text(stringResource(R.string.accion_no))
            }
        },
    )
}

@Preview
@Composable
private fun DialogoCerrarSesionPreview() {
    CanchAppTheme { DialogoCerrarSesion({}, {}) }
}
