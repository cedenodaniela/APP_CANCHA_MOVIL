package com.canchapp.movil.auth.data

import com.canchapp.movil.core.sesion.Sesion
import com.canchapp.movil.core.sesion.SesionRepository
import com.canchapp.movil.core.sesion.UsuarioSesion
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Inicio de sesión y registro contra el backend (RF-01).
 *
 * Si las credenciales son válidas, guarda la sesión cifrada. La navegación observa la sesión,
 * así que no hace falta navegar desde aquí: el cliente pasa a «Reservar» y el administrador a «Agenda».
 */
@Singleton
class AuthRepository @Inject constructor(
    private val api: AuthApi,
    private val sesiones: SesionRepository,
) {

    /** @return el usuario autenticado, o la falla de red o del backend para mostrarla */
    suspend fun iniciarSesion(email: String, password: String): Result<UsuarioSesion> = intentar {
        val respuesta = api.iniciarSesion(SolicitudLogin(email, password))
        sesiones.guardar(Sesion(respuesta.token, respuesta.usuario))
        respuesta.usuario
    }

    /**
     * Crea una cuenta de cliente. No inicia sesión: la persona vuelve al login con su correo escrito,
     * como en el flujo de RF-01.
     */
    suspend fun registrar(solicitud: SolicitudRegistro): Result<Unit> = intentar {
        api.registrar(solicitud)
    }

    /**
     * Pide el enlace de recuperación (RF-02).
     *
     * @return el mensaje del backend, que es el mismo exista o no la cuenta
     */
    suspend fun solicitarRecuperacion(email: String): Result<String> = intentar {
        api.solicitarRecuperacion(SolicitudRecuperacion(email)).mensaje
    }

    /** Crea la contraseña nueva con el token del enlace del correo (RF-02). No inicia sesión. */
    suspend fun restablecerContrasena(token: String, password: String): Result<Unit> = intentar {
        api.restablecerContrasena(SolicitudRestablecer(token, password))
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
