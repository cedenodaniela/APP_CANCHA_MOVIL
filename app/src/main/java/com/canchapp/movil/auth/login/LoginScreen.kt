package com.canchapp.movil.auth.login

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.canchapp.movil.R
import com.canchapp.movil.auth.componentes.PantallaAcceso
import com.canchapp.movil.core.red.MensajeError
import com.canchapp.movil.ui.componentes.Aviso
import com.canchapp.movil.ui.componentes.textoDe
import com.canchapp.movil.ui.theme.CanchAppTheme

/** Etiquetas para las pruebas de pantalla. */
object EtiquetasLogin {
    const val CORREO = "login_correo"
    const val CONTRASENA = "login_contrasena"
    const val INGRESAR = "login_ingresar"
}

/** Pantalla 1 · Inicio de sesión (RF-01), conectada a su ViewModel. */
@Composable
fun LoginScreen(alCrearCuenta: () -> Unit, viewModel: LoginViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LoginContenido(
        estado = estado,
        alCambiarEmail = viewModel::alCambiarEmail,
        alCambiarPassword = viewModel::alCambiarPassword,
        alAlternarPassword = viewModel::alternarPasswordVisible,
        alIngresar = viewModel::iniciarSesion,
        alCrearCuenta = alCrearCuenta,
    )
}

/** Inicio de sesión sin estado propio: se prueba y se previsualiza con cualquier [EstadoLogin]. */
@Composable
fun LoginContenido(
    estado: EstadoLogin,
    alCambiarEmail: (String) -> Unit,
    alCambiarPassword: (String) -> Unit,
    alAlternarPassword: () -> Unit,
    alIngresar: () -> Unit,
    alCrearCuenta: () -> Unit,
) {
    val foco = LocalFocusManager.current

    PantallaAcceso(
        titulo = stringResource(R.string.titulo_inicio_sesion),
        subtitulo = stringResource(R.string.subtitulo_inicio_sesion),
    ) {
        estado.error?.let { Aviso(textoDe(it)) }

        OutlinedTextField(
            value = estado.email,
            onValueChange = alCambiarEmail,
            label = { Text(stringResource(R.string.campo_correo)) },
            placeholder = { Text(stringResource(R.string.ejemplo_correo)) },
            leadingIcon = { Icon(painterResource(R.drawable.ic_correo), contentDescription = null) },
            isError = estado.emailInvalido,
            supportingText = if (estado.emailInvalido) {
                { Text(stringResource(R.string.error_correo)) }
            } else {
                null
            },
            singleLine = true,
            enabled = !estado.cargando,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                autoCorrectEnabled = false,
            ),
            keyboardActions = KeyboardActions(onNext = { foco.moveFocus(FocusDirection.Down) }),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentType = ContentType.EmailAddress }
                .testTag(EtiquetasLogin.CORREO),
        )

        OutlinedTextField(
            value = estado.password,
            onValueChange = alCambiarPassword,
            label = { Text(stringResource(R.string.campo_contrasena)) },
            leadingIcon = { Icon(painterResource(R.drawable.ic_candado), contentDescription = null) },
            trailingIcon = {
                // IconButton ya tiene un área táctil de 48 dp (RNF-05).
                IconButton(onClick = alAlternarPassword) {
                    Icon(
                        painter = painterResource(if (estado.passwordVisible) R.drawable.ic_oculto else R.drawable.ic_visible),
                        contentDescription = stringResource(
                            if (estado.passwordVisible) R.string.ocultar_contrasena else R.string.mostrar_contrasena,
                        ),
                    )
                }
            },
            isError = estado.passwordVacia,
            supportingText = if (estado.passwordVacia) {
                { Text(stringResource(R.string.error_contrasena)) }
            } else {
                null
            },
            singleLine = true,
            enabled = !estado.cargando,
            visualTransformation = if (estado.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                autoCorrectEnabled = false,
            ),
            keyboardActions = KeyboardActions(onDone = {
                foco.clearFocus()
                alIngresar()
            }),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentType = ContentType.Password }
                .testTag(EtiquetasLogin.CONTRASENA),
        )

        Button(
            onClick = {
                foco.clearFocus()
                alIngresar()
            },
            enabled = !estado.cargando,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .testTag(EtiquetasLogin.INGRESAR),
        ) {
            if (estado.cargando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.ingresando))
            } else {
                Text(stringResource(R.string.accion_ingresar))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.sin_cuenta),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = alCrearCuenta, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.accion_crear_cuenta))
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun LoginPreview() {
    CanchAppTheme {
        LoginContenido(EstadoLogin(), {}, {}, {}, {}, {})
    }
}

@Preview(showSystemUi = true)
@Composable
private fun LoginConErrorPreview() {
    CanchAppTheme {
        LoginContenido(
            EstadoLogin(
                email = "ana@correo.com",
                password = "clave",
                error = MensajeError.DelServidor("CREDENCIALES_INVALIDAS", "Correo o contraseña incorrectos"),
            ),
            {}, {}, {}, {}, {},
        )
    }
}
