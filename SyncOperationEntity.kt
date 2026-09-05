package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_operations")
data class SyncOperationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val entityType: String,       // "MEDICINE", "INVOICE", "PARTY", "DOCTOR", "PATIENT", "SETTINGS", "ADMIN_USER"
    val entityId: String,         // ID as string
    val operation: String,        // "INSERT", "UPDATE", "DELETE"
    val payloadJson: String,      // JSON serialized data
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "PENDING", // "PENDING", "SYNCING", "SYNCED", "FAILED"
    val retryCount: Int = 0,
    val errorMessage: String? = null,
    val deviceId: String = ""
)
