package com.canchapp.movil.auth.registro

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
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
import com.canchapp.movil.auth.registro.RegistroViewModel.Companion.LONGITUD_MINIMA_CONTRASENA
import com.canchapp.movil.ui.componentes.Aviso
import com.canchapp.movil.ui.componentes.textoDe
import com.canchapp.movil.ui.theme.CanchAppTheme

/** Etiquetas para las pruebas de pantalla. */
object EtiquetasRegistro {
    const val NOMBRE = "registro_nombre"
    const val APELLIDO = "registro_apellido"
    const val CORREO = "registro_correo"
    const val TELEFONO = "registro_telefono"
    const val CONTRASENA = "registro_contrasena"
    const val CONFIRMACION = "registro_confirmacion"
    const val CREAR = "registro_crear"
}

/**
 * Pantalla 1 · Registro (RF-01), conectada a su ViewModel.
 *
 * @param alCuentaCreada se llama una vez con el correo de la cuenta creada, para volver al login
 */
@Composable
fun RegistroScreen(
    alCuentaCreada: (email: String) -> Unit,
    alIniciarSesion: () -> Unit,
    viewModel: RegistroViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    LaunchedEffect(estado.cuentaCreada) {
        estado.cuentaCreada?.let { email ->
            viewModel.cuentaCreadaMostrada()
            alCuentaCreada(email)
        }
    }

    RegistroContenido(
        estado = estado,
        acciones = AccionesRegistro(
            alCambiarNombre = viewModel::alCambiarNombre,
            alCambiarApellido = viewModel::alCambiarApellido,
            alCambiarEmail = viewModel::alCambiarEmail,
            alCambiarTelefono = viewModel::alCambiarTelefono,
            alCambiarPassword = viewModel::alCambiarPassword,
            alCambiarConfirmacion = viewModel::alCambiarConfirmacion,
            alAlternarPassword = viewModel::alternarPasswordVisible,
            alRegistrar = viewModel::registrar,
            alIniciarSesion = alIniciarSesion,
        ),
    )
}

/** Lo que la persona puede hacer en el registro. */
data class AccionesRegistro(
    val alCambiarNombre: (String) -> Unit = {},
    val alCambiarApellido: (String) -> Unit = {},
    val alCambiarEmail: (String) -> Unit = {},
    val alCambiarTelefono: (String) -> Unit = {},
    val alCambiarPassword: (String) -> Unit = {},
    val alCambiarConfirmacion: (String) -> Unit = {},
    val alAlternarPassword: () -> Unit = {},
    val alRegistrar: () -> Unit = {},
    val alIniciarSesion: () -> Unit = {},
)

