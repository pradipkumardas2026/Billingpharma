package com.example.ui.viewmodel

data class BillDraftItem(
    val tempId: Long = System.nanoTime(),
    val medicineId: Long,
    val productName: String,
    val manufacturer: String,
    val pack: String = "10's",
    val batchNo: String,
    val expDate: String,
    val qty: Int = 1,
    val freeQty: Int = 0,
    val mrp: Double,
    val price: Double,
    val discountPercent: Double = 0.0,
    val bonusPercent: Double = 0.0,
    val sgstPercent: Double = 6.0,
    val cgstPercent: Double = 6.0,
    val netRate: Double = 0.0,
    val itemTotalAmount: Double = 0.0
)
