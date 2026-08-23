package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "sync_queue")
data class SyncQueueItem(
    @PrimaryKey
    val operationId: String = "",
    val collection: String, // "meters", "billingCycles", "dailyReadings", "appSettings"
    val documentId: String,
    val operationType: String, // "INSERT", "UPDATE", "DELETE"
    val payloadJson: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val syncStatus: String = "PENDING", // "PENDING", "IN_PROGRESS", "FAILED", "COMPLETED"
    val errorMessage: String? = null
) {
    // Constructor ensuring operationId is never blank
    fun getEffectiveId(): String = operationId.ifBlank { "${collection}_${documentId}" }
}
