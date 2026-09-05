package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medicines")
data class MedicineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val productName: String,
    val productType: String = "Tablet",
    val packagingType: String = "1×10T",
    val composition: String = "",
    val manufacturerName: String = "",
    val companyName: String = "",
    val batchNumber: String = "",
    val mfgDate: String = "",
    val expiryDate: String = "",
    val mrp: Double = 0.0,
    val price: Double = 0.0,
    val purchaseRate: Double = 0.0,
    val saleRate: Double = 0.0,
    val gstPercent: Double = 12.0,
    val stockQuantity: Int = 0,
    val rackLocation: String = "",
    val freeQuantity: Int = 0,
    val lowStockLevel: Int = 10,
    val createdAt: Long = System.currentTimeMillis()
) {
    val manufacturer: String get() = manufacturerName.ifEmpty { companyName }
    val name: String get() = productName
    val batch: String get() = batchNumber
}
