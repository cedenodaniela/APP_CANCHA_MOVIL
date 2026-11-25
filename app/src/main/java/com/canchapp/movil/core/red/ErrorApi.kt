package com.canchapp.movil.core.red

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

/** Cuerpo uniforme de los errores del backend: `{ codigo, mensaje }` (app_cancha/AGENTS.md §6). */
@Serializable
data class ErrorApi(val codigo: String, val mensaje: String)

/** Qué mostrarle al usuario cuando falla una llamada a la API. */
sealed interface MensajeError {
    /** El backend explicó el problema en español: se muestra tal cual. */
    data class DelServidor(val codigo: String, val texto: String) : MensajeError

    /** No hubo respuesta del servidor. */
    data object SinConexion : MensajeError

    /** Respuesta sin el cuerpo esperado. */
    data object Inesperado : MensajeError
}

private val json = Json { ignoreUnknownKeys = true }

/**
 * Traduce una excepción de Retrofit u OkHttp a un [MensajeError].
 *
 * La app nunca inventa el texto de una regla de negocio: si el backend envió `mensaje`, se usa ese.
 * Solo «sin conexión» y «error inesperado» tienen textos propios, en `strings.xml`.
 */
fun Throwable.aMensajeError(): MensajeError = when (this) {
    is HttpException -> response()?.errorBody()?.string()
        ?.let { cuerpo -> runCatching { json.decodeFromString(ErrorApi.serializer(), cuerpo) }.getOrNull() }
        ?.let { MensajeError.DelServidor(it.codigo, it.mensaje) }
        ?: MensajeError.Inesperado
    is IOException -> MensajeError.SinConexion
    else -> MensajeError.Inesperado
}
