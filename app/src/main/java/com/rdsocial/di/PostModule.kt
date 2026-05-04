package com.rdsocial.di

import com.rdsocial.data.repository.FirebasePostRepository
import com.rdsocial.data.repository.LocationRepositoryImpl
import com.rdsocial.domain.repository.LocationRepository
import com.rdsocial.domain.repository.PostRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PostModule {

    @Binds
    @Singleton
    abstract fun bindPostRepository(
        impl: FirebasePostRepository,
    ): PostRepository

    @Binds
    @Singleton
    abstract fun bindLocationRepository(
        impl: LocationRepositoryImpl,
    ): LocationRepository
}
