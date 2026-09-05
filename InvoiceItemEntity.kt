package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "invoice_items")
data class InvoiceItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val invoiceNumber: Long,
    val slNo: Int,
    val medicineId: Long,
    val productName: String,
    val manufacturer: String = "",
    val pack: String = "",
    val batchNo: String = "",
    val expDate: String = "",
    val qty: Int = 0,
    val freeQty: Int = 0,
    val mrp: Double = 0.0,
    val price: Double = 0.0,
    val discountPercent: Double = 0.0,
    val bonusPercent: Double = 0.0,
    val sgstPercent: Double = 6.0,
    val cgstPercent: Double = 6.0,
    val netRate: Double = 0.0,
    val itemTotalAmount: Double = 0.0
)
