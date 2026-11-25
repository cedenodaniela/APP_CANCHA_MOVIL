package com.canchapp.movil.auth.login

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.canchapp.movil.R
import com.canchapp.movil.auth.componentes.BotonAcceso
import com.canchapp.movil.auth.componentes.CampoAcceso
import com.canchapp.movil.auth.componentes.CampoContrasena
import com.canchapp.movil.auth.componentes.EnlaceAcceso
import com.canchapp.movil.auth.componentes.PantallaAcceso
import com.canchapp.movil.core.red.MensajeError
import com.canchapp.movil.ui.componentes.Aviso
import com.canchapp.movil.ui.componentes.TipoAviso
import com.canchapp.movil.ui.componentes.textoDe
import com.canchapp.movil.ui.theme.CanchAppTheme

/** Etiquetas para las pruebas de pantalla. */
object EtiquetasLogin {
    const val CORREO = "login_correo"
    const val CONTRASENA = "login_contrasena"
    const val INGRESAR = "login_ingresar"
}

/**
 * Pantalla 1 · Inicio de sesión (RF-01), conectada a su ViewModel.
 *
 * @param correoRegistrado correo de la cuenta que se acaba de crear en el registro, o `null`
 * @param alMostrarRegistro avisa que ya se usó [correoRegistrado], para no repetir el aviso
 */
@Composable
fun LoginScreen(
    alCrearCuenta: () -> Unit,
    correoRegistrado: String? = null,
    alMostrarRegistro: () -> Unit = {},
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    LaunchedEffect(correoRegistrado) {
        correoRegistrado?.let { email ->
            viewModel.mostrarCuentaCreada(email)
            alMostrarRegistro()
        }
    }

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
    val ingresar = {
        foco.clearFocus()
        alIngresar()
    }

    PantallaAcceso(
        titulo = stringResource(R.string.titulo_inicio_sesion),
        subtitulo = stringResource(R.string.subtitulo_inicio_sesion),
    ) {
        if (estado.cuentaCreada) {
            Aviso(stringResource(R.string.cuenta_creada), tipo = TipoAviso.EXITO)
        }
        estado.error?.let { Aviso(textoDe(it)) }

        CampoAcceso(
            valor = estado.email,
            alCambiar = alCambiarEmail,
            etiqueta = stringResource(R.string.campo_correo),
            icono = R.drawable.ic_correo,
            tipoContenido = ContentType.EmailAddress,
            etiquetaPrueba = EtiquetasLogin.CORREO,
            error = stringResource(R.string.error_correo).takeIf { estado.emailInvalido },
            ejemplo = stringResource(R.string.ejemplo_correo),
            habilitado = !estado.cargando,
            teclado = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next, autoCorrectEnabled = false),
            acciones = KeyboardActions(onNext = { foco.moveFocus(FocusDirection.Down) }),
        )

        CampoContrasena(
            valor = estado.password,
            alCambiar = alCambiarPassword,
            etiqueta = stringResource(R.string.campo_contrasena),
            visible = estado.passwordVisible,
            alAlternarVisible = alAlternarPassword,
            tipoContenido = ContentType.Password,
            etiquetaPrueba = EtiquetasLogin.CONTRASENA,
            error = stringResource(R.string.error_contrasena).takeIf { estado.passwordVacia },
            habilitado = !estado.cargando,
            acciones = KeyboardActions(onDone = { ingresar() }),
        )

        BotonAcceso(
            texto = stringResource(R.string.accion_ingresar),
            textoCargando = stringResource(R.string.ingresando),
            cargando = estado.cargando,
            alPulsar = ingresar,
            etiquetaPrueba = EtiquetasLogin.INGRESAR,
        )

        EnlaceAcceso(
            pregunta = stringResource(R.string.sin_cuenta),
            accion = stringResource(R.string.accion_crear_cuenta),
            alPulsar = alCrearCuenta,
        )
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
