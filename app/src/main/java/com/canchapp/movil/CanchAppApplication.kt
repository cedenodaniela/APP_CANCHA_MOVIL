package com.canchapp.movil

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** Punto de entrada de la app: habilita la inyección de dependencias con Hilt. */
@HiltAndroidApp
class CanchAppApplication : Application()
