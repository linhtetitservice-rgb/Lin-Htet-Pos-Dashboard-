package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.DateRangePreset
import com.example.model.PaymentMethod
import com.example.model.TransactionRecord
import com.example.model.TransactionType
import com.example.ui.components.AddTransactionDialog
import com.example.ui.components.CustomDateRangePickerDialog
import com.example.ui.components.PaymentBadge
import com.example.ui.components.TransactionDetailDialog
import com.example.ui.theme.AYAPayPurple
import com.example.ui.theme.AYAPayPurpleContainer
import com.example.ui.theme.CBPayRed
import com.example.ui.theme.CBPayRedContainer
import com.example.ui.theme.CashGreen
import com.example.ui.theme.CashGreenContainer
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkOutline
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ExpenseColor
import com.example.ui.theme.ExpenseContainer
import com.example.ui.theme.IncomeColor
import com.example.ui.theme.IncomeContainer
import com.example.ui.theme.KPayBlue
import com.example.ui.theme.KPayBlueContainer
import com.example.ui.theme.PurpleContainerLight
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurplePrimaryDeep
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WavePayYellow
import com.example.ui.theme.WavePayYellowContainer
import com.example.ui.viewmodel.ShopViewModel
import com.example.util.DocumentExporter
import com.example.util.Formatters
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransactionsScreen(
    viewModel: ShopViewModel
) {
    val context = LocalContext.current
    val transactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val allTransactions by viewModel.transactions.collectAsStateWithLifecycle()
    val typeFilter by viewModel.transactionTypeFilter.collectAsStateWithLifecycle()
    val paymentFilter by viewModel.paymentMethodFilter.collectAsStateWithLifecycle()
    val datePreset by viewModel.transactionDatePreset.collectAsStateWithLifecycle()
    val customStartDate by viewModel.customStartDate.collectAsStateWithLifecycle()
    val customEndDate by viewModel.customEndDate.collectAsStateWithLifecycle()
    val searchQuery by viewModel.transactionSearchQuery.collectAsStateWithLifecycle()
    val isExporting by viewModel.isExporting.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedDetailTx by remember { mutableStateOf<TransactionRecord?>(null) }
    var deleteCandidateTx by remember { mutableStateOf<TransactionRecord?>(null) }
    var showCustomDateDialog by remember { mutableStateOf(false) }
    var latestExportedFile by remember { mutableStateOf<File?>(null) }
    var latestExportedMime by remember { mutableStateOf("application/pdf") }
    var showExportSuccessDialog by remember { mutableStateOf(false) }

    // Summary calculations
    val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    val netBalance = totalIncome - totalExpense

    // Channel specific calculations
    val cashIncome = transactions.filter { it.type == TransactionType.INCOME && it.paymentMethod == PaymentMethod.CASH }.sumOf { it.amount }
    val cashExpense = transactions.filter { it.type == TransactionType.EXPENSE && it.paymentMethod == PaymentMethod.CASH }.sumOf { it.amount }
    val kpayIncome = transactions.filter { it.type == TransactionType.INCOME && it.paymentMethod == PaymentMethod.KPAY }.sumOf { it.amount }
    val kpayExpense = transactions.filter { it.type == TransactionType.EXPENSE && it.paymentMethod == PaymentMethod.KPAY }.sumOf { it.amount }
    val waveIncome = transactions.filter { it.type == TransactionType.INCOME && it.paymentMethod == PaymentMethod.WAVE_PAY }.sumOf { it.amount }
    val waveExpense = transactions.filter { it.type == TransactionType.EXPENSE && it.paymentMethod == PaymentMethod.WAVE_PAY }.sumOf { it.amount }

    val hasActiveFilters = searchQuery.isNotBlank() ||
            typeFilter != null ||
            paymentFilter != null ||
            datePreset != DateRangePreset.ALL

    Scaffold(
        containerColor = DarkBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PurpleContainerLight,
                contentColor = PurplePrimaryDeep,
                modifier = Modifier.testTag("fab_add_transaction")
            ) {
                Icon(Icons.Default.Add, contentDescription = "စာရင်းအသစ်မှတ်မည်")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(DarkBackground)
                .testTag("transactions_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Title Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                    border = BorderStroke(1.dp, PurplePrimary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = PurplePrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = "📖 ငွေစာရင်း လယ်ဂျာ (Ledger)",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "ရက်စွဲအလိုက် ငွေပေးချေမှုပုံစံများ (Cash, KPay, WavePay) ဖြင့် စစ်ဆေးခြင်း",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }

                        Button(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PurpleContainerLight,
                                contentColor = PurplePrimaryDeep
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("btn_ledger_add_tx")
                        ) {
                            Text("စာရင်းသွင်း ➕", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Ledger Search Field
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.transactionSearchQuery.value = it },
                    placeholder = { Text("စာရင်းအမျိုးအစား၊ ပစ္စည်း၊ မှတ်ချက်၊ ပမာဏ ရှာရန်...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PurplePrimary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.transactionSearchQuery.value = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurplePrimary,
                        unfocusedBorderColor = DarkOutline,
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ledger_search_input")
                )
            }

            // Date Range Filter Section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.DateRange, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(16.dp))
                            Text("ရက်စွဲအပိုင်းအခြား (Date Range):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        }
                        if (datePreset == DateRangePreset.CUSTOM && customStartDate != null && customEndDate != null) {
                            Text(
                                text = "${Formatters.formatDate(customStartDate!!)} - ${Formatters.formatDate(customEndDate!!)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PurplePrimary
                            )
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(vertical = 2.dp),
                        modifier = Modifier.testTag("date_range_filters")
                    ) {
                        items(DateRangePreset.values()) { preset ->
                            val isSelected = datePreset == preset
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (preset == DateRangePreset.CUSTOM) {
                                        showCustomDateDialog = true
                                    } else {
                                        viewModel.setDateRangePreset(preset)
                                    }
                                },
                                label = {
                                    Text(
                                        text = preset.myanmarLabel,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PurpleContainerLight,
                                    selectedLabelColor = PurplePrimaryDeep,
                                    containerColor = DarkSurface,
                                    labelColor = TextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = DarkOutline.copy(alpha = 0.5f),
                                    selectedBorderColor = PurplePrimary
                                )
                            )
                        }
                    }
                }
            }

            // Payment Method Filter Section (Cash, KPay, WavePay, etc.)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(16.dp))
                        Text("ငွေပေးချေမှုပုံစံ (Payment Method):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(vertical = 2.dp),
                        modifier = Modifier.testTag("payment_method_filters")
                    ) {
                        item {
                            FilterChip(
                                selected = paymentFilter == null,
                                onClick = { viewModel.paymentMethodFilter.value = null },
                                label = { Text("ငွေချေမှုအားလုံး", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PurpleContainerLight,
                                    selectedLabelColor = PurplePrimaryDeep,
                                    containerColor = DarkSurface,
                                    labelColor = TextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = paymentFilter == null,
                                    borderColor = DarkOutline.copy(alpha = 0.5f),
                                    selectedBorderColor = PurplePrimary
                                )
                            )
                        }

                        items(PaymentMethod.values()) { method ->
                            val isSelected = paymentFilter == method
                            val (badgeBg, badgeText) = when (method) {
                                PaymentMethod.KPAY -> Pair(KPayBlueContainer, KPayBlue)
                                PaymentMethod.WAVE_PAY -> Pair(WavePayYellowContainer, WavePayYellow)
                                PaymentMethod.CASH -> Pair(CashGreenContainer, CashGreen)
                                PaymentMethod.CB_PAY -> Pair(CBPayRedContainer, CBPayRed)
                                PaymentMethod.AYA_PAY -> Pair(AYAPayPurpleContainer, AYAPayPurple)
                                PaymentMethod.OTHER -> Pair(DarkSurfaceVariant, TextSecondary)
                            }

                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    viewModel.paymentMethodFilter.value = if (isSelected) null else method
                                },
                                label = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(badgeText)
                                        )
                                        Text(
                                            text = method.myanmarLabel,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = badgeBg,
                                    selectedLabelColor = badgeText,
                                    containerColor = DarkSurface,
                                    labelColor = TextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = DarkOutline.copy(alpha = 0.5f),
                                    selectedBorderColor = badgeText
                                )
                            )
                        }
                    }
                }
            }

            // Transaction Type Filters (All, Income, Expense)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = typeFilter == null,
                        onClick = { viewModel.transactionTypeFilter.value = null },
                        label = { Text("အားလုံး", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PurpleContainerLight,
                            selectedLabelColor = PurplePrimaryDeep,
                            containerColor = DarkSurface,
                            labelColor = TextSecondary
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = typeFilter == TransactionType.INCOME,
                        onClick = { viewModel.transactionTypeFilter.value = TransactionType.INCOME },
                        label = { Text("➕ ဝင်ငွေသာ", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IncomeColor,
                            selectedLabelColor = Color(0xFF14532D),
                            containerColor = DarkSurface,
                            labelColor = IncomeColor
                        ),
                        modifier = Modifier.weight(1.2f)
                    )

                    FilterChip(
                        selected = typeFilter == TransactionType.EXPENSE,
                        onClick = { viewModel.transactionTypeFilter.value = TransactionType.EXPENSE },
                        label = { Text("➖ ထွက်ငွေသာ", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ExpenseColor,
                            selectedLabelColor = Color(0xFF601410),
                            containerColor = DarkSurface,
                            labelColor = ExpenseColor
                        ),
                        modifier = Modifier.weight(1.2f)
                    )
                }
            }

            // Filter status and reset bar
            if (hasActiveFilters) {
                item {
                    Surface(
                        color = DarkSurfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, PurplePrimary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.FilterAlt, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "ရွေးချယ်ထားသော စာရင်း: ${transactions.size} စောင်",
                                    fontSize = 12.sp,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Text(
                                text = "Filter ရှင်းထုတ်မည် ✕",
                                fontSize = 11.sp,
                                color = PurplePrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { viewModel.resetTransactionFilters() }
                                    .padding(4.dp)
                                    .testTag("btn_reset_ledger_filters")
                            )
                        }
                    }
                }
            }

            // Detailed Ledger Financial Summary Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, DarkOutline.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ledger_summary_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "📊 လယ်ဂျာ ငွေစာရင်းချုပ် (Ledger Financial Summary)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = PurplePrimary
                        )

                        // 3 Main Key Metrics: Income, Expense, Net Balance
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Total Income
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = DarkSurfaceVariant,
                                border = BorderStroke(1.dp, IncomeColor.copy(alpha = 0.3f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("ဝင်ငွေ စုစုပေါင်း", fontSize = 11.sp, color = TextSecondary)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "+${Formatters.formatKyat(totalIncome, includeSuffix = false)}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = IncomeColor
                                    )
                                }
                            }

                            // Total Expense
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = DarkSurfaceVariant,
                                border = BorderStroke(1.dp, ExpenseColor.copy(alpha = 0.3f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("ထွက်ငွေ စုစုပေါင်း", fontSize = 11.sp, color = TextSecondary)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "-${Formatters.formatKyat(totalExpense, includeSuffix = false)}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ExpenseColor
                                    )
                                }
                            }

                            // Net Balance
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = DarkSurfaceVariant,
                                border = BorderStroke(1.dp, (if (netBalance >= 0) IncomeColor else ExpenseColor).copy(alpha = 0.3f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("အသားတင် လက်ကျန်", fontSize = 11.sp, color = TextSecondary)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        Formatters.formatKyat(netBalance, includeSuffix = false),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (netBalance >= 0) IncomeColor else ExpenseColor
                                    )
                                }
                            }
                        }

                        // Payment Channel Breakdown Pills
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = DarkSurfaceVariant,
                            border = BorderStroke(1.dp, DarkOutline.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("ငွေပေးချေမှု ပုံစံအလိုက် ခွဲခြမ်းစိတ်ဖြာချက်:", fontSize = 11.sp, color = TextSecondary)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Cash Breakdown
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(CashGreen))
                                        Text("💵 Cash:", fontSize = 11.sp, color = TextPrimary)
                                        Text(Formatters.formatKyat(cashIncome - cashExpense), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CashGreen)
                                    }

                                    // KPay Breakdown
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(KPayBlue))
                                        Text("🔷 KPay:", fontSize = 11.sp, color = TextPrimary)
                                        Text(Formatters.formatKyat(kpayIncome - kpayExpense), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = KPayBlue)
                                    }

                                    // WavePay Breakdown
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(WavePayYellow))
                                        Text("🟡 Wave:", fontSize = 11.sp, color = TextPrimary)
                                        Text(Formatters.formatKyat(waveIncome - waveExpense), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WavePayYellow)
                                    }
                                }
                            }
                        }

                        // Export & Share Actions for Filtered Ledger
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val cal = java.util.Calendar.getInstance()
                                    viewModel.exportMonthlyReportPdf(
                                        cal.get(java.util.Calendar.YEAR),
                                        cal.get(java.util.Calendar.MONTH)
                                    ) { file ->
                                        if (file != null) {
                                            latestExportedFile = file
                                            latestExportedMime = "application/pdf"
                                            showExportSuccessDialog = true
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, PurplePrimary.copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f).testTag("btn_export_ledger_pdf")
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("PDF ထုတ်မည်", fontSize = 11.sp, color = PurplePrimary, fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = {
                                    val cal = java.util.Calendar.getInstance()
                                    viewModel.exportMonthlyReportPng(
                                        cal.get(java.util.Calendar.YEAR),
                                        cal.get(java.util.Calendar.MONTH)
                                    ) { file ->
                                        if (file != null) {
                                            latestExportedFile = file
                                            latestExportedMime = "image/png"
                                            showExportSuccessDialog = true
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, DarkOutline),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f).testTag("btn_export_ledger_img")
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ပုံအဖြစ်သိမ်း", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Ledger List Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📜 စာရင်းမှတ်တမ်းများ (${transactions.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )

                    Text(
                        text = "အသေးစိတ်ကြည့်ရန် စာရင်းကို နှိပ်ပါ",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            // Empty State
            if (transactions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = BorderStroke(1.dp, DarkOutline.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                modifier = Modifier.size(44.dp),
                                tint = PurplePrimary.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (hasActiveFilters) "ရွေးချယ်ထားသော Filter နှင့် ကိုက်ညီသည့် စာရင်းမရှိပါ" else "စာရင်းမှတ်တမ်း မရှိသေးပါ",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            if (hasActiveFilters) {
                                OutlinedButton(
                                    onClick = { viewModel.resetTransactionFilters() },
                                    border = BorderStroke(1.dp, PurplePrimary.copy(alpha = 0.5f))
                                ) {
                                    Icon(Icons.Default.FilterAltOff, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Filter အားလုံး ရှင်းထုတ်မည်", color = PurplePrimary, fontSize = 12.sp)
                                }
                            } else {
                                Button(
                                    onClick = { showAddDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = PurpleContainerLight, contentColor = PurplePrimaryDeep)
                                ) {
                                    Text("စာရင်းအသစ်သွင်းမည် ➕", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Detailed Itemized Ledger Cards
            items(transactions, key = { it.id }) { tx ->
                val isIncome = tx.type == TransactionType.INCOME
                val (channelBg, channelText, channelIconLabel) = when (tx.paymentMethod) {
                    PaymentMethod.KPAY -> Triple(KPayBlueContainer, KPayBlue, "KPay")
                    PaymentMethod.WAVE_PAY -> Triple(WavePayYellowContainer, WavePayYellow, "Wave")
                    PaymentMethod.CASH -> Triple(CashGreenContainer, CashGreen, "Cash")
                    PaymentMethod.CB_PAY -> Triple(CBPayRedContainer, CBPayRed, "CB")
                    PaymentMethod.AYA_PAY -> Triple(AYAPayPurpleContainer, AYAPayPurple, "AYA")
                    PaymentMethod.OTHER -> Triple(DarkSurfaceVariant, TextSecondary, "Other")
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, DarkOutline.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedDetailTx = tx }
                        .testTag("ledger_item_${tx.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            // Payment Method Channel Avatar
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(channelBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = channelIconLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = channelText
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = tx.category,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = TextPrimary
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isIncome) IncomeContainer else ExpenseContainer
                                    ) {
                                        Text(
                                            text = if (isIncome) "IN" else "OUT",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isIncome) IncomeColor else ExpenseColor,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }

                                if (!tx.productName.isNullOrBlank()) {
                                    Text(
                                        text = "📦 ${tx.productName}${if (tx.quantity != null) " (${tx.quantity} ခု)" else ""}",
                                        fontSize = 12.sp,
                                        color = PurplePrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                if (tx.note.isNotBlank()) {
                                    Text(
                                        text = tx.note,
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        maxLines = 1
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${Formatters.formatDateTime(tx.dateMillis)} • ${tx.paymentMethod.myanmarLabel}",
                                    fontSize = 10.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                        contentDescription = null,
                                        tint = if (isIncome) IncomeColor else ExpenseColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = Formatters.formatKyat(tx.amount),
                                        fontWeight = FontWeight.Bold,
                                        color = if (isIncome) IncomeColor else ExpenseColor,
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            IconButton(
                                onClick = { deleteCandidateTx = tx },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "ဖျက်မည်", tint = DarkOutline.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Transaction Dialog
    if (showAddDialog) {
        AddTransactionDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { type, cat, amt, method, note ->
                viewModel.addTransaction(type, cat, amt, method, note)
                showAddDialog = false
            }
        )
    }

    // Detail Inspection Modal
    selectedDetailTx?.let { tx ->
        TransactionDetailDialog(
            transaction = tx,
            onDismiss = { selectedDetailTx = null },
            onDelete = { candidate ->
                deleteCandidateTx = candidate
                selectedDetailTx = null
            }
        )
    }

    // Custom Date Range Picker Dialog
    if (showCustomDateDialog) {
        CustomDateRangePickerDialog(
            initialStartMillis = customStartDate,
            initialEndMillis = customEndDate,
            onDismiss = { showCustomDateDialog = false },
            onConfirm = { start, end ->
                viewModel.setCustomDateRange(start, end)
                showCustomDateDialog = false
            }
        )
    }

    // Delete Confirmation Dialog
    if (deleteCandidateTx != null) {
        AlertDialog(
            onDismissRequest = { deleteCandidateTx = null },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(20.dp),
            title = { Text("စာရင်း ဖျက်ရန် သေချာပါသလား?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "${deleteCandidateTx!!.category} (${Formatters.formatKyat(deleteCandidateTx!!.amount)}) ကို စာရင်းထဲမှ ဖျက်ပါမည်။",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTransaction(deleteCandidateTx!!)
                        deleteCandidateTx = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseColor, contentColor = Color(0xFF601410)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("ဖျက်မည်", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { deleteCandidateTx = null },
                    border = BorderStroke(1.dp, DarkOutline),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("မဖျက်ပါ", color = TextSecondary)
                }
            }
        )
    }

    // Export Success Dialog
    if (showExportSuccessDialog && latestExportedFile != null) {
        AlertDialog(
            onDismissRequest = { showExportSuccessDialog = false },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = PurplePrimary)
                    Text("စာရင်းဖိုင် ထုတ်ယူပြီးပါပြီ ✅", fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("လယ်ဂျာ စာရင်းရှင်းတမ်းဖိုင် သိမ်းဆည်းပြီးပါပြီ။", color = TextSecondary)
                    Surface(
                        color = DarkSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = latestExportedFile!!.name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = PurplePrimary,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        DocumentExporter.shareDocument(context, latestExportedFile!!, latestExportedMime, "ငွေစာရင်း လယ်ဂျာ အစီရင်ခံစာ")
                        showExportSuccessDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PurpleContainerLight, contentColor = PurplePrimaryDeep),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ဖိုင် Share မည်", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        DocumentExporter.viewDocument(context, latestExportedFile!!, latestExportedMime)
                        showExportSuccessDialog = false
                    },
                    border = BorderStroke(1.dp, DarkOutline),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ဖိုင်ဖွင့်ကြည့်မည်", color = TextSecondary)
                }
            }
        )
    }
}
