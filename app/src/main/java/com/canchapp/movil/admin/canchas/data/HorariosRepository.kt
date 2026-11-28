package com.canchapp.movil.admin.canchas.data

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
 * Horario de atención, tarifas y bloqueos contra el backend (RF-05).
 *
 * No repite reglas: si dos tarifas se cruzan o un bloqueo cae sobre reservas activas, lo dice el
 * backend con su mensaje.
 */
@Singleton
class HorariosRepository @Inject constructor(
    private val api: HorariosApi,
) {

    suspend fun horario(): Result<HorarioDto> = intentar { api.horario() }

    suspend fun guardarHorario(horario: HorarioDto): Result<HorarioDto> = intentar { api.guardarHorario(horario) }

    suspend fun tarifas(): Result<List<TarifaDto>> = intentar { api.tarifas() }

    /** @param id `null` para crear una tarifa nueva */
    suspend fun guardarTarifa(id: Long?, solicitud: SolicitudTarifa): Result<TarifaDto> = intentar {
        if (id == null) api.crearTarifa(solicitud) else api.editarTarifa(id, solicitud)
    }

    suspend fun eliminarTarifa(id: Long): Result<Unit> = intentar { api.eliminarTarifa(id) }

    suspend fun bloqueos(): Result<List<BloqueoDto>> = intentar { api.bloqueos() }

    suspend fun crearBloqueo(solicitud: SolicitudBloqueo): Result<BloqueoDto> = intentar { api.crearBloqueo(solicitud) }

    suspend fun eliminarBloqueo(id: Long): Result<Unit> = intentar { api.eliminarBloqueo(id) }

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
object ModuloHorarios {

    @Provides
    @Singleton
    fun horariosApi(retrofit: Retrofit): HorariosApi = retrofit.create()
}
