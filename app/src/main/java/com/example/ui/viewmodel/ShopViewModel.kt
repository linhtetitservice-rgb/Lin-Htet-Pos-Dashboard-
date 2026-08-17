package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.DashboardSummary
import com.example.data.repository.ShopRepository
import com.example.model.DateRangePreset
import com.example.model.MonthlySettlement
import com.example.model.PaymentMethod
import com.example.model.Product
import com.example.model.StockLog
import com.example.model.TelegramConfig
import com.example.model.TransactionRecord
import com.example.model.TransactionType
import com.example.util.BackupFileSummary
import com.example.util.DatabaseBackupManager
import com.example.util.DocumentExporter
import com.example.util.Formatters
import com.example.util.RestoreMode
import com.example.util.RestoreResult
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar

class ShopViewModel(application: Application) : AndroidViewModel(application) {
    val repository = ShopRepository(application)

    val products: StateFlow<List<Product>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockProducts: StateFlow<List<Product>> = repository.lowStockProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<TransactionRecord>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stockLogs: StateFlow<List<StockLog>> = repository.allStockLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSettlements: StateFlow<List<MonthlySettlement>> = repository.allSettlements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Telegram Config State
    private val _telegramConfig = MutableStateFlow(repository.getTelegramConfig())
    val telegramConfig: StateFlow<TelegramConfig> = _telegramConfig.asStateFlow()

    // Status Message / Notifications
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _isSendingTelegram = MutableStateFlow(false)
    val isSendingTelegram: StateFlow<Boolean> = _isSendingTelegram.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _isBackingUp = MutableStateFlow(false)
    val isBackingUp: StateFlow<Boolean> = _isBackingUp.asStateFlow()

    private val _isRestoring = MutableStateFlow(false)
    val isRestoring: StateFlow<Boolean> = _isRestoring.asStateFlow()

    // Monthly Settlement selected date
    private val currentCal = Calendar.getInstance()
    private val _selectedYear = MutableStateFlow(currentCal.get(Calendar.YEAR))
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    private val _selectedMonth = MutableStateFlow(currentCal.get(Calendar.MONTH)) // 0-indexed
    val selectedMonth: StateFlow<Int> = _selectedMonth.asStateFlow()

    private val _monthlyStats = MutableStateFlow<MonthlySettlement?>(null)
    val monthlyStats: StateFlow<MonthlySettlement?> = _monthlyStats.asStateFlow()

    // Search and filter states
    val productSearchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("All")
    val showOnlyLowStock = MutableStateFlow(false)

    val transactionSearchQuery = MutableStateFlow("")
    val transactionTypeFilter = MutableStateFlow<TransactionType?>(null)
    val paymentMethodFilter = MutableStateFlow<PaymentMethod?>(null)
    val transactionDatePreset = MutableStateFlow(DateRangePreset.ALL)
    val customStartDate = MutableStateFlow<Long?>(null)
    val customEndDate = MutableStateFlow<Long?>(null)

