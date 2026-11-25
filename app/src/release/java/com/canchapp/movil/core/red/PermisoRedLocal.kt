package com.canchapp.movil.core.red

import androidx.activity.ComponentActivity

/** En release el backend es un servidor HTTPS público: no hace falta el permiso de red local. */
object PermisoRedLocal {
    @Suppress("UNUSED_PARAMETER")
    fun solicitar(actividad: ComponentActivity) = Unit
}
