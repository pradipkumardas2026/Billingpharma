package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey val invoiceNumber: Long,
    val date: Long = System.currentTimeMillis(),
    val dateFormatted: String = "",
    val customerType: String = "PATIENT",
    val customerId: Long = 0L,
    val customerName: String = "",
    val customerAddress: String = "",
    val customerDl: String = "",
    val customerGstPan: String = "",
    val customerPhone: String = "",
    val doctorDetails: String = "",
    val totalMrpValue: Double = 0.0,
    val itemCount: Int = 0,
    val totalQty: Int = 0,
    val totalFree: Int = 0,
    val totalAmount: Double = 0.0,
    val lessDiscount: Double = 0.0,
    val cgstAmount: Double = 0.0,
    val sgstAmount: Double = 0.0,
    val adjustmentAmount: Double = 0.0,
    val netAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val dueAmount: Double = 0.0,
    val amountInWords: String = "",
    val note: String = ""
) {
    val id: Long get() = invoiceNumber
    val dateStr: String get() = dateFormatted
    val customerGst: String get() = customerGstPan
}
