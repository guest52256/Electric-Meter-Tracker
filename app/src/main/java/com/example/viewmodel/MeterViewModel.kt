package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.MeterRepository
import com.example.data.firebase.CloudSyncStatus
import com.example.data.firebase.FirestoreSyncManager
import com.example.model.DailyReading
import com.example.model.Meter
import com.example.model.MeterBillingCycle
import com.example.ui.theme.AppColorPalette
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.ThemePreferences
import com.example.ui.navigation.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MeterDashboardCardState(
    val meter: Meter,
    val billingCycle: MeterBillingCycle?,
    val latestReading: DailyReading?,
    val previousBillReading: Double,
    val currentReading: Double,
    val unitsSinceBill: Double,
    val isAlert: Boolean,
    val lastReadingDate: String
)

data class DashboardOverviewState(
    val totalActiveMeters: Int = 0,
    val totalCurrentUnits: Double = 0.0,
    val totalAlertsCount: Int = 0,
    val totalReadingsCount: Int = 0,
    val meterCards: List<MeterDashboardCardState> = emptyList(),
    val recentReadings: List<DailyReading> = emptyList()
)

class MeterViewModel(
    application: Application,
    private val repository: MeterRepository
) : AndroidViewModel(application) {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)

    // ----------------------------------------------------
    // THEME & CUSTOMIZATION MANAGEMENT
    // ----------------------------------------------------
    private val themePreferences = ThemePreferences(application)
    val themeMode: StateFlow<AppThemeMode> = themePreferences.themeMode
    val colorPalette: StateFlow<AppColorPalette> = themePreferences.colorPalette
    val showThemeDialog = MutableStateFlow(false)

    fun setThemeMode(mode: AppThemeMode) {
        themePreferences.setThemeMode(mode)
    }

    fun setColorPalette(palette: AppColorPalette) {
        themePreferences.setColorPalette(palette)
    }

    fun openThemeDialog() {
        showThemeDialog.value = true
    }

    fun dismissThemeDialog() {
        showThemeDialog.value = false
    }

    // ----------------------------------------------------
    // APP EXIT CONFIRMATION DIALOG
    // ----------------------------------------------------
    val showExitConfirmDialog = MutableStateFlow(false)

    fun openExitConfirmDialog() {
        showExitConfirmDialog.value = true
    }

    fun dismissExitConfirmDialog() {
        showExitConfirmDialog.value = false
    }

    // ----------------------------------------------------
    // FIRESTORE CLOUD PERSISTENCE & DIALOGS
    // ----------------------------------------------------
    val syncStatus: StateFlow<CloudSyncStatus> = repository.firestoreSyncManager.syncStatus
    val showCloudDialog = MutableStateFlow(false)
    val showDeveloperDialog = MutableStateFlow(false)

    // Firebase Auth & Google Sign-In
    val authManager: com.example.data.firebase.FirebaseAuthManager = repository.firestoreSyncManager.authManager
    val currentUser = authManager.currentUser
    val authState = authManager.authState
    val selectedNavigationScreen = MutableStateFlow<Screen>(Screen.DASHBOARD)

    fun signInWithGoogle(activityContext: Context) {
        viewModelScope.launch {
            // Clear local DB to ensure new user starts clean or old user pulls fresh from cloud
            repository.clearLocalDatabase()
            val result = authManager.signInWithGoogle(activityContext)
            if (result.isSuccess) {
                android.widget.Toast.makeText(activityContext, "Signed in successfully", android.widget.Toast.LENGTH_SHORT).show()
                // Pull/Push full sync with newly authenticated token
                repository.firestoreSyncManager.performFullSync()
            } else {
                android.widget.Toast.makeText(activityContext, "Sign in failed: ${result.exceptionOrNull()?.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    fun signInAnonymously(context: Context) {
        viewModelScope.launch {
            repository.clearLocalDatabase()
            val result = authManager.signInAnonymously()
            if (result.isSuccess) {
                android.widget.Toast.makeText(context, "Continuing as Guest", android.widget.Toast.LENGTH_SHORT).show()
                repository.firestoreSyncManager.performFullSync()
            } else {
                android.widget.Toast.makeText(context, "Guest sign in failed: ${result.exceptionOrNull()?.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    fun signOut(context: Context) {
        viewModelScope.launch {
            val syncMgr = repository.firestoreSyncManager
            var pending = syncMgr.syncStatus.value.pendingCount
            if (pending > 0) {
                android.widget.Toast.makeText(context, "Syncing pending records before sign out...", android.widget.Toast.LENGTH_SHORT).show()
                syncMgr.syncPendingQueue()
                
                var attempts = 0
                while (syncMgr.syncStatus.value.pendingCount > 0 && attempts < 10) {
                    kotlinx.coroutines.delay(1000L)
                    syncMgr.syncPendingQueue()
                    attempts++
                }
                pending = syncMgr.syncStatus.value.pendingCount
            }

            if (pending > 0) {
                android.widget.Toast.makeText(
                    context,
                    "Cannot sign out: $pending record(s) still pending sync with Firebase.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            } else {
                repository.clearLocalDatabase()
                authManager.signOut()
                android.widget.Toast.makeText(context, "Data fully synced. Signed out successfully.", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun forcePushAllData(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.firestoreSyncManager.forcePushAllData()
            withContext(Dispatchers.Main) {
                if (result.isSuccess) {
                    android.widget.Toast.makeText(context, "Data inserted/uploaded to Firestore successfully!", android.widget.Toast.LENGTH_LONG).show()
                } else {
                    android.widget.Toast.makeText(context, "Upload Error: ${result.exceptionOrNull()?.message}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun performCombinedSyncAndUpload(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.firestoreSyncManager.performCombinedSyncAndUpload()
            withContext(Dispatchers.Main) {
                if (result.isSuccess) {
                    val count = result.getOrNull() ?: 0
                    android.widget.Toast.makeText(context, "Sync & upload completed successfully! ($count records processed)", android.widget.Toast.LENGTH_LONG).show()
                } else {
                    android.widget.Toast.makeText(context, "Sync & Upload Error: ${result.exceptionOrNull()?.message}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun openCloudDialog() {
        showCloudDialog.value = true
    }

    fun dismissCloudDialog() {
        showCloudDialog.value = false
        repository.firestoreSyncManager.clearStatusMessage()
    }

    fun openDeveloperDialog() {
        showDeveloperDialog.value = true
    }

    fun dismissDeveloperDialog() {
        showDeveloperDialog.value = false
    }

    fun triggerCloudSync() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.syncWithCloud()
        }
    }

    // ----------------------------------------------------
    // BASE DATA FLOWS
    // ----------------------------------------------------
    val allMeters: StateFlow<List<Meter>> = repository.allMeters
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeMeters: StateFlow<List<Meter>> = repository.activeMeters
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val billingCycles: StateFlow<List<MeterBillingCycle>> = repository.allBillingCycles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBillingCycles: StateFlow<List<MeterBillingCycle>> = billingCycles

    val allReadings: StateFlow<List<DailyReading>> = repository.allReadings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val alertReadings: StateFlow<List<DailyReading>> = repository.alertReadings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ----------------------------------------------------
    // DASHBOARD OVERVIEW AGGREGATION
    // ----------------------------------------------------
    val dashboardOverview: StateFlow<DashboardOverviewState> = combine(
        activeMeters,
        billingCycles,
        allReadings
    ) { meters, cycles, readings ->
        val meterCards = meters.map { meter ->
            val cycle = cycles.find { it.meterId == meter.id }
            val readingsForMeter = readings.filter { it.meterId == meter.id }.sortedByDescending { it.timestamp }
            val latest = readingsForMeter.firstOrNull()

            val prevBill = cycle?.previousBillReading ?: 0.0
            val currentR = latest?.currentReading ?: prevBill
            val units = latest?.unitsSinceBill ?: (currentR - prevBill).coerceAtLeast(0.0)
            val isAlert = units >= 100.0
            val dateStr = latest?.dateString ?: cycle?.cycleStartFormattedDate ?: "N/A"

            MeterDashboardCardState(
                meter = meter,
                billingCycle = cycle,
                latestReading = latest,
                previousBillReading = prevBill,
                currentReading = currentR,
                unitsSinceBill = units,
                isAlert = isAlert,
                lastReadingDate = dateStr
            )
        }

        val totalUnits = meterCards.sumOf { it.unitsSinceBill }
        val alertsCount = meterCards.count { it.isAlert }

        DashboardOverviewState(
            totalActiveMeters = meters.size,
            totalCurrentUnits = totalUnits,
            totalAlertsCount = alertsCount,
            totalReadingsCount = readings.size,
            meterCards = meterCards,
            recentReadings = readings.take(10)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardOverviewState())

    // ----------------------------------------------------
    // ADD READING STATE & ACTIONS
    // ----------------------------------------------------
    val addReadingMeterId = MutableStateFlow<Long?>(null)
    val addReadingCurrentInput = MutableStateFlow("")
    val addReadingDate = MutableStateFlow(dateFormat.format(Date()))
    val addReadingNotes = MutableStateFlow("")
    val addReadingError = MutableStateFlow<String?>(null)
    val addReadingSuccessMessage = MutableStateFlow<String?>(null)
    val addReadingDuplicateWarning = MutableStateFlow(false)

    fun selectMeterForAdd(meterId: Long) {
        addReadingMeterId.value = meterId
        checkDuplicateWarning()
    }

    fun onCurrentReadingInputChanged(input: String) {
        addReadingCurrentInput.value = input
        addReadingError.value = null
        checkDuplicateWarning()
    }

    fun onDateChanged(dateStr: String) {
        addReadingDate.value = dateStr
        checkDuplicateWarning()
    }

    fun onNotesChanged(notes: String) {
        addReadingNotes.value = notes
    }

    private fun checkDuplicateWarning() {
        val meterId = addReadingMeterId.value ?: return
        val dateStr = addReadingDate.value
        viewModelScope.launch(Dispatchers.IO) {
            val isDup = repository.checkDuplicateReadingToday(meterId, dateStr)
            addReadingDuplicateWarning.value = isDup
        }
    }

    fun submitDailyReading(onSuccess: (DailyReading) -> Unit) {
        val meterId = addReadingMeterId.value
        if (meterId == null) {
            addReadingError.value = "Please select a meter"
            return
        }

        val readingVal = addReadingCurrentInput.value.toDoubleOrNull()
        if (readingVal == null) {
            addReadingError.value = "Please enter a valid numeric reading"
            return
        }

        val dateStr = addReadingDate.value
        val notes = addReadingNotes.value

        viewModelScope.launch(Dispatchers.IO) {
            val meter = repository.getMeterById(meterId)
            if (meter == null) {
                addReadingError.value = "Meter not found"
                return@launch
            }

            val result = repository.addDailyReading(
                meterId = meterId,
                meterName = meter.name,
                currentReading = readingVal,
                dateString = dateStr,
                notes = notes
            )

            result.onSuccess { saved ->
                addReadingSuccessMessage.value = "Reading added successfully! Units: ${saved.unitsSinceBill.toInt()}"
                addReadingCurrentInput.value = ""
                addReadingNotes.value = ""
                addReadingError.value = null
                onSuccess(saved)
            }.onFailure { error ->
                addReadingError.value = error.message ?: "Failed to save reading"
            }
        }
    }

    fun clearAddReadingMessages() {
        addReadingError.value = null
        addReadingSuccessMessage.value = null
    }

    // ----------------------------------------------------
    // BILLING CYCLE STATE & ACTIONS
    // ----------------------------------------------------
    val billCycleMeterId = MutableStateFlow<Long?>(null)
    val billCycleNewReadingInput = MutableStateFlow("")
    val billCycleStatusMessage = MutableStateFlow<String?>(null)
    val billCycleError = MutableStateFlow<String?>(null)

    fun selectMeterForCycleUpdate(meterId: Long) {
        billCycleMeterId.value = meterId
        billCycleError.value = null
    }

    fun onBillCycleReadingInputChanged(input: String) {
        billCycleNewReadingInput.value = input
        billCycleError.value = null
    }

    fun updateBillingCycle(onSuccess: () -> Unit) {
        val meterId = billCycleMeterId.value
        if (meterId == null) {
            billCycleError.value = "Please select a meter"
            return
        }

        val newBillReading = billCycleNewReadingInput.value.toDoubleOrNull()
        if (newBillReading == null) {
            billCycleError.value = "Please enter a valid previous bill reading"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            repository.updateBillingCycle(meterId, newBillReading)
            billCycleStatusMessage.value = "Previous Bill Reading updated to $newBillReading"
            billCycleNewReadingInput.value = ""
            billCycleError.value = null
            onSuccess()
        }
    }

    // Direct Edit / Delete / Reset Billing Cycle Dialog
    val editingBillingCycle = MutableStateFlow<MeterBillingCycle?>(null)
    val editBillCycleReadingInput = MutableStateFlow("")
    val editBillCycleDateInput = MutableStateFlow("")
    val editBillCycleError = MutableStateFlow<String?>(null)
    val cycleToDelete = MutableStateFlow<MeterBillingCycle?>(null)

    fun startEditingBillingCycle(cycle: MeterBillingCycle) {
        editingBillingCycle.value = cycle
        editBillCycleReadingInput.value = cycle.previousBillReading.toString()
        editBillCycleDateInput.value = cycle.cycleStartFormattedDate.ifBlank { dateFormat.format(Date(cycle.cycleStartDate)) }
        editBillCycleError.value = null
    }

    fun dismissEditingBillingCycle() {
        editingBillingCycle.value = null
        editBillCycleError.value = null
    }

    fun onEditBillCycleReadingChanged(input: String) {
        editBillCycleReadingInput.value = input
        editBillCycleError.value = null
    }

    fun onEditBillCycleDateChanged(dateStr: String) {
        editBillCycleDateInput.value = dateStr
        editBillCycleError.value = null
    }

    fun submitEditBillingCycle(onSuccess: (MeterBillingCycle) -> Unit) {
        val cycle = editingBillingCycle.value ?: return
        val readingVal = editBillCycleReadingInput.value.toDoubleOrNull()
        if (readingVal == null || readingVal < 0) {
            editBillCycleError.value = "Please enter a valid non-negative reading"
            return
        }

        val dateStr = editBillCycleDateInput.value.trim().ifBlank { cycle.cycleStartFormattedDate }

        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.updateBillingCycleFull(
                meterId = cycle.meterId,
                newPreviousBillReading = readingVal,
                startDateFormatted = dateStr
            )

            result.onSuccess { updated ->
                billCycleStatusMessage.value = "Billing cycle baseline updated to $readingVal"
                dismissEditingBillingCycle()
                onSuccess(updated)
            }.onFailure { error ->
                editBillCycleError.value = error.message ?: "Failed to update billing cycle"
            }
        }
    }

    fun requestDeleteBillingCycle(cycle: MeterBillingCycle) {
        cycleToDelete.value = cycle
    }

    fun dismissDeleteBillingCycleDialog() {
        cycleToDelete.value = null
    }

    fun confirmDeleteBillingCycle(onSuccess: () -> Unit = {}) {
        val cycle = cycleToDelete.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.resetBillingCycle(cycle.meterId)
            billCycleStatusMessage.value = "Billing cycle baseline reset to 0.0"
            cycleToDelete.value = null
            onSuccess()
        }
    }

    // ----------------------------------------------------
    // METERS MANAGEMENT STATE & ACTIONS
    // ----------------------------------------------------
    val showAddMeterDialog = MutableStateFlow(false)
    val newMeterName = MutableStateFlow("")
    val newMeterInitialBillReading = MutableStateFlow("")
    val meterManagementError = MutableStateFlow<String?>(null)
    val meterManagementSuccess = MutableStateFlow<String?>(null)

    fun openAddMeterDialog() {
        showAddMeterDialog.value = true
        meterManagementError.value = null
    }

    fun dismissAddMeterDialog() {
        showAddMeterDialog.value = false
        newMeterName.value = ""
        newMeterInitialBillReading.value = ""
        meterManagementError.value = null
    }

    fun onNewMeterNameChanged(name: String) {
        newMeterName.value = name
    }

    fun onNewMeterInitialBillReadingChanged(reading: String) {
        newMeterInitialBillReading.value = reading
    }

    fun saveNewMeter(onSuccess: () -> Unit) {
        val name = newMeterName.value.trim()
        if (name.isBlank()) {
            meterManagementError.value = "Please enter a meter name"
            return
        }

        val initialReading = newMeterInitialBillReading.value.toDoubleOrNull() ?: 0.0

        viewModelScope.launch(Dispatchers.IO) {
            repository.insertMeter(name, initialReading)
            meterManagementSuccess.value = "Meter '$name' added successfully"
            dismissAddMeterDialog()
            onSuccess()
        }
    }

    fun toggleMeterStatus(meter: Meter) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setMeterActiveStatus(meter.id, !meter.isActive)
        }
    }

    fun renameMeter(meterId: Long, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateMeterName(meterId, newName)
        }
    }

    // Edit Meter Dialog State
    val editingMeter = MutableStateFlow<Meter?>(null)
    val editMeterName = MutableStateFlow("")
    val editMeterActive = MutableStateFlow(true)
    val editMeterError = MutableStateFlow<String?>(null)

    fun startEditingMeter(meter: Meter) {
        editingMeter.value = meter
        editMeterName.value = meter.name
        editMeterActive.value = meter.isActive
        editMeterError.value = null
    }

    fun dismissEditingMeter() {
        editingMeter.value = null
        editMeterError.value = null
    }

    fun onEditMeterNameChanged(name: String) {
        editMeterName.value = name
        editMeterError.value = null
    }

    fun onEditMeterActiveChanged(active: Boolean) {
        editMeterActive.value = active
    }

    fun submitEditMeter(onSuccess: (Meter) -> Unit) {
        val meter = editingMeter.value ?: return
        val name = editMeterName.value.trim()
        if (name.isBlank()) {
            editMeterError.value = "Meter name cannot be empty"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.updateMeter(
                meterId = meter.id,
                newName = name,
                isActive = editMeterActive.value
            )

            result.onSuccess { updated ->
                meterManagementSuccess.value = "Meter '${updated.name}' updated successfully"
                dismissEditingMeter()
                onSuccess(updated)
            }.onFailure { error ->
                editMeterError.value = error.message ?: "Failed to update meter"
            }
        }
    }

    // Delete Meter Dialog State
    val meterToDelete = MutableStateFlow<Meter?>(null)

    fun requestDeleteMeter(meter: Meter) {
        meterToDelete.value = meter
    }

    fun dismissDeleteMeterDialog() {
        meterToDelete.value = null
    }

    fun confirmDeleteMeter(onSuccess: () -> Unit = {}) {
        val meter = meterToDelete.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMeter(meter.id)
            meterManagementSuccess.value = "Meter '${meter.name}' and all its history deleted"
            meterToDelete.value = null
            onSuccess()
        }
    }

    // ----------------------------------------------------
    // HISTORY, SEARCH & FILTERS
    // ----------------------------------------------------
    val historySearchQuery = MutableStateFlow("")
    val historyMeterFilterId = MutableStateFlow<Long?>(null)
    val historyAlertOnlyFilter = MutableStateFlow(false)

    val filteredReadings: StateFlow<List<DailyReading>> = combine(
        allReadings,
        historySearchQuery,
        historyMeterFilterId,
        historyAlertOnlyFilter
    ) { readings, query, meterId, alertOnly ->
        readings.filter { reading ->
            val matchesQuery = query.isBlank() ||
                    reading.meterName.contains(query, ignoreCase = true) ||
                    reading.dateString.contains(query, ignoreCase = true) ||
                    reading.notes.contains(query, ignoreCase = true)

            val matchesMeter = meterId == null || reading.meterId == meterId
            val matchesAlert = !alertOnly || reading.isAlert

            matchesQuery && matchesMeter && matchesAlert
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setHistorySearchQuery(query: String) {
        historySearchQuery.value = query
    }

    fun setHistoryMeterFilter(meterId: Long?) {
        historyMeterFilterId.value = meterId
    }

    fun toggleHistoryAlertFilter() {
        historyAlertOnlyFilter.value = !historyAlertOnlyFilter.value
    }

    fun deleteReading(reading: DailyReading) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteReading(reading)
        }
    }

    fun deleteReadingById(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteReadingById(id)
        }
    }

    // ----------------------------------------------------
    // EDIT / UPDATE READING STATE & ACTIONS
    // ----------------------------------------------------
    val editingReading = MutableStateFlow<DailyReading?>(null)
    val editReadingCurrentInput = MutableStateFlow("")
    val editReadingDate = MutableStateFlow("")
    val editReadingNotes = MutableStateFlow("")
    val editReadingError = MutableStateFlow<String?>(null)
    val editReadingSuccess = MutableStateFlow<String?>(null)

    fun startEditingReading(reading: DailyReading) {
        editingReading.value = reading
        editReadingCurrentInput.value = reading.currentReading.toString()
        editReadingDate.value = reading.dateString
        editReadingNotes.value = reading.notes
        editReadingError.value = null
        editReadingSuccess.value = null
    }

    fun dismissEditingReading() {
        editingReading.value = null
        editReadingError.value = null
        editReadingSuccess.value = null
    }

    fun onEditReadingInputChanged(input: String) {
        editReadingCurrentInput.value = input
        editReadingError.value = null
    }

    fun onEditReadingDateChanged(dateStr: String) {
        editReadingDate.value = dateStr
        editReadingError.value = null
    }

    fun onEditReadingNotesChanged(notes: String) {
        editReadingNotes.value = notes
    }

    fun submitEditReading(onSuccess: (DailyReading) -> Unit) {
        val reading = editingReading.value ?: return
        val currentVal = editReadingCurrentInput.value.toDoubleOrNull()
        if (currentVal == null) {
            editReadingError.value = "Please enter a valid numeric reading"
            return
        }

        val dateStr = editReadingDate.value.trim().ifBlank { reading.dateString }
        val notes = editReadingNotes.value

        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.updateDailyReading(
                readingId = reading.id,
                currentReading = currentVal,
                dateString = dateStr,
                notes = notes
            )

            result.onSuccess { updated ->
                editReadingSuccess.value = "Reading updated successfully! Units: ${updated.unitsSinceBill.toInt()}"
                dismissEditingReading()
                onSuccess(updated)
            }.onFailure { error ->
                editReadingError.value = error.message ?: "Failed to update reading"
            }
        }
    }

    // ----------------------------------------------------
    // EXPORT & REPORT HELPERS (CSV & Reports)
    // ----------------------------------------------------
    fun getAllDataCsvContent(): String {
        return com.example.util.CsvBackupManager.generateAllDataCsv(
            meters = allMeters.value,
            cycles = allBillingCycles.value,
            readings = allReadings.value
        )
    }

    fun exportAllDataToLocalFile(context: Context): Result<java.io.File> {
        val csv = getAllDataCsvContent()
        return com.example.util.CsvBackupManager.saveCsvToLocalBackup(context, csv)
    }

    fun exportAllDataToUri(context: Context, uri: android.net.Uri): Result<Unit> {
        val csv = getAllDataCsvContent()
        return com.example.util.CsvBackupManager.writeCsvToUri(context, uri, csv)
    }

    fun generateCsvExport(readings: List<DailyReading>): String {
        val sb = StringBuilder()
        sb.append("Date,Meter Name,Previous Bill Reading,Current Reading,Units Since Bill,Status,Notes\n")
        readings.forEach { r ->
            sb.append("\"${r.dateString}\",\"${r.meterName}\",${r.previousBillReading},${r.currentReading},${r.unitsSinceBill},\"${r.alertStatusText}\",\"${r.notes.replace("\"", "\"\"")}\"\n")
        }
        return sb.toString()
    }

    fun generateTextSummary(readings: List<DailyReading>): String {
        val sb = StringBuilder()
        sb.append("⚡ Electric Meter Reading Report\n")
        sb.append("Generated on: ${dateFormat.format(Date())}\n")
        sb.append("Total Records: ${readings.size}\n\n")

        readings.forEach { r ->
            val alertSymbol = if (r.isAlert) "🔴 ALERT (>=100 Units)" else "🟢 Normal"
            sb.append("• ${r.dateString} - ${r.meterName}\n")
            sb.append("  Previous Bill: ${r.previousBillReading} | Current: ${r.currentReading}\n")
            sb.append("  Units: ${r.unitsSinceBill} [$alertSymbol]\n")
            if (r.notes.isNotBlank()) {
                sb.append("  Notes: ${r.notes}\n")
            }
            sb.append("\n")
        }
        return sb.toString()
    }
}

class MeterViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MeterViewModel::class.java)) {
            val database = AppDatabase.getDatabase(application)
            val firestoreSyncManager = FirestoreSyncManager(application)
            val repository = MeterRepository(
                meterDao = database.meterDao(),
                billingCycleDao = database.billingCycleDao(),
                dailyReadingDao = database.dailyReadingDao(),
                syncQueueDao = database.syncQueueDao(),
                firestoreSyncManager = firestoreSyncManager
            )
            return MeterViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
