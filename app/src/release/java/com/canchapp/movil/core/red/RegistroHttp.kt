package com.canchapp.movil.core.red

import okhttp3.OkHttpClient

/** En release no se registra el tráfico HTTP. */
object RegistroHttp {
    @Suppress("UNUSED_PARAMETER")
    fun agregar(builder: OkHttpClient.Builder) = Unit
}
