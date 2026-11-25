package com.canchapp.movil

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.canchapp.movil.core.navegacion.CanchAppNavHost
import com.canchapp.movil.core.navegacion.tokenDeEnlaceRecuperacion
import com.canchapp.movil.core.red.PermisoRedLocal
import com.canchapp.movil.ui.theme.CanchAppTheme
import dagger.hilt.android.AndroidEntryPoint

/** Única Activity de la app: todas las pantallas son destinos de Navigation Compose. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** Token del enlace de recuperación (RF-02) que todavía no se abrió. */
    private var tokenRestablecer by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Barra de estado con íconos blancos: las pantallas empiezan con la franja verde institucional.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        // Solo en debug: Android 17 exige este permiso para llegar al backend local.
        PermisoRedLocal.solicitar(this)
        // Al rotar la pantalla el intent es el mismo: el enlace ya se abrió y no se repite.
        if (savedInstanceState == null) recibirEnlace(intent)
        setContent {
            CanchAppTheme {
                CanchAppNavHost(
                    tokenRestablecer = tokenRestablecer,
                    alUsarToken = { tokenRestablecer = null },
                )
            }
        }
    }

    /** Con `launchMode="singleTask"`, un enlace abierto con la app ya en marcha llega aquí. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        recibirEnlace(intent)
    }

    private fun recibirEnlace(intent: Intent?) {
        intent?.data?.let(::tokenDeEnlaceRecuperacion)?.let { tokenRestablecer = it }
    }
}
