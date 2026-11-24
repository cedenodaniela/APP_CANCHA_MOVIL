package com.canchapp.movil.core.sesion

import kotlinx.serialization.Serializable

/** Roles de la versión 1 (DERCAS §3). Llegan del backend en el login. */
@Serializable
enum class Rol { CLIENTE, ADMINISTRADOR }

/** Usuario autenticado: lo mínimo para decidir a qué pantalla ir (RF-01). */
@Serializable
data class UsuarioSesion(val id: Long, val nombre: String, val rol: Rol)

/**
 * Sesión guardada en el celular: el JWT que emitió el backend y su usuario.
 * El token vence a las 24 h (RNF-02); la app no lo valida, solo lo envía.
 */
@Serializable
data class Sesion(val token: String, val usuario: UsuarioSesion)

/**
 * Lo que la capa de red necesita de la sesión. Existe para que el interceptor
 * se pueda probar sin Android ni DataStore.
 */
interface FuenteSesion {
    /** Token actual, o `null` si no hay sesión. Se lee sin bloquear. */
    fun tokenActual(): String?

    /** Cierra la sesión: el backend respondió que el token ya no sirve. */
    suspend fun cerrarSesion()
}
