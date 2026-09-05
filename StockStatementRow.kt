package com.example.util

data class StockStatementRow(
    val productName: String,
    val companyName: String,
    val packagingType: String = "",
    val openingQty: Int,
    val openingAmount: Double,
    val receiptQty: Int,
    val receiptAmount: Double,
    val issueQty: Int,
    val issueAmount: Double,
    val closingQty: Int,
    val closingAmount: Double
) {
    val medicineName: String get() = productName
    val manufacturer: String get() = companyName
    val unitType: String get() = packagingType
    val openingStock: Int get() = openingQty
    val inwardQuantity: Int get() = receiptQty
    val outwardQuantity: Int get() = issueQty
    val closingStock: Int get() = closingQty
    val closingValuation: Double get() = closingAmount
}
