package com.appsbase.attendly.di

import com.appsbase.attendly.data.repository.AttendanceRepositoryImpl
import com.appsbase.attendly.domain.repository.AttendanceRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAttendanceRepository(
        repositoryImpl: AttendanceRepositoryImpl
    ): AttendanceRepository
}
