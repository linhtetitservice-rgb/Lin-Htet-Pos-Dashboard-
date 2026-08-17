package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.model.MonthlySettlement
import com.example.model.Product
import com.example.model.StockLog
import com.example.model.TransactionRecord

@Database(
    entities = [
        Product::class,
        TransactionRecord::class,
        StockLog::class,
        MonthlySettlement::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun transactionDao(): TransactionDao
    abstract fun stockLogDao(): StockLogDao
    abstract fun monthlySettlementDao(): MonthlySettlementDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "myanmar_shop_database.db"
                ).fallbackToDestructiveMigration(false)
                 .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
