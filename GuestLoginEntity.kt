package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "guest_logins")
data class GuestLoginEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mobileNumber: String,
    val loginTimestamp: Long = System.currentTimeMillis()
)
