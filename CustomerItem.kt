package com.example.ui.screens

import com.example.data.local.entity.DoctorEntity
import com.example.data.local.entity.PartyEntity
import com.example.data.local.entity.PatientEntity

data class CustomerItem(
    val name: String,
    val type: String,
    val phone: String = "",
    val address: String = "",
    val dlNumber: String = "",
    val gstin: String = "",
    val extraDetails: String = "",
    val partyEntity: PartyEntity? = null,
    val doctorEntity: DoctorEntity? = null,
    val patientEntity: PatientEntity? = null
) {
    val dl: String get() = dlNumber
    val gst: String get() = gstin
    val doctor: String get() = extraDetails
}
