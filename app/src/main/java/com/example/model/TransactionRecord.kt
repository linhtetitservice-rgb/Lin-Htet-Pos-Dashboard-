package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: TransactionType,
    val category: String,
    val amount: Double,
    val paymentMethod: PaymentMethod,
    val note: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    val productId: Long? = null,
    val productName: String? = null,
    val quantity: Int? = null,
    val unitPrice: Double? = null
)
