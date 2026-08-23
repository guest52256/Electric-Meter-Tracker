package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_readings")
data class DailyReading(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val meterId: Long,
    val meterName: String,
    val dateString: String, // e.g. "19 Aug 2026"
    val timestamp: Long = System.currentTimeMillis(),
    val previousBillReading: Double,
    val currentReading: Double,
    val unitsSinceBill: Double, // currentReading - previousBillReading
    val isAlert: Boolean, // unitsSinceBill >= 100.0
    val alertStatusText: String = if (unitsSinceBill >= 100.0) "ALERT" else "NORMAL",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deviceId: String = "",
    val lastSyncedAt: Long? = null,
    val syncStatus: String = "PENDING", // PENDING, SYNCED, FAILED
    val version: Long = 1L
)
