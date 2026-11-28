package com.canchapp.movil.cliente.reservar.data

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

/** Un bloque de disponibilidad que devuelve `GET /api/v1/disponibilidad` (RF-06). */
@Serializable
data class BloqueDisponibleDto(
    val canchaId: Long,
    val fecha: String,
    val inicio: String,
    val fin: String,
    val ocupado: Boolean,
    val precio: String,
)

/**
 * Disponibilidad de las canchas para una fecha, contra el backend (RF-06).
 *
 * `canchaId` en `null` para consultar las canchas activas; si se envía, debe existir y estar activa
 * (la inactiva no se ofrece al cliente, RF-04). La fecha va en «AAAA-MM-DD» y solo se acepta desde
 * hoy hasta hoy + `diasAnticipacion` (RN-02); el backend responde 400 con el motivo si no es válida.
 */
interface DisponibilidadApi {

    @GET("disponibilidad")
    suspend fun disponibilidad(@Query("canchaId") canchaId: Long?, @Query("fecha") fecha: String): List<BloqueDisponibleDto>
}
