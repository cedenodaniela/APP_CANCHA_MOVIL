package com.canchapp.movil.core.sesion

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

private val Context.almacenSesion: DataStore<Preferences> by preferencesDataStore(name = "canchapp_sesion")

/** Dependencias de la sesión para Hilt. */
@Module
@InstallIn(SingletonComponent::class)
abstract class ModuloSesion {

    @Binds
    abstract fun cifrador(impl: CifradorKeystore): CifradorToken

    @Binds
    abstract fun fuenteSesion(impl: SesionRepository): FuenteSesion

    companion object {
        @Provides
        @Singleton
        fun almacen(@ApplicationContext contexto: Context): DataStore<Preferences> = contexto.almacenSesion

        /** Alcance de la app: vive mientras el proceso vive. */
        @Provides
        @Singleton
        fun alcanceApp(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
