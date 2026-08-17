package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.AppDatabase
import com.example.data.remote.TelegramService
import com.example.model.MonthlySettlement
import com.example.model.PaymentMethod
import com.example.model.Product
import com.example.model.StockChangeType
import com.example.model.StockLog
import com.example.model.TelegramConfig
import com.example.model.TransactionRecord
import com.example.model.TransactionType
import com.example.util.Formatters
import com.example.util.RestoreMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Calendar

data class DashboardSummary(
    val todayIncome: Double = 0.0,
    val todayExpense: Double = 0.0,
    val todayNetProfit: Double = 0.0,
    val monthIncome: Double = 0.0,
    val monthExpense: Double = 0.0,
    val monthNetProfit: Double = 0.0,
    val todayKpayTotal: Double = 0.0,
    val todayWaveTotal: Double = 0.0,
    val todayCashTotal: Double = 0.0,
    val totalProductsCount: Int = 0,
    val lowStockCount: Int = 0,
    val totalInventoryCostValue: Double = 0.0,
    val totalInventoryRetailValue: Double = 0.0
)

class ShopRepository(private val context: Context) {
    private val database = AppDatabase.getInstance(context)
    private val productDao = database.productDao()
    private val transactionDao = database.transactionDao()
    private val stockLogDao = database.stockLogDao()
    private val monthlySettlementDao = database.monthlySettlementDao()
    private val telegramService = TelegramService()

    private val prefs: SharedPreferences =
        context.getSharedPreferences("shop_manager_prefs", Context.MODE_PRIVATE)

    // Flow streams
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
    val lowStockProducts: Flow<List<Product>> = productDao.getLowStockProducts()
    val allTransactions: Flow<List<TransactionRecord>> = transactionDao.getAllTransactions()
    val allStockLogs: Flow<List<StockLog>> = stockLogDao.getAllLogs()
    val allSettlements: Flow<List<MonthlySettlement>> = monthlySettlementDao.getAllSettlements()

    fun getLogsForProduct(productId: Long): Flow<List<StockLog>> =
        stockLogDao.getLogsForProduct(productId)

    fun getTransactionsForDateRange(startDate: Long, endDate: Long): Flow<List<TransactionRecord>> =
        transactionDao.getTransactionsByDateRange(startDate, endDate)

    suspend fun getTransactionsBetween(startDate: Long, endDate: Long): List<TransactionRecord> =
        withContext(Dispatchers.IO) {
            transactionDao.getTransactionsByDateRangeSync(startDate, endDate)
        }

    fun getSettlementForMonth(monthKey: String): Flow<MonthlySettlement?> =
        monthlySettlementDao.getSettlementByMonth(monthKey)

    // Products operations
    suspend fun addProduct(product: Product): Long = withContext(Dispatchers.IO) {
        val id = productDao.insert(product)
        if (product.stockQuantity > 0) {
            stockLogDao.insert(
                StockLog(
                    productId = id,
                    productName = product.name,
                    changeType = StockChangeType.RESTOCK,
                    quantityChanged = product.stockQuantity,
                    previousStock = 0,
                    newStock = product.stockQuantity,
                    costPerUnit = product.costPrice,
                    note = "ပစ္စည်းအသစ် စတင်ထည့်သွင်းခြင်း",
                    paymentMethod = PaymentMethod.CASH
                )
            )
        }
        id
    }

    suspend fun updateProduct(product: Product) = withContext(Dispatchers.IO) {
        productDao.update(product)
    }

    suspend fun deleteProduct(product: Product) = withContext(Dispatchers.IO) {
        productDao.delete(product)
    }

