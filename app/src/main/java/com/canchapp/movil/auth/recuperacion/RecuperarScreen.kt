package com.canchapp.movil.auth.recuperacion

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.autofill.ContentType
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
import com.canchapp.movil.auth.componentes.EnlaceAcceso
import com.canchapp.movil.auth.componentes.PantallaAcceso
import com.canchapp.movil.ui.componentes.Aviso
import com.canchapp.movil.ui.componentes.TipoAviso
import com.canchapp.movil.ui.componentes.textoDe
import com.canchapp.movil.ui.theme.CanchAppTheme

/** Etiquetas para las pruebas de pantalla. */
object EtiquetasRecuperar {
    const val CORREO = "recuperar_correo"
    const val ENVIAR = "recuperar_enviar"
}

/**
 * Pantalla 1 · «¿Olvidaste tu contraseña?» (RF-02), conectada a su ViewModel.
 *
 * @param correoInicial el correo que la persona ya había escrito en el login
 */
@Composable
fun RecuperarScreen(
    alVolver: () -> Unit,
    correoInicial: String = "",
    viewModel: RecuperarViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    LaunchedEffect(correoInicial) { viewModel.usarCorreo(correoInicial) }

    RecuperarContenido(
        estado = estado,
        alCambiarEmail = viewModel::alCambiarEmail,
        alEnviar = viewModel::enviar,
        alVolver = alVolver,
    )
}

/** Recuperación sin estado propio: se prueba y se previsualiza con cualquier [EstadoRecuperar]. */
@Composable
fun RecuperarContenido(
    estado: EstadoRecuperar,
    alCambiarEmail: (String) -> Unit,
    alEnviar: () -> Unit,
    alVolver: () -> Unit,
) {
    val foco = LocalFocusManager.current
    val enviar = {
        foco.clearFocus()
        alEnviar()
    }

    PantallaAcceso(
        titulo = stringResource(R.string.titulo_recuperar),
        subtitulo = stringResource(R.string.subtitulo_recuperar),
    ) {
        estado.enviado?.let { Aviso(it, tipo = TipoAviso.EXITO) }
        estado.error?.let { Aviso(textoDe(it)) }

        CampoAcceso(
            valor = estado.email,
            alCambiar = alCambiarEmail,
            etiqueta = stringResource(R.string.campo_correo),
            icono = R.drawable.ic_correo,
            tipoContenido = ContentType.EmailAddress,
            etiquetaPrueba = EtiquetasRecuperar.CORREO,
            error = stringResource(R.string.error_correo).takeIf { estado.emailInvalido },
            ayuda = stringResource(R.string.ayuda_enlace_recuperacion),
            ejemplo = stringResource(R.string.ejemplo_correo),
            habilitado = !estado.cargando,
            teclado = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Send, autoCorrectEnabled = false),
            acciones = KeyboardActions(onSend = { enviar() }),
        )

        BotonAcceso(
            texto = stringResource(R.string.accion_enviar_enlace),
            textoCargando = stringResource(R.string.enviando),
            cargando = estado.cargando,
            alPulsar = enviar,
            etiquetaPrueba = EtiquetasRecuperar.ENVIAR,
        )

        EnlaceAcceso(
            pregunta = stringResource(R.string.recorde_contrasena),
            accion = stringResource(R.string.accion_iniciar_sesion),
            alPulsar = alVolver,
        )
    }
}

@Preview(showSystemUi = true)
@Composable
private fun RecuperarPreview() {
    CanchAppTheme {
        RecuperarContenido(EstadoRecuperar(), {}, {}, {})
    }
}

@Preview(showSystemUi = true)
@Composable
private fun RecuperarEnviadoPreview() {
    CanchAppTheme {
        RecuperarContenido(
            EstadoRecuperar(
                email = "ana@correo.com",
                enviado = "Si el correo está registrado, te enviamos un enlace para crear una contraseña nueva.",
            ),
            {}, {}, {},
        )
    }
}
