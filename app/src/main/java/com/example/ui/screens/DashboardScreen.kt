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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.util.DocumentExporter
import java.io.File
import java.util.Calendar
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.PaymentMethod
import com.example.model.Product
import com.example.model.TransactionType
import com.example.ui.components.AddEditProductDialog
import com.example.ui.components.AddTransactionDialog
import com.example.ui.components.PaymentBadge
import com.example.ui.components.QuickSaleDialog
import com.example.ui.components.RestockDialog
import com.example.ui.components.StatCard
import com.example.ui.components.StockBadge
import com.example.ui.theme.CashGreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkOutline
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ExpenseColor
import com.example.ui.theme.ExpenseContainer
import com.example.ui.theme.IncomeColor
import com.example.ui.theme.IncomeContainer
import com.example.ui.theme.IncomeSoft
import com.example.ui.theme.KPayBlue
import com.example.ui.theme.KPayBlueContainer
import com.example.ui.theme.PurpleContainer
import com.example.ui.theme.PurpleContainerLight
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurplePrimaryDeep
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningColor
import com.example.ui.theme.WarningContainer
import com.example.ui.theme.WavePayYellow
import com.example.ui.theme.WavePayYellowContainer
import com.example.ui.viewmodel.ShopViewModel
import com.example.util.Formatters

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: ShopViewModel,
    onNavigateToInventory: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToSettlement: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val dashboardSummary by viewModel.dashboardSummary.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val lowStockProducts by viewModel.lowStockProducts.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val telegramConfig by viewModel.telegramConfig.collectAsStateWithLifecycle()
    val isExporting by viewModel.isExporting.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var showQuickSaleDialog by remember { mutableStateOf(false) }
    var showRestockDialog by remember { mutableStateOf(false) }
    var restockTargetProduct by remember { mutableStateOf<Product?>(null) }
    var showAddTxDialog by remember { mutableStateOf(false) }
    var showAddProductDialog by remember { mutableStateOf(false) }

    var latestExportedFile by remember { mutableStateOf<File?>(null) }
    var latestExportedMime by remember { mutableStateOf("application/pdf") }
    var showExportSuccessDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = telegramConfig.shopName.ifBlank { "ဆိုင်စာရင်း" },
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = PurplePrimary,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = Formatters.formatDate(System.currentTimeMillis()),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = DarkOutline.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, DarkOutline),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "ADMIN",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = PurplePrimary
                        )
                    }
                }
            }
        }

        // Sophisticated Dark Hero Card (Net Profit / Monthly Totals)
        item {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                border = BorderStroke(1.dp, DarkOutline.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "စုစုပေါင်း အသားတင်အမြတ်",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = PurplePrimary
                        )

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = DarkOutline.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = "ယခုလ (THIS MONTH)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PurpleContainerLight,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Main Profit Display
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = Formatters.formatNumber(dashboardSummary.monthNetProfit),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "ကျပ်",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                            color = PurplePrimary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    HorizontalDivider(color = DarkOutline.copy(alpha = 0.4f), thickness = 1.dp)

                    // Income & Expense Breakdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ဝင်ငွေ (INCOME)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "+ ${Formatters.formatKyat(dashboardSummary.monthIncome)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = IncomeSoft
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ထွက်ငွေ (EXPENSE)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "- ${Formatters.formatKyat(dashboardSummary.monthExpense)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ExpenseColor
                            )
                        }
                    }

                    HorizontalDivider(color = DarkOutline.copy(alpha = 0.3f), thickness = 0.8.dp)

                    // Quick PDF & PNG Export and Settlement Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = {
                                val nowCal = Calendar.getInstance()
                                viewModel.exportMonthlyReportPdf(nowCal.get(Calendar.YEAR), nowCal.get(Calendar.MONTH)) { file ->
                                    if (file != null) {
                                        latestExportedFile = file
                                        latestExportedMime = "application/pdf"
                                        showExportSuccessDialog = true
                                    }
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFE11D48).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFFE11D48).copy(alpha = 0.4f)),
                            modifier = Modifier.weight(1f).testTag("quick_export_pdf_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFFB7185), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("PDF ထုတ်မည်", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFB7185))
                            }
                        }

                        Surface(
                            onClick = {
                                val nowCal = Calendar.getInstance()
                                viewModel.exportMonthlyReportPng(nowCal.get(Calendar.YEAR), nowCal.get(Calendar.MONTH)) { file ->
                                    if (file != null) {
                                        latestExportedFile = file
                                        latestExportedMime = "image/png"
                                        showExportSuccessDialog = true
                                    }
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF2563EB).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFF2563EB).copy(alpha = 0.4f)),
                            modifier = Modifier.weight(1f).testTag("quick_export_png_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("PNG ပုံရိပ်", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF60A5FA))
                            }
                        }

                        Surface(
                            onClick = onNavigateToSettlement,
                            shape = RoundedCornerShape(10.dp),
                            color = PurplePrimary.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, PurplePrimary.copy(alpha = 0.4f)),
                            modifier = Modifier.weight(1.1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("လချုပ်အပြည့်အစုံ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PurplePrimary)
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(13.dp))
                            }
                        }
                    }
                }
            }
        }

        // Today's Overview Grid
        item {
            Text(
                text = "ယနေ့ စာရင်း အကျဉ်းချုပ် (Today's Stats)",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "ယနေ့ ဝင်ငွေ",
                    value = Formatters.formatKyat(dashboardSummary.todayIncome),
                    subtitle = "အရောင်းရငွေ",
                    icon = Icons.Default.TrendingUp,
                    containerColor = DarkSurface,
                    contentColor = IncomeColor,
                    modifier = Modifier.weight(1f),
                    tag = "stat_today_income"
                )

                StatCard(
                    title = "ယနေ့ ထွက်ငွေ",
                    value = Formatters.formatKyat(dashboardSummary.todayExpense),
                    subtitle = "ကုန်ကျစရိတ်",
                    icon = Icons.Default.TrendingDown,
                    containerColor = DarkSurface,
                    contentColor = ExpenseColor,
                    modifier = Modifier.weight(1f),
                    tag = "stat_today_expense"
                )
            }
        }

        // Quick 2-Column Action Tiles
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Stock Restock Tile
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, DarkOutline.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            if (products.isNotEmpty()) {
                                restockTargetProduct = products.first()
                                showRestockDialog = true
                            } else {
                                showAddProductDialog = true
                            }
                        }
                        .testTag("action_restock")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(PurplePrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📦", fontSize = 16.sp)
                        }
                        Text("Stock ဖြည့်ရန်", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(
                            text = if (lowStockProducts.isNotEmpty()) "${lowStockProducts.size} မျိုး ကျန်ရှိ" else "ပစ္စည်း ${products.size} မျိုး",
                            fontSize = 11.sp,
                            color = if (lowStockProducts.isNotEmpty()) WarningColor else TextSecondary
                        )
                    }
                }

                // Monthly Settlement Tile
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, DarkOutline.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onNavigateToSettlement)
                        .testTag("action_monthly_settlement")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(PurplePrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📊", fontSize = 16.sp)
                        }
                        Text("လချုပ်ရှင်းတမ်း", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("အမြတ်နှင့် ကုန်ကျစရိတ်", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }
        }

        // Additional Quick Actions (Sale & Expense)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { showQuickSaleDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = IncomeColor, contentColor = Color(0xFF14532D)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_quick_sale")
                ) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("⚡ ရောင်းချမည်", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { showAddTxDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseContainer, contentColor = ExpenseColor),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, ExpenseColor.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_add_expense")
                ) {
                    Icon(Icons.Default.TrendingDown, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("➖ စရိတ်မှတ်မည်", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Payments Split Today (KPay, Wave, Cash)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkOutline.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ယနေ့ ငွေလက်ခံရရှိမှု နည်းလမ်းများ",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = KPayBlueContainer,
                            border = BorderStroke(1.dp, KPayBlue.copy(alpha = 0.25f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("KBZPay", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = KPayBlue)
                                Text(
                                    Formatters.formatKyat(dashboardSummary.todayKpayTotal),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = KPayBlue
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = WavePayYellowContainer,
                            border = BorderStroke(1.dp, WavePayYellow.copy(alpha = 0.25f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("WavePay", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WavePayYellow)
                                Text(
                                    Formatters.formatKyat(dashboardSummary.todayWaveTotal),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WavePayYellow
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF14532D).copy(alpha = 0.3f),
                            border = BorderStroke(1.dp, CashGreen.copy(alpha = 0.25f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("ငွေသား (Cash)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CashGreen)
                                Text(
                                    Formatters.formatKyat(dashboardSummary.todayCashTotal),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CashGreen
                                )
                            }
                        }
                    }
                }
            }
        }

        // Low Stock Alert (if any)
        if (lowStockProducts.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = WarningContainer),
                    border = BorderStroke(1.dp, WarningColor.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = WarningColor, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "Stock ကုန်ခါနီး (${lowStockProducts.size} မျိုး)",
                                    fontWeight = FontWeight.Bold,
                                    color = WarningColor,
                                    fontSize = 13.sp
                                )
                            }
                            TextButton(onClick = { viewModel.sendLowStockAlertToTelegram() }) {
                                Text("Telegram ပို့ ✈️", fontSize = 11.sp, color = PurplePrimary)
                            }
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(top = 4.dp)
                        ) {
                            items(lowStockProducts) { product ->
                                Surface(
                                    color = DarkSurface,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, DarkOutline),
                                    modifier = Modifier.width(160.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = product.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "ကျန်: ${product.stockQuantity} ${product.unit}",
                                            fontSize = 11.sp,
                                            color = WarningColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Recent Transactions Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "နောက်ဆုံး ငွေပေးချေမှုနှင့် မှတ်တမ်းများ",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                TextButton(onClick = onNavigateToTransactions) {
                    Text("အားလုံးကြည့်မည်", fontSize = 12.sp, color = PurplePrimary)
                }
            }

            if (transactions.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, DarkOutline.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("စာရင်းမှတ်တမ်းများ မရှိသေးပါ။", color = TextSecondary, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { viewModel.seedSampleData() },
                            colors = ButtonDefaults.buttonColors(containerColor = PurpleContainerLight, contentColor = PurplePrimaryDeep)
                        ) {
                            Text("နမူနာ စာရင်းများ ထည့်မည် ✨", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        items(transactions.take(4)) { tx ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkOutline.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        val isIncome = tx.type == TransactionType.INCOME
                        val (iconBg, iconColor, iconText) = when (tx.paymentMethod) {
                            PaymentMethod.KPAY -> Triple(KPayBlueContainer, KPayBlue, "KBZ")
                            PaymentMethod.WAVE_PAY -> Triple(WavePayYellowContainer, WavePayYellow, "WAVE")
                            else -> Triple(
                                if (isIncome) IncomeContainer else ExpenseContainer,
                                if (isIncome) IncomeColor else ExpenseColor,
                                if (isIncome) "➕" else "➖"
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(iconBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = iconText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = iconColor
                            )
                        }

                        Column {
                            Text(
                                text = tx.category,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "${Formatters.formatDate(tx.dateMillis)} • ${tx.paymentMethod.myanmarLabel}",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Text(
                        text = "${if (tx.type == TransactionType.INCOME) "+ " else "- "}${Formatters.formatKyat(tx.amount)}",
                        fontWeight = FontWeight.Bold,
                        color = if (tx.type == TransactionType.INCOME) PurplePrimary else ExpenseColor,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Prominent Telegram Dispatch Button at Bottom (Sophisticated Theme Style)
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = { viewModel.sendTodaySummaryToTelegram() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PurpleContainerLight,
                    contentColor = PurplePrimaryDeep
                ),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("action_telegram_send_bottom")
            ) {
                Text("✈️  Telegram သို့ နေ့စဉ်စာရင်း ပို့ရန်", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    // Dialog handlers
    if (showQuickSaleDialog) {
        QuickSaleDialog(
            products = products,
            onDismiss = { showQuickSaleDialog = false },
            onConfirm = { productId, qty, price, method, note ->
                viewModel.quickSale(productId, qty, price, method, note)
                showQuickSaleDialog = false
            }
        )
    }

    if (showRestockDialog && restockTargetProduct != null) {
        RestockDialog(
            product = restockTargetProduct!!,
            onDismiss = {
                showRestockDialog = false
                restockTargetProduct = null
            },
            onConfirm = { addQty, unitCost, method, recordExpense, note ->
                viewModel.restockProduct(
                    productId = restockTargetProduct!!.id,
                    addQuantity = addQty,
                    costPerUnit = unitCost,
                    paymentMethod = method,
                    recordAsExpense = recordExpense,
                    note = note
                )
                showRestockDialog = false
                restockTargetProduct = null
            }
        )
    }

    if (showAddTxDialog) {
        AddTransactionDialog(
            onDismiss = { showAddTxDialog = false },
            onConfirm = { type, cat, amt, method, note ->
                viewModel.addTransaction(type, cat, amt, method, note)
                showAddTxDialog = false
            }
        )
    }

    if (showAddProductDialog) {
        AddEditProductDialog(
            onDismiss = { showAddProductDialog = false },
            onConfirm = { newProduct ->
                viewModel.addProduct(newProduct)
                showAddProductDialog = false
            }
        )
    }

    if (showExportSuccessDialog && latestExportedFile != null) {
        val nowCal = Calendar.getInstance()
        val monthLabel = Formatters.formatMonthYear(nowCal.timeInMillis)

        AlertDialog(
            onDismissRequest = { showExportSuccessDialog = false },
            containerColor = DarkSurface,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (latestExportedMime == "application/pdf") Icons.Default.PictureAsPdf else Icons.Default.Image,
                        contentDescription = null,
                        tint = if (latestExportedMime == "application/pdf") Color(0xFFE11D48) else Color(0xFF2563EB)
                    )
                    Text(
                        text = if (latestExportedMime == "application/pdf") "PDF စာရင်း ထုတ်ယူပြီးပါပြီ" else "PNG ပုံရိပ် ထုတ်ယူပြီးပါပြီ",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "$monthLabel စာရင်းရှင်းတမ်းဖိုင် (${latestExportedFile!!.name}) ကို အောင်မြင်စွာ ထုတ်ယူပြီးပါပြီ။",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "အခြားအက်ပ်များ (Telegram, Viber, Gmail) သို့ ပေးပို့နိုင်သလို တိုက်ရိုက် ကြည့်ရှုနိုင်ပါသည်။",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        DocumentExporter.shareDocument(
                            context = context,
                            file = latestExportedFile!!,
                            mimeType = latestExportedMime,
                            title = "$monthLabel စာရင်းရှင်းတမ်း"
                        )
                        showExportSuccessDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PurpleContainerLight,
                        contentColor = PurplePrimaryDeep
                    )
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("အခြားအက်ပ်သို့ ပို့မည် (Share)", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        DocumentExporter.viewDocument(
                            context = context,
                            file = latestExportedFile!!,
                            mimeType = latestExportedMime
                        )
                        showExportSuccessDialog = false
                    },
                    border = BorderStroke(1.dp, PurplePrimary.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ကြည့်ရှုမည် (Open)", color = PurplePrimary)
                }
            }
        )
    }
}
