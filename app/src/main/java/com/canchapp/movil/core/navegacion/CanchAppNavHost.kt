package com.canchapp.movil.core.navegacion

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.canchapp.movil.R
import com.canchapp.movil.auth.login.LoginScreen
import com.canchapp.movil.auth.recuperacion.RecuperarScreen
import com.canchapp.movil.auth.recuperacion.RestablecerScreen
import com.canchapp.movil.auth.registro.RegistroScreen
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
 *
 * El enlace de recuperación del correo (RF-02) llega como [tokenRestablecer] desde `MainActivity`,
 * y no como deep link del grafo: la navegación por sesión limpia la pila al arrancar y lo borraría.
 * Se abre después de esa navegación.
 *
 * @param tokenRestablecer token del enlace `canchapp://restablecer-contrasena?token=…`, si se abrió uno
 * @param alUsarToken avisa que el enlace ya se abrió, para no abrirlo otra vez
 */
/** Clave con la que el registro le pasa al login el correo de la cuenta creada. */
private const val CLAVE_CORREO_REGISTRADO = "correo_registrado"

@Composable
fun CanchAppNavHost(
    tokenRestablecer: String? = null,
    alUsarToken: () -> Unit = {},
    viewModel: NavegacionViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    // Sobrevive a cerrar la sesión y a rotar la pantalla hasta que el login muestre el aviso.
    var contrasenaRestablecida by rememberSaveable { mutableStateOf(false) }

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

    LaunchedEffect(tokenRestablecer) {
        tokenRestablecer?.let { token ->
            navController.navigate(Destino.Restablecer(token)) { launchSingleTop = true }
            alUsarToken()
        }
    }

    NavHost(navController = navController, startDestination = inicio) {
        composable<Destino.InicioSesion> { entrada ->
            // El registro deja aquí el correo de la cuenta creada para confirmarla en el login.
            val correoRegistrado by entrada.savedStateHandle
                .getStateFlow<String?>(CLAVE_CORREO_REGISTRADO, null)
                .collectAsStateWithLifecycle()
            LoginScreen(
                alCrearCuenta = { navController.navigate(Destino.Registro) { launchSingleTop = true } },
                correoRegistrado = correoRegistrado,
                alMostrarRegistro = { entrada.savedStateHandle[CLAVE_CORREO_REGISTRADO] = null },
                alOlvideContrasena = { email ->
                    navController.navigate(Destino.Recuperar(email)) { launchSingleTop = true }
                },
                contrasenaRestablecida = contrasenaRestablecida,
                alMostrarRestablecida = { contrasenaRestablecida = false },
            )
        }
        composable<Destino.Recuperar> { entrada ->
            RecuperarScreen(
                correoInicial = entrada.toRoute<Destino.Recuperar>().email,
                alVolver = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(Destino.InicioSesion) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                },
            )
        }
        composable<Destino.Restablecer> { entrada ->
            RestablecerScreen(
                token = entrada.toRoute<Destino.Restablecer>().token,
                alRestablecida = {
                    contrasenaRestablecida = true
                    if (estado is EstadoSesion.Activa) {
                        // Con la contraseña nueva se vuelve a iniciar sesión; la navegación por sesión
                        // lleva sola al login.
                        viewModel.cerrarSesion()
                    } else {
                        navController.navigate(Destino.InicioSesion) {
                            popUpTo(navController.graph.id) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                },
                alPedirEnlace = {
                    navController.navigate(Destino.Recuperar()) {
                        popUpTo<Destino.Restablecer> { inclusive = true }
                    }
                },
            )
        }
        composable<Destino.Registro> {
            RegistroScreen(
                alCuentaCreada = { email ->
                    navController.previousBackStackEntry?.savedStateHandle?.set(CLAVE_CORREO_REGISTRADO, email)
                    navController.popBackStack()
                },
                alIniciarSesion = { navController.popBackStack() },
            )
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
