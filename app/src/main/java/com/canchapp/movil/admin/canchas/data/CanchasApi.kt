package com.canchapp.movil.admin.canchas.data

import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

/**
 * Una cancha, tal como la envía `/api/v1/canchas`.
 *
 * @param fotoVersion cambia cada vez que cambia la foto; `null` si no tiene foto
 */
@Serializable
data class CanchaDto(
    val id: Long,
    val nombre: String,
    val tipoCesped: String,
    val anchoMetros: Int,
    val largoMetros: Int,
    val activa: Boolean,
    val fotoVersion: Long? = null,
)

/** Cuerpo de alta y edición. Un número vacío se envía vacío y el backend explica qué falta. */
@Serializable
data class SolicitudCancha(val nombre: String, val tipoCesped: String, val anchoMetros: Int?, val largoMetros: Int?)

/** Cuerpo de `PATCH /canchas/{id}/estado`. [confirmar] va en `true` después de ver cuántas reservas activas tiene. */
@Serializable
data class SolicitudEstado(val activa: Boolean, val confirmar: Boolean = false)

/** Canchas del complejo en el backend (RF-04). Crear, editar y cambiar estado o foto es solo del administrador. */
interface CanchasApi {

    /** El administrador recibe todas, también las inactivas. */
    @GET("canchas")
    suspend fun listar(): List<CanchaDto>

    @GET("canchas/{id}")
    suspend fun obtener(@Path("id") id: Long): CanchaDto

    /** 201; 400 `CANCHA_INVALIDA`; 409 `CANCHA_REPETIDA`. */
    @POST("canchas")
    suspend fun crear(@Body solicitud: SolicitudCancha): CanchaDto

    @PUT("canchas/{id}")
    suspend fun editar(@Path("id") id: Long, @Body solicitud: SolicitudCancha): CanchaDto

    /** 409 `RESERVAS_ACTIVAS` si se desactiva sin confirmar y tiene reservas activas. */
    @PATCH("canchas/{id}/estado")
    suspend fun cambiarEstado(@Path("id") id: Long, @Body solicitud: SolicitudEstado): CanchaDto

    /** La foto va en la parte `foto`, en JPG o PNG de hasta 5 MB; el backend la guarda como WebP. */
    @Multipart
    @PUT("canchas/{id}/foto")
    suspend fun subirFoto(@Path("id") id: Long, @Part foto: MultipartBody.Part): CanchaDto
}
