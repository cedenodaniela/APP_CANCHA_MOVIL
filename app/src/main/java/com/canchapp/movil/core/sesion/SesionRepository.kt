package com.canchapp.movil.core.sesion

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** Estado de la sesión al abrir la app: mientras se lee el disco no se decide la pantalla inicial. */
sealed interface EstadoSesion {
    data object Cargando : EstadoSesion
    data object SinSesion : EstadoSesion
    data class Activa(val sesion: Sesion) : EstadoSesion
}

/**
 * Guarda la sesión en DataStore, cifrada con [CifradorToken] (RF-01, RNF-02).
 *
 * Expone el estado como [StateFlow] para que la navegación reaccione sola: si el backend
 * rechaza el token y se cierra la sesión, la app vuelve al inicio de sesión.
 */
@Singleton
class SesionRepository @Inject constructor(
    private val almacen: DataStore<Preferences>,
    private val cifrador: CifradorToken,
    alcance: CoroutineScope,
) : FuenteSesion {

    val estado: StateFlow<EstadoSesion> = almacen.data
        .map { preferencias -> leer(preferencias[CLAVE_SESION]) }
        .stateIn(alcance, SharingStarted.Eagerly, EstadoSesion.Cargando)

    suspend fun guardar(sesion: Sesion) {
        val cifrada = cifrador.cifrar(Json.encodeToString(Sesion.serializer(), sesion))
        almacen.edit { it[CLAVE_SESION] = cifrada }
    }

    override suspend fun cerrarSesion() {
        almacen.edit { it.remove(CLAVE_SESION) }
    }

    override fun tokenActual(): String? = (estado.value as? EstadoSesion.Activa)?.sesion?.token

    private fun leer(cifrada: String?): EstadoSesion {
        if (cifrada == null) return EstadoSesion.SinSesion
        // Si no se puede descifrar (por ejemplo, la clave del Keystore se perdió), se pide iniciar sesión otra vez.
        val sesion = runCatching { Json.decodeFromString(Sesion.serializer(), cifrador.descifrar(cifrada)) }.getOrNull()
        return if (sesion != null) EstadoSesion.Activa(sesion) else EstadoSesion.SinSesion
    }

    private companion object {
        val CLAVE_SESION = stringPreferencesKey("sesion")
    }
}
