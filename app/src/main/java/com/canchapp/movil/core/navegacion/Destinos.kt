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

    /** Pantalla 2 · Disponibilidad y reserva del cliente (RF-06, RF-07). */
    @Serializable
    data object Reservar : Destino

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