    // Filtered Products
    val filteredProducts: StateFlow<List<Product>> = combine(
        products,
        productSearchQuery,
        selectedCategory,
        showOnlyLowStock
    ) { list, query, category, lowStockOnly ->
        val trimmedQuery = query.trim()
        list.filter { product ->
            val matchesQuery = trimmedQuery.isBlank() ||
                    product.name.contains(trimmedQuery, ignoreCase = true) ||
                    product.barcode.contains(trimmedQuery, ignoreCase = true) ||
                    product.category.contains(trimmedQuery, ignoreCase = true) ||
                    product.id.toString() == trimmedQuery

            val matchesCategory = category == "All" || product.category == category
            val matchesLowStock = !lowStockOnly || product.isLowStock

            matchesQuery && matchesCategory && matchesLowStock
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private data class DateFilterParams(
        val preset: DateRangePreset,
        val customStart: Long?,
        val customEnd: Long?
    )

    private val dateFilterState = combine(
        transactionDatePreset,
        customStartDate,
        customEndDate
    ) { preset, start, end ->
        DateFilterParams(preset, start, end)
    }

    // Filtered Transactions for Detailed Ledger View
    val filteredTransactions: StateFlow<List<TransactionRecord>> = combine(
        transactions,
        transactionSearchQuery,
        transactionTypeFilter,
        paymentMethodFilter,
        dateFilterState
    ) { list, query, typeFilter, payFilter, dateParams ->
        val trimmedQuery = query.trim()
        list.filter { tx ->
            val matchesQuery = trimmedQuery.isBlank() ||
                    tx.category.contains(trimmedQuery, ignoreCase = true) ||
                    tx.note.contains(trimmedQuery, ignoreCase = true) ||
                    (tx.productName?.contains(trimmedQuery, ignoreCase = true) ?: false) ||
                    Formatters.formatKyat(tx.amount).contains(trimmedQuery, ignoreCase = true) ||
                    tx.amount.toString().contains(trimmedQuery)

            val matchesType = typeFilter == null || tx.type == typeFilter
            val matchesPay = payFilter == null || tx.paymentMethod == payFilter

            val matchesDate = when (dateParams.preset) {
                DateRangePreset.ALL -> true
                DateRangePreset.TODAY -> {
                    val (start, end) = Formatters.getStartAndEndOfToday()
                    tx.dateMillis in start..end
                }
                DateRangePreset.YESTERDAY -> {
                    val (start, end) = Formatters.getStartAndEndOfYesterday()
                    tx.dateMillis in start..end
                }
                DateRangePreset.THIS_WEEK -> {
                    val (start, end) = Formatters.getStartAndEndOfWeek()
                    tx.dateMillis in start..end
                }
                DateRangePreset.THIS_MONTH -> {
                    val now = Calendar.getInstance()
                    val (start, end) = Formatters.getStartAndEndOfMonth(now.get(Calendar.YEAR), now.get(Calendar.MONTH))
                    tx.dateMillis in start..end
                }
                DateRangePreset.LAST_MONTH -> {
                    val (start, end) = Formatters.getStartAndEndOfLastMonth()
                    tx.dateMillis in start..end
                }
                DateRangePreset.CUSTOM -> {
                    val start = dateParams.customStart ?: 0L
                    val end = dateParams.customEnd ?: Long.MAX_VALUE
                    tx.dateMillis in start..end
                }
            }

            matchesQuery && matchesType && matchesPay && matchesDate
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Live Dashboard Summary
    val dashboardSummary: StateFlow<DashboardSummary> = combine(
        products,
        transactions
    ) { prodList, txList ->
        val (todayStart, todayEnd) = Formatters.getStartAndEndOfToday()
        val nowCal = Calendar.getInstance()
        val (monthStart, monthEnd) = Formatters.getStartAndEndOfMonth(
            nowCal.get(Calendar.YEAR),
            nowCal.get(Calendar.MONTH)
        )

        val todayTxs = txList.filter { it.dateMillis in todayStart..todayEnd }
        val monthTxs = txList.filter { it.dateMillis in monthStart..monthEnd }

        val todayIncome = todayTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val todayExpense = todayTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

        val monthIncome = monthTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val monthExpense = monthTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

        val todayKpay = todayTxs.filter { it.type == TransactionType.INCOME && it.paymentMethod == PaymentMethod.KPAY }.sumOf { it.amount }
        val todayWave = todayTxs.filter { it.type == TransactionType.INCOME && it.paymentMethod == PaymentMethod.WAVE_PAY }.sumOf { it.amount }
        val todayCash = todayTxs.filter { it.type == TransactionType.INCOME && it.paymentMethod == PaymentMethod.CASH }.sumOf { it.amount }

        val lowStock = prodList.count { it.isLowStock }
        val costVal = prodList.sumOf { it.totalCostValue }
        val retailVal = prodList.sumOf { it.totalRetailValue }

        DashboardSummary(
            todayIncome = todayIncome,
            todayExpense = todayExpense,
            todayNetProfit = todayIncome - todayExpense,
            monthIncome = monthIncome,
            monthExpense = monthExpense,
            monthNetProfit = monthIncome - monthExpense,
            todayKpayTotal = todayKpay,
            todayWaveTotal = todayWave,
            todayCashTotal = todayCash,
            totalProductsCount = prodList.size,
            lowStockCount = lowStock,
            totalInventoryCostValue = costVal,
            totalInventoryRetailValue = retailVal
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardSummary())

    init {
        loadMonthlySettlement(currentCal.get(Calendar.YEAR), currentCal.get(Calendar.MONTH))
    }

    fun showSnackbar(message: String) {
        _snackbarMessage.value = message
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    // Monthly selection
    fun setSelectedMonthAndYear(year: Int, month: Int) {
        _selectedYear.value = year
        _selectedMonth.value = month
        loadMonthlySettlement(year, month)
    }

    fun loadMonthlySettlement(year: Int, month: Int) {
        viewModelScope.launch {
            val stats = repository.calculateMonthlyStats(year, month)
            _monthlyStats.value = stats
        }
    }

    fun closeMonthSettlement(notes: String) {
        viewModelScope.launch {
            val current = _monthlyStats.value ?: return@launch
            val updated = current.copy(
                isClosed = true,
                closedAtMillis = System.currentTimeMillis(),
                notes = notes
            )
            repository.saveMonthlySettlement(updated)
            _monthlyStats.value = updated
            showSnackbar("လချုပ် စာရင်းကို အောင်မြင်စွာ ပိတ်သိမ်းသိမ်းဆည်းလိုက်ပါပြီ ✅")
        }
    }

    // Product actions
    fun addProduct(product: Product) {
        viewModelScope.launch {
            repository.addProduct(product)
            showSnackbar("${product.name} ကို ပစ္စည်းစာရင်းထဲ ထည့်သွင်းပြီးပါပြီ ✅")
        }
    }

    fun updateProduct(product: Product) {
        viewModelScope.launch {
            repository.updateProduct(product)
            showSnackbar("${product.name} အချက်အလက်ကို ပြင်ဆင်ပြီးပါပြီ ✅")
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            showSnackbar("${product.name} ကို ဖျက်လိုက်ပါပြီ")
        }
    }

    fun restockProduct(
        productId: Long,
        addQuantity: Int,
        costPerUnit: Double,
        paymentMethod: PaymentMethod,
        recordAsExpense: Boolean,
        note: String
    ) {
        viewModelScope.launch {
            val result = repository.restockProduct(
                productId = productId,
                addedQuantity = addQuantity,
                costPerUnit = costPerUnit,
                paymentMethod = paymentMethod,
                recordAsExpense = recordAsExpense,
                note = note
            )
            if (result.isSuccess) {
                showSnackbar("Stock +$addQuantity ခု ထပ်ဖြည့်ပြီးပါပြီ 📦")
                loadMonthlySettlement(_selectedYear.value, _selectedMonth.value)
            } else {
                showSnackbar("မှားယွင်းမှု: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun quickSale(
        productId: Long,
        quantity: Int,
        unitPrice: Double,
        paymentMethod: PaymentMethod,
        note: String
    ) {
        viewModelScope.launch {
            val result = repository.quickSale(
                productId = productId,
                quantityToSell = quantity,
                unitSellingPrice = unitPrice,
                paymentMethod = paymentMethod,
                note = note
            )
            if (result.isSuccess) {
                showSnackbar("ရောင်းချမှု မှတ်တမ်းတင်ပြီးပါပြီ (${paymentMethod.shortCode}) 💰")
                loadMonthlySettlement(_selectedYear.value, _selectedMonth.value)
            } else {
                showSnackbar("မှားယွင်းမှု: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    // Transaction Actions
    fun addTransaction(
        type: TransactionType,
        category: String,
        amount: Double,
        paymentMethod: PaymentMethod,
        note: String
    ) {
        viewModelScope.launch {
            val record = TransactionRecord(
                type = type,
                category = category,
                amount = amount,
                paymentMethod = paymentMethod,
                note = note,
                dateMillis = System.currentTimeMillis()
            )
            repository.addTransaction(record)
            showSnackbar("${type.myanmarLabel} ${Formatters.formatKyat(amount)} မှတ်တမ်းတင်ပြီးပါပြီ")
            loadMonthlySettlement(_selectedYear.value, _selectedMonth.value)
        }
    }

    fun deleteTransaction(transaction: TransactionRecord) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            showSnackbar("စာရင်းမှတ်တမ်းကို ဖျက်လိုက်ပါပြီ")
            loadMonthlySettlement(_selectedYear.value, _selectedMonth.value)
        }
    }

    fun setDateRangePreset(preset: DateRangePreset) {
        transactionDatePreset.value = preset
    }

    fun setCustomDateRange(startMillis: Long, endMillis: Long) {
        customStartDate.value = startMillis
        customEndDate.value = endMillis
        transactionDatePreset.value = DateRangePreset.CUSTOM
    }

    fun resetTransactionFilters() {
        transactionSearchQuery.value = ""
        transactionTypeFilter.value = null
        paymentMethodFilter.value = null
        transactionDatePreset.value = DateRangePreset.ALL
        customStartDate.value = null
        customEndDate.value = null
    }

    // Telegram Configurations & Actions
    fun updateTelegramConfig(config: TelegramConfig) {
        repository.saveTelegramConfig(config)
        _telegramConfig.value = config
        showSnackbar("ဆက်တင်များကို သိမ်းဆည်းပြီးပါပြီ ✅")
    }

    fun sendTodaySummaryToTelegram() {
        viewModelScope.launch {
            _isSendingTelegram.value = true
            try {
                val message = repository.buildTodaySummaryMessage()
                val result = repository.sendTelegramMessage(message)
                if (result.isSuccess) {
                    showSnackbar("ယနေ့စာရင်းချုပ် Telegram သို့ ပို့ပြီးပါပြီ 🚀")
                } else {
                    showSnackbar("မအောင်မြင်ပါ: ${result.exceptionOrNull()?.message}")
                }
            } finally {
                _isSendingTelegram.value = false
            }
        }
    }

    fun sendMonthlyStatementToTelegram(year: Int, month: Int) {
        viewModelScope.launch {
            _isSendingTelegram.value = true
            try {
                val message = repository.buildMonthlyStatementMessage(year, month)
                val result = repository.sendTelegramMessage(message)
                if (result.isSuccess) {
                    showSnackbar("လချုပ်ရှင်းတမ်း Telegram သို့ ပို့ပြီးပါပြီ 🚀")
                } else {
                    showSnackbar("မအောင်မြင်ပါ: ${result.exceptionOrNull()?.message}")
                }
            } finally {
                _isSendingTelegram.value = false
            }
        }
    }

    fun sendLowStockAlertToTelegram() {
        viewModelScope.launch {
            _isSendingTelegram.value = true
            try {
                val message = repository.buildLowStockAlertMessage()
                val result = repository.sendTelegramMessage(message)
                if (result.isSuccess) {
                    showSnackbar("Stock သတိပေးချက် Telegram သို့ ပို့ပြီးပါပြီ 🚀")
                } else {
                    showSnackbar("မအောင်မြင်ပါ: ${result.exceptionOrNull()?.message}")
                }
            } finally {
                _isSendingTelegram.value = false
            }
        }
    }

    fun testTelegramConnection(botToken: String, chatId: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isSendingTelegram.value = true
            val testMsg = "🔔 <b>Telegram ချိတ်ဆက်မှု စမ်းသပ်ခြင်း အောင်မြင်ပါသည်!</b>\n\nဆိုင်စာရင်း (Shop Manager) နှင့် ချိတ်ဆက်မှု အဆင်ပြေစွာ ဆောင်ရွက်ပြီးပါပြီ။"
            val result = repository.sendTelegramMessage(testMsg)
            _isSendingTelegram.value = false
            if (result.isSuccess) {
                onResult(true, "ချိတ်ဆက်မှု အောင်မြင်ပါသည် ✅")
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "ချိတ်ဆက်၍ မရပါ")
            }
        }
    }

    fun exportMonthlyReportPdf(year: Int, month: Int, onComplete: (File?) -> Unit) {
        viewModelScope.launch {
            _isExporting.value = true
            try {
                val file = withContext(Dispatchers.IO) {
                    val (monthStart, monthEnd) = Formatters.getStartAndEndOfMonth(year, month)
                    val txs = repository.getTransactionsBetween(monthStart, monthEnd)
                    val stats = repository.calculateMonthlyStats(year, month)
                    val config = repository.getTelegramConfig()
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                    }
                    val monthLabel = Formatters.formatMonthYear(cal.timeInMillis)
                    DocumentExporter.generateMonthlyReportPdf(
                        context = getApplication(),
                        monthLabel = monthLabel,
                        settlement = stats,
                        transactions = txs,
                        config = config
                    )
                }
                showSnackbar("PDF စာရင်းရှင်းတမ်း ထုတ်ယူပြီးပါပြီ 📄")
                onComplete(file)
            } catch (e: Exception) {
                showSnackbar("PDF ထုတ်ယူရာတွင် အမှားဖြစ်ပေါ်ပါသည်: ${e.message}")
                onComplete(null)
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun exportMonthlyReportPng(year: Int, month: Int, onComplete: (File?) -> Unit) {
        viewModelScope.launch {
            _isExporting.value = true
            try {
                val file = withContext(Dispatchers.IO) {
                    val (monthStart, monthEnd) = Formatters.getStartAndEndOfMonth(year, month)
                    val txs = repository.getTransactionsBetween(monthStart, monthEnd)
                    val stats = repository.calculateMonthlyStats(year, month)
                    val config = repository.getTelegramConfig()
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                    }
                    val monthLabel = Formatters.formatMonthYear(cal.timeInMillis)
                    DocumentExporter.generateMonthlyReportPng(
                        context = getApplication(),
                        monthLabel = monthLabel,
                        settlement = stats,
                        transactions = txs,
                        config = config
                    )
                }
                showSnackbar("PNG ပုံရိပ် စာရင်းရှင်းတမ်း ထုတ်ယူပြီးပါပြီ 🖼️")
                onComplete(file)
            } catch (e: Exception) {
                showSnackbar("PNG ထုတ်ယူရာတွင် အမှားဖြစ်ပေါ်ပါသည်: ${e.message}")
                onComplete(null)
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun seedSampleData() {
        viewModelScope.launch {
            repository.seedSampleData()
            showSnackbar("နမူနာ ဆိုင်စာရင်းနှင့် Stock ပစ္စည်းများကို ထည့်သွင်းပေးပြီးပါပြီ ✨")
            loadMonthlySettlement(_selectedYear.value, _selectedMonth.value)
        }
    }

    // Local Database Backup & Restore
    fun exportLocalDatabaseBackup(onComplete: (File?) -> Unit) {
        viewModelScope.launch {
            _isBackingUp.value = true
            try {
                val file = DatabaseBackupManager.createBackupFile(getApplication(), repository)
                showSnackbar("ဒေတာဘေ့စ် Backup ဖိုင် (${file.name}) ဖန်တီးပြီးပါပြီ 💾")
                onComplete(file)
            } catch (e: Exception) {
                showSnackbar("Backup ထုတ်ယူရာတွင် အမှားဖြစ်ပေါ်ပါသည်: ${e.message}")
                onComplete(null)
            } finally {
                _isBackingUp.value = false
            }
        }
    }

    fun exportBackupToUri(uri: Uri, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isBackingUp.value = true
            try {
                val success = DatabaseBackupManager.writeBackupToUri(getApplication(), repository, uri)
                if (success) {
                    showSnackbar("ဒေတာဘေ့စ် Backup ကို ရွေးချယ်ထားသော ဖိုင်တွဲထဲသို့ သိမ်းဆည်းပြီးပါပြီ 📁")
                } else {
                    showSnackbar("ဖိုင်သိမ်းဆည်း၍ မရပါ")
                }
                onComplete(success)
            } catch (e: Exception) {
                showSnackbar("Backup သိမ်းဆည်းရာတွင် အမှားဖြစ်ပေါ်ပါသည်: ${e.message}")
                onComplete(false)
            } finally {
                _isBackingUp.value = false
            }
        }
    }

    fun restoreDatabaseFromJson(
        jsonString: String,
        mode: RestoreMode,
        onComplete: (RestoreResult) -> Unit
    ) {
        viewModelScope.launch {
            _isRestoring.value = true
            try {
                val result = DatabaseBackupManager.restoreDatabaseFromJson(repository, jsonString, mode)
                if (result.isSuccess) {
                    _telegramConfig.value = repository.getTelegramConfig()
                    loadMonthlySettlement(_selectedYear.value, _selectedMonth.value)
                    showSnackbar("ဒေတာများ အောင်မြင်စွာ ပြန်လည်ရယူပြီးပါပြီ (ပစ္စည်း: ${result.restoredProducts}၊ စာရင်း: ${result.restoredTransactions}) 🎉")
                } else {
                    showSnackbar("ဒေတာ ပြန်လည်ရယူ၍ မရပါ: ${result.errorMessage}")
                }
                onComplete(result)
            } catch (e: Exception) {
                val errResult = RestoreResult(isSuccess = false, errorMessage = e.localizedMessage)
                showSnackbar("ပြန်လည်ရယူရာတွင် အမှားဖြစ်ပေါ်ပါသည်: ${e.message}")
                onComplete(errResult)
            } finally {
                _isRestoring.value = false
            }
        }
    }
}
