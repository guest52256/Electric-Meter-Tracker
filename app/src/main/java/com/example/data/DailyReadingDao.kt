package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.DailyReading
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyReadingDao {
    @Query("SELECT * FROM daily_readings ORDER BY timestamp DESC, id DESC")
    fun getAllReadings(): Flow<List<DailyReading>>

    @Query("SELECT * FROM daily_readings ORDER BY timestamp DESC, id DESC")
    suspend fun getAllDailyReadingsList(): List<DailyReading>

    @Query("SELECT * FROM daily_readings WHERE meterId = :meterId ORDER BY timestamp DESC, id DESC")
    fun getReadingsForMeter(meterId: Long): Flow<List<DailyReading>>

    @Query("SELECT * FROM daily_readings WHERE meterId = :meterId ORDER BY timestamp DESC, id DESC LIMIT 1")
    fun getLatestReadingForMeter(meterId: Long): Flow<DailyReading?>

    @Query("SELECT * FROM daily_readings WHERE meterId = :meterId ORDER BY timestamp DESC, id DESC LIMIT 1")
    suspend fun getLatestReadingForMeterDirect(meterId: Long): DailyReading?

    @Query("SELECT * FROM daily_readings WHERE meterId = :meterId AND dateString = :dateString LIMIT 1")
    suspend fun getReadingForMeterAndDate(meterId: Long, dateString: String): DailyReading?

    @Query("SELECT * FROM daily_readings WHERE id = :id LIMIT 1")
    suspend fun getReadingById(id: Long): DailyReading?

    @Query("SELECT * FROM daily_readings WHERE isAlert = 1 ORDER BY timestamp DESC")
    fun getAlertReadings(): Flow<List<DailyReading>>

    @Query("SELECT * FROM daily_readings WHERE syncStatus = 'PENDING'")
    suspend fun getPendingReadings(): List<DailyReading>

    @Query("SELECT COUNT(*) FROM daily_readings WHERE syncStatus = 'PENDING'")
    fun getPendingReadingsCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReading(reading: DailyReading): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadings(readings: List<DailyReading>)

    @Update
    suspend fun updateReading(reading: DailyReading)

    @Query("UPDATE daily_readings SET syncStatus = 'SYNCED', lastSyncedAt = :syncedAt WHERE id = :id")
    suspend fun markReadingSynced(id: Long, syncedAt: Long)

    @Delete
    suspend fun deleteReading(reading: DailyReading)

    @Query("DELETE FROM daily_readings WHERE id = :id")
    suspend fun deleteReadingById(id: Long)

    @Query("DELETE FROM daily_readings WHERE meterId = :meterId")
    suspend fun deleteReadingsForMeter(meterId: Long)

    @Query("DELETE FROM daily_readings")
    suspend fun deleteAllReadings()
}
