package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.repository.ShopRepository
import com.example.model.MonthlySettlement
import com.example.model.PaymentMethod
import com.example.model.Product
import com.example.model.StockChangeType
import com.example.model.StockLog
import com.example.model.TelegramConfig
import com.example.model.TransactionRecord
import com.example.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class RestoreMode {
    REPLACE, // Wipes all tables and restores exact snapshot
    MERGE    // Inserts/updates without wiping existing records
}

data class BackupFileSummary(
    val version: Int,
    val backupTimestamp: Long,
    val formattedDate: String,
    val shopName: String,
    val totalProducts: Int,
    val totalTransactions: Int,
    val totalStockLogs: Int,
    val totalSettlements: Int
)

data class RestoreResult(
    val isSuccess: Boolean,
    val errorMessage: String? = null,
    val restoredProducts: Int = 0,
    val restoredTransactions: Int = 0,
    val restoredStockLogs: Int = 0,
    val restoredSettlements: Int = 0
)

object DatabaseBackupManager {

    private const val BACKUP_VERSION = 1
    private const val APP_IDENTIFIER = "MyanmarShopManager"

    suspend fun generateBackupJsonString(repository: ShopRepository): String = withContext(Dispatchers.IO) {
        val products = repository.getAllProductsForBackup()
        val transactions = repository.getAllTransactionsForBackup()
        val logs = repository.getAllStockLogsForBackup()
        val settlements = repository.getAllSettlementsForBackup()
        val config = repository.getTelegramConfig()

        val root = JSONObject()
        root.put("app", APP_IDENTIFIER)
        root.put("version", BACKUP_VERSION)
        root.put("backupTimestamp", System.currentTimeMillis())
        root.put("formattedDate", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))

        // Shop Config
        val shopObj = JSONObject().apply {
            put("shopName", config.shopName)
            put("shopPhone", config.shopPhone)
            put("shopAddress", config.shopAddress)
            put("currencySymbol", config.currencySymbol)
            put("botToken", config.botToken)
            put("chatId", config.chatId)
        }
        root.put("shopProfile", shopObj)

        // Summary counts
        val summaryObj = JSONObject().apply {
            put("totalProducts", products.size)
            put("totalTransactions", transactions.size)
            put("totalStockLogs", logs.size)
            put("totalSettlements", settlements.size)
        }
        root.put("summary", summaryObj)

