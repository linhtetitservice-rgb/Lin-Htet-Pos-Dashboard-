package com.example.data.local

import androidx.room.TypeConverter
import com.example.model.PaymentMethod
import com.example.model.StockChangeType
import com.example.model.TransactionType

class Converters {
    @TypeConverter
    fun fromTransactionType(value: TransactionType?): String? = value?.name

    @TypeConverter
    fun toTransactionType(value: String?): TransactionType? =
        value?.let { runCatching { TransactionType.valueOf(it) }.getOrDefault(TransactionType.INCOME) }

    @TypeConverter
    fun fromPaymentMethod(value: PaymentMethod?): String? = value?.name

    @TypeConverter
    fun toPaymentMethod(value: String?): PaymentMethod? =
        value?.let { runCatching { PaymentMethod.valueOf(it) }.getOrDefault(PaymentMethod.CASH) }

    @TypeConverter
    fun fromStockChangeType(value: StockChangeType?): String? = value?.name

    @TypeConverter
    fun toStockChangeType(value: String?): StockChangeType? =
        value?.let { runCatching { StockChangeType.valueOf(it) }.getOrDefault(StockChangeType.RESTOCK) }
}
