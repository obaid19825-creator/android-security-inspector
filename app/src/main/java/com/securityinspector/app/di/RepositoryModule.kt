package com.securityinspector.app.di

import com.securityinspector.app.data.repository.FileAnalysisRepositoryImpl
import com.securityinspector.app.data.repository.SecurityScanRepositoryImpl
import com.securityinspector.app.domain.repository.FileAnalysisRepository
import com.securityinspector.app.domain.repository.SecurityScanRepository
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
    abstract fun bindSecurityScanRepository(
        impl: SecurityScanRepositoryImpl
    ): SecurityScanRepository

    @Binds
    @Singleton
    abstract fun bindFileAnalysisRepository(
        impl: FileAnalysisRepositoryImpl
    ): FileAnalysisRepository
}
