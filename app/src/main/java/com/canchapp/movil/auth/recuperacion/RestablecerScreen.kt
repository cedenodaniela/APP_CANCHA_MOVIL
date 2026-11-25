package com.canchapp.movil.auth.recuperacion

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.canchapp.movil.R
import com.canchapp.movil.auth.componentes.BotonAcceso
import com.canchapp.movil.auth.componentes.CampoContrasena
import com.canchapp.movil.auth.componentes.EnlaceAcceso
import com.canchapp.movil.auth.componentes.PantallaAcceso
import com.canchapp.movil.auth.registro.RegistroViewModel.Companion.LONGITUD_MINIMA_CONTRASENA
import com.canchapp.movil.core.red.MensajeError
import com.canchapp.movil.ui.componentes.Aviso
import com.canchapp.movil.ui.componentes.textoDe
import com.canchapp.movil.ui.theme.CanchAppTheme

/** Etiquetas para las pruebas de pantalla. */
object EtiquetasRestablecer {
    const val CONTRASENA = "restablecer_contrasena"
    const val CONFIRMACION = "restablecer_confirmacion"
    const val GUARDAR = "restablecer_guardar"
}

/**
 * Pantalla 1 · Contraseña nueva (RF-02), que se abre desde el enlace del correo.
 *
 * @param token el que trae el enlace `canchapp://restablecer-contrasena?token=…`
 * @param alRestablecida se llama una vez, cuando la contraseña ya cambió, para volver al login
 */
@Composable
fun RestablecerScreen(
    token: String,
    alRestablecida: () -> Unit,
    alPedirEnlace: () -> Unit,
    viewModel: RestablecerViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    LaunchedEffect(estado.restablecida) {
        if (estado.restablecida) {
            viewModel.restablecidaMostrada()
            alRestablecida()
        }
    }

    RestablecerContenido(
        estado = estado,
        acciones = AccionesRestablecer(
            alCambiarPassword = viewModel::alCambiarPassword,
            alCambiarConfirmacion = viewModel::alCambiarConfirmacion,
            alAlternarPassword = viewModel::alternarPasswordVisible,
            alGuardar = { viewModel.guardar(token) },
            alPedirEnlace = alPedirEnlace,
        ),
    )
}

/** Lo que la persona puede hacer en la pantalla de contraseña nueva. */
data class AccionesRestablecer(
    val alCambiarPassword: (String) -> Unit = {},
    val alCambiarConfirmacion: (String) -> Unit = {},
    val alAlternarPassword: () -> Unit = {},
    val alGuardar: () -> Unit = {},
    val alPedirEnlace: () -> Unit = {},
)

/** Contraseña nueva sin estado propio: se prueba y se previsualiza con cualquier [EstadoRestablecer]. */
@Composable
fun RestablecerContenido(estado: EstadoRestablecer, acciones: AccionesRestablecer) {
    val foco = LocalFocusManager.current
    val habilitado = !estado.cargando
    val guardar = {
        foco.clearFocus()
        acciones.alGuardar()
    }

    PantallaAcceso(
        titulo = stringResource(R.string.titulo_restablecer),
        subtitulo = stringResource(R.string.subtitulo_restablecer),
    ) {
        estado.error?.let { Aviso(textoDe(it)) }

        CampoContrasena(
            valor = estado.password,
            alCambiar = acciones.alCambiarPassword,
            etiqueta = stringResource(R.string.campo_contrasena_nueva),
            visible = estado.passwordVisible,
            alAlternarVisible = acciones.alAlternarPassword,
            tipoContenido = ContentType.NewPassword,
            etiquetaPrueba = EtiquetasRestablecer.CONTRASENA,
            error = pluralStringResource(R.plurals.error_contrasena_corta, LONGITUD_MINIMA_CONTRASENA, LONGITUD_MINIMA_CONTRASENA)
                .takeIf { CampoRestablecer.PASSWORD in estado.errores },
            ayuda = pluralStringResource(R.plurals.ayuda_contrasena, LONGITUD_MINIMA_CONTRASENA, LONGITUD_MINIMA_CONTRASENA),
            habilitado = habilitado,
            accionTeclado = ImeAction.Next,
            acciones = KeyboardActions(onNext = { foco.moveFocus(FocusDirection.Down) }),
        )

        CampoContrasena(
            valor = estado.confirmacion,
            alCambiar = acciones.alCambiarConfirmacion,
            etiqueta = stringResource(R.string.campo_confirmacion),
            visible = estado.passwordVisible,
            alAlternarVisible = acciones.alAlternarPassword,
            tipoContenido = ContentType.NewPassword,
            etiquetaPrueba = EtiquetasRestablecer.CONFIRMACION,
            error = stringResource(R.string.error_confirmacion).takeIf { CampoRestablecer.CONFIRMACION in estado.errores },
            habilitado = habilitado,
            accionTeclado = ImeAction.Done,
            acciones = KeyboardActions(onDone = { guardar() }),
        )

        BotonAcceso(
            texto = stringResource(R.string.accion_guardar_contrasena),
            textoCargando = stringResource(R.string.guardando),
            cargando = estado.cargando,
            alPulsar = guardar,
            etiquetaPrueba = EtiquetasRestablecer.GUARDAR,
        )

        EnlaceAcceso(
            pregunta = stringResource(R.string.enlace_vencido),
            accion = stringResource(R.string.accion_pedir_enlace),
            alPulsar = acciones.alPedirEnlace,
        )
    }
}

@Preview(showSystemUi = true)
@Composable
private fun RestablecerPreview() {
    CanchAppTheme {
        RestablecerContenido(EstadoRestablecer(), AccionesRestablecer())
    }
}

@Preview(showSystemUi = true)
@Composable
private fun RestablecerEnlaceVencidoPreview() {
    CanchAppTheme {
        RestablecerContenido(
            EstadoRestablecer(
                error = MensajeError.DelServidor("ENLACE_INVALIDO", "El enlace no es válido o ya venció. Solicita uno nuevo."),
            ),
            AccionesRestablecer(),
        )
    }
}
