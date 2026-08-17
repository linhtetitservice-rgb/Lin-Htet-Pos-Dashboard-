package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monthly_settlements")
data class MonthlySettlement(
    @PrimaryKey
    val monthKey: String, // Format: "YYYY-MM", e.g., "2026-08"
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val totalStockPurchaseCost: Double = 0.0,
    val netProfit: Double = 0.0,
    val totalSalesCount: Int = 0,
    val kpayTotal: Double = 0.0,
    val waveTotal: Double = 0.0,
    val cashTotal: Double = 0.0,
    val cbPayTotal: Double = 0.0,
    val ayaPayTotal: Double = 0.0,
    val otherPayTotal: Double = 0.0,
    val endingStockValueCost: Double = 0.0,
    val endingStockValueRetail: Double = 0.0,
    val closedAtMillis: Long? = null,
    val notes: String = "",
    val isClosed: Boolean = false
)
