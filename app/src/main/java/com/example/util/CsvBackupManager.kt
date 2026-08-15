package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.example.model.DailyReading
import com.example.model.Meter
import com.example.model.MeterBillingCycle
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvBackupManager {
    private const val TAG = "CsvBackupManager"

    /**
     * Generates a complete CSV export of all meter readings and historical data
     */
    fun generateAllDataCsv(
        meters: List<Meter>,
        cycles: List<MeterBillingCycle>,
        readings: List<DailyReading>
    ): String {
        val sb = StringBuilder()

        // 1. Metadata header
        sb.append("# Electric Meter Tracker - Complete User Data Backup\n")
        sb.append("# Export Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).format(Date())}\n")
        sb.append("# Total Meters: ${meters.size}, Total Records: ${readings.size}\n")
        sb.append("\n")

        // 2. Daily Readings Section
        sb.append("--- DAILY METER READINGS ---\n")
        sb.append("Record_ID,Meter_ID,Meter_Name,Reading_Date,Timestamp_MS,Previous_Bill_Baseline,Current_Meter_Reading,Units_Consumed_Since_Bill,Threshold_Exceeded,Status_Label,Notes_Remarks,Device_ID,Sync_Status\n")

        val sortedReadings = readings.sortedWith(compareBy({ it.meterId }, { it.timestamp }))
        for (r in sortedReadings) {
            val safeMeterName = escapeCsv(r.meterName)
            val safeDate = escapeCsv(r.dateString)
            val safeNotes = escapeCsv(r.notes)
            val safeStatus = escapeCsv(r.alertStatusText)
            val safeDevice = escapeCsv(r.deviceId)

            sb.append("${r.id},${r.meterId},$safeMeterName,$safeDate,${r.timestamp},${r.previousBillReading},${r.currentReading},${r.unitsSinceBill},${r.isAlert},$safeStatus,$safeNotes,$safeDevice,${r.syncStatus}\n")
        }

        sb.append("\n")

        // 3. Meters Configuration & Baselines Section
        sb.append("--- METERS & BILLING CYCLES CONFIGURATION ---\n")
        sb.append("Meter_ID,Meter_Name,Is_Active,Cycle_Start_Date,Previous_Bill_Reading,Device_ID,Version\n")
        for (m in meters) {
            val cycle = cycles.find { it.meterId == m.id }
            val prevBill = cycle?.previousBillReading ?: 0.0
            val cycleDate = cycle?.cycleStartFormattedDate ?: ""
            sb.append("${m.id},${escapeCsv(m.name)},${m.isActive},${escapeCsv(cycleDate)},$prevBill,${escapeCsv(m.deviceId)},${m.version}\n")
        }

        return sb.toString()
    }

    /**
     * Escapes standard CSV fields
     */
    private fun escapeCsv(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    /**
     * Saves CSV file directly into app's documents folder for local backup
     */
    fun saveCsvToLocalBackup(context: Context, csvContent: String): Result<File> {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
            val fileName = "meter_readings_backup_$timeStamp.csv"

            // Save to external files dir or files dir
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val file = File(dir, fileName)
            FileOutputStream(file).use { out ->
                out.write(csvContent.toByteArray(Charsets.UTF_8))
            }
            Log.d(TAG, "CSV backup successfully created at: ${file.absolutePath}")
            Result.success(file)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write CSV file: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Writes CSV content to a user-chosen Storage Access Framework URI (e.g. Downloads or SD Card)
     */
    fun writeCsvToUri(context: Context, uri: Uri, csvContent: String): Result<Unit> {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(csvContent.toByteArray(Charsets.UTF_8))
                stream.flush()
            } ?: return Result.failure(IllegalStateException("Could not open output stream for selected location"))
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error writing to URI: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Creates an Intent to share or send the CSV file to WhatsApp, Drive, Gmail, etc.
     */
    fun createShareCsvIntent(context: Context, file: File): Intent {
        val authority = "${context.packageName}.fileprovider"
        val contentUri: Uri = FileProvider.getUriForFile(context, authority, file)

        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, "Electric Meter Readings Backup (${file.name})")
            putExtra(Intent.EXTRA_TEXT, "Here is the exported CSV backup containing all electric meter readings and consumption history.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
