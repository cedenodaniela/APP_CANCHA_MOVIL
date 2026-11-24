package com.canchapp.movil.core.navegacion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canchapp.movil.core.sesion.EstadoSesion
import com.canchapp.movil.core.sesion.SesionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Expone la sesión a la navegación y permite cerrarla desde cualquier pantalla. */
@HiltViewModel
class NavegacionViewModel @Inject constructor(
    private val sesiones: SesionRepository,
) : ViewModel() {

    val estado: StateFlow<EstadoSesion> = sesiones.estado

    fun cerrarSesion() {
        viewModelScope.launch { sesiones.cerrarSesion() }
    }
}
