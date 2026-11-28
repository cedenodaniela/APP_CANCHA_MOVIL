package com.canchapp.movil.ui.componentes

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.canchapp.movil.R
import com.canchapp.movil.ui.theme.CanchAppTheme
import com.canchapp.movil.ui.theme.Verde
import kotlinx.coroutines.launch

/** Secciones principales del administrador en la app, en el orden del menú. */
enum class SeccionAdministrador(@StringRes val titulo: Int, @DrawableRes val icono: Int) {
    AGENDA(R.string.menu_agenda, R.drawable.ic_agenda),
    CANCHAS(R.string.menu_canchas, R.drawable.ic_cancha),
    AJUSTES(R.string.menu_ajustes, R.drawable.ic_ajustes),
}

/** Etiquetas para las pruebas de pantalla. */
object EtiquetasMenu {
    const val SALIR = "menu_salir"
    fun seccion(s: SeccionAdministrador) = "menu_${s.name}"
}

/**
 * Menú lateral (hamburguesa) del administrador: Agenda, Canchas, Ajustes del complejo y Salir.
 *
 * Envuelve una pantalla principal y le pasa `abrirMenu` para el ícono ☰ de su barra superior.
 * «Salir» pide confirmación antes de cerrar la sesión.
 *
 * @param actual la sección que se está viendo, marcada en el menú
 * @param nombre el nombre del administrador, para la cabecera
 */
@Composable
fun MenuAdministrador(
    actual: SeccionAdministrador,
    nombre: String,
    alIrA: (SeccionAdministrador) -> Unit,
    alSalir: () -> Unit,
    contenido: @Composable (abrirMenu: () -> Unit) -> Unit,
) {
    val cajon = rememberDrawerState(DrawerValue.Closed)
    val alcance = rememberCoroutineScope()
    var confirmandoSalida by rememberSaveable { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = cajon,
        drawerContent = {
            ContenidoMenu(
                actual = actual,
                nombre = nombre,
                alElegir = { seccion ->
                    alcance.launch { cajon.close() }
                    if (seccion != actual) alIrA(seccion)
                },
                alSalir = {
                    alcance.launch { cajon.close() }
                    confirmandoSalida = true
                },
            )
        },
    ) {
        contenido { alcance.launch { cajon.open() } }
    }

    if (confirmandoSalida) {
        DialogoCerrarSesion(
            alConfirmar = {
                confirmandoSalida = false
                alSalir()
            },
            alCancelar = { confirmandoSalida = false },
        )
    }
}

/** Lo que se ve dentro del menú. Separado para probarlo y previsualizarlo sin el cajón. */
@Composable
fun ContenidoMenu(
    actual: SeccionAdministrador,
    nombre: String,
    alElegir: (SeccionAdministrador) -> Unit,
    alSalir: () -> Unit,
) {
    ModalDrawerSheet(modifier = Modifier.widthIn(max = 320.dp)) {
        // Cabecera con la franja verde institucional y el logo, como las pantallas de acceso.
        Column(
            Modifier
                .fillMaxWidth()
                .background(Verde)
                .statusBarsPadding()
                .padding(20.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.logo_canchapp),
                contentDescription = stringResource(R.string.descripcion_logo),
                modifier = Modifier
                    .widthIn(max = 160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimary)
            Text(stringResource(R.string.rol_administrador), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimary)
        }
        Spacer(Modifier.height(8.dp))
        SeccionAdministrador.entries.forEach { seccion ->
            NavigationDrawerItem(
                label = { Text(stringResource(seccion.titulo)) },
                icon = { Icon(painterResource(seccion.icono), contentDescription = null) },
                selected = seccion == actual,
                onClick = { alElegir(seccion) },
                modifier = Modifier
                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                    .testTag(EtiquetasMenu.seccion(seccion)),
            )
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.menu_salir)) },
            icon = { Icon(painterResource(R.drawable.ic_salir), contentDescription = null) },
            selected = false,
            onClick = alSalir,
            modifier = Modifier
                .padding(NavigationDrawerItemDefaults.ItemPadding)
                .testTag(EtiquetasMenu.SALIR),
        )
    }
}

/** Ícono ☰ para la barra superior de una pantalla principal del administrador. */
@Composable
fun BotonMenu(alAbrir: () -> Unit) {
    IconButton(onClick = alAbrir) {
        Icon(painterResource(R.drawable.ic_menu), contentDescription = stringResource(R.string.menu_abrir))
    }
}

@Preview(showSystemUi = true)
@Composable
private fun ContenidoMenuPreview() {
    CanchAppTheme { ContenidoMenu(SeccionAdministrador.CANCHAS, "Daniela", {}, {}) }
}
