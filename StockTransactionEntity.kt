package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stock_transactions")
data class StockTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val medicineId: Long,
    val productName: String,
    val companyName: String = "",
    val type: String,
    val referenceInvoice: Long? = null,
    val qty: Int = 0,
    val freeQty: Int = 0,
    val rate: Double = 0.0,
    val amount: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val dateFormatted: String = ""
)
