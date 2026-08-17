package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.ShopRepository
import com.example.model.DateRangePreset
import com.example.model.PaymentMethod
import com.example.model.Product
import com.example.model.TransactionRecord
import com.example.model.TransactionType
import com.example.util.DatabaseBackupManager
import com.example.util.Formatters
import com.example.util.RestoreMode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ဆိုင်စာရင်း (Shop Manager)", appName)
    }

    @Test
    fun `test database backup export and restore`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = ShopRepository(context)

        // Seed some data
        val sampleProduct = Product(
            name = "Test Coffee Mix",
            barcode = "999888777",
            category = "အအေး/ဖျော်ရည်",
            costPrice = 300.0,
            sellingPrice = 450.0,
            stockQuantity = 20
        )
        val prodId = repository.addProduct(sampleProduct)

        val sampleTx = TransactionRecord(
            type = TransactionType.INCOME,
            category = "အရောင်းရငွေ",
            amount = 4500.0,
            paymentMethod = PaymentMethod.KPAY,
            note = "Test sale",
            productId = prodId,
            productName = "Test Coffee Mix",
            quantity = 10,
            unitPrice = 450.0
        )
        repository.addTransaction(sampleTx)

        // Export backup JSON
        val json = DatabaseBackupManager.generateBackupJsonString(repository)
        assertTrue(json.isNotBlank())
        assertTrue(json.contains("Test Coffee Mix"))

        val summary = DatabaseBackupManager.parseBackupSummary(json)
        assertNotNull(summary)
        assertTrue((summary?.totalProducts ?: 0) >= 1)
        assertTrue((summary?.totalTransactions ?: 0) >= 1)

        // Test Restore
        val result = DatabaseBackupManager.restoreDatabaseFromJson(repository, json, RestoreMode.REPLACE)
        assertTrue(result.isSuccess)
        assertTrue(result.restoredProducts >= 1)
        assertTrue(result.restoredTransactions >= 1)
    }

    @Test
    fun `test date helper methods for ledger filtering`() {
        val (todayStart, todayEnd) = Formatters.getStartAndEndOfToday()
        assertTrue(todayEnd > todayStart)
        assertTrue(System.currentTimeMillis() in todayStart..todayEnd)

        val (yStart, yEnd) = Formatters.getStartAndEndOfYesterday()
        assertTrue(yEnd > yStart)
        assertTrue(yEnd < todayStart)

        val (wStart, wEnd) = Formatters.getStartAndEndOfWeek()
        assertTrue(wEnd > wStart)

        assertEquals("KBZPay (KPay)", PaymentMethod.KPAY.myanmarLabel)
        assertEquals("WavePay", PaymentMethod.WAVE_PAY.myanmarLabel)
        assertEquals("ငွေသား", PaymentMethod.CASH.myanmarLabel)
    }
}
