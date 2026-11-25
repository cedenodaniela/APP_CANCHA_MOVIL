package com.canchapp.movil.auth.data

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.create
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ModuloAuth {

    @Provides
    @Singleton
    fun authApi(retrofit: Retrofit): AuthApi = retrofit.create()
}
