package com.example.data.local.entity

import androidx.room.Entity

@Entity(tableName = "sync_tombstones", primaryKeys = ["entityType", "entityId"])
data class TombstoneEntity(
    val entityType: String,
    val entityId: String,
    val deletedAt: Long = System.currentTimeMillis(),
    val deviceId: String = "",
    val userMobile: String = ""
)
