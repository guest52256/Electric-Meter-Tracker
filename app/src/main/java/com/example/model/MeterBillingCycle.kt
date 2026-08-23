package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meter_billing_cycles")
data class MeterBillingCycle(
    @PrimaryKey
    val meterId: Long,
    val previousBillReading: Double,
    val cycleStartDate: Long = System.currentTimeMillis(),
    val cycleStartFormattedDate: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deviceId: String = "",
    val lastSyncedAt: Long? = null,
    val syncStatus: String = "PENDING", // PENDING, SYNCED, FAILED
    val version: Long = 1L
)
