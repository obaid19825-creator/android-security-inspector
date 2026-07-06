package com.securityinspector.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.securityinspector.app.data.local.entity.ScanResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanResultDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ScanResultEntity): Long

    @Query("SELECT * FROM scan_results ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<ScanResultEntity>>

    @Query("SELECT * FROM scan_results WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ScanResultEntity?

    @Query("SELECT * FROM scan_results ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(): ScanResultEntity?

    @Query("DELETE FROM scan_results WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Delete
    suspend fun delete(entity: ScanResultEntity)
}
