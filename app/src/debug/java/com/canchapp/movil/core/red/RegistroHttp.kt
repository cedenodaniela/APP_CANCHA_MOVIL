package com.canchapp.movil.core.red

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

/**
 * Solo en debug: escribe en Logcat las solicitudes y respuestas de la API.
 * Oculta el encabezado `Authorization` para que el token no aparezca en los registros.
 */
object RegistroHttp {
    fun agregar(builder: OkHttpClient.Builder) {
        builder.addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
                redactHeader("Authorization")
            },
        )
    }
}
