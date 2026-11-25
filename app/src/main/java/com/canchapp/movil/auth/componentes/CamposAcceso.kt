package com.canchapp.movil.auth.componentes

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.canchapp.movil.R

/*
 * Campos y botones comunes de las pantallas de acceso: el login y el registro se ven y se
 * comportan igual. Los errores se muestran con texto debajo del campo, no solo en rojo.
 */

/**
 * Campo de texto de una sola línea.
 *
 * @param error texto del error, o `null` si el campo está bien
 * @param ayuda texto de apoyo cuando no hay error
 * @param tipoContenido para que el autocompletado de Android sepa qué ofrecer
 */
@Composable
fun CampoAcceso(
    valor: String,
    alCambiar: (String) -> Unit,
    etiqueta: String,
    @DrawableRes icono: Int,
    tipoContenido: ContentType,
    etiquetaPrueba: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    ayuda: String? = null,
    ejemplo: String? = null,
    habilitado: Boolean = true,
    teclado: KeyboardOptions = KeyboardOptions.Default,
    acciones: KeyboardActions = KeyboardActions.Default,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(etiqueta) },
        placeholder = ejemplo?.let { { Text(it) } },
        leadingIcon = { Icon(painterResource(icono), contentDescription = null) },
        isError = error != null,
        supportingText = (error ?: ayuda)?.let { { Text(it) } },
        singleLine = true,
        enabled = habilitado,
        keyboardOptions = teclado,
        keyboardActions = acciones,
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentType = tipoContenido }
            .testTag(etiquetaPrueba),
    )
}

/** Contraseña con el botón para mostrarla u ocultarla. */
@Composable
fun CampoContrasena(
    valor: String,
    alCambiar: (String) -> Unit,
    etiqueta: String,
    visible: Boolean,
    alAlternarVisible: () -> Unit,
    tipoContenido: ContentType,
    etiquetaPrueba: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    ayuda: String? = null,
    habilitado: Boolean = true,
    accionTeclado: ImeAction = ImeAction.Done,
    acciones: KeyboardActions = KeyboardActions.Default,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(etiqueta) },
        leadingIcon = { Icon(painterResource(R.drawable.ic_candado), contentDescription = null) },
        trailingIcon = {
            // IconButton ya tiene un área táctil de 48 dp (RNF-05).
            IconButton(onClick = alAlternarVisible) {
                Icon(
                    painter = painterResource(if (visible) R.drawable.ic_oculto else R.drawable.ic_visible),
                    contentDescription = stringResource(if (visible) R.string.ocultar_contrasena else R.string.mostrar_contrasena),
                )
            }
        },
        isError = error != null,
        supportingText = (error ?: ayuda)?.let { { Text(it) } },
        singleLine = true,
        enabled = habilitado,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = accionTeclado,
            autoCorrectEnabled = false,
        ),
        keyboardActions = acciones,
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentType = tipoContenido }
            .testTag(etiquetaPrueba),
    )
}

/** Botón principal de la pantalla; mientras espera al backend, muestra el progreso y no se puede pulsar. */
@Composable
fun BotonAcceso(
    texto: String,
    textoCargando: String,
    cargando: Boolean,
    alPulsar: () -> Unit,
    etiquetaPrueba: String,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = alPulsar,
        enabled = !cargando,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .testTag(etiquetaPrueba),
    ) {
        if (cargando) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(Modifier.width(12.dp))
            Text(textoCargando)
        } else {
            Text(texto)
        }
    }
}

/** Pregunta y enlace al pie de la tarjeta, por ejemplo «¿No tienes cuenta? Crear cuenta». */
@Composable
fun EnlaceAcceso(pregunta: String, accion: String, alPulsar: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = pregunta,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = alPulsar, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(accion)
        }
    }
}
