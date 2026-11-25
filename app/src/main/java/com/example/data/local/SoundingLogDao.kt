package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SoundingLogDao {
    @Query("SELECT * FROM sounding_logs ORDER BY createdAtEpochMs DESC, id DESC")
    fun observeAllLogs(): Flow<List<SoundingLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: SoundingLogEntity): Long

    @Query("DELETE FROM sounding_logs WHERE id = :id")
    suspend fun deleteLogById(id: Long)

    @Query("DELETE FROM sounding_logs")
    suspend fun clearAllLogs()
}
