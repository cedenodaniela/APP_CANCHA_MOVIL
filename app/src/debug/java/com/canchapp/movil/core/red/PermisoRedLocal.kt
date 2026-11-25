package com.canchapp.movil.core.red

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/**
 * Solo en debug: pide el permiso de red local que Android 17 (API 37) exige para llegar al backend
 * de desarrollo (10.0.2.2 desde el emulador). Sin él, las conexiones se bloquean y la app muestra
 * «No se pudo conectar con el servidor».
 */
object PermisoRedLocal {

    private const val PERMISO = "android.permission.ACCESS_LOCAL_NETWORK"

    /** Android 17. */
    private const val API_RED_LOCAL = 37

    /** Se llama en `onCreate`, antes de que la Activity esté iniciada. */
    fun solicitar(actividad: ComponentActivity) {
        if (Build.VERSION.SDK_INT < API_RED_LOCAL) return
        if (ContextCompat.checkSelfPermission(actividad, PERMISO) == PackageManager.PERMISSION_GRANTED) return
        actividad.registerForActivityResult(ActivityResultContracts.RequestPermission()) { }.launch(PERMISO)
    }
}
