package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "purchase_invoices")
data class PurchaseInvoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val invoiceNumber: String,
    val date: Long = System.currentTimeMillis(),
    val dateFormatted: String = "",
    val companyName: String = "",
    val companyGst: String = "",
    val companyPhone: String = "",
    val itemsSummary: String = "",
    val totalQty: Int = 0,
    val taxableAmount: Double = 0.0,
    val gstRatePercent: Double = 12.0,
    val cgstAmount: Double = 0.0,
    val sgstAmount: Double = 0.0,
    val totalGstAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
