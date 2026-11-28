package com.canchapp.movil

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import javax.inject.Inject

/**
 * Punto de entrada de la app: habilita la inyección de dependencias con Hilt.
 *
 * También configura Coil con el mismo [OkHttpClient] de la API: las fotos de canchas solo se
 * sirven con sesión (RF-04), y ese cliente agrega el JWT.
 */
@HiltAndroidApp
class CanchAppApplication : Application(), SingletonImageLoader.Factory {

    @Inject
    lateinit var clienteHttp: OkHttpClient

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { clienteHttp })) }
            .build()
}
