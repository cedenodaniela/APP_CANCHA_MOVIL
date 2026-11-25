package com.canchapp.movil.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val EsquemaCanchApp = lightColorScheme(
    primary = Verde,
    onPrimary = Color.White,
    secondary = Azul,
    onSecondary = Color.White,
    tertiary = Naranja,
    onTertiary = Color.White,
    error = Rojo,
    onError = Color.White,
    background = Fondo,
    onBackground = Texto,
    surface = Superficie,
    onSurface = Texto,
    onSurfaceVariant = TextoSuave,
    outline = Borde,
)

/**
 * Tema de CanchApp con Material 3.
 *
 * Usa siempre la paleta institucional: sin colores dinámicos del sistema ni modo oscuro,
 * para que cada estado se vea igual que en el panel web (app_cancha/AGENTS.md §6).
 */
@Composable
fun CanchAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EsquemaCanchApp,
        typography = TipografiaCanchApp,
        content = content,
    )
}
