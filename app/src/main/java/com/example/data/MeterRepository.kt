package com.example.data

import android.util.Log
import com.example.data.firebase.FirestoreSyncManager
import com.example.model.DailyReading
import com.example.model.Meter
import com.example.model.MeterBillingCycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MeterRepository(
    private val meterDao: MeterDao,
    private val billingCycleDao: BillingCycleDao,
    private val dailyReadingDao: DailyReadingDao,
    private val syncQueueDao: SyncQueueDao,
    val firestoreSyncManager: FirestoreSyncManager,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val tag = "MeterRepository"

    val allMeters: Flow<List<Meter>> = meterDao.getAllMeters()
    val activeMeters: Flow<List<Meter>> = meterDao.getActiveMeters()
    val allBillingCycles: Flow<List<MeterBillingCycle>> = billingCycleDao.getAllBillingCycles()
    val allReadings: Flow<List<DailyReading>> = dailyReadingDao.getAllReadings()
    val alertReadings: Flow<List<DailyReading>> = dailyReadingDao.getAlertReadings()

    init {
        // Initialize Firestore Sync Manager with DAOs
        firestoreSyncManager.initialize(meterDao, billingCycleDao, dailyReadingDao, syncQueueDao)

        // Seed required initial meters if the database is empty
        scope.launch {
            seedInitialMetersIfEmpty()
        }
    }

    /**
     * Seeds the initial meters as required:
     * 1. Muhammad Iqbal S/O Luqman
     * 2. Sadaqat Ali S/O Liaquat Ali
     */
    private suspend fun seedInitialMetersIfEmpty() {
        val count = meterDao.getMeterCount()
        if (count == 0) {
            Log.d(tag, "Seeding initial default meters...")
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
            val formattedDate = dateFormat.format(Date())
            val now = System.currentTimeMillis()

            val meter1 = Meter(
                id = 1L,
                name = "Muhammad Iqbal S/O Luqman",
                isActive = true,
                createdAt = now,
                updatedAt = now,
                deviceId = firestoreSyncManager.deviceId,
                syncStatus = "PENDING"
            )
            meterDao.insertMeter(meter1)
            val cycle1 = MeterBillingCycle(
                meterId = 1L,
                previousBillReading = 0.0,
                cycleStartDate = now,
                cycleStartFormattedDate = formattedDate,
                createdAt = now,
                updatedAt = now,
                deviceId = firestoreSyncManager.deviceId,
                syncStatus = "PENDING"
            )
            billingCycleDao.insertOrUpdateBillingCycle(cycle1)

            val meter2 = Meter(
                id = 2L,
                name = "Sadaqat Ali S/O Liaquat Ali",
                isActive = true,
                createdAt = now + 1,
                updatedAt = now + 1,
                deviceId = firestoreSyncManager.deviceId,
                syncStatus = "PENDING"
            )
            meterDao.insertMeter(meter2)
            val cycle2 = MeterBillingCycle(
                meterId = 2L,
                previousBillReading = 0.0,
                cycleStartDate = now + 1,
                cycleStartFormattedDate = formattedDate,
                createdAt = now + 1,
                updatedAt = now + 1,
                deviceId = firestoreSyncManager.deviceId,
                syncStatus = "PENDING"
            )
            billingCycleDao.insertOrUpdateBillingCycle(cycle2)

            // Trigger sync
            firestoreSyncManager.pushMeter(meter1)
            firestoreSyncManager.pushBillingCycle(cycle1)
            firestoreSyncManager.pushMeter(meter2)
            firestoreSyncManager.pushBillingCycle(cycle2)
        }
    }

    fun getReadingsForMeter(meterId: Long): Flow<List<DailyReading>> =
        dailyReadingDao.getReadingsForMeter(meterId)

    fun getBillingCycleForMeter(meterId: Long): Flow<MeterBillingCycle?> =
        billingCycleDao.getBillingCycleForMeter(meterId)

    suspend fun getMeterById(id: Long): Meter? = meterDao.getMeterById(id)

    suspend fun getBillingCycleForMeterDirect(meterId: Long): MeterBillingCycle? =
        billingCycleDao.getBillingCycleForMeterDirect(meterId)

    suspend fun getLatestReadingForMeterDirect(meterId: Long): DailyReading? =
        dailyReadingDao.getLatestReadingForMeterDirect(meterId)

    /**
     * Add a new meter locally first (Offline-First), then push to Firestore
     */
    suspend fun insertMeter(name: String, initialBillReading: Double): Long {
        val now = System.currentTimeMillis()
        val meter = Meter(
            name = name.trim(),
            isActive = true,
            createdAt = now,
            updatedAt = now,
            deviceId = firestoreSyncManager.deviceId,
            syncStatus = "PENDING"
        )
        val meterId = meterDao.insertMeter(meter)
        val savedMeter = meter.copy(id = meterId)

        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
        val formattedDate = dateFormat.format(Date(now))

        val cycle = MeterBillingCycle(
            meterId = meterId,
            previousBillReading = initialBillReading,
            cycleStartDate = now,
            cycleStartFormattedDate = formattedDate,
            createdAt = now,
            updatedAt = now,
            deviceId = firestoreSyncManager.deviceId,
            syncStatus = "PENDING"
        )
        billingCycleDao.insertOrUpdateBillingCycle(cycle)

        // Asynchronously persist to Firebase Firestore
        scope.launch {
            firestoreSyncManager.pushMeter(savedMeter)
            firestoreSyncManager.pushBillingCycle(cycle)
        }

        return meterId
    }

    suspend fun setMeterActiveStatus(meterId: Long, isActive: Boolean) {
        val now = System.currentTimeMillis()
        meterDao.setMeterActive(meterId, isActive, now)
        val meter = meterDao.getMeterById(meterId)
        if (meter != null) {
            val updated = meter.copy(isActive = isActive, updatedAt = now, syncStatus = "PENDING")
            meterDao.updateMeter(updated)
            scope.launch {
                firestoreSyncManager.pushMeter(updated)
            }
        }
    }

    suspend fun updateMeter(meterId: Long, newName: String, isActive: Boolean): Result<Meter> {
        val meter = meterDao.getMeterById(meterId)
            ?: return Result.failure(IllegalArgumentException("Meter with ID #$meterId not found."))
        val trimmedName = newName.trim()
        if (trimmedName.isBlank()) {
            return Result.failure(IllegalArgumentException("Meter name cannot be empty."))
        }

        val now = System.currentTimeMillis()
        val updated = meter.copy(
            name = trimmedName,
            isActive = isActive,
            updatedAt = now,
            syncStatus = "PENDING",
            version = meter.version + 1
        )
        meterDao.updateMeter(updated)
        scope.launch {
            firestoreSyncManager.pushMeter(updated)
        }
        return Result.success(updated)
    }

    suspend fun deleteMeter(meterId: Long): Result<Unit> {
        return try {
            // 1. Delete associated daily readings
            dailyReadingDao.deleteReadingsForMeter(meterId)
            // 2. Delete billing cycle
            billingCycleDao.deleteBillingCycleForMeter(meterId)
            // 3. Delete meter
            meterDao.deleteMeter(meterId)

            // 4. Sync deletions to cloud
            scope.launch {
                firestoreSyncManager.deleteMeter(meterId)
                firestoreSyncManager.deleteBillingCycle(meterId)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateMeterName(meterId: Long, newName: String) {
        val meter = meterDao.getMeterById(meterId)
        if (meter != null) {
            val now = System.currentTimeMillis()
            val updated = meter.copy(
                name = newName.trim(),
                updatedAt = now,
                syncStatus = "PENDING",
                version = meter.version + 1
            )
            meterDao.updateMeter(updated)
            scope.launch {
                firestoreSyncManager.pushMeter(updated)
            }
        }
    }

    suspend fun updateBillingCycle(meterId: Long, newPreviousBillReading: Double) {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
        val now = System.currentTimeMillis()
        val formattedDate = dateFormat.format(Date(now))
        updateBillingCycleFull(meterId, newPreviousBillReading, formattedDate)
    }

    suspend fun updateBillingCycleFull(
        meterId: Long,
        newPreviousBillReading: Double,
        startDateFormatted: String
    ): Result<MeterBillingCycle> {
        val now = System.currentTimeMillis()
        val currentCycle = billingCycleDao.getBillingCycleForMeterDirect(meterId)
        val formattedDate = startDateFormatted.trim().ifBlank {
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
            dateFormat.format(Date(now))
        }

        val cycle = MeterBillingCycle(
            meterId = meterId,
            previousBillReading = newPreviousBillReading,
            cycleStartDate = currentCycle?.cycleStartDate ?: now,
            cycleStartFormattedDate = formattedDate,
            createdAt = currentCycle?.createdAt ?: now,
            updatedAt = now,
            deviceId = firestoreSyncManager.deviceId,
            syncStatus = "PENDING",
            version = (currentCycle?.version ?: 0L) + 1
        )
        billingCycleDao.insertOrUpdateBillingCycle(cycle)
        scope.launch {
            firestoreSyncManager.pushBillingCycle(cycle)
        }
        return Result.success(cycle)
    }

    suspend fun resetBillingCycle(meterId: Long): Result<MeterBillingCycle> {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
        val now = System.currentTimeMillis()
        val formattedDate = dateFormat.format(Date(now))
        return updateBillingCycleFull(meterId, 0.0, formattedDate)
    }

    suspend fun deleteBillingCycle(meterId: Long): Result<Unit> {
        return try {
            billingCycleDao.deleteBillingCycleForMeter(meterId)
            scope.launch {
                firestoreSyncManager.deleteBillingCycle(meterId)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkDuplicateReadingToday(meterId: Long, dateString: String): Boolean {
        return dailyReadingDao.getReadingForMeterAndDate(meterId, dateString) != null
    }

    /**
     * Add Daily Reading - Local First
     * Calculates Units Since Bill = currentReading - previousBillReading
     * Calculates 100-Unit Alert
     * Saves to Room immediately, then syncs to Firestore
     */
    suspend fun addDailyReading(
        meterId: Long,
        meterName: String,
        currentReading: Double,
        dateString: String,
        timestamp: Long = System.currentTimeMillis(),
        notes: String = ""
    ): Result<DailyReading> {
        val cycle = billingCycleDao.getBillingCycleForMeterDirect(meterId)
        val previousBill = cycle?.previousBillReading ?: 0.0

        if (currentReading < previousBill) {
            return Result.failure(
                IllegalArgumentException("Current Reading ($currentReading) cannot be lower than Previous Bill Reading ($previousBill).")
            )
        }

        val units = currentReading - previousBill
        val isAlert = units >= 100.0
        val now = System.currentTimeMillis()

        // Check if there's an existing reading for this meter & date to update/merge cleanly
        val existing = dailyReadingDao.getReadingForMeterAndDate(meterId, dateString)

        val reading = DailyReading(
            id = existing?.id ?: 0L,
            meterId = meterId,
            meterName = meterName,
            dateString = dateString,
            timestamp = timestamp,
            previousBillReading = previousBill,
            currentReading = currentReading,
            unitsSinceBill = units,
            isAlert = isAlert,
            alertStatusText = if (isAlert) "ALERT" else "NORMAL",
            notes = notes,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            deviceId = firestoreSyncManager.deviceId,
            syncStatus = "PENDING",
            version = (existing?.version ?: 0L) + 1
        )

        val insertedId = dailyReadingDao.insertReading(reading)
        val savedReading = reading.copy(id = if (reading.id != 0L) reading.id else insertedId)
        com.example.widget.MeterWidgetUpdater.updateAllWidgets(firestoreSyncManager.context)

        // Asynchronously persist to Firebase Firestore
        scope.launch {
            firestoreSyncManager.pushReading(savedReading)
        }

        return Result.success(savedReading)
    }

    /**
     * Update an existing reading, recalculate units & alerts, and sync to Firestore
     */
    suspend fun updateDailyReading(
        readingId: Long,
        currentReading: Double,
        dateString: String,
        notes: String
    ): Result<DailyReading> {
        val existing = dailyReadingDao.getReadingById(readingId)
            ?: return Result.failure(IllegalArgumentException("Reading record with ID #$readingId not found."))

        val cycle = billingCycleDao.getBillingCycleForMeterDirect(existing.meterId)
        val previousBill = cycle?.previousBillReading ?: existing.previousBillReading

        if (currentReading < previousBill) {
            return Result.failure(
                IllegalArgumentException("Current Reading ($currentReading) cannot be lower than Previous Bill Reading ($previousBill).")
            )
        }

        val units = currentReading - previousBill
        val isAlert = units >= 100.0
        val now = System.currentTimeMillis()

        val updatedReading = existing.copy(
            currentReading = currentReading,
            previousBillReading = previousBill,
            unitsSinceBill = units,
            isAlert = isAlert,
            alertStatusText = if (isAlert) "ALERT" else "NORMAL",
            dateString = dateString.ifBlank { existing.dateString },
            notes = notes,
            updatedAt = now,
            syncStatus = "PENDING",
            version = existing.version + 1
        )

        dailyReadingDao.updateReading(updatedReading)
        com.example.widget.MeterWidgetUpdater.updateAllWidgets(firestoreSyncManager.context)
        scope.launch {
            firestoreSyncManager.pushReading(updatedReading)
        }

        return Result.success(updatedReading)
    }

    suspend fun deleteReading(reading: DailyReading) {
        dailyReadingDao.deleteReading(reading)
        com.example.widget.MeterWidgetUpdater.updateAllWidgets(firestoreSyncManager.context)
        scope.launch {
            firestoreSyncManager.deleteReading(reading)
        }
    }

    suspend fun deleteReadingById(id: Long) {
        val reading = dailyReadingDao.getReadingById(id)
        if (reading != null) {
            deleteReading(reading)
        } else {
            dailyReadingDao.deleteReadingById(id)
        }
    }

    suspend fun syncWithCloud(): Result<Unit> {
        return firestoreSyncManager.performFullSync()
    }
}
