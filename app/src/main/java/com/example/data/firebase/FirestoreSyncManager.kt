package com.example.data.firebase

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.BillingCycleDao
import com.example.data.DailyReadingDao
import com.example.data.MeterDao
import com.example.data.SyncQueueDao
import com.example.model.DailyReading
import com.example.model.Meter
import com.example.model.MeterBillingCycle
import com.example.model.SyncQueueItem
import com.example.util.NetworkMonitor
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

enum class SyncState {
    SYNCED,      // 🟢 All data synced
    SYNCING,     // 🟠 Syncing...
    OFFLINE,     // 🔴 Offline — Data saved locally
    ERROR        // ⚠️ Sync Error
}

data class CloudSyncStatus(
    val state: SyncState = SyncState.SYNCED,
    val isOnline: Boolean = true,
    val isSyncing: Boolean = false,
    val lastSyncedAt: Long? = null,
    val pendingCount: Int = 0,
    val totalSyncedReadings: Int = 0,
    val totalSyncedMeters: Int = 0,
    val syncMessage: String? = null,
    val errorMessage: String? = null,
    val projectId: String = "kinza-digital-hub",
    val deviceId: String = ""
) {
    val displayStatusText: String
        get() = when {
            !isOnline -> "Offline — Data saved locally"
            isSyncing -> "Syncing..."
            pendingCount > 0 -> "$pendingCount record${if (pendingCount > 1) "s" else ""} waiting to sync"
            errorMessage != null -> "Sync Error: $errorMessage"
            lastSyncedAt != null -> "All data synced"
            else -> "All data synced"
        }
}

