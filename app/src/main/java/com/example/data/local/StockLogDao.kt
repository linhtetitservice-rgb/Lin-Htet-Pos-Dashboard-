package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.model.StockLog
import kotlinx.coroutines.flow.Flow

@Dao
interface StockLogDao {
    @Query("SELECT * FROM stock_logs ORDER BY dateMillis DESC")
    fun getAllLogs(): Flow<List<StockLog>>

    @Query("SELECT * FROM stock_logs WHERE productId = :productId ORDER BY dateMillis DESC")
    fun getLogsForProduct(productId: Long): Flow<List<StockLog>>

    @Query("SELECT * FROM stock_logs WHERE dateMillis >= :startMillis AND dateMillis <= :endMillis ORDER BY dateMillis DESC")
    fun getLogsByDateRange(startMillis: Long, endMillis: Long): Flow<List<StockLog>>

    @Query("SELECT * FROM stock_logs WHERE dateMillis >= :startMillis AND dateMillis <= :endMillis ORDER BY dateMillis DESC")
    suspend fun getLogsByDateRangeSync(startMillis: Long, endMillis: Long): List<StockLog>

    @Query("SELECT * FROM stock_logs ORDER BY dateMillis DESC")
    suspend fun getAllLogsSync(): List<StockLog>

    @Query("DELETE FROM stock_logs")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: StockLog): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<StockLog>)
}
