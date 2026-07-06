package com.securityinspector.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.securityinspector.app.data.local.dao.ScanResultDao
import com.securityinspector.app.data.local.entity.ScanResultEntity

@Database(
    entities = [ScanResultEntity::class],
    version = 1,
    exportSchema = true
)
abstract class ScanDatabase : RoomDatabase() {
    abstract fun scanResultDao(): ScanResultDao

    companion object {
        const val DATABASE_NAME = "security_inspector.db"
    }
}
