package com.canchapp.movil.core.navegacion

import android.net.Uri

/** Esquema y host del enlace que envía el backend por correo (RF-02, `CANCHAPP_URL_RECUPERACION`). */
const val ESQUEMA_ENLACE = "canchapp"
const val HOST_RESTABLECER = "restablecer-contrasena"

/**
 * Token de un enlace `canchapp://restablecer-contrasena?token=…`, o `null` si el enlace no es
 * de recuperación o no trae token.
 */
fun tokenDeEnlaceRecuperacion(enlace: Uri): String? =
    enlace.takeIf { it.scheme == ESQUEMA_ENLACE && it.host == HOST_RESTABLECER }
        ?.getQueryParameter("token")
        ?.takeIf { it.isNotBlank() }
