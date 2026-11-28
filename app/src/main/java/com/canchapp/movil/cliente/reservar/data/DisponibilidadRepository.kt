package com.canchapp.movil.cliente.reservar.data

import kotlinx.coroutines.CancellationException
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.Retrofit
import retrofit2.create
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Disponibilidad de las canchas contra el backend (RF-06).
 *
 * No repite reglas: si un bloque está ocupado o el rango de fechas no sirve, el backend lo dice con
 * su mensaje y la pantalla lo muestra (la app no calcula nada).
 */
@Singleton
class DisponibilidadRepository @Inject constructor(
    private val api: DisponibilidadApi,
) {

    /** Bloques del día. `canchaId` en `null` para las canchas activas. */
    suspend fun disponibilidad(canchaId: Long?, fecha: LocalDate): Result<List<BloqueDisponibleDto>> = intentar {
        api.disponibilidad(canchaId, fecha.toString())
    }

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
object ModuloDisponibilidad {

    @Provides
    @Singleton
    fun disponibilidadApi(retrofit: Retrofit): DisponibilidadApi = retrofit.create()
}
