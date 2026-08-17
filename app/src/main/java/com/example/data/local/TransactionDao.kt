package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.model.TransactionRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY dateMillis DESC")
    fun getAllTransactions(): Flow<List<TransactionRecord>>

    @Query("SELECT * FROM transactions WHERE dateMillis >= :startDateMillis AND dateMillis <= :endDateMillis ORDER BY dateMillis DESC")
    fun getTransactionsByDateRange(startDateMillis: Long, endDateMillis: Long): Flow<List<TransactionRecord>>

    @Query("SELECT * FROM transactions WHERE dateMillis >= :startDateMillis AND dateMillis <= :endDateMillis ORDER BY dateMillis DESC")
    suspend fun getTransactionsByDateRangeSync(startDateMillis: Long, endDateMillis: Long): List<TransactionRecord>

    @Query("SELECT * FROM transactions ORDER BY dateMillis DESC")
    suspend fun getAllTransactionsSync(): List<TransactionRecord>

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionRecord>)

    @Delete
    suspend fun delete(transaction: TransactionRecord)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)
}
