package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.model.MonthlySettlement
import com.example.util.DocumentExporter
import java.io.File
import com.example.ui.theme.AYAPayPurple
import com.example.ui.theme.AYAPayPurpleContainer
import com.example.ui.theme.CBPayRed
import com.example.ui.theme.CBPayRedContainer
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
import com.example.ui.theme.PurpleContainerLight
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurplePrimaryDeep
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WavePayYellow
import com.example.ui.theme.WavePayYellowContainer
import com.example.ui.viewmodel.ShopViewModel
import com.example.util.Formatters
import java.util.Calendar

@Composable
fun SettlementScreen(
    viewModel: ShopViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val selectedYear by viewModel.selectedYear.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val monthlyStats by viewModel.monthlyStats.collectAsStateWithLifecycle()
    val isSendingTelegram by viewModel.isSendingTelegram.collectAsStateWithLifecycle()
    val isExporting by viewModel.isExporting.collectAsStateWithLifecycle()

    var showCloseMonthDialog by remember { mutableStateOf(false) }
    var closeNotes by remember { mutableStateOf("") }

    var latestExportedFile by remember { mutableStateOf<File?>(null) }
    var latestExportedMime by remember { mutableStateOf("application/pdf") }
    var showExportSuccessDialog by remember { mutableStateOf(false) }

    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, selectedYear)
        set(Calendar.MONTH, selectedMonth)
    }
    val monthLabel = Formatters.formatMonthYear(cal.timeInMillis)

    val stats = monthlyStats ?: MonthlySettlement(monthKey = Formatters.getMonthKey(selectedYear, selectedMonth))

    fun previousMonth() {
        if (selectedMonth == 0) {
            viewModel.setSelectedMonthAndYear(selectedYear - 1, 11)
        } else {
            viewModel.setSelectedMonthAndYear(selectedYear, selectedMonth - 1)
        }
    }

    fun nextMonth() {
        if (selectedMonth == 11) {
            viewModel.setSelectedMonthAndYear(selectedYear + 1, 0)
        } else {
            viewModel.setSelectedMonthAndYear(selectedYear, selectedMonth + 1)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .testTag("settlement_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Month Selector Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "နောက်သို့", tint = TextPrimary)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = { previousMonth() }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "လ အရင်", tint = PurplePrimary)
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSurfaceVariant,
                        border = BorderStroke(1.dp, DarkOutline.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = monthLabel,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = PurplePrimary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }

                    IconButton(onClick = { nextMonth() }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "လ နောက်", tint = PurplePrimary)
                    }
                }

                Spacer(modifier = Modifier.width(32.dp))
            }
        }

        // Net Profit Hero Card
        item {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                border = BorderStroke(1.dp, DarkOutline.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "လချုပ် အသားတင် အမြတ်/အရှုံး",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = PurplePrimary
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (stats.netProfit >= 0) IncomeContainer else ExpenseContainer
                        ) {
                            Text(
                                text = if (stats.netProfit >= 0) "အမြတ် (PROFIT)" else "အရှုံး (LOSS)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (stats.netProfit >= 0) IncomeColor else ExpenseColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = Formatters.formatNumber(stats.netProfit),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (stats.netProfit >= 0) Color.White else ExpenseColor
                        )
                        Text(
                            text = "ကျပ်",
                            fontSize = 15.sp,
                            color = PurplePrimary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    HorizontalDivider(color = DarkOutline.copy(alpha = 0.4f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("စုစုပေါင်း ဝင်ငွေ (Total Income)", fontSize = 11.sp, color = TextSecondary)
                            Text(
                                "+ ${Formatters.formatKyat(stats.totalIncome)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = IncomeSoft
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("စုစုပေါင်း ထွက်ငွေ (Total Expense)", fontSize = 11.sp, color = TextSecondary)
                            Text(
                                "- ${Formatters.formatKyat(stats.totalExpense)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = ExpenseColor
                            )
                        }
                    }
                }
            }
        }

        // Action Buttons: Send to Telegram & Close Month
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.sendMonthlyStatementToTelegram(selectedYear, selectedMonth) },
                    enabled = !isSendingTelegram,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PurpleContainerLight,
                        contentColor = PurplePrimaryDeep
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("send_monthly_telegram_btn")
                ) {
                    if (isSendingTelegram) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = PurplePrimaryDeep)
                    } else {
                        Text("✈️ Telegram သို့ ပို့မည်", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = { showCloseMonthDialog = true },
                    border = BorderStroke(1.dp, PurplePrimary.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("close_month_btn")
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (stats.isClosed) "လချုပ်ပြီးပါပြီ ✓" else "လချုပ်ပိတ်သိမ်းမည်",
                        color = PurplePrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Export as PDF / PNG Documents Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, PurplePrimary.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(PurpleContainerLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    tint = PurplePrimaryDeep,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "စာရင်းရှင်းတမ်း ထုတ်ယူရန် (Export)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "PDF စာရင်းချုပ်စာရွက် သို့မဟုတ် PNG ရုပ်ပုံကတ်ပြား ထုတ်မည်",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        if (isExporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = PurplePrimary
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Export PDF Button
                        Button(
                            onClick = {
                                viewModel.exportMonthlyReportPdf(selectedYear, selectedMonth) { file ->
                                    if (file != null) {
                                        latestExportedFile = file
                                        latestExportedMime = "application/pdf"
                                        showExportSuccessDialog = true
                                    }
                                }
                            },
                            enabled = !isExporting,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE11D48), // Vibrant PDF Rose/Red
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("export_pdf_btn")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PDF ထုတ်မည်", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Export PNG Button
                        Button(
                            onClick = {
                                viewModel.exportMonthlyReportPng(selectedYear, selectedMonth) { file ->
                                    if (file != null) {
                                        latestExportedFile = file
                                        latestExportedMime = "image/png"
                                        showExportSuccessDialog = true
                                    }
                                }
                            },
                            enabled = !isExporting,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2563EB), // Blue for PNG Image
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("export_png_btn")
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PNG ပုံရိပ်", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Payment Channel Breakdown
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkOutline.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ငွေပေးချေမှု နည်းလမ်းအလိုက် ဝင်ငွေခွဲခြမ်းမှု",
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
                            border = BorderStroke(1.dp, KPayBlue.copy(alpha = 0.3f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("KBZPay", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = KPayBlue)
                                Text(
                                    Formatters.formatKyat(stats.kpayTotal),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = KPayBlue
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = WavePayYellowContainer,
                            border = BorderStroke(1.dp, WavePayYellow.copy(alpha = 0.3f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("WavePay", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WavePayYellow)
                                Text(
                                    Formatters.formatKyat(stats.waveTotal),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WavePayYellow
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF14532D).copy(alpha = 0.3f),
                            border = BorderStroke(1.dp, CashGreen.copy(alpha = 0.3f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("ငွေသား", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CashGreen)
                                Text(
                                    Formatters.formatKyat(stats.cashTotal),
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

        // Inventory Value in Settlement
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkOutline.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "📦 လကုန် လက်ကျန် Stock တန်ဖိုး တွက်ချက်မှု",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("ပစ္စည်းဝယ်ယူစရိတ် စုစုပေါင်း (Stock Purchase):", fontSize = 12.sp, color = TextSecondary)
                        Text(Formatters.formatKyat(stats.totalStockPurchaseCost), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("ဆိုင်လက်ကျန် ဝယ်ရင်းတန်ဖိုး (Cost Value):", fontSize = 12.sp, color = TextSecondary)
                        Text(Formatters.formatKyat(stats.endingStockValueCost), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("ဆိုင်လက်ကျန် ရောင်းဈေးတန်ဖိုး (Retail Value):", fontSize = 12.sp, color = TextSecondary)
                        Text(Formatters.formatKyat(stats.endingStockValueRetail), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = IncomeColor)
                    }
                }
            }
        }
    }

    if (showCloseMonthDialog) {
        AlertDialog(
            onDismissRequest = { showCloseMonthDialog = false },
            containerColor = DarkSurface,
            title = { Text("$monthLabel လချုပ် ပိတ်သိမ်းမည်", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("လချုပ် စာရင်းကို အပြီးသတ် ပိတ်သိမ်း၍ အမြတ်အရှုံး မှတ်တမ်းအဖြစ် သိမ်းဆည်းပါမည်။", color = TextSecondary, fontSize = 13.sp)
                    OutlinedTextField(
                        value = closeNotes,
                        onValueChange = { closeNotes = it },
                        label = { Text("လချုပ် မှတ်ချက် / သုံးသပ်ချက်") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurplePrimary,
                            unfocusedBorderColor = DarkOutline
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.closeMonthSettlement(closeNotes)
                        showCloseMonthDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PurpleContainerLight,
                        contentColor = PurplePrimaryDeep
                    )
                ) {
                    Text("ပိတ်သိမ်း အတည်ပြုမည်", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showCloseMonthDialog = false },
                    border = BorderStroke(1.dp, DarkOutline)
                ) {
                    Text("မလုပ်တော့ပါ", color = TextSecondary)
                }
            }
        )
    }

    if (showExportSuccessDialog && latestExportedFile != null) {
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
                        text = "$monthLabel စာရင်းရှင်းတမ်းဖိုင် (${latestExportedFile!!.name}) ကို အောင်မြင်စွာ ဖန်တီးပြီးပါပြီ။",
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
