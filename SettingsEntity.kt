package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    val businessName: String = "",
    val address: String = "",
    val gstNumber: String = "",
    val dlNumber: String = "",
    val contactNumber: String = "",
    val adminPassword: String = "654321",
    val adminMobile: String = "9002625428"
)
