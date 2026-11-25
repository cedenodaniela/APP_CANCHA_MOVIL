package com.canchapp.movil

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.canchapp.movil.core.navegacion.CanchAppNavHost
import com.canchapp.movil.ui.theme.CanchAppTheme
import dagger.hilt.android.AndroidEntryPoint

/** Única Activity de la app: todas las pantallas son destinos de Navigation Compose. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Barra de estado con íconos blancos: las pantallas empiezan con la franja verde institucional.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        setContent {
            CanchAppTheme {
                CanchAppNavHost()
            }
        }
    }
}
