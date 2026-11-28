package com.canchapp.movil.admin.ajustes.data

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

/** Datos generales del complejo. */
@Serializable
data class ComplejoDto(val nombre: String?, val direccion: String?, val telefono: String?)

/** Cuenta a la que el cliente transfiere. [tipoCuenta] es «AHORROS» o «CORRIENTE». */
@Serializable
data class DatosBancariosDto(val banco: String?, val tipoCuenta: String?, val numeroCuenta: String?, val titular: String?)

/** Parámetros de las reglas de reserva. Los rangos válidos los decide el backend. */
@Serializable
data class PoliticaDto(
    val minutosExpiracion: Int?,
    val horasPlazoCancelacion: Int?,
    val maxReservasEfectivo: Int?,
    val diasAnticipacion: Int?,
    val limitarTrasInasistencia: Boolean?,
)

/** Configuración completa, tal como la envía y la recibe `/api/v1/configuracion`. */
@Serializable
data class ConfiguracionDto(
    val complejo: ComplejoDto,
    val datosBancarios: DatosBancariosDto,
    val politica: PoliticaDto,
)

/** Configuración del complejo en el backend (RF-03). */
interface ConfiguracionApi {

    /** 200 con la configuración vigente, para cualquier usuario con sesión. */
    @GET("configuracion")
    suspend fun consultar(): ConfiguracionDto

    /**
     * Reemplaza la configuración completa. 200 con lo guardado; 400 `CONFIGURACION_INVALIDA` con el
     * motivo si falta un dato o un valor está fuera de rango; 403 si no es administrador.
     */
    @PUT("configuracion")
    suspend fun guardar(@Body configuracion: ConfiguracionDto): ConfiguracionDto
}
