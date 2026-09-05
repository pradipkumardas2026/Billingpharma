package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "doctors")
data class DoctorEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val doctorName: String,
    val qualification: String = "",
    val doctorType: String = "Allopathic",
    val phoneNumber: String = "",
    val address: String = ""
)
