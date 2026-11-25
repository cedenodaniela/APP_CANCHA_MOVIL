package com.canchapp.movil.auth.componentes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.canchapp.movil.R
import com.canchapp.movil.ui.theme.Borde
import com.canchapp.movil.ui.theme.Fondo
import com.canchapp.movil.ui.theme.Superficie
import com.canchapp.movil.ui.theme.Verde

/** Cuánto se monta la tarjeta sobre la franja verde. */
private val SOLAPE = 48.dp

/**
 * Marco común de las pantallas de acceso (inicio de sesión, registro y recuperación):
 * franja verde con el logo, tarjeta blanca con el formulario y pie con el lema.
 *
 * Es el mismo diseño que tenía la versión Ionic, para que el login y el registro se vean idénticos.
 * El contenido se desplaza y sube sobre el teclado, así el botón nunca queda tapado.
 */
@Composable
fun PantallaAcceso(
    titulo: String,
    subtitulo: String,
    modifier: Modifier = Modifier,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Fondo)
            .imePadding(),
    ) {
        val altoVisible = maxHeight
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .heightIn(min = altoVisible),
        ) {
            // La franja y la tarjeta se solapan: el espacio negativo monta la tarjeta sobre el verde.
            Column(verticalArrangement = Arrangement.spacedBy(-SOLAPE)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Verde, RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                        .statusBarsPadding()
                        .padding(start = 24.dp, end = 24.dp, top = 32.dp, bottom = SOLAPE + 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.logo_canchapp),
                        contentDescription = stringResource(R.string.descripcion_logo),
                        modifier = Modifier
                            .widthIn(max = 260.dp)
                            .fillMaxWidth(0.72f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Superficie)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }

                Surface(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(horizontal = 16.dp)
                        .widthIn(max = 440.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Superficie,
                    border = BorderStroke(1.dp, Borde),
                    shadowElevation = 6.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = titulo,
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.semantics { heading() },
                            )
                            Text(
                                text = subtitulo,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        contenido()
                    }
                }
            }

            // Empuja el lema al fondo cuando la pantalla es más alta que el contenido.
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.lema),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .navigationBarsPadding()
                    .padding(top = 24.dp, bottom = 16.dp),
            )
        }
    }
}