    suspend fun restockProduct(
        productId: Long,
        addedQuantity: Int,
        costPerUnit: Double,
        paymentMethod: PaymentMethod,
        recordAsExpense: Boolean,
        note: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val product = productDao.getProductByIdSync(productId)
                ?: return@withContext Result.failure(Exception("Product not found"))

            val oldStock = product.stockQuantity
            val newStock = oldStock + addedQuantity
            productDao.updateStock(productId, newStock)

            // Log stock movement
            stockLogDao.insert(
                StockLog(
                    productId = productId,
                    productName = product.name,
                    changeType = StockChangeType.RESTOCK,
                    quantityChanged = addedQuantity,
                    previousStock = oldStock,
                    newStock = newStock,
                    costPerUnit = costPerUnit,
                    note = if (note.isBlank()) "Stock ထပ်ဖြည့်ခြင်း" else note,
                    paymentMethod = paymentMethod
                )
            )

            // Record as Expense if requested
            if (recordAsExpense && costPerUnit > 0) {
                val totalCost = addedQuantity * costPerUnit
                transactionDao.insert(
                    TransactionRecord(
                        type = TransactionType.EXPENSE,
                        category = "ပစ္စည်းဝယ်ယူစရိတ် (Stock)",
                        amount = totalCost,
                        paymentMethod = paymentMethod,
                        note = "Stock ဖြည့်: ${product.name} (x$addedQuantity)",
                        productId = productId,
                        productName = product.name,
                        quantity = addedQuantity,
                        unitPrice = costPerUnit
                    )
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun quickSale(
        productId: Long,
        quantityToSell: Int,
        unitSellingPrice: Double,
        paymentMethod: PaymentMethod,
        note: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val product = productDao.getProductByIdSync(productId)
                ?: return@withContext Result.failure(Exception("Product not found"))

            if (product.stockQuantity < quantityToSell) {
                return@withContext Result.failure(Exception("လက်ကျန် Stock မလုံလောက်ပါ (လက်ကျန်: ${product.stockQuantity})"))
            }

            val oldStock = product.stockQuantity
            val newStock = oldStock - quantityToSell
            productDao.updateStock(productId, newStock)

            // Log stock reduction
            stockLogDao.insert(
                StockLog(
                    productId = productId,
                    productName = product.name,
                    changeType = StockChangeType.SALE,
                    quantityChanged = -quantityToSell,
                    previousStock = oldStock,
                    newStock = newStock,
                    costPerUnit = product.costPrice,
                    note = if (note.isBlank()) "အရောင်း အရေအတွက်: $quantityToSell" else note,
                    paymentMethod = paymentMethod
                )
            )

            // Record Income transaction
            val totalIncome = quantityToSell * unitSellingPrice
            transactionDao.insert(
                TransactionRecord(
                    type = TransactionType.INCOME,
                    category = "အရောင်းရငွေ (Sales)",
                    amount = totalIncome,
                    paymentMethod = paymentMethod,
                    note = "ရောင်းချမှု: ${product.name} (x$quantityToSell) ${if (note.isNotBlank()) "[$note]" else ""}",
                    productId = productId,
                    productName = product.name,
                    quantity = quantityToSell,
                    unitPrice = unitSellingPrice
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Direct Transactions
    suspend fun addTransaction(record: TransactionRecord): Long = withContext(Dispatchers.IO) {
        transactionDao.insert(record)
    }

    suspend fun deleteTransaction(record: TransactionRecord) = withContext(Dispatchers.IO) {
        transactionDao.delete(record)
    }

    // Financial Analysis & Settlements
    suspend fun calculateMonthlyStats(year: Int, monthZeroIndexed: Int): MonthlySettlement = withContext(Dispatchers.IO) {
        val (startMillis, endMillis) = Formatters.getStartAndEndOfMonth(year, monthZeroIndexed)
        val monthKey = Formatters.getMonthKey(year, monthZeroIndexed)
        val transactions = transactionDao.getTransactionsByDateRangeSync(startMillis, endMillis)
        val allProductsList = productDao.getAllProductsSync()

        var totalIncome = 0.0
        var totalExpense = 0.0
        var totalStockPurchase = 0.0
        var kpay = 0.0
        var wave = 0.0
        var cash = 0.0
        var cbPay = 0.0
        var ayaPay = 0.0
        var otherPay = 0.0
        var salesCount = 0

        for (tx in transactions) {
            if (tx.type == TransactionType.INCOME) {
                totalIncome += tx.amount
                salesCount++
                when (tx.paymentMethod) {
                    PaymentMethod.KPAY -> kpay += tx.amount
                    PaymentMethod.WAVE_PAY -> wave += tx.amount
                    PaymentMethod.CASH -> cash += tx.amount
                    PaymentMethod.CB_PAY -> cbPay += tx.amount
                    PaymentMethod.AYA_PAY -> ayaPay += tx.amount
                    PaymentMethod.OTHER -> otherPay += tx.amount
                }
            } else {
                totalExpense += tx.amount
                if (tx.category.contains("Stock", ignoreCase = true) || tx.category.contains("ပစ္စည်း", ignoreCase = true)) {
                    totalStockPurchase += tx.amount
                }
            }
        }

        val endingStockCost = allProductsList.sumOf { it.totalCostValue }
        val endingStockRetail = allProductsList.sumOf { it.totalRetailValue }
        val netProfit = totalIncome - totalExpense

        val existing = monthlySettlementDao.getSettlementByMonthSync(monthKey)

        MonthlySettlement(
            monthKey = monthKey,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            totalStockPurchaseCost = totalStockPurchase,
            netProfit = netProfit,
            totalSalesCount = salesCount,
            kpayTotal = kpay,
            waveTotal = wave,
            cashTotal = cash,
            cbPayTotal = cbPay,
            ayaPayTotal = ayaPay,
            otherPayTotal = otherPay,
            endingStockValueCost = endingStockCost,
            endingStockValueRetail = endingStockRetail,
            closedAtMillis = existing?.closedAtMillis,
            notes = existing?.notes ?: "",
            isClosed = existing?.isClosed ?: false
        )
    }

    suspend fun saveMonthlySettlement(settlement: MonthlySettlement) = withContext(Dispatchers.IO) {
        monthlySettlementDao.insertOrUpdate(settlement)
    }

    // Config & Preferences
    fun getTelegramConfig(): TelegramConfig {
        return TelegramConfig(
            botToken = prefs.getString("tg_bot_token", "") ?: "",
            chatId = prefs.getString("tg_chat_id", "") ?: "",
            shopName = prefs.getString("shop_name", "ရွှေမင်းသမီး ကုန်စုံဆိုင်") ?: "ရွှေမင်းသမီး ကုန်စုံဆိုင်",
            shopPhone = prefs.getString("shop_phone", "09-123456789") ?: "09-123456789",
            shopAddress = prefs.getString("shop_address", "ရန်ကုန်မြို့") ?: "ရန်ကုန်မြို့",
            currencySymbol = prefs.getString("currency_symbol", "Ks") ?: "Ks"
        )
    }

    fun saveTelegramConfig(config: TelegramConfig) {
        prefs.edit()
            .putString("tg_bot_token", config.botToken)
            .putString("tg_chat_id", config.chatId)
            .putString("shop_name", config.shopName)
            .putString("shop_phone", config.shopPhone)
            .putString("shop_address", config.shopAddress)
            .putString("currency_symbol", config.currencySymbol)
            .apply()
    }

    // Telegram Bot dispatch
    suspend fun sendTelegramMessage(text: String): Result<String> {
        val config = getTelegramConfig()
        return telegramService.sendMessage(
            botToken = config.botToken,
            chatId = config.chatId,
            text = text,
            parseMode = "HTML"
        )
    }

    // Telegram & Voucher Message Builders
    suspend fun buildTodaySummaryMessage(): String = withContext(Dispatchers.IO) {
        val config = getTelegramConfig()
        val (todayStart, todayEnd) = Formatters.getStartAndEndOfToday()
        val txs = transactionDao.getTransactionsByDateRangeSync(todayStart, todayEnd)

        val incomeTxs = txs.filter { it.type == TransactionType.INCOME }
        val expenseTxs = txs.filter { it.type == TransactionType.EXPENSE }

        val totalIncome = incomeTxs.sumOf { it.amount }
        val totalExpense = expenseTxs.sumOf { it.amount }
        val net = totalIncome - totalExpense

        val kpayIncome = incomeTxs.filter { it.paymentMethod == PaymentMethod.KPAY }.sumOf { it.amount }
        val waveIncome = incomeTxs.filter { it.paymentMethod == PaymentMethod.WAVE_PAY }.sumOf { it.amount }
        val cashIncome = incomeTxs.filter { it.paymentMethod == PaymentMethod.CASH }.sumOf { it.amount }
        val otherIncome = incomeTxs.filter { it.paymentMethod != PaymentMethod.KPAY && it.paymentMethod != PaymentMethod.WAVE_PAY && it.paymentMethod != PaymentMethod.CASH }.sumOf { it.amount }

        """
🏪 <b>${config.shopName}</b>
📅 <b>နေ့စဉ် အရောင်း/အဝယ် စာရင်းချုပ် (${Formatters.formatDate(System.currentTimeMillis())})</b>
━━━━━━━━━━━━━━━━━━
💰 <b>စုစုပေါင်း ဝင်ငွေ:</b> ${Formatters.formatKyat(totalIncome)}
💸 <b>စုစုပေါင်း ထွက်ငွေ:</b> ${Formatters.formatKyat(totalExpense)}
✨ <b>ယနေ့ အသားတင် အမြတ်:</b> ${Formatters.formatKyat(net)}

💳 <b>ငွေပေးချေမှု အသေးစိတ် (Payment Channels):</b>
• 🔵 KPay: ${Formatters.formatKyat(kpayIncome)}
• 🟡 WavePay: ${Formatters.formatKyat(waveIncome)}
• 🟢 ငွေသား (Cash): ${Formatters.formatKyat(cashIncome)}
${if (otherIncome > 0) "• ⚪ အခြား (Other): ${Formatters.formatKyat(otherIncome)}\n" else ""}
📦 <b>အရောင်း အကြိမ်ရေ:</b> ${incomeTxs.size} ကြိမ်
⏰ <i>Generated at: ${Formatters.formatDateTime(System.currentTimeMillis())}</i>
        """.trimIndent()
    }

    suspend fun buildMonthlyStatementMessage(year: Int, monthZeroIndexed: Int): String = withContext(Dispatchers.IO) {
        val config = getTelegramConfig()
        val stats = calculateMonthlyStats(year, monthZeroIndexed)
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroIndexed)
        }
        val monthName = Formatters.formatMonthYear(cal.timeInMillis)

        """
📊 <b>${config.shopName}</b>
📑 <b>တစ်လတာ လချုပ်ရှင်းတမ်း (${monthName})</b>
━━━━━━━━━━━━━━━━━━
💰 <b>စုစုပေါင်း ဝင်ငွေ (Total Revenue):</b> ${Formatters.formatKyat(stats.totalIncome)}
📦 <b>ပစ္စည်းဝယ်ယူစရိတ် (Stock Purchases):</b> ${Formatters.formatKyat(stats.totalStockPurchaseCost)}
💸 <b>အထွေထွေ ဆိုင်စရိတ် (Expenses):</b> ${Formatters.formatKyat(stats.totalExpense - stats.totalStockPurchaseCost)}
📉 <b>စုစုပေါင်း ကုန်ကျစရိတ်:</b> ${Formatters.formatKyat(stats.totalExpense)}
━━━━━━━━━━━━━━━━━━
💎 <b>အသားတင် အမြတ်ငွေ (Net Profit):</b> ${Formatters.formatKyat(stats.netProfit)}
🏷️ <b>အရောင်း အရေအတွက်:</b> ${stats.totalSalesCount} ကြိမ်

💳 <b>ငွေပေးချေမှု လချုပ် (Payment Summary):</b>
• 🔵 KPay စုစုပေါင်း: ${Formatters.formatKyat(stats.kpayTotal)}
• 🟡 WavePay စုစုပေါင်း: ${Formatters.formatKyat(stats.waveTotal)}
• 🟢 ငွေသား စုစုပေါင်း: ${Formatters.formatKyat(stats.cashTotal)}
${if (stats.cbPayTotal > 0) "• 🔴 CB Pay: ${Formatters.formatKyat(stats.cbPayTotal)}\n" else ""}${if (stats.ayaPayTotal > 0) "• 🟣 AYA Pay: ${Formatters.formatKyat(stats.ayaPayTotal)}\n" else ""}
🏬 <b>လက်ကျန် ပစ္စည်းတန်ဖိုး (Ending Stock):</b>
• ဝယ်ရင်းဈေးဖြင့်: ${Formatters.formatKyat(stats.endingStockValueCost)}
• ရောင်းဈေးဖြင့်: ${Formatters.formatKyat(stats.endingStockValueRetail)}

🔒 <b>လချုပ် အခြေအနေ:</b> ${if (stats.isClosed) "✅ လချုပ်ပိတ်သိမ်းပြီး" else "⏳ စာရင်းလည်ပတ်ဆဲ"}
📞 <i>ဆက်သွယ်ရန်: ${config.shopPhone}</i>
        """.trimIndent()
    }

    suspend fun buildLowStockAlertMessage(): String = withContext(Dispatchers.IO) {
        val config = getTelegramConfig()
        val products = productDao.getAllProductsSync().filter { it.isLowStock }

        if (products.isEmpty()) {
            return@withContext "✅ <b>${config.shopName}</b>\n\nလက်ရှိတွင် ပစ္စည်းအားလုံး Stock လုံလောက်စွာ ရှိနေပါသည်။"
        }

        val itemsList = products.joinToString("\n") { p ->
            "• ⚠️ <b>${p.name}</b> - လက်ကျန်: <b>${p.stockQuantity} ${p.unit}</b> (သတိပေးကန့်သတ်: ${p.lowStockThreshold})"
        }

        """
⚠️ <b>${config.shopName} - ပစ္စည်း Stock ကုန်ခါနီး သတိပေးချက်</b>
━━━━━━━━━━━━━━━━━━
အောက်ပါ ပစ္စည်းများ Stock နည်းနေသဖြင့် ထပ်ဖြည့်ရန် လိုအပ်ပါသည်:

$itemsList

⏰ <i>စစ်ဆေးချိန်: ${Formatters.formatDateTime(System.currentTimeMillis())}</i>
        """.trimIndent()
    }

    // Seed Demo Data for Myanmar Shop
    suspend fun seedSampleData() = withContext(Dispatchers.IO) {
        val sampleProducts = listOf(
            Product(name = "Premier Coffee Mix (30 pcs)", category = "အချိုရည်နှင့် အသင့်သောက်", stockQuantity = 3, unit = "ထုပ်", costPrice = 6500.0, sellingPrice = 7800.0, lowStockThreshold = 5),
            Product(name = "Royal Myanmar Tea Mix (30 pcs)", category = "အချိုရည်နှင့် အသင့်သောက်", stockQuantity = 24, unit = "ထုပ်", costPrice = 7200.0, sellingPrice = 8500.0, lowStockThreshold = 5),
            Product(name = "Mama Instant Noodles (ခေါက်ဆွဲခြောက်)", category = "စားသောက်ကုန်", stockQuantity = 45, unit = "ထုပ်", costPrice = 600.0, sellingPrice = 800.0, lowStockThreshold = 10),
            Product(name = "Yum Yum Instant Noodle Box", category = "စားသောက်ကုန်", stockQuantity = 2, unit = "ဖာ", costPrice = 18000.0, sellingPrice = 21500.0, lowStockThreshold = 4),
            Product(name = "ရွှေကြာ သနပ်ခါးခဲ (Shwe Pyi Nann)", category = "အလှကုန်နှင့် လူသုံးကုန်", stockQuantity = 15, unit = "တုံး", costPrice = 2200.0, sellingPrice = 3000.0, lowStockThreshold = 5),
            Product(name = "Sunkist Orange Juice (1L)", category = "အချိုရည်နှင့် အသင့်သောက်", stockQuantity = 8, unit = "ဘူး", costPrice = 3400.0, sellingPrice = 4200.0, lowStockThreshold = 4),
            Product(name = "Lifebuoy ဆပ်ပြာတုံး", category = "အလှကုန်နှင့် လူသုံးကုန်", stockQuantity = 4, unit = "တုံး", costPrice = 1100.0, sellingPrice = 1500.0, lowStockThreshold = 6),
            Product(name = "ဆန် ရွှေဘိုပေါ်ဆန်း (Shwebo Paw San 1 Pyi)", category = "စားသောက်ကုန်", stockQuantity = 18, unit = "ပြည်", costPrice = 6800.0, sellingPrice = 8000.0, lowStockThreshold = 5),
            Product(name = "မြေပဲဆီသန့် (Pure Peanut Oil 1L)", category = "စားသောက်ကုန်", stockQuantity = 12, unit = "ပုလင်း", costPrice = 13500.0, sellingPrice = 16000.0, lowStockThreshold = 3),
            Product(name = "Colgate သွားတိုက်ဆေး (150g)", category = "အလှကုန်နှင့် လူသုံးကုန်", stockQuantity = 1, unit = "ဘူး", costPrice = 3800.0, sellingPrice = 4800.0, lowStockThreshold = 5)
        )

        productDao.insertAll(sampleProducts)

        val now = System.currentTimeMillis()
        val dayMillis = 86400000L

        val sampleTransactions = listOf(
            TransactionRecord(
                type = TransactionType.INCOME,
                category = "အရောင်းရငွေ (Sales)",
                amount = 23400.0,
                paymentMethod = PaymentMethod.KPAY,
                note = "Premier Coffee x3 ရောင်းချမှု (KPay ဖြင့်ရှင်း)",
                dateMillis = now - 3600000L
            ),
            TransactionRecord(
                type = TransactionType.INCOME,
                category = "အရောင်းရငွေ (Sales)",
                amount = 17000.0,
                paymentMethod = PaymentMethod.WAVE_PAY,
                note = "Royal Tea Mix x2 (WavePay ဖြင့်ရှင်း)",
                dateMillis = now - 7200000L
            ),
            TransactionRecord(
                type = TransactionType.INCOME,
                category = "အရောင်းရငွေ (Sales)",
                amount = 32000.0,
                paymentMethod = PaymentMethod.CASH,
                note = "ဆီသန့် ၂ ပုလင်း (ငွေသား)",
                dateMillis = now - 14400000L
            ),
            TransactionRecord(
                type = TransactionType.EXPENSE,
                category = "ဆိုင်စရိတ် (Electricity/Shop)",
                amount = 25000.0,
                paymentMethod = PaymentMethod.KPAY,
                note = "ဆိုင် မီးဖိုး ပေးချေခြင်း (KPay)",
                dateMillis = now - 28800000L
            ),
            TransactionRecord(
                type = TransactionType.EXPENSE,
                category = "ပစ္စည်းဝယ်ယူစရိတ် (Stock)",
                amount = 130000.0,
                paymentMethod = PaymentMethod.WAVE_PAY,
                note = "Coffee Mix ဖာအသစ် ဝယ်ယူဖြည့်တင်းခြင်း",
                dateMillis = now - dayMillis
            ),
            TransactionRecord(
                type = TransactionType.INCOME,
                category = "အရောင်းရငွေ (Sales)",
                amount = 45000.0,
                paymentMethod = PaymentMethod.KPAY,
                note = "ကုန်စုံ အရောင်း (KPay)",
                dateMillis = now - (dayMillis * 2)
            ),
            TransactionRecord(
                type = TransactionType.INCOME,
                category = "အရောင်းရငွေ (Sales)",
                amount = 56000.0,
                paymentMethod = PaymentMethod.CASH,
                note = "ဆန်နှင့် ဆီ ရောင်းချမှု (ငွေသား)",
                dateMillis = now - (dayMillis * 3)
            )
        )
        transactionDao.insertAll(sampleTransactions)
    }

    // Database Backup & Restore Operations
    suspend fun getAllProductsForBackup(): List<Product> = withContext(Dispatchers.IO) {
        productDao.getAllProductsSync()
    }

    suspend fun getAllTransactionsForBackup(): List<TransactionRecord> = withContext(Dispatchers.IO) {
        transactionDao.getAllTransactionsSync()
    }

    suspend fun getAllStockLogsForBackup(): List<StockLog> = withContext(Dispatchers.IO) {
        stockLogDao.getAllLogsSync()
    }

    suspend fun getAllSettlementsForBackup(): List<MonthlySettlement> = withContext(Dispatchers.IO) {
        monthlySettlementDao.getAllSettlementsSync()
    }

    suspend fun restoreEntireDatabase(
        products: List<Product>,
        transactions: List<TransactionRecord>,
        stockLogs: List<StockLog>,
        settlements: List<MonthlySettlement>,
        config: TelegramConfig?,
        mode: RestoreMode
    ) = withContext(Dispatchers.IO) {
        if (mode == RestoreMode.REPLACE) {
            monthlySettlementDao.deleteAll()
            stockLogDao.deleteAll()
            transactionDao.deleteAll()
            productDao.deleteAll()
        }

        if (products.isNotEmpty()) {
            productDao.insertAll(products)
        }
        if (transactions.isNotEmpty()) {
            transactionDao.insertAll(transactions)
        }
        if (stockLogs.isNotEmpty()) {
            stockLogDao.insertAll(stockLogs)
        }
        if (settlements.isNotEmpty()) {
            monthlySettlementDao.insertAll(settlements)
        }
        if (config != null) {
            saveTelegramConfig(config)
        }
    }
}
