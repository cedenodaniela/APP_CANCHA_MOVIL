package com.canchapp.movil.ui.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.canchapp.movil.R
import com.canchapp.movil.ui.theme.CanchAppTheme
import com.canchapp.movil.ui.theme.FondoError
import com.canchapp.movil.ui.theme.FondoExito
import com.canchapp.movil.ui.theme.Rojo
import com.canchapp.movil.ui.theme.Verde

enum class TipoAviso { ERROR, EXITO }

/**
 * Aviso dentro de una pantalla, con icono y texto: un estado nunca se comunica solo con color
 * (app_cancha/AGENTS.md §6). Lo anuncian también los lectores de pantalla.
 */
@Composable
fun Aviso(texto: String, modifier: Modifier = Modifier, tipo: TipoAviso = TipoAviso.ERROR) {
    val color = if (tipo == TipoAviso.EXITO) Verde else Rojo
    val fondo = if (tipo == TipoAviso.EXITO) FondoExito else FondoError
    val icono = if (tipo == TipoAviso.EXITO) R.drawable.ic_exito else R.drawable.ic_alerta

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        color = fondo,
        contentColor = color,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(painter = painterResource(icono), contentDescription = null, modifier = Modifier.size(20.dp))
            Text(text = texto, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Preview
@Composable
private fun AvisoPreview() {
    CanchAppTheme {
        Aviso("Correo o contraseña incorrectos")
    }
}
