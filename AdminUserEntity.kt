package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "admin_users")
data class AdminUserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    val mobileNumber: String,
    val password: String,
    val createdByMobile: String = "9002625428",
    val createdAt: Long = System.currentTimeMillis()
)
