package com.canchapp.movil.admin.ajustes.data

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.create
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * Lee y guarda la configuración del complejo (RF-03).
 *
 * No revisa rangos: si un valor no sirve, el backend responde 400 con el motivo y la pantalla
 * lo muestra tal cual (la app no repite reglas de negocio).
 */
@Singleton
class ConfiguracionRepository @Inject constructor(
    private val api: ConfiguracionApi,
) {

    suspend fun consultar(): Result<ConfiguracionDto> = intentar { api.consultar() }

    suspend fun guardar(configuracion: ConfiguracionDto): Result<ConfiguracionDto> = intentar { api.guardar(configuracion) }

    private inline fun <T> intentar(bloque: () -> T): Result<T> =
        try {
            Result.success(bloque())
        } catch (cancelada: CancellationException) {
            throw cancelada
        } catch (error: Exception) {
            Result.failure(error)
        }
}

@Module
@InstallIn(SingletonComponent::class)
object ModuloConfiguracion {

    @Provides
    @Singleton
    fun configuracionApi(retrofit: Retrofit): ConfiguracionApi = retrofit.create()
}
