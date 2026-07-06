package com.securityinspector.app.di

import android.content.Context
import androidx.room.Room
import com.securityinspector.app.data.local.ScanDatabase
import com.securityinspector.app.data.local.dao.ScanResultDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideScanDatabase(@ApplicationContext context: Context): ScanDatabase =
        Room.databaseBuilder(context, ScanDatabase::class.java, ScanDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideScanResultDao(database: ScanDatabase): ScanResultDao = database.scanResultDao()
}
