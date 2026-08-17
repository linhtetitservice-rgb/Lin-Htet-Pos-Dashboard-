package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val barcode: String = "",
    val category: String = "အထွေထွေ (General)",
    val stockQuantity: Int = 0,
    val unit: String = "ခု",
    val costPrice: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val lowStockThreshold: Int = 5,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val totalCostValue: Double get() = stockQuantity * costPrice
    val totalRetailValue: Double get() = stockQuantity * sellingPrice
    val profitMarginPerUnit: Double get() = sellingPrice - costPrice
    val profitPercentage: Double get() = if (costPrice > 0) ((sellingPrice - costPrice) / costPrice) * 100.0 else 0.0
    val isLowStock: Boolean get() = stockQuantity <= lowStockThreshold
    val isOutOfStock: Boolean get() = stockQuantity <= 0
}