class FirestoreSyncManager(
    val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val tag = "FirestoreSyncManager"
    private val prefs: SharedPreferences =
        context.getSharedPreferences("meter_firestore_prefs", Context.MODE_PRIVATE)

    val deviceId: String = getOrCreateDeviceId()

    val networkMonitor = NetworkMonitor(context, scope)
    val authManager: FirebaseAuthManager = FirebaseAuthManager(context)
    val restApi: FirestoreRestApi = FirestoreRestApi()

    private var meterDao: MeterDao? = null
    private var billingCycleDao: BillingCycleDao? = null
    private var dailyReadingDao: DailyReadingDao? = null
    private var syncQueueDao: SyncQueueDao? = null

    private var metersListener: ListenerRegistration? = null
    private var cyclesListener: ListenerRegistration? = null
    private var readingsListener: ListenerRegistration? = null

    private var periodicSyncJob: Job? = null

    val firestore: FirebaseFirestore? by lazy {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                try {
                    FirebaseApp.initializeApp(context)
                } catch (e: Exception) {
                    val options = FirebaseOptions.Builder()
                        .setApplicationId("1:965439409911:android:aecb605b6222d36696cdf2")
                        .setApiKey("AIzaSyDvCAx1EU-o0XztFDbt7isO44vh-jSqI1Q")
                        .setProjectId("kinza-digital-hub")
                        .setStorageBucket("kinza-digital-hub.firebasestorage.app")
                        .setGcmSenderId("965439409911")
                        .build()
                    FirebaseApp.initializeApp(context, options)
                }
            }
            val db = FirebaseFirestore.getInstance()
            try {
                val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .build()
                db.firestoreSettings = settings
            } catch (se: Exception) {
                Log.w(tag, "FirestoreSettings already applied or skipped: ${se.message}")
            }
            Log.d(tag, "Firebase Firestore initialized for project: kinza-digital-hub, device: $deviceId")
            db
        } catch (e: Throwable) {
            Log.e(tag, "Firestore initialization error: ${e.message}", e)
            null
        }
    }

    private val _syncStatus = MutableStateFlow(
        CloudSyncStatus(
            state = if (networkMonitor.isOnline.value) SyncState.SYNCED else SyncState.OFFLINE,
            isOnline = networkMonitor.isOnline.value,
            lastSyncedAt = prefs.getLong("last_synced_at", 0L).takeIf { it > 0 },
            projectId = "kinza-digital-hub",
            deviceId = deviceId
        )
    )
    val syncStatus: StateFlow<CloudSyncStatus> = _syncStatus.asStateFlow()

    private fun getOrCreateDeviceId(): String {
        var id = prefs.getString("device_unique_id", null)
        if (id.isNullOrBlank()) {
            id = "android_${UUID.randomUUID().toString().take(8)}"
            prefs.edit().putString("device_unique_id", id).apply()
        }
        return id
    }

    /**
     * Start continuous listeners, network observer, and periodic sync
     */
    fun initialize(
        mDao: MeterDao,
        bDao: BillingCycleDao,
        dDao: DailyReadingDao,
        qDao: SyncQueueDao
    ) {
        this.meterDao = mDao
        this.billingCycleDao = bDao
        this.dailyReadingDao = dDao
        this.syncQueueDao = qDao

        // 1. Observe network changes
        scope.launch {
            networkMonitor.isOnline.collect { online ->
                Log.d(tag, "Network status changed: online=$online")
                _syncStatus.value = _syncStatus.value.copy(
                    isOnline = online,
                    state = if (!online) SyncState.OFFLINE else if (_syncStatus.value.isSyncing) SyncState.SYNCING else SyncState.SYNCED
                )
                if (online) {
                    // Auto sync immediately when internet comes back
                    syncPendingQueue()
                }
            }
        }

        // 2. Observe pending queue count
        scope.launch {
            qDao.getPendingCountFlow().collect { count ->
                _syncStatus.value = _syncStatus.value.copy(
                    pendingCount = count,
                    state = if (!_syncStatus.value.isOnline) SyncState.OFFLINE
                    else if (_syncStatus.value.isSyncing) SyncState.SYNCING
                    else SyncState.SYNCED
                )
            }
        }

        // 3. Start real-time Firestore listeners for multi-device synchronization
        startRealtimeListeners()

        // 4. Start periodic background sync (every 45 seconds when active)
        startPeriodicSync()

        // 5. Initial sync
        scope.launch {
            if (networkMonitor.isOnline.value) {
                syncPendingQueue()
            }
        }
    }

    private fun startPeriodicSync() {
        periodicSyncJob?.cancel()
        periodicSyncJob = scope.launch {
            while (true) {
                delay(45_000L)
                if (networkMonitor.isOnline.value) {
                    Log.d(tag, "Periodic sync triggered")
                    syncPendingQueue()
                }
            }
        }
    }

    /**
     * Real-time listeners for meters, billingCycles, dailyReadings collections
     */
    private fun startRealtimeListeners() {
        val userId = authManager.getUserId() ?: return
        val db = firestore ?: return
        val mDao = meterDao ?: return
        val bDao = billingCycleDao ?: return
        val dDao = dailyReadingDao ?: return

        // 1. Collection 'meters'
        try {
            metersListener?.remove()
            metersListener = db.collection("users/$userId/meters").addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(tag, "Meters listener error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    scope.launch(Dispatchers.IO) {
                        for (dc in snapshot.documentChanges) {
                            val doc = dc.document
                            val id = doc.getLong("id") ?: doc.id.removePrefix("meter_").toLongOrNull() ?: continue
                            when (dc.type) {
                                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                    val name = doc.getString("name") ?: "Meter $id"
                                    val isActive = doc.getBoolean("isActive") ?: true
                                    val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                                    val updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                                    val remoteDeviceId = doc.getString("deviceId") ?: ""
                                    val version = doc.getLong("version") ?: 1L

                                    val localMeter = mDao.getMeterById(id)
                                    // Conflict resolution: update only if remote is newer or not present locally
                                    if (localMeter == null || updatedAt >= localMeter.updatedAt || remoteDeviceId != deviceId) {
                                        mDao.insertMeter(
                                            Meter(
                                                id = id,
                                                name = name,
                                                isActive = isActive,
                                                createdAt = createdAt,
                                                updatedAt = updatedAt,
                                                deviceId = remoteDeviceId,
                                                lastSyncedAt = System.currentTimeMillis(),
                                                syncStatus = "SYNCED",
                                                version = version
                                            )
                                        )
                                    }
                                }
                                DocumentChange.Type.REMOVED -> {
                                    mDao.deleteMeter(id)
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Error setting up meters listener: ${e.message}")
        }

        // 2. Collection 'billingCycles'
        try {
            cyclesListener?.remove()
            cyclesListener = db.collection("users/$userId/billingCycles").addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(tag, "Billing cycles listener error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    scope.launch(Dispatchers.IO) {
                        for (dc in snapshot.documentChanges) {
                            val doc = dc.document
                            val meterId = doc.getLong("meterId") ?: doc.id.removePrefix("cycle_").toLongOrNull() ?: continue
                            when (dc.type) {
                                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                    val prevBill = doc.getDouble("previousBillReading") ?: 0.0
                                    val startDate = doc.getLong("cycleStartDate") ?: System.currentTimeMillis()
                                    val formattedDate = doc.getString("cycleStartFormattedDate") ?: ""
                                    val createdAt = doc.getLong("createdAt") ?: startDate
                                    val updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                                    val remoteDeviceId = doc.getString("deviceId") ?: ""
                                    val version = doc.getLong("version") ?: 1L

                                    val localCycle = bDao.getBillingCycleForMeterDirect(meterId)
                                    if (localCycle == null || updatedAt >= localCycle.updatedAt || remoteDeviceId != deviceId) {
                                        bDao.insertOrUpdateBillingCycle(
                                            MeterBillingCycle(
                                                meterId = meterId,
                                                previousBillReading = prevBill,
                                                cycleStartDate = startDate,
                                                cycleStartFormattedDate = formattedDate,
                                                createdAt = createdAt,
                                                updatedAt = updatedAt,
                                                deviceId = remoteDeviceId,
                                                lastSyncedAt = System.currentTimeMillis(),
                                                syncStatus = "SYNCED",
                                                version = version
                                            )
                                        )
                                    }
                                }
                                DocumentChange.Type.REMOVED -> {
                                    bDao.deleteBillingCycleForMeter(meterId)
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Error setting up billing cycles listener: ${e.message}")
        }

        // 3. Collection 'dailyReadings'
        try {
            readingsListener?.remove()
            readingsListener = db.collection("users/$userId/dailyReadings").addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(tag, "Daily readings listener error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    scope.launch(Dispatchers.IO) {
                        for (dc in snapshot.documentChanges) {
                            val doc = dc.document
                            val id = doc.getLong("id") ?: doc.id.removePrefix("reading_").toLongOrNull() ?: continue
                            val meterId = doc.getLong("meterId") ?: 1L
                            val dateString = doc.getString("dateString") ?: ""

                            when (dc.type) {
                                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                    val meterName = doc.getString("meterName") ?: "Meter"
                                    val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                                    val previousBillReading = doc.getDouble("previousBillReading") ?: 0.0
                                    val currentReading = doc.getDouble("currentReading") ?: 0.0
                                    val unitsSinceBill = doc.getDouble("unitsSinceBill") ?: (currentReading - previousBillReading)
                                    val isAlert = doc.getBoolean("isAlert") ?: (unitsSinceBill >= 100.0)
                                    val alertStatusText = doc.getString("alertStatusText") ?: if (isAlert) "ALERT" else "NORMAL"
                                    val notes = doc.getString("notes") ?: ""
                                    val createdAt = doc.getLong("createdAt") ?: timestamp
                                    val updatedAt = doc.getLong("updatedAt") ?: timestamp
                                    val remoteDeviceId = doc.getString("deviceId") ?: ""
                                    val version = doc.getLong("version") ?: 1L

                                    // Check duplicate by meterId + dateString or ID
                                    val existing = dDao.getReadingForMeterAndDate(meterId, dateString)
                                        ?: dDao.getReadingById(id)

                                    if (existing == null || updatedAt >= existing.updatedAt || remoteDeviceId != deviceId) {
                                        dDao.insertReading(
                                            DailyReading(
                                                id = existing?.id ?: id,
                                                meterId = meterId,
                                                meterName = meterName,
                                                dateString = dateString,
                                                timestamp = timestamp,
                                                previousBillReading = previousBillReading,
                                                currentReading = currentReading,
                                                unitsSinceBill = unitsSinceBill,
                                                isAlert = isAlert,
                                                alertStatusText = alertStatusText,
                                                notes = notes,
                                                createdAt = createdAt,
                                                updatedAt = updatedAt,
                                                deviceId = remoteDeviceId,
                                                lastSyncedAt = System.currentTimeMillis(),
                                                syncStatus = "SYNCED",
                                                version = version
                                            )
                                        )
                                    }
                                }
                                DocumentChange.Type.REMOVED -> {
                                    val existing = dDao.getReadingForMeterAndDate(meterId, dateString)
                                        ?: dDao.getReadingById(id)
                                    if (existing != null) {
                                        dDao.deleteReadingById(existing.id)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Error setting up readings listener: ${e.message}")
        }
    }

    // Helper for deterministic doc IDs
    fun getMeterDocId(meterId: Long): String = "meter_$meterId"
    fun getCycleDocId(meterId: Long): String = "cycle_$meterId"
    fun getReadingDocId(meterId: Long, dateString: String): String {
        val sanitizedDate = dateString.trim().replace(" ", "_").replace("/", "_").lowercase()
        return "reading_${meterId}_$sanitizedDate"
    }

    /**
     * Safe task awaiter with timeout to prevent coroutine suspension deadlocks
     */
    private suspend fun <T> awaitTaskWithTimeout(task: com.google.android.gms.tasks.Task<T>, timeoutMs: Long = 3500L): T? {
        return try {
            withTimeoutOrNull(timeoutMs) {
                suspendCancellableCoroutine { cont ->
                    task.addOnCompleteListener { completedTask ->
                        if (completedTask.isSuccessful) {
                            if (cont.isActive) cont.resume(completedTask.result)
                        } else {
                            if (cont.isActive) cont.resumeWithException(
                                completedTask.exception ?: Exception("Task failed")
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "awaitTaskWithTimeout notice: ${e.message}")
            null
        }
    }

    /**
     * Push a meter to Firestore
     */
    suspend fun pushMeter(meter: Meter) = withContext(Dispatchers.IO) {
        val userId = authManager.getUserId() ?: return@withContext
        val db = firestore
        val mDao = meterDao
        val qDao = syncQueueDao
        val docId = getMeterDocId(meter.id)

        val meterMap = hashMapOf(
            "id" to meter.id,
            "name" to meter.name,
            "isActive" to meter.isActive,
            "createdAt" to meter.createdAt,
            "updatedAt" to meter.updatedAt,
            "deviceId" to (meter.deviceId.ifEmpty { deviceId }),
            "lastSyncedAt" to System.currentTimeMillis(),
            "syncStatus" to "SYNCED",
            "version" to meter.version
        )

        var pushedSuccessfully = false

        // 1. Native SDK write
        if (db != null) {
            try {
                val task = db.collection("users/$userId/meters").document(docId).set(meterMap, SetOptions.merge())
                awaitTaskWithTimeout(task, 4000L)
                pushedSuccessfully = true
            } catch (e: Exception) {
                Log.w(tag, "SDK meter push notice: ${e.message}")
            }
        }

        // 2. Direct REST write with Auth token as fallback
        if (!pushedSuccessfully) {
            try {
                val idToken = authManager.getIdToken()
                val restResult = restApi.putDocument("users/$userId/meters", docId, meterMap, idToken)
                if (restResult.isSuccess) {
                    pushedSuccessfully = true
                }
            } catch (e: Exception) {
                Log.w(tag, "REST meter push notice: ${e.message}")
            }
        }

        if (pushedSuccessfully) {
            mDao?.markMeterSynced(meter.id, System.currentTimeMillis())
            qDao?.deleteByDocumentId(docId)
            qDao?.deleteOperation(docId)
            Log.d(tag, "Meter $docId successfully synced to Firestore")
        } else {
            Log.w(tag, "Failed to push meter $docId to Firestore")
            qDao?.insertOperation(
                SyncQueueItem(
                    operationId = docId,
                    collection = "users/$userId/meters",
                    documentId = docId,
                    operationType = "INSERT",
                    payloadJson = meter.name
                )
            )
        }
    }

    /**
     * Push a billing cycle to Firestore
     */
    suspend fun pushBillingCycle(cycle: MeterBillingCycle) = withContext(Dispatchers.IO) {
        val userId = authManager.getUserId() ?: return@withContext
        val db = firestore
        val bDao = billingCycleDao
        val qDao = syncQueueDao
        val docId = getCycleDocId(cycle.meterId)

        val cycleMap = hashMapOf(
            "meterId" to cycle.meterId,
            "previousBillReading" to cycle.previousBillReading,
            "cycleStartDate" to cycle.cycleStartDate,
            "cycleStartFormattedDate" to cycle.cycleStartFormattedDate,
            "createdAt" to cycle.createdAt,
            "updatedAt" to cycle.updatedAt,
            "deviceId" to (cycle.deviceId.ifEmpty { deviceId }),
            "lastSyncedAt" to System.currentTimeMillis(),
            "syncStatus" to "SYNCED",
            "version" to cycle.version
        )

        var pushedSuccessfully = false

        // 1. Native SDK write
        if (db != null) {
            try {
                val task = db.collection("users/$userId/billingCycles").document(docId).set(cycleMap, SetOptions.merge())
                awaitTaskWithTimeout(task, 4000L)
                pushedSuccessfully = true
            } catch (e: Exception) {
                Log.w(tag, "SDK cycle push notice: ${e.message}")
            }
        }

        // 2. Direct REST write with Auth token as fallback
        if (!pushedSuccessfully) {
            try {
                val idToken = authManager.getIdToken()
                val restResult = restApi.putDocument("users/$userId/billingCycles", docId, cycleMap, idToken)
                if (restResult.isSuccess) {
                    pushedSuccessfully = true
                }
            } catch (e: Exception) {
                Log.w(tag, "REST cycle push notice: ${e.message}")
            }
        }

        if (pushedSuccessfully) {
            bDao?.markBillingCycleSynced(cycle.meterId, System.currentTimeMillis())
            qDao?.deleteByDocumentId(docId)
            qDao?.deleteOperation(docId)
            Log.d(tag, "Billing cycle $docId successfully synced to Firestore")
        } else {
            Log.w(tag, "Failed to push billing cycle $docId to Firestore")
            qDao?.insertOperation(
                SyncQueueItem(
                    operationId = docId,
                    collection = "users/$userId/billingCycles",
                    documentId = docId,
                    operationType = "INSERT",
                    payloadJson = cycle.previousBillReading.toString()
                )
            )
        }
    }

    /**
     * Push a daily reading to Firestore
     */
    suspend fun pushReading(reading: DailyReading) = withContext(Dispatchers.IO) {
        val userId = authManager.getUserId() ?: return@withContext
        val db = firestore
        val dDao = dailyReadingDao
        val qDao = syncQueueDao
        val docId = getReadingDocId(reading.meterId, reading.dateString)

        val readingMap = hashMapOf(
            "id" to reading.id,
            "meterId" to reading.meterId,
            "meterName" to reading.meterName,
            "dateString" to reading.dateString,
            "timestamp" to reading.timestamp,
            "previousBillReading" to reading.previousBillReading,
            "currentReading" to reading.currentReading,
            "unitsSinceBill" to reading.unitsSinceBill,
            "isAlert" to reading.isAlert,
            "alertStatusText" to reading.alertStatusText,
            "notes" to reading.notes,
            "createdAt" to reading.createdAt,
            "updatedAt" to reading.updatedAt,
            "deviceId" to (reading.deviceId.ifEmpty { deviceId }),
            "lastSyncedAt" to System.currentTimeMillis(),
            "syncStatus" to "SYNCED",
            "version" to reading.version
        )

        var pushedSuccessfully = false

        // 1. Native SDK write
        if (db != null) {
            try {
                val task = db.collection("users/$userId/dailyReadings").document(docId).set(readingMap, SetOptions.merge())
                awaitTaskWithTimeout(task, 4000L)
                pushedSuccessfully = true
            } catch (e: Exception) {
                Log.w(tag, "SDK reading push notice: ${e.message}")
            }
        }

        // 2. Direct REST write with Auth token as fallback
        if (!pushedSuccessfully) {
            try {
                val idToken = authManager.getIdToken()
                val restResult = restApi.putDocument("users/$userId/dailyReadings", docId, readingMap, idToken)
                if (restResult.isSuccess) {
                    pushedSuccessfully = true
                }
            } catch (e: Exception) {
                Log.w(tag, "REST reading push notice: ${e.message}")
            }
        }

        if (pushedSuccessfully) {
            dDao?.markReadingSynced(reading.id, System.currentTimeMillis())
            qDao?.deleteByDocumentId(docId)
            qDao?.deleteOperation(docId)
            Log.d(tag, "Daily reading $docId successfully synced to Firestore")
        } else {
            Log.w(tag, "Failed to push daily reading $docId to Firestore")
            qDao?.insertOperation(
                SyncQueueItem(
                    operationId = docId,
                    collection = "users/$userId/dailyReadings",
                    documentId = docId,
                    operationType = "INSERT",
                    payloadJson = "${reading.currentReading}"
                )
            )
        }
    }

    /**
     * Delete a reading from Firestore
     */
    suspend fun deleteReading(reading: DailyReading) = withContext(Dispatchers.IO) {
        val userId = authManager.getUserId() ?: return@withContext
        val db = firestore
        val docId = getReadingDocId(reading.meterId, reading.dateString)
        val qDao = syncQueueDao

        try {
            val idToken = authManager.getIdToken()
            restApi.deleteDocument("users/$userId/dailyReadings", docId, idToken)
        } catch (e: Exception) {
            Log.w(tag, "REST delete reading notice: ${e.message}")
        }

        if (db != null) {
            try {
                val task = db.collection("users/$userId/dailyReadings").document(docId).delete()
                awaitTaskWithTimeout(task, 3000L)
            } catch (e: Exception) {
                Log.w(tag, "deleteReading SDK note: ${e.message}")
            }
        }
        qDao?.deleteByDocumentId(docId)
        qDao?.deleteOperation("del_$docId")
    }

    /**
     * Delete a meter from Firestore
     */
    suspend fun deleteMeter(meterId: Long) = withContext(Dispatchers.IO) {
        val userId = authManager.getUserId() ?: return@withContext
        val db = firestore
        val docId = getMeterDocId(meterId)
        val qDao = syncQueueDao

        try {
            val idToken = authManager.getIdToken()
            restApi.deleteDocument("users/$userId/meters", docId, idToken)
        } catch (e: Exception) {
            Log.w(tag, "REST delete meter notice: ${e.message}")
        }

        if (db != null) {
            try {
                val task = db.collection("users/$userId/meters").document(docId).delete()
                awaitTaskWithTimeout(task, 3000L)
            } catch (e: Exception) {
                Log.w(tag, "deleteMeter SDK note: ${e.message}")
            }
        }
        qDao?.deleteByDocumentId(docId)
        qDao?.deleteOperation("del_$docId")
    }

    /**
     * Delete a billing cycle from Firestore
     */
    suspend fun deleteBillingCycle(meterId: Long) = withContext(Dispatchers.IO) {
        val userId = authManager.getUserId() ?: return@withContext
        val db = firestore
        val docId = getCycleDocId(meterId)
        val qDao = syncQueueDao

        try {
            val idToken = authManager.getIdToken()
            restApi.deleteDocument("users/$userId/billingCycles", docId, idToken)
        } catch (e: Exception) {
            Log.w(tag, "REST delete cycle notice: ${e.message}")
        }

        if (db != null) {
            try {
                val task = db.collection("users/$userId/billingCycles").document(docId).delete()
                awaitTaskWithTimeout(task, 3000L)
            } catch (e: Exception) {
                Log.w(tag, "deleteBillingCycle SDK note: ${e.message}")
            }
        }
        qDao?.deleteByDocumentId(docId)
        qDao?.deleteOperation("del_$docId")
    }

    /**
     * Process pending queue and perform bidirectional sync
     */
    suspend fun syncPendingQueue(): Result<Unit> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(
            IllegalStateException("Firestore is not initialized")
        )
        val mDao = meterDao ?: return@withContext Result.failure(IllegalStateException("No DAOs"))
        val bDao = billingCycleDao ?: return@withContext Result.failure(IllegalStateException("No DAOs"))
        val dDao = dailyReadingDao ?: return@withContext Result.failure(IllegalStateException("No DAOs"))
        val qDao = syncQueueDao

        if (!networkMonitor.isOnline.value) {
            _syncStatus.value = _syncStatus.value.copy(
                state = SyncState.OFFLINE,
                isSyncing = false,
                isOnline = false
            )
            return@withContext Result.failure(IllegalStateException("Device is offline"))
        }

        val userId = authManager.getUserId() ?: run {
            _syncStatus.value = _syncStatus.value.copy(
                state = SyncState.ERROR,
                isSyncing = false,
                errorMessage = "Not signed in"
            )
            return@withContext Result.failure(IllegalStateException("Not signed in"))
        }

        _syncStatus.value = _syncStatus.value.copy(
            state = SyncState.SYNCING,
            isSyncing = true,
            errorMessage = null,
            syncMessage = "Syncing with Firebase Firestore..."
        )

        try {
            // 1. Push all pending meters
            val pendingMeters = mDao.getPendingMeters()
            for (m in pendingMeters) {
                pushMeter(m)
            }

            // 2. Push all pending billing cycles
            val pendingCycles = bDao.getPendingBillingCycles()
            for (c in pendingCycles) {
                pushBillingCycle(c)
            }

            // 3. Push all pending daily readings
            val pendingReadings = dDao.getPendingReadings()
            for (r in pendingReadings) {
                pushReading(r)
            }

            // 4. Pull remote snapshots safely with timeout
            try {
                val metersSnap = awaitTaskWithTimeout(db.collection("users/$userId/meters").get(), 4000L)
                if (metersSnap != null) {
                    for (doc in metersSnap.documents) {
                        val id = doc.getLong("id") ?: doc.id.removePrefix("meter_").toLongOrNull() ?: continue
                        val name = doc.getString("name") ?: "Meter $id"
                        val isActive = doc.getBoolean("isActive") ?: true
                        val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                        val updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                        val remoteDeviceId = doc.getString("deviceId") ?: ""
                        val version = doc.getLong("version") ?: 1L

                        val local = mDao.getMeterById(id)
                        if (local == null) {
                            mDao.insertMeter(
                                Meter(
                                    id = id,
                                    name = name,
                                    isActive = isActive,
                                    createdAt = createdAt,
                                    updatedAt = updatedAt,
                                    deviceId = remoteDeviceId,
                                    lastSyncedAt = System.currentTimeMillis(),
                                    syncStatus = "SYNCED",
                                    version = version
                                )
                            )
                        }
                    }
                }

                val readingsSnap = awaitTaskWithTimeout(db.collection("users/$userId/dailyReadings").get(), 4000L)
                if (readingsSnap != null) {
                    for (doc in readingsSnap.documents) {
                        val id = doc.getLong("id") ?: doc.id.removePrefix("reading_").toLongOrNull() ?: continue
                        val meterId = doc.getLong("meterId") ?: 1L
                        val dateString = doc.getString("dateString") ?: ""
                        val meterName = doc.getString("meterName") ?: "Meter"
                        val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                        val prevBill = doc.getDouble("previousBillReading") ?: 0.0
                        val currentR = doc.getDouble("currentReading") ?: 0.0
                        val units = doc.getDouble("unitsSinceBill") ?: (currentR - prevBill)
                        val isAlert = doc.getBoolean("isAlert") ?: (units >= 100.0)
                        val alertText = doc.getString("alertStatusText") ?: if (isAlert) "ALERT" else "NORMAL"
                        val notes = doc.getString("notes") ?: ""
                        val remoteDeviceId = doc.getString("deviceId") ?: ""
                        val version = doc.getLong("version") ?: 1L

                        val existing = dDao.getReadingForMeterAndDate(meterId, dateString)
                            ?: dDao.getReadingById(id)

                        if (existing == null) {
                            dDao.insertReading(
                                DailyReading(
                                    id = id,
                                    meterId = meterId,
                                    meterName = meterName,
                                    dateString = dateString,
                                    timestamp = timestamp,
                                    previousBillReading = prevBill,
                                    currentReading = currentR,
                                    unitsSinceBill = units,
                                    isAlert = isAlert,
                                    alertStatusText = alertText,
                                    notes = notes,
                                    createdAt = timestamp,
                                    updatedAt = timestamp,
                                    deviceId = remoteDeviceId,
                                    lastSyncedAt = System.currentTimeMillis(),
                                    syncStatus = "SYNCED",
                                    version = version
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(tag, "Remote pull note: ${e.message}")
            }

            // 5. Update global app settings in Firestore
            try {
                val settingsMap = hashMapOf(
                    "defaultAlertThreshold" to 100.0,
                    "appTitle" to "Electric Meter Reading",
                    "lastSyncedAt" to System.currentTimeMillis(),
                    "updatedAt" to System.currentTimeMillis(),
                    "deviceId" to deviceId,
                    "version" to 1L
                )
                db.collection("users/$userId/appSettings").document("global_config").set(settingsMap, SetOptions.merge())
            } catch (e: Exception) {
                Log.w(tag, "Settings sync error: ${e.message}")
            }

            // 6. Clean completed queue items
            qDao?.clearCompletedOperations()

            val now = System.currentTimeMillis()
            prefs.edit().putLong("last_synced_at", now).apply()

            val remainingPending = qDao?.getPendingOperations()?.size ?: 0

            _syncStatus.value = _syncStatus.value.copy(
                state = SyncState.SYNCED,
                isSyncing = false,
                lastSyncedAt = now,
                pendingCount = remainingPending,
                syncMessage = "All data synced",
                errorMessage = null
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(tag, "Sync pending queue error: ${e.message}")
            _syncStatus.value = _syncStatus.value.copy(
                state = if (networkMonitor.isOnline.value) SyncState.SYNCED else SyncState.OFFLINE,
                isSyncing = false,
                errorMessage = e.localizedMessage ?: "Sync error"
            )
            Result.failure(e)
        } finally {
            if (_syncStatus.value.isSyncing) {
                _syncStatus.value = _syncStatus.value.copy(
                    isSyncing = false,
                    state = if (networkMonitor.isOnline.value) SyncState.SYNCED else SyncState.OFFLINE
                )
            }
        }
    }

    /**
     * Pull/Retrieve all remote data from Firestore collection into local Room database
     */
    suspend fun pullDataFromFirestore(): Result<Int> = withContext(Dispatchers.IO) {
        val mDao = meterDao ?: return@withContext Result.failure(IllegalStateException("MeterDao is null"))
        val bDao = billingCycleDao ?: return@withContext Result.failure(IllegalStateException("BillingCycleDao is null"))
        val dDao = dailyReadingDao ?: return@withContext Result.failure(IllegalStateException("DailyReadingDao is null"))
        val db = firestore ?: return@withContext Result.failure(IllegalStateException("Firestore is null"))

        if (!networkMonitor.isOnline.value) {
            return@withContext Result.failure(IllegalStateException("Device is offline"))
        }

        val userId = authManager.getUserId() ?: return@withContext Result.failure(IllegalStateException("Not signed in"))

        _syncStatus.value = _syncStatus.value.copy(
            state = SyncState.SYNCING,
            isSyncing = true,
            syncMessage = "Retrieving (pulling) data from Firestore..."
        )

        var pulledCount = 0
        try {
            // 1. Pull meters
            val metersSnap = awaitTaskWithTimeout(db.collection("users/$userId/meters").get(), 5000L)
            if (metersSnap != null) {
                for (doc in metersSnap.documents) {
                    val id = doc.getLong("id") ?: doc.id.removePrefix("meter_").toLongOrNull() ?: continue
                    val name = doc.getString("name") ?: "Meter $id"
                    val isActive = doc.getBoolean("isActive") ?: true
                    val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    val updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                    val remoteDeviceId = doc.getString("deviceId") ?: ""
                    val version = doc.getLong("version") ?: 1L

                    val local = mDao.getMeterById(id)
                    if (local == null) {
                        mDao.insertMeter(
                            Meter(
                                id = id,
                                name = name,
                                isActive = isActive,
                                createdAt = createdAt,
                                updatedAt = updatedAt,
                                deviceId = remoteDeviceId,
                                lastSyncedAt = System.currentTimeMillis(),
                                syncStatus = "SYNCED",
                                version = version
                            )
                        )
                        pulledCount++
                    }
                }
            }

            // 2. Pull billing cycles
            val cyclesSnap = awaitTaskWithTimeout(db.collection("users/$userId/billingCycles").get(), 5000L)
            if (cyclesSnap != null) {
                for (doc in cyclesSnap.documents) {
                    val meterId = doc.getLong("meterId") ?: doc.id.removePrefix("cycle_").toLongOrNull() ?: continue
                    val prevReading = doc.getDouble("previousBillReading") ?: 0.0
                    val cycleStartDate = doc.getLong("cycleStartDate") ?: System.currentTimeMillis()
                    val cycleStartFormattedDate = doc.getString("cycleStartFormattedDate") ?: ""
                    val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    val updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                    val remoteDeviceId = doc.getString("deviceId") ?: ""
                    val version = doc.getLong("version") ?: 1L

                    val localCycle = bDao.getBillingCycleForMeterDirect(meterId)
                    if (localCycle == null) {
                        bDao.insertOrUpdateBillingCycle(
                            MeterBillingCycle(
                                meterId = meterId,
                                previousBillReading = prevReading,
                                cycleStartDate = cycleStartDate,
                                cycleStartFormattedDate = cycleStartFormattedDate,
                                createdAt = createdAt,
                                updatedAt = updatedAt,
                                deviceId = remoteDeviceId,
                                lastSyncedAt = System.currentTimeMillis(),
                                syncStatus = "SYNCED",
                                version = version
                            )
                        )
                        pulledCount++
                    }
                }
            }

            // 3. Pull daily readings
            val readingsSnap = awaitTaskWithTimeout(db.collection("users/$userId/dailyReadings").get(), 5000L)
            if (readingsSnap != null) {
                for (doc in readingsSnap.documents) {
                    val id = doc.getLong("id") ?: doc.id.removePrefix("reading_").toLongOrNull() ?: continue
                    val meterId = doc.getLong("meterId") ?: 1L
                    val dateString = doc.getString("dateString") ?: ""
                    val meterName = doc.getString("meterName") ?: "Meter"
                    val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                    val prevBill = doc.getDouble("previousBillReading") ?: 0.0
                    val currentR = doc.getDouble("currentReading") ?: 0.0
                    val units = doc.getDouble("unitsSinceBill") ?: (currentR - prevBill)
                    val isAlert = doc.getBoolean("isAlert") ?: (units >= 100.0)
                    val alertText = doc.getString("alertStatusText") ?: if (isAlert) "ALERT" else "NORMAL"
                    val notes = doc.getString("notes") ?: ""
                    val remoteDeviceId = doc.getString("deviceId") ?: ""
                    val version = doc.getLong("version") ?: 1L

                    val existing = dDao.getReadingForMeterAndDate(meterId, dateString)
                        ?: dDao.getReadingById(id)

                    if (existing == null) {
                        dDao.insertReading(
                            DailyReading(
                                id = id,
                                meterId = meterId,
                                meterName = meterName,
                                dateString = dateString,
                                timestamp = timestamp,
                                previousBillReading = prevBill,
                                currentReading = currentR,
                                unitsSinceBill = units,
                                isAlert = isAlert,
                                alertStatusText = alertText,
                                notes = notes,
                                createdAt = timestamp,
                                updatedAt = timestamp,
                                deviceId = remoteDeviceId,
                                lastSyncedAt = System.currentTimeMillis(),
                                syncStatus = "SYNCED",
                                version = version
                            )
                        )
                        pulledCount++
                    }
                }
            }

            val now = System.currentTimeMillis()
            prefs.edit().putLong("last_synced_at", now).apply()
            _syncStatus.value = _syncStatus.value.copy(
                state = SyncState.SYNCED,
                isSyncing = false,
                lastSyncedAt = now,
                syncMessage = "Successfully retrieved $pulledCount records from Firestore!"
            )
            Result.success(pulledCount)
        } catch (e: Exception) {
            Log.w(tag, "pullDataFromFirestore error: ${e.message}")
            _syncStatus.value = _syncStatus.value.copy(
                state = if (networkMonitor.isOnline.value) SyncState.SYNCED else SyncState.OFFLINE,
                isSyncing = false,
                errorMessage = e.message
            )
            Result.failure(e)
        }
    }

    /**
     * Force push all local Room database records to Firestore cloud
     */
    suspend fun forcePushAllData(): Result<Int> = withContext(Dispatchers.IO) {
        val mDao = meterDao ?: return@withContext Result.failure(IllegalStateException("MeterDao is null"))
        val bDao = billingCycleDao ?: return@withContext Result.failure(IllegalStateException("BillingCycleDao is null"))
        val dDao = dailyReadingDao ?: return@withContext Result.failure(IllegalStateException("DailyReadingDao is null"))

        _syncStatus.value = _syncStatus.value.copy(
            state = SyncState.SYNCING,
            isSyncing = true,
            syncMessage = "Pushing all data to Firestore (kinza-digital-hub)..."
        )

        var totalPushed = 0
        try {
            val meters = mDao.getAllMetersList()
            for (meter in meters) {
                pushMeter(meter)
                totalPushed++
            }

            val cycles = bDao.getAllBillingCyclesList()
            for (cycle in cycles) {
                pushBillingCycle(cycle)
                totalPushed++
            }

            val readings = dDao.getAllDailyReadingsList()
            for (reading in readings) {
                pushReading(reading)
                totalPushed++
            }

            val now = System.currentTimeMillis()
            prefs.edit().putLong("last_synced_at", now).apply()
            _syncStatus.value = _syncStatus.value.copy(
                state = SyncState.SYNCED,
                isSyncing = false,
                lastSyncedAt = now,
                totalSyncedMeters = meters.size,
                totalSyncedReadings = readings.size,
                syncMessage = "Successfully pushed $totalPushed records to Firestore!"
            )
            Result.success(totalPushed)
        } catch (e: Exception) {
            Log.w(tag, "forcePushAllData error: ${e.message}")
            _syncStatus.value = _syncStatus.value.copy(
                state = if (networkMonitor.isOnline.value) SyncState.SYNCED else SyncState.OFFLINE,
                isSyncing = false,
                errorMessage = e.message
            )
            Result.failure(e)
        }
    }

    /**
     * Full manual sync trigger (e.g. from Cloud dialog)
     */
    suspend fun performFullSync(): Result<Unit> {
        forcePushAllData()
        return syncPendingQueue()
    }

    /**
     * Combined sync and upload function:
     * 1. Pulls remote records from Firestore and updates/reconciles local Room storage.
     * 2. Detects admin deletions: removes local records if they were deleted on remote Firestore.
     * 3. Pushes/uploads any pending local additions or edits to Firestore.
     */
    suspend fun performCombinedSyncAndUpload(): Result<Int> = withContext(Dispatchers.IO) {
        val mDao = meterDao ?: return@withContext Result.failure(IllegalStateException("MeterDao is null"))
        val bDao = billingCycleDao ?: return@withContext Result.failure(IllegalStateException("BillingCycleDao is null"))
        val dDao = dailyReadingDao ?: return@withContext Result.failure(IllegalStateException("DailyReadingDao is null"))
        val db = firestore ?: return@withContext Result.failure(IllegalStateException("Firestore is null"))

        if (!networkMonitor.isOnline.value) {
            return@withContext Result.failure(IllegalStateException("Device is offline"))
        }

        val userId = authManager.getUserId() ?: return@withContext Result.failure(IllegalStateException("Not signed in"))

        _syncStatus.value = _syncStatus.value.copy(
            state = SyncState.SYNCING,
            isSyncing = true,
            syncMessage = "Checking remote database and syncing..."
        )

        var totalProcessed = 0
        try {
            val localMeters = mDao.getAllMetersList()
            val localCycles = bDao.getAllBillingCyclesList()
            val localReadings = dDao.getAllDailyReadingsList()

            // Step 1: Pull remote data and verify remote deletions (admin deletion reflection)
            val remoteMeterIds = mutableSetOf<Long>()
            val metersSnap = awaitTaskWithTimeout(db.collection("users/$userId/meters").get(), 5000L)
            if (metersSnap != null) {
                for (doc in metersSnap.documents) {
                    val id = doc.getLong("id") ?: doc.id.removePrefix("meter_").toLongOrNull() ?: continue
                    remoteMeterIds.add(id)
                    val name = doc.getString("name") ?: "Meter $id"
                    val isActive = doc.getBoolean("isActive") ?: true
                    val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    val updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                    val remoteDeviceId = doc.getString("deviceId") ?: ""
                    val version = doc.getLong("version") ?: 1L

                    val local = mDao.getMeterById(id)
                    if (local == null) {
                        mDao.insertMeter(
                            Meter(
                                id = id,
                                name = name,
                                isActive = isActive,
                                createdAt = createdAt,
                                updatedAt = updatedAt,
                                deviceId = remoteDeviceId,
                                lastSyncedAt = System.currentTimeMillis(),
                                syncStatus = "SYNCED",
                                version = version
                            )
                        )
                        totalProcessed++
                    } else if (local.syncStatus == "SYNCED") {
                        mDao.updateMeter(
                            local.copy(
                                name = name,
                                isActive = isActive,
                                updatedAt = updatedAt,
                                lastSyncedAt = System.currentTimeMillis(),
                                syncStatus = "SYNCED",
                                version = version
                            )
                        )
                    }
                }
            }

            // Remove local meters that were deleted on remote Firestore by admin
            for (localMeter in localMeters) {
                if (localMeter.syncStatus == "SYNCED" && !remoteMeterIds.contains(localMeter.id)) {
                    Log.d(tag, "Admin deleted meter ${localMeter.id} on Firestore. Removing locally.")
                    mDao.deleteMeter(localMeter.id)
                    // Also delete associated billing cycles and readings locally
                    bDao.deleteBillingCycleForMeter(localMeter.id)
                    dDao.deleteReadingsForMeter(localMeter.id)
                }
            }

            val remoteCycleMeterIds = mutableSetOf<Long>()
            val cyclesSnap = awaitTaskWithTimeout(db.collection("users/$userId/billingCycles").get(), 5000L)
            if (cyclesSnap != null) {
                for (doc in cyclesSnap.documents) {
                    val meterId = doc.getLong("meterId") ?: doc.id.removePrefix("cycle_").toLongOrNull() ?: continue
                    remoteCycleMeterIds.add(meterId)
                    val prevReading = doc.getDouble("previousBillReading") ?: 0.0
                    val cycleStartDate = doc.getLong("cycleStartDate") ?: System.currentTimeMillis()
                    val cycleStartFormattedDate = doc.getString("cycleStartFormattedDate") ?: ""
                    val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    val updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                    val remoteDeviceId = doc.getString("deviceId") ?: ""
                    val version = doc.getLong("version") ?: 1L

                    val localCycle = bDao.getBillingCycleForMeterDirect(meterId)
                    if (localCycle == null) {
                        bDao.insertOrUpdateBillingCycle(
                            MeterBillingCycle(
                                meterId = meterId,
                                previousBillReading = prevReading,
                                cycleStartDate = cycleStartDate,
                                cycleStartFormattedDate = cycleStartFormattedDate,
                                createdAt = createdAt,
                                updatedAt = updatedAt,
                                deviceId = remoteDeviceId,
                                lastSyncedAt = System.currentTimeMillis(),
                                syncStatus = "SYNCED",
                                version = version
                            )
                        )
                        totalProcessed++
                    } else if (localCycle.syncStatus == "SYNCED") {
                        bDao.insertOrUpdateBillingCycle(
                            localCycle.copy(
                                previousBillReading = prevReading,
                                cycleStartDate = cycleStartDate,
                                cycleStartFormattedDate = cycleStartFormattedDate,
                                updatedAt = updatedAt,
                                lastSyncedAt = System.currentTimeMillis(),
                                syncStatus = "SYNCED",
                                version = version
                            )
                        )
                    }
                }
            }

            for (localCycle in localCycles) {
                if (localCycle.syncStatus == "SYNCED" && !remoteCycleMeterIds.contains(localCycle.meterId)) {
                    Log.d(tag, "Admin deleted billing cycle for meter ${localCycle.meterId} on Firestore. Removing locally.")
                    bDao.deleteBillingCycleForMeter(localCycle.meterId)
                }
            }

            val remoteReadingIds = mutableSetOf<Long>()
            val readingsSnap = awaitTaskWithTimeout(db.collection("users/$userId/dailyReadings").get(), 5000L)
            if (readingsSnap != null) {
                for (doc in readingsSnap.documents) {
                    val id = doc.getLong("id") ?: doc.id.removePrefix("reading_").toLongOrNull() ?: continue
                    remoteReadingIds.add(id)
                    val meterId = doc.getLong("meterId") ?: 1L
                    val dateString = doc.getString("dateString") ?: ""
                    val meterName = doc.getString("meterName") ?: "Meter"
                    val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                    val prevBill = doc.getDouble("previousBillReading") ?: 0.0
                    val currentR = doc.getDouble("currentReading") ?: 0.0
                    val units = doc.getDouble("unitsSinceBill") ?: (currentR - prevBill)
                    val isAlert = doc.getBoolean("isAlert") ?: (units >= 100.0)
                    val alertText = doc.getString("alertStatusText") ?: if (isAlert) "ALERT" else "NORMAL"
                    val notes = doc.getString("notes") ?: ""
                    val remoteDeviceId = doc.getString("deviceId") ?: ""
                    val version = doc.getLong("version") ?: 1L

                    val existing = dDao.getReadingForMeterAndDate(meterId, dateString)
                        ?: dDao.getReadingById(id)

                    if (existing == null) {
                        dDao.insertReading(
                            DailyReading(
                                id = id,
                                meterId = meterId,
                                meterName = meterName,
                                dateString = dateString,
                                timestamp = timestamp,
                                previousBillReading = prevBill,
                                currentReading = currentR,
                                unitsSinceBill = units,
                                isAlert = isAlert,
                                alertStatusText = alertText,
                                notes = notes,
                                createdAt = timestamp,
                                updatedAt = timestamp,
                                deviceId = remoteDeviceId,
                                lastSyncedAt = System.currentTimeMillis(),
                                syncStatus = "SYNCED",
                                version = version
                            )
                        )
                        totalProcessed++
                    } else if (existing.syncStatus == "SYNCED") {
                        dDao.updateReading(
                            existing.copy(
                                meterName = meterName,
                                previousBillReading = prevBill,
                                currentReading = currentR,
                                unitsSinceBill = units,
                                isAlert = isAlert,
                                alertStatusText = alertText,
                                notes = notes,
                                updatedAt = timestamp,
                                lastSyncedAt = System.currentTimeMillis(),
                                syncStatus = "SYNCED",
                                version = version
                            )
                        )
                    }
                }
            }

            for (localReading in localReadings) {
                if (localReading.syncStatus == "SYNCED" && !remoteReadingIds.contains(localReading.id)) {
                    Log.d(tag, "Admin deleted reading ${localReading.id} on Firestore. Removing locally.")
                    dDao.deleteReading(localReading)
                }
            }

            // Step 2: Push/Upload pending local data to Firestore
            val pendingMeters = mDao.getAllMetersList().filter { it.syncStatus == "PENDING" }
            for (m in pendingMeters) {
                pushMeter(m)
                totalProcessed++
            }
            val pendingCycles = bDao.getAllBillingCyclesList().filter { it.syncStatus == "PENDING" }
            for (c in pendingCycles) {
                pushBillingCycle(c)
                totalProcessed++
            }
            val pendingReadings = dDao.getAllDailyReadingsList().filter { it.syncStatus == "PENDING" }
            for (r in pendingReadings) {
                pushReading(r)
                totalProcessed++
            }

            val now = System.currentTimeMillis()
            prefs.edit().putLong("last_synced_at", now).apply()
            _syncStatus.value = _syncStatus.value.copy(
                state = SyncState.SYNCED,
                isSyncing = false,
                lastSyncedAt = now,
                syncMessage = "Combined sync & upload completed successfully!"
            )
            Result.success(totalProcessed)
        } catch (e: Exception) {
            Log.w(tag, "performCombinedSyncAndUpload error: ${e.message}")
            _syncStatus.value = _syncStatus.value.copy(
                state = if (networkMonitor.isOnline.value) SyncState.SYNCED else SyncState.OFFLINE,
                isSyncing = false,
                errorMessage = e.message
            )
            Result.failure(e)
        }
    }

    fun clearStatusMessage() {
        _syncStatus.value = _syncStatus.value.copy(syncMessage = null, errorMessage = null)
    }

    fun stop() {
        periodicSyncJob?.cancel()
        networkMonitor.unregister()
        metersListener?.remove()
        cyclesListener?.remove()
        readingsListener?.remove()
    }
}
