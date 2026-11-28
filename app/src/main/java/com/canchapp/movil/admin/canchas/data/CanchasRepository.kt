package com.canchapp.movil.admin.canchas.data

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.create
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * Canchas del complejo contra el backend (RF-04).
 *
 * No repite reglas: si un dato no sirve, el nombre ya existe o hay reservas activas, lo dice el
 * backend con su mensaje.
 */
@Singleton
class CanchasRepository @Inject constructor(
    private val api: CanchasApi,
    private val urlApi: HttpUrl,
) {

    suspend fun listar(): Result<List<CanchaDto>> = intentar { api.listar() }

    suspend fun obtener(id: Long): Result<CanchaDto> = intentar { api.obtener(id) }

    suspend fun crear(solicitud: SolicitudCancha): Result<CanchaDto> = intentar { api.crear(solicitud) }

    suspend fun editar(id: Long, solicitud: SolicitudCancha): Result<CanchaDto> = intentar { api.editar(id, solicitud) }

    suspend fun cambiarEstado(id: Long, activa: Boolean, confirmar: Boolean): Result<CanchaDto> =
        intentar { api.cambiarEstado(id, SolicitudEstado(activa, confirmar)) }

    /** @param jpg la foto ya preparada por [PreparadorFoto] */
    suspend fun subirFoto(id: Long, jpg: ByteArray): Result<CanchaDto> = intentar {
        val parte = MultipartBody.Part.createFormData("foto", "cancha.jpg", jpg.toRequestBody(JPEG))
        api.subirFoto(id, parte)
    }

    /**
     * Dirección de la foto. Pide sesión, así que la carga Coil con el cliente HTTP de la app.
     * [CanchaDto.fotoVersion] va como `?v=` para no mostrar una foto vieja guardada en caché.
     */
    fun urlFoto(cancha: CanchaDto): String? = cancha.fotoVersion?.let { version ->
        urlApi.newBuilder()
            .addPathSegments("canchas/${cancha.id}/foto")
            .addQueryParameter("v", version.toString())
            .build()
            .toString()
    }

    private inline fun <T> intentar(bloque: () -> T): Result<T> =
        try {
            Result.success(bloque())
        } catch (cancelada: CancellationException) {
            throw cancelada
        } catch (error: Exception) {
            Result.failure(error)
        }

    private companion object {
        val JPEG = "image/jpeg".toMediaType()
    }
}

@Module
@InstallIn(SingletonComponent::class)
object ModuloCanchas {

    @Provides
    @Singleton
    fun canchasApi(retrofit: Retrofit): CanchasApi = retrofit.create()
}
