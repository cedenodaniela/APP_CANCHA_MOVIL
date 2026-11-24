package com.canchapp.movil.core.navegacion

import com.canchapp.movil.core.sesion.Rol
import org.junit.Assert.assertEquals
import org.junit.Test

class DestinosTest {

    @Test
    fun `dado un cliente, cuando inicia sesión, entonces entra a Reservar (RF-01)`() {
        assertEquals(Destino.Reservar, destinoInicio(Rol.CLIENTE))
    }

    @Test
    fun `dado un administrador, cuando inicia sesión, entonces entra a Agenda (RF-01)`() {
        assertEquals(Destino.Agenda, destinoInicio(Rol.ADMINISTRADOR))
    }
}
