package com.canchapp.movil.ui.componentes

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.canchapp.movil.R
import com.canchapp.movil.core.red.MensajeError

/**
 * Texto para mostrar un [MensajeError] en pantalla.
 *
 * Si el backend envió `mensaje`, se muestra ese; la app solo pone texto propio cuando no hay
 * conexión o la respuesta no trae el cuerpo esperado (app_cancha/AGENTS.md §6).
 */
@Composable
fun textoDe(mensaje: MensajeError): String = when (mensaje) {
    is MensajeError.DelServidor -> mensaje.texto
    MensajeError.SinConexion -> stringResource(R.string.error_sin_conexion)
    MensajeError.Inesperado -> stringResource(R.string.error_inesperado)
}
