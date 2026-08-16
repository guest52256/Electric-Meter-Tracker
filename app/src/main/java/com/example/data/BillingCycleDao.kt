package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.MeterBillingCycle
import kotlinx.coroutines.flow.Flow

@Dao
interface BillingCycleDao {
    @Query("SELECT * FROM meter_billing_cycles")
    fun getAllBillingCycles(): Flow<List<MeterBillingCycle>>

    @Query("SELECT * FROM meter_billing_cycles")
    suspend fun getAllBillingCyclesList(): List<MeterBillingCycle>

    @Query("SELECT * FROM meter_billing_cycles WHERE meterId = :meterId LIMIT 1")
    fun getBillingCycleForMeter(meterId: Long): Flow<MeterBillingCycle?>

    @Query("SELECT * FROM meter_billing_cycles WHERE meterId = :meterId LIMIT 1")
    suspend fun getBillingCycleForMeterDirect(meterId: Long): MeterBillingCycle?

    @Query("SELECT * FROM meter_billing_cycles WHERE syncStatus = 'PENDING'")
    suspend fun getPendingBillingCycles(): List<MeterBillingCycle>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBillingCycle(cycle: MeterBillingCycle)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBillingCycles(cycles: List<MeterBillingCycle>)

    @Query("UPDATE meter_billing_cycles SET syncStatus = 'SYNCED', lastSyncedAt = :syncedAt WHERE meterId = :meterId")
    suspend fun markBillingCycleSynced(meterId: Long, syncedAt: Long)

    @Query("DELETE FROM meter_billing_cycles WHERE meterId = :meterId")
    suspend fun deleteBillingCycleForMeter(meterId: Long)

    @Query("DELETE FROM meter_billing_cycles")
    suspend fun deleteAllBillingCycles()
}
