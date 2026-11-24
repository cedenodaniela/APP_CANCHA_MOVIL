package com.canchapp.movil.core.red

import com.canchapp.movil.BuildConfig
import com.canchapp.movil.core.sesion.FuenteSesion
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Cliente HTTP de la API `/api/v1` del repositorio APP_CANCHA_BACKEND.
 *
 * La URL sale de `BuildConfig.API_URL`: en debug apunta al backend local desde el emulador
 * (`10.0.2.2`) y en release a la URL HTTPS del servidor (RNF-02).
 */
@Module
@InstallIn(SingletonComponent::class)
object ModuloRed {

    @Provides
    @Singleton
    fun urlApi(): HttpUrl = BuildConfig.API_URL.toHttpUrl()

    @Provides
    @Singleton
    fun json(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun clienteHttp(fuenteSesion: FuenteSesion, urlApi: HttpUrl): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(InterceptorAutenticacion(fuenteSesion, urlApi))
            .apply { RegistroHttp.agregar(this) }
            .build()

    @Provides
    @Singleton
    fun retrofit(cliente: OkHttpClient, urlApi: HttpUrl, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(urlApi)
            .client(cliente)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
}
