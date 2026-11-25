package com.canchapp.movil.auth.data

import com.canchapp.movil.core.sesion.UsuarioSesion
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

/** Cuerpo de `POST /api/v1/auth/login`. */
@Serializable
data class SolicitudLogin(val email: String, val password: String)

/**
 * Respuesta del login: el JWT y el usuario.
 *
 * El backend también envía `expiraEn`; la app no lo usa porque no valida el token: si vence,
 * el backend responde 401 y la sesión se cierra (core/red/InterceptorAutenticacion).
 */
@Serializable
data class RespuestaLogin(val token: String, val usuario: UsuarioSesion)

/** Cuerpo de `POST /api/v1/usuarios`. El teléfono es opcional: si está vacío no se envía. */
@Serializable
data class SolicitudRegistro(
    val nombre: String,
    val apellido: String,
    val email: String,
    val telefono: String? = null,
    val password: String,
)

/** Cuerpo de `POST /api/v1/auth/recuperacion` (RF-02). */
@Serializable
data class SolicitudRecuperacion(val email: String)

/** Respuesta de `POST /api/v1/auth/recuperacion`: el texto que se muestra, igual exista o no la cuenta. */
@Serializable
data class RespuestaRecuperacion(val mensaje: String)

/** Cuerpo de `POST /api/v1/auth/recuperacion/restablecer` (RF-02). */
@Serializable
data class SolicitudRestablecer(val token: String, val password: String)

/** Endpoints de autenticación, registro y recuperación de contraseña del backend (RF-01, RF-02). */
interface AuthApi {

    /** 200 con el token; 401 con «Correo o contraseña incorrectos» si las credenciales fallan. */
    @POST("auth/login")
    suspend fun iniciarSesion(@Body solicitud: SolicitudLogin): RespuestaLogin

    /**
     * Crea una cuenta de CLIENTE. 201 si se creó; 400 si faltan datos o la contraseña es corta;
     * 409 si el correo ya está registrado. La app no usa la cuenta devuelta: después inicia sesión.
     */
    @POST("usuarios")
    suspend fun registrar(@Body solicitud: SolicitudRegistro)

    /** 202 con el mismo mensaje exista o no la cuenta; 400 solo si el texto no es un correo. */
    @POST("auth/recuperacion")
    suspend fun solicitarRecuperacion(@Body solicitud: SolicitudRecuperacion): RespuestaRecuperacion

    /** 204 si cambió; 400 `ENLACE_INVALIDO` si el enlace venció o ya se usó, o si la contraseña es corta. */
    @POST("auth/recuperacion/restablecer")
    suspend fun restablecerContrasena(@Body solicitud: SolicitudRestablecer)
}
