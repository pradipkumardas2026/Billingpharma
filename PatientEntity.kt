package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "patients")
data class PatientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val patientName: String,
    val phoneNumber: String = "",
    val doctorName: String = "",
    val address: String = ""
)
