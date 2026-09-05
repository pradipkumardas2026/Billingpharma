package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "parties")
data class PartyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val partyName: String,
    val dlNumber: String = "",
    val gstPanNumber: String = "",
    val contactNumber: String = "",
    val address: String = ""
)
