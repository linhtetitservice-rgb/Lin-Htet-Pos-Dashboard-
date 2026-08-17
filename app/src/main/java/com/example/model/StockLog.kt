package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stock_logs")
data class StockLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long,
    val productName: String,
    val changeType: StockChangeType,
    val quantityChanged: Int,
    val previousStock: Int,
    val newStock: Int,
    val costPerUnit: Double = 0.0,
    val note: String = "",
    val paymentMethod: PaymentMethod? = null,
    val dateMillis: Long = System.currentTimeMillis()
)
