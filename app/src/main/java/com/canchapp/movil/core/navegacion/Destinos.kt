package com.canchapp.movil.core.navegacion

import com.canchapp.movil.core.sesion.Rol
import kotlinx.serialization.Serializable

/** Destinos de la app (Navigation Compose con rutas tipadas). Se agregan a medida que llega cada RF. */
sealed interface Destino {

    /** Pantalla 1 · Inicio de sesión (RF-01). */
    @Serializable
    data object InicioSesion : Destino

    /** Pantalla 1 · Registro de clientes (RF-01). */
    @Serializable
    data object Registro : Destino

    /** Pantalla 1 · «¿Olvidaste tu contraseña?» (RF-02). Llega con el correo escrito en el login. */
    @Serializable
    data class Recuperar(val email: String = "") : Destino

    /** Pantalla 1 · Contraseña nueva (RF-02). Se abre desde el enlace del correo, que trae el token. */
    @Serializable
    data class Restablecer(val token: String) : Destino

    /** Pantalla 2 · Disponibilidad y reserva del cliente (RF-06, RF-07). */
    @Serializable
    data object Reservar : Destino

    /** Pantalla 10 · Ajustes del complejo, solo del administrador (RF-03). */
    @Serializable
    data object Ajustes : Destino

    /** Pantalla 9 · Canchas del administrador (RF-04). */
    @Serializable
    data object Canchas : Destino

    /** Pantalla 9 · Alta o edición de una cancha (RF-04). [id] = 0 es una cancha nueva. */
    @Serializable
    data class CanchaFormulario(val id: Long = 0) : Destino

    /** Pantalla 7 · Agenda del día del administrador (RF-15). */
    @Serializable
    data object Agenda : Destino
}

/**
 * Pantalla de inicio según el rol (RF-01): el cliente entra a «Reservar» y el administrador a «Agenda».
 */
fun destinoInicio(rol: Rol): Destino = when (rol) {
    Rol.CLIENTE -> Destino.Reservar
    Rol.ADMINISTRADOR -> Destino.Agenda
}
