package com.canchapp.movil.core.navegacion

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.canchapp.movil.R
import com.canchapp.movil.auth.login.LoginScreen
import com.canchapp.movil.core.sesion.EstadoSesion
import com.canchapp.movil.ui.componentes.PantallaPendiente

/**
 * Navegación de la app con protección por rol (RF-01).
 *
 * - Sin sesión: inicio de sesión.
 * - Con sesión: la pantalla de inicio de su rol. Si la sesión se cierra (por «Salir» o porque el
 *   backend respondió 401), vuelve sola al inicio de sesión.
 *
 * La autorización real la hace el backend en cada endpoint (RNF-02); esto solo evita mostrar
 * pantallas que no corresponden al usuario.
 */
@Composable
fun CanchAppNavHost(viewModel: NavegacionViewModel = hiltViewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    if (estado is EstadoSesion.Cargando) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    val navController = rememberNavController()
    val inicio = (estado as? EstadoSesion.Activa)?.let { destinoInicio(it.sesion.usuario.rol) } ?: Destino.InicioSesion

    LaunchedEffect(estado) {
        val destino = (estado as? EstadoSesion.Activa)?.let { destinoInicio(it.sesion.usuario.rol) } ?: Destino.InicioSesion
        navController.navigate(destino) {
            popUpTo(navController.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }

    NavHost(navController = navController, startDestination = inicio) {
        composable<Destino.InicioSesion> {
            LoginScreen(alCrearCuenta = { navController.navigate(Destino.Registro) })
        }
        composable<Destino.Registro> {
            PantallaPendiente(titulo = stringResource(R.string.titulo_registro), requerimiento = "RF-01 (registro)")
        }
        composable<Destino.Reservar> {
            PantallaPendiente(
                titulo = stringResource(R.string.titulo_reservar),
                requerimiento = "RF-06 y RF-07",
                alSalir = viewModel::cerrarSesion,
            )
        }
        composable<Destino.Agenda> {
            PantallaPendiente(
                titulo = stringResource(R.string.titulo_agenda),
                requerimiento = "RF-15",
                alSalir = viewModel::cerrarSesion,
            )
        }
    }
}
