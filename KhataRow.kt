package com.example.util

data class KhataRow(
    val invoiceNumber: Long,
    val date: String,
    val partyName: String,
    val productName: String,
    val quantity: Int,
    val free: Int,
    val billAmount: Double,
    val isFirstItemOfInvoice: Boolean,
    val note: String = ""
)
