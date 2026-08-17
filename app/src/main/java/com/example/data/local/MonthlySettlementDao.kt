package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.model.MonthlySettlement
import kotlinx.coroutines.flow.Flow

@Dao
interface MonthlySettlementDao {
    @Query("SELECT * FROM monthly_settlements ORDER BY monthKey DESC")
    fun getAllSettlements(): Flow<List<MonthlySettlement>>

    @Query("SELECT * FROM monthly_settlements WHERE monthKey = :monthKey")
    fun getSettlementByMonth(monthKey: String): Flow<MonthlySettlement?>

    @Query("SELECT * FROM monthly_settlements WHERE monthKey = :monthKey")
    suspend fun getSettlementByMonthSync(monthKey: String): MonthlySettlement?

    @Query("SELECT * FROM monthly_settlements ORDER BY monthKey DESC")
    suspend fun getAllSettlementsSync(): List<MonthlySettlement>

    @Query("DELETE FROM monthly_settlements")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settlement: MonthlySettlement)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(settlements: List<MonthlySettlement>)

    @Query("DELETE FROM monthly_settlements WHERE monthKey = :monthKey")
    suspend fun deleteByMonth(monthKey: String)
}
