package com.canchapp.movil.admin.canchas.data

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/** Un día del horario. [dia] va de «LUNES» a «DOMINGO»; las horas son «HH:mm» y pueden ser «24:00». */
@Serializable
data class DiaDto(val dia: String, val abierto: Boolean, val apertura: String? = null, val cierre: String? = null)

@Serializable
data class HorarioDto(val dias: List<DiaDto>)

/** Una tarifa. [precioHora] llega como texto con 2 decimales («25.00»). */
@Serializable
data class TarifaDto(val id: Long, val dias: List<String>, val inicio: String, val fin: String, val precioHora: String)

/** Alta o edición de una tarifa. El precio se envía como texto para no perder decimales. */
@Serializable
data class SolicitudTarifa(val dias: List<String>, val inicio: String, val fin: String, val precioHora: String?)

/** Un bloqueo. [canchaId] es `null` si bloquea todo el complejo; [fecha] es «AAAA-MM-DD». */
@Serializable
data class BloqueoDto(
    val id: Long,
    val canchaId: Long? = null,
    val fecha: String,
    val inicio: String,
    val fin: String,
    val motivo: String,
)

/** [confirmar] va en `true` después de ver cuántas reservas activas hay en ese horario. */
@Serializable
data class SolicitudBloqueo(
    val canchaId: Long?,
    val fecha: String?,
    val inicio: String,
    val fin: String,
    val motivo: String,
    val confirmar: Boolean = false,
)

/** Horario de atención, tarifas y bloqueos en el backend (RF-05). Cambiarlos es solo del administrador. */
interface HorariosApi {

    @GET("horario")
    suspend fun horario(): HorarioDto

    /** 400 `HORARIO_INVALIDO` si un día no es válido. */
    @PUT("horario")
    suspend fun guardarHorario(@Body horario: HorarioDto): HorarioDto

    @GET("tarifas")
    suspend fun tarifas(): List<TarifaDto>

    /** 409 `TARIFA_SUPERPUESTA` si se cruza con otra. */
    @POST("tarifas")
    suspend fun crearTarifa(@Body solicitud: SolicitudTarifa): TarifaDto

    @PUT("tarifas/{id}")
    suspend fun editarTarifa(@Path("id") id: Long, @Body solicitud: SolicitudTarifa): TarifaDto

    @DELETE("tarifas/{id}")
    suspend fun eliminarTarifa(@Path("id") id: Long)

    /** Los bloqueos de hoy en adelante. */
    @GET("bloqueos")
    suspend fun bloqueos(): List<BloqueoDto>

    /** 409 `RESERVAS_ACTIVAS` si el horario tiene reservas activas y no se confirmó. */
    @POST("bloqueos")
    suspend fun crearBloqueo(@Body solicitud: SolicitudBloqueo): BloqueoDto

    @DELETE("bloqueos/{id}")
    suspend fun eliminarBloqueo(@Path("id") id: Long)
}