        // Products Array
        val productsArray = JSONArray()
        products.forEach { p ->
            val obj = JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("barcode", p.barcode)
                put("category", p.category)
                put("costPrice", p.costPrice)
                put("sellingPrice", p.sellingPrice)
                put("stockQuantity", p.stockQuantity)
                put("unit", p.unit)
                put("lowStockThreshold", p.lowStockThreshold)
                put("updatedAt", p.updatedAt)
            }
            productsArray.put(obj)
        }
        root.put("products", productsArray)

        // Transactions Array
        val transactionsArray = JSONArray()
        transactions.forEach { tx ->
            val obj = JSONObject().apply {
                put("id", tx.id)
                put("type", tx.type.name)
                put("category", tx.category)
                put("amount", tx.amount)
                put("paymentMethod", tx.paymentMethod.name)
                put("note", tx.note)
                put("dateMillis", tx.dateMillis)
                put("productId", tx.productId ?: JSONObject.NULL)
                put("productName", tx.productName ?: JSONObject.NULL)
                put("quantity", tx.quantity ?: JSONObject.NULL)
                put("unitPrice", tx.unitPrice ?: JSONObject.NULL)
            }
            transactionsArray.put(obj)
        }
        root.put("transactions", transactionsArray)

        // Stock Logs Array
        val logsArray = JSONArray()
        logs.forEach { log ->
            val obj = JSONObject().apply {
                put("id", log.id)
                put("productId", log.productId)
                put("productName", log.productName)
                put("changeType", log.changeType.name)
                put("quantityChanged", log.quantityChanged)
                put("previousStock", log.previousStock)
                put("newStock", log.newStock)
                put("costPerUnit", log.costPerUnit)
                put("note", log.note)
                put("paymentMethod", log.paymentMethod?.name ?: JSONObject.NULL)
                put("dateMillis", log.dateMillis)
            }
            logsArray.put(obj)
        }
        root.put("stockLogs", logsArray)

        // Monthly Settlements Array
        val settlementsArray = JSONArray()
        settlements.forEach { s ->
            val obj = JSONObject().apply {
                put("monthKey", s.monthKey)
                put("totalIncome", s.totalIncome)
                put("totalExpense", s.totalExpense)
                put("totalStockPurchaseCost", s.totalStockPurchaseCost)
                put("netProfit", s.netProfit)
                put("totalSalesCount", s.totalSalesCount)
                put("kpayTotal", s.kpayTotal)
                put("waveTotal", s.waveTotal)
                put("cashTotal", s.cashTotal)
                put("cbPayTotal", s.cbPayTotal)
                put("ayaPayTotal", s.ayaPayTotal)
                put("otherPayTotal", s.otherPayTotal)
                put("endingStockValueCost", s.endingStockValueCost)
                put("endingStockValueRetail", s.endingStockValueRetail)
                put("closedAtMillis", s.closedAtMillis ?: JSONObject.NULL)
                put("notes", s.notes)
                put("isClosed", s.isClosed)
            }
            settlementsArray.put(obj)
        }
        root.put("monthlySettlements", settlementsArray)

        root.toString(2)
    }

    suspend fun createBackupFile(context: Context, repository: ShopRepository): File = withContext(Dispatchers.IO) {
        val jsonString = generateBackupJsonString(repository)
        val backupDir = File(context.cacheDir, "backups").apply { if (!exists()) mkdirs() }
        val dateTag = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val backupFile = File(backupDir, "ShopManager_Backup_$dateTag.json")

        FileOutputStream(backupFile).use { out ->
            out.write(jsonString.toByteArray(Charsets.UTF_8))
        }
        backupFile
    }

    suspend fun writeBackupToUri(context: Context, repository: ShopRepository, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonString = generateBackupJsonString(repository)
            context.contentResolver.openOutputStream(uri)?.use { outStream ->
                outStream.write(jsonString.toByteArray(Charsets.UTF_8))
                outStream.flush()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun parseBackupSummary(jsonString: String): BackupFileSummary? {
        return try {
            val root = JSONObject(jsonString)
            val version = root.optInt("version", 1)
            val timestamp = root.optLong("backupTimestamp", System.currentTimeMillis())
            val formattedDate = root.optString("formattedDate", "")
            val shopObj = root.optJSONObject("shopProfile")
            val shopName = shopObj?.optString("shopName", "Shop") ?: "Shop"
            val summaryObj = root.optJSONObject("summary")

            val productsArray = root.optJSONArray("products")
            val txArray = root.optJSONArray("transactions")
            val logsArray = root.optJSONArray("stockLogs")
            val stArray = root.optJSONArray("monthlySettlements")

            val totalProducts = summaryObj?.optInt("totalProducts") ?: (productsArray?.length() ?: 0)
            val totalTx = summaryObj?.optInt("totalTransactions") ?: (txArray?.length() ?: 0)
            val totalLogs = summaryObj?.optInt("totalStockLogs") ?: (logsArray?.length() ?: 0)
            val totalSt = summaryObj?.optInt("totalSettlements") ?: (stArray?.length() ?: 0)

            BackupFileSummary(
                version = version,
                backupTimestamp = timestamp,
                formattedDate = formattedDate,
                shopName = shopName,
                totalProducts = totalProducts,
                totalTransactions = totalTx,
                totalStockLogs = totalLogs,
                totalSettlements = totalSt
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun restoreDatabaseFromJson(
        repository: ShopRepository,
        jsonString: String,
        mode: RestoreMode
    ): RestoreResult = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            val productsArray = root.optJSONArray("products") ?: JSONArray()
            val transactionsArray = root.optJSONArray("transactions") ?: JSONArray()
            val logsArray = root.optJSONArray("stockLogs") ?: JSONArray()
            val settlementsArray = root.optJSONArray("monthlySettlements") ?: JSONArray()
            val shopObj = root.optJSONObject("shopProfile")

            val parsedProducts = mutableListOf<Product>()
            for (i in 0 until productsArray.length()) {
                val obj = productsArray.getJSONObject(i)
                parsedProducts.add(
                    Product(
                        id = obj.optLong("id", 0L),
                        name = obj.getString("name"),
                        barcode = obj.optString("barcode", ""),
                        category = obj.optString("category", "အထွေထွေ"),
                        costPrice = obj.optDouble("costPrice", 0.0),
                        sellingPrice = obj.optDouble("sellingPrice", 0.0),
                        stockQuantity = obj.optInt("stockQuantity", 0),
                        unit = obj.optString("unit", "ခု"),
                        lowStockThreshold = obj.optInt("lowStockThreshold", 5),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }

            val parsedTransactions = mutableListOf<TransactionRecord>()
            for (i in 0 until transactionsArray.length()) {
                val obj = transactionsArray.getJSONObject(i)
                val typeName = obj.optString("type", TransactionType.INCOME.name)
                val txType = try { TransactionType.valueOf(typeName) } catch (e: Exception) { TransactionType.INCOME }

                val payName = obj.optString("paymentMethod", PaymentMethod.CASH.name)
                val payMethod = try { PaymentMethod.valueOf(payName) } catch (e: Exception) { PaymentMethod.CASH }

                val prodId = if (obj.has("productId") && !obj.isNull("productId")) obj.getLong("productId") else null
                val prodName = if (obj.has("productName") && !obj.isNull("productName")) obj.getString("productName") else null
                val qty = if (obj.has("quantity") && !obj.isNull("quantity")) obj.getInt("quantity") else null
                val unitPrice = if (obj.has("unitPrice") && !obj.isNull("unitPrice")) obj.getDouble("unitPrice") else null

                parsedTransactions.add(
                    TransactionRecord(
                        id = obj.optLong("id", 0L),
                        type = txType,
                        category = obj.optString("category", "အထွေထွေ"),
                        amount = obj.optDouble("amount", 0.0),
                        paymentMethod = payMethod,
                        note = obj.optString("note", ""),
                        dateMillis = obj.optLong("dateMillis", System.currentTimeMillis()),
                        productId = prodId,
                        productName = prodName,
                        quantity = qty,
                        unitPrice = unitPrice
                    )
                )
            }

            val parsedLogs = mutableListOf<StockLog>()
            for (i in 0 until logsArray.length()) {
                val obj = logsArray.getJSONObject(i)
                val changeTypeName = obj.optString("changeType", StockChangeType.RESTOCK.name)
                val changeType = try { StockChangeType.valueOf(changeTypeName) } catch (e: Exception) { StockChangeType.RESTOCK }

                val payMethod = if (obj.has("paymentMethod") && !obj.isNull("paymentMethod")) {
                    val pName = obj.getString("paymentMethod")
                    try { PaymentMethod.valueOf(pName) } catch (e: Exception) { null }
                } else null

                parsedLogs.add(
                    StockLog(
                        id = obj.optLong("id", 0L),
                        productId = obj.optLong("productId", 0L),
                        productName = obj.optString("productName", ""),
                        changeType = changeType,
                        quantityChanged = obj.optInt("quantityChanged", 0),
                        previousStock = obj.optInt("previousStock", 0),
                        newStock = obj.optInt("newStock", 0),
                        costPerUnit = obj.optDouble("costPerUnit", 0.0),
                        note = obj.optString("note", ""),
                        paymentMethod = payMethod,
                        dateMillis = obj.optLong("dateMillis", System.currentTimeMillis())
                    )
                )
            }

            val parsedSettlements = mutableListOf<MonthlySettlement>()
            for (i in 0 until settlementsArray.length()) {
                val obj = settlementsArray.getJSONObject(i)
                val closedAt = if (obj.has("closedAtMillis") && !obj.isNull("closedAtMillis")) obj.getLong("closedAtMillis") else null

                parsedSettlements.add(
                    MonthlySettlement(
                        monthKey = obj.getString("monthKey"),
                        totalIncome = obj.optDouble("totalIncome", 0.0),
                        totalExpense = obj.optDouble("totalExpense", 0.0),
                        totalStockPurchaseCost = obj.optDouble("totalStockPurchaseCost", 0.0),
                        netProfit = obj.optDouble("netProfit", 0.0),
                        totalSalesCount = obj.optInt("totalSalesCount", 0),
                        kpayTotal = obj.optDouble("kpayTotal", 0.0),
                        waveTotal = obj.optDouble("waveTotal", 0.0),
                        cashTotal = obj.optDouble("cashTotal", 0.0),
                        cbPayTotal = obj.optDouble("cbPayTotal", 0.0),
                        ayaPayTotal = obj.optDouble("ayaPayTotal", 0.0),
                        otherPayTotal = obj.optDouble("otherPayTotal", 0.0),
                        endingStockValueCost = obj.optDouble("endingStockValueCost", 0.0),
                        endingStockValueRetail = obj.optDouble("endingStockValueRetail", 0.0),
                        closedAtMillis = closedAt,
                        notes = obj.optString("notes", ""),
                        isClosed = obj.optBoolean("isClosed", false)
                    )
                )
            }

            val restoredConfig = if (shopObj != null) {
                TelegramConfig(
                    shopName = shopObj.optString("shopName", "ရွှေမင်းသမီး ကုန်စုံဆိုင်"),
                    shopPhone = shopObj.optString("shopPhone", "09-123456789"),
                    shopAddress = shopObj.optString("shopAddress", "ရန်ကုန်မြို့"),
                    currencySymbol = shopObj.optString("currencySymbol", "Ks"),
                    botToken = shopObj.optString("botToken", ""),
                    chatId = shopObj.optString("chatId", "")
                )
            } else null

            repository.restoreEntireDatabase(
                products = parsedProducts,
                transactions = parsedTransactions,
                stockLogs = parsedLogs,
                settlements = parsedSettlements,
                config = restoredConfig,
                mode = mode
            )

            RestoreResult(
                isSuccess = true,
                restoredProducts = parsedProducts.size,
                restoredTransactions = parsedTransactions.size,
                restoredStockLogs = parsedLogs.size,
                restoredSettlements = parsedSettlements.size
            )
        } catch (e: Exception) {
            e.printStackTrace()
            RestoreResult(
                isSuccess = false,
                errorMessage = e.localizedMessage ?: "ဖိုင်ဖတ်ရှု၍ မရပါ (Invalid backup format)"
            )
        }
    }

    fun shareBackupFile(context: Context, file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Shop Manager Data Backup - ${file.name}")
            putExtra(Intent.EXTRA_TEXT, "Myanmar Shop Manager Database Backup File.\nဤဖိုင်ကို သိမ်းဆည်းထားပါက အက်ပ်အသစ်သွင်းသည့်အခါ အချက်အလက်များ အကုန်ပြန်လည်ရရှိနိုင်ပါသည်။")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "ဒေတာဘေ့စ် Backup ဖိုင်အား ပေးပို့/သိမ်းဆည်းမည် (Share Backup)"))
    }
}
