package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meters")
data class Meter(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deviceId: String = "",
    val lastSyncedAt: Long? = null,
    val syncStatus: String = "PENDING", // PENDING, SYNCED, FAILED
    val version: Long = 1L
)