/** Registro sin estado propio: se prueba y se previsualiza con cualquier [EstadoRegistro]. */
@Composable
fun RegistroContenido(estado: EstadoRegistro, acciones: AccionesRegistro) {
    val foco = LocalFocusManager.current
    val siguiente = KeyboardActions(onNext = { foco.moveFocus(FocusDirection.Down) })
    val habilitado = !estado.cargando
    fun errorDe(campo: CampoRegistro, texto: String) = texto.takeIf { campo in estado.errores }
    val enviar = {
        foco.clearFocus()
        acciones.alRegistrar()
    }

    PantallaAcceso(
        titulo = stringResource(R.string.titulo_registro),
        subtitulo = stringResource(R.string.subtitulo_registro),
    ) {
        estado.error?.let { Aviso(textoDe(it)) }

        CampoAcceso(
            valor = estado.nombre,
            alCambiar = acciones.alCambiarNombre,
            etiqueta = stringResource(R.string.campo_nombre),
            icono = R.drawable.ic_persona,
            tipoContenido = ContentType.PersonFirstName,
            etiquetaPrueba = EtiquetasRegistro.NOMBRE,
            error = errorDe(CampoRegistro.NOMBRE, stringResource(R.string.error_nombre)),
            habilitado = habilitado,
            teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            acciones = siguiente,
        )

        CampoAcceso(
            valor = estado.apellido,
            alCambiar = acciones.alCambiarApellido,
            etiqueta = stringResource(R.string.campo_apellido),
            icono = R.drawable.ic_persona,
            tipoContenido = ContentType.PersonLastName,
            etiquetaPrueba = EtiquetasRegistro.APELLIDO,
            error = errorDe(CampoRegistro.APELLIDO, stringResource(R.string.error_apellido)),
            habilitado = habilitado,
            teclado = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            acciones = siguiente,
        )

        CampoAcceso(
            valor = estado.email,
            alCambiar = acciones.alCambiarEmail,
            etiqueta = stringResource(R.string.campo_correo),
            icono = R.drawable.ic_correo,
            tipoContenido = ContentType.EmailAddress,
            etiquetaPrueba = EtiquetasRegistro.CORREO,
            error = errorDe(CampoRegistro.EMAIL, stringResource(R.string.error_correo)),
            ejemplo = stringResource(R.string.ejemplo_correo),
            habilitado = habilitado,
            teclado = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next, autoCorrectEnabled = false),
            acciones = siguiente,
        )

        CampoAcceso(
            valor = estado.telefono,
            alCambiar = acciones.alCambiarTelefono,
            etiqueta = stringResource(R.string.campo_telefono),
            icono = R.drawable.ic_telefono,
            tipoContenido = ContentType.PhoneNumber,
            etiquetaPrueba = EtiquetasRegistro.TELEFONO,
            ejemplo = stringResource(R.string.ejemplo_telefono),
            habilitado = habilitado,
            teclado = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
            acciones = siguiente,
        )

        CampoContrasena(
            valor = estado.password,
            alCambiar = acciones.alCambiarPassword,
            etiqueta = stringResource(R.string.campo_contrasena),
            visible = estado.passwordVisible,
            alAlternarVisible = acciones.alAlternarPassword,
            tipoContenido = ContentType.NewPassword,
            etiquetaPrueba = EtiquetasRegistro.CONTRASENA,
            error = errorDe(CampoRegistro.PASSWORD, pluralStringResource(R.plurals.error_contrasena_corta, LONGITUD_MINIMA_CONTRASENA, LONGITUD_MINIMA_CONTRASENA)),
            ayuda = pluralStringResource(R.plurals.ayuda_contrasena, LONGITUD_MINIMA_CONTRASENA, LONGITUD_MINIMA_CONTRASENA),
            habilitado = habilitado,
            accionTeclado = ImeAction.Next,
            acciones = siguiente,
        )

        CampoContrasena(
            valor = estado.confirmacion,
            alCambiar = acciones.alCambiarConfirmacion,
            etiqueta = stringResource(R.string.campo_confirmacion),
            visible = estado.passwordVisible,
            alAlternarVisible = acciones.alAlternarPassword,
            tipoContenido = ContentType.NewPassword,
            etiquetaPrueba = EtiquetasRegistro.CONFIRMACION,
            error = errorDe(CampoRegistro.CONFIRMACION, stringResource(R.string.error_confirmacion)),
            habilitado = habilitado,
            accionTeclado = ImeAction.Done,
            acciones = KeyboardActions(onDone = { enviar() }),
        )

        BotonAcceso(
            texto = stringResource(R.string.accion_registrarse),
            textoCargando = stringResource(R.string.registrando),
            cargando = estado.cargando,
            alPulsar = enviar,
            etiquetaPrueba = EtiquetasRegistro.CREAR,
        )

        EnlaceAcceso(
            pregunta = stringResource(R.string.con_cuenta),
            accion = stringResource(R.string.accion_iniciar_sesion),
            alPulsar = acciones.alIniciarSesion,
        )
    }
}

@Preview(showSystemUi = true)
@Composable
private fun RegistroPreview() {
    CanchAppTheme {
        RegistroContenido(EstadoRegistro(), AccionesRegistro())
    }
}

@Preview(showSystemUi = true)
@Composable
private fun RegistroConErroresPreview() {
    CanchAppTheme {
        RegistroContenido(
            EstadoRegistro(
                email = "ana.correo.com",
                password = "123",
                confirmacion = "1234",
                errores = setOf(CampoRegistro.NOMBRE, CampoRegistro.EMAIL, CampoRegistro.PASSWORD, CampoRegistro.CONFIRMACION),
            ),
            AccionesRegistro(),
        )
    }
}
