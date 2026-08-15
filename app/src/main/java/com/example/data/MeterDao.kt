package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.Meter
import kotlinx.coroutines.flow.Flow

@Dao
interface MeterDao {
    @Query("SELECT * FROM meters ORDER BY id ASC")
    fun getAllMeters(): Flow<List<Meter>>

    @Query("SELECT * FROM meters ORDER BY id ASC")
    suspend fun getAllMetersList(): List<Meter>

    @Query("SELECT * FROM meters WHERE isActive = 1 ORDER BY id ASC")
    fun getActiveMeters(): Flow<List<Meter>>

    @Query("SELECT * FROM meters WHERE id = :id LIMIT 1")
    suspend fun getMeterById(id: Long): Meter?

    @Query("SELECT * FROM meters WHERE name = :name LIMIT 1")
    suspend fun getMeterByName(name: String): Meter?

    @Query("SELECT COUNT(*) FROM meters")
    suspend fun getMeterCount(): Int

    @Query("SELECT * FROM meters WHERE syncStatus = 'PENDING'")
    suspend fun getPendingMeters(): List<Meter>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeter(meter: Meter): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeters(meters: List<Meter>)

    @Update
    suspend fun updateMeter(meter: Meter)

    @Query("UPDATE meters SET syncStatus = 'SYNCED', lastSyncedAt = :syncedAt WHERE id = :meterId")
    suspend fun markMeterSynced(meterId: Long, syncedAt: Long)

    @Query("UPDATE meters SET isActive = :isActive, updatedAt = :updatedAt, syncStatus = 'PENDING' WHERE id = :meterId")
    suspend fun setMeterActive(meterId: Long, isActive: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM meters WHERE id = :meterId")
    suspend fun deleteMeter(meterId: Long)
}
