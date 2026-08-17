package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.UploadFile
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import com.example.model.TelegramConfig
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkOutline
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.IncomeColor
import com.example.ui.theme.KPayBlue
import com.example.ui.theme.PurpleContainerLight
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurplePrimaryDeep
import com.example.ui.theme.TelegramBrandColor
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WavePayYellow
import com.example.ui.viewmodel.ShopViewModel
import com.example.util.BackupFileSummary
import com.example.util.DatabaseBackupManager
import com.example.util.RestoreMode
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: ShopViewModel
) {
    val context = LocalContext.current
    val telegramConfig by viewModel.telegramConfig.collectAsStateWithLifecycle()
    val isSendingTelegram by viewModel.isSendingTelegram.collectAsStateWithLifecycle()
    val isBackingUp by viewModel.isBackingUp.collectAsStateWithLifecycle()
    val isRestoring by viewModel.isRestoring.collectAsStateWithLifecycle()

    val products by viewModel.products.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val settlements by viewModel.allSettlements.collectAsStateWithLifecycle()

    var shopName by remember(telegramConfig) { mutableStateOf(telegramConfig.shopName) }
    var shopPhone by remember(telegramConfig) { mutableStateOf(telegramConfig.shopPhone) }
    var shopAddress by remember(telegramConfig) { mutableStateOf(telegramConfig.shopAddress) }
    var botToken by remember(telegramConfig) { mutableStateOf(telegramConfig.botToken) }
    var chatId by remember(telegramConfig) { mutableStateOf(telegramConfig.chatId) }

    var testResultMsg by remember { mutableStateOf<String?>(null) }

    // Backup & Restore Dialog States
    var latestBackupFile by remember { mutableStateOf<File?>(null) }
    var showBackupSuccessDialog by remember { mutableStateOf(false) }
    var restorePendingContent by remember { mutableStateOf<String?>(null) }
    var restorePendingSummary by remember { mutableStateOf<BackupFileSummary?>(null) }
    var selectedRestoreMode by remember { mutableStateOf(RestoreMode.REPLACE) }

    // SAF Document Creator (Direct save to local phone storage)
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportBackupToUri(uri) { success ->
                // Handled in ViewModel snackbar
            }
        }
    }

    // SAF File Picker (Select backup file to restore)
    val selectBackupFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val reader = BufferedReader(InputStreamReader(stream))
                    val content = reader.readText()
                    val summary = DatabaseBackupManager.parseBackupSummary(content)
                    if (summary != null) {
                        restorePendingContent = content
                        restorePendingSummary = summary
                    } else {
                        // Invalid file format
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Settings Header
        item {
            Text(
                text = "⚙️ ဆိုင်ပြင်ဆင်ချက်များနှင့် ဒေတာ ထိန်းသိမ်းမှု",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = PurplePrimary
            )
            Text(
                text = "ဒေတာဘေ့စ် Backup၊ Telegram Bot နှင့် ဆိုင်အချက်အလက်များ",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        // Local Database Backup & Restore Card (Prominent & Easy to use)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, PurplePrimary.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth().testTag("backup_restore_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(PurplePrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = PurplePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "ဒေတာဘေ့စ် သိမ်းဆည်း/ပြန်လည်ရယူခြင်း",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "Local Backup & Database Restore",
                                fontSize = 11.sp,
                                color = PurplePrimary
                            )
                        }
                    }

                    Text(
                        text = "ဖုန်းပြောင်းလဲခြင်း သို့မဟုတ် အက်ပ်ဖျက်လိုက်ပါက ဒေတာများ မပျောက်ပျက်စေရန် သင်၏ ကုန်ပစ္စည်းများနှင့် စာရင်းများအားလုံးကို Local Storage သို့ Backup ဖိုင် (.json) အဖြစ် ထုတ်ယူသိမ်းဆည်းနိုင်ပါသည်။",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )

                    // Current database stats badge row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurfaceVariant)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📦 ပစ္စည်း", fontSize = 10.sp, color = TextMuted)
                            Text("${products.size} မျိုး", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🧾 စာရင်း", fontSize = 10.sp, color = TextMuted)
                            Text("${transactions.size} စောင်", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📋 လချုပ်", fontSize = 10.sp, color = TextMuted)
                            Text("${settlements.size} လ", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }

                    HorizontalDivider(color = DarkOutline.copy(alpha = 0.3f), thickness = 0.8.dp)

                    // Action Buttons for Backup & Restore
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Export Backup File Button
                        Button(
                            onClick = {
                                viewModel.exportLocalDatabaseBackup { file ->
                                    if (file != null) {
                                        latestBackupFile = file
                                        showBackupSuccessDialog = true
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PurpleContainerLight,
                                contentColor = PurplePrimaryDeep
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("export_backup_btn")
                        ) {
                            if (isBackingUp) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = PurplePrimaryDeep)
                            } else {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Backup ထုတ်မည် 💾", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Restore Backup File Button
                        OutlinedButton(
                            onClick = {
                                selectBackupFileLauncher.launch("*/*")
                            },
                            border = BorderStroke(1.dp, PurplePrimary.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("restore_backup_btn")
                        ) {
                            if (isRestoring) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = PurplePrimary)
                            } else {
                                Icon(Icons.Default.Restore, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("ဒေတာ ပြန်သွင်းမည် 🔄", fontSize = 12.sp, color = PurplePrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Direct Save to Local Phone Storage option (SAF)
                    Surface(
                        onClick = {
                            val defaultName = "ShopManager_Backup_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.json"
                            createDocumentLauncher.launch(defaultName)
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(0.8.dp, DarkOutline.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth().testTag("save_to_storage_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "ဖုန်းတွင်း Memory / Downloads သို့ တိုက်ရိုက်သိမ်းရန် 📁",
                                fontSize = 11.sp,
                                color = Color(0xFFE2E8F0),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Shop Profile Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkOutline.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Store, contentDescription = null, tint = PurplePrimary)
                        Text("ဆိုင်အချက်အလက် (Shop Info)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                    }

                    OutlinedTextField(
                        value = shopName,
                        onValueChange = { shopName = it },
                        label = { Text("ဆိုင်အမည် (Shop Name)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurplePrimary,
                            unfocusedBorderColor = DarkOutline
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_shop_name")
                    )

                    OutlinedTextField(
                        value = shopPhone,
                        onValueChange = { shopPhone = it },
                        label = { Text("ဖုန်းနံပါတ် (Phone)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurplePrimary,
                            unfocusedBorderColor = DarkOutline
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = shopAddress,
                        onValueChange = { shopAddress = it },
                        label = { Text("လိပ်စာ (Address)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurplePrimary,
                            unfocusedBorderColor = DarkOutline
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Telegram Bot Integration Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkOutline.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(TelegramBrandColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✈️", fontSize = 14.sp)
                        }
                        Text("Telegram Bot ချိတ်ဆက်မှု", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                    }

                    Text(
                        text = "Telegram @BotFather ထံမှ Bot Token နှင့် Group/Channel Chat ID ထည့်သွင်းပါက နေ့စဉ်စာရင်းနှင့် Stock သတိပေးချက်များကို တိုက်ရိုက် ရောက်ရှိပါမည်။",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )

                    OutlinedTextField(
                        value = botToken,
                        onValueChange = { botToken = it },
                        label = { Text("Telegram Bot Token") },
                        placeholder = { Text("e.g. 7123456789:AAHq...", color = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurplePrimary,
                            unfocusedBorderColor = DarkOutline
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_bot_token")
                    )

                    OutlinedTextField(
                        value = chatId,
                        onValueChange = { chatId = it },
                        label = { Text("Telegram Chat ID / Channel ID") },
                        placeholder = { Text("e.g. -1001234567890 သို့မဟုတ် 123456789", color = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurplePrimary,
                            unfocusedBorderColor = DarkOutline
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_chat_id")
                    )
                }
            }
        }

        // Save & Test Telegram Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val config = TelegramConfig(
                            botToken = botToken.trim(),
                            chatId = chatId.trim(),
                            shopName = shopName.trim(),
                            shopPhone = shopPhone.trim(),
                            shopAddress = shopAddress.trim(),
                            currencySymbol = "Ks"
                        )
                        viewModel.updateTelegramConfig(config)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PurpleContainerLight,
                        contentColor = PurplePrimaryDeep
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("save_settings_btn")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ပြင်ဆင်ချက်များ သိမ်းမည်", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        val config = TelegramConfig(
                            botToken = botToken.trim(),
                            chatId = chatId.trim(),
                            shopName = shopName.trim(),
                            shopPhone = shopPhone.trim(),
                            shopAddress = shopAddress.trim(),
                            currencySymbol = "Ks"
                        )
                        viewModel.updateTelegramConfig(config)
                        viewModel.testTelegramConnection(botToken.trim(), chatId.trim()) { success, msg ->
                            testResultMsg = msg
                        }
                    },
                    border = BorderStroke(1.dp, PurplePrimary.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("test_telegram_btn")
                ) {
                    if (isSendingTelegram) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = PurplePrimary)
                    } else {
                        Icon(Icons.Default.Send, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Telegram Test ✈️", color = PurplePrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (testResultMsg != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(testResultMsg!!, color = IncomeColor, fontSize = 12.sp)
            }
        }

        // Sample Data Seeder Card
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
                    Text("နမူနာ စာရင်းများ (Demo Sample Data)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                    Text(
                        "စမ်းသပ်အသုံးပြုရန်အတွက် ကုန်ပစ္စည်းများနှင့် အရောင်း/အဝယ် စာရင်းများကို တစ်ချက်နှိပ်ရုံဖြင့် ထည့်သွင်းပေးပါမည်။",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )

                    Button(
                        onClick = { viewModel.seedSampleData() },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant, contentColor = PurplePrimary),
                        border = BorderStroke(1.dp, PurplePrimary.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("seed_sample_data_btn")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("နမူနာ ပစ္စည်းနှင့် စာရင်းများ ထည့်သွင်းမည် ✨", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Backup Success Dialog
    if (showBackupSuccessDialog && latestBackupFile != null) {
        AlertDialog(
            onDismissRequest = { showBackupSuccessDialog = false },
            containerColor = DarkSurface,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.SdCard, contentDescription = null, tint = PurplePrimary)
                    Text("Backup ဖိုင် ထုတ်ယူပြီးပါပြီ 💾", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "သင်၏ ဆိုင်ဒေတာဘေ့စ် အချက်အလက်များအားလုံးကို Backup ဖိုင်အဖြစ် အောင်မြင်စွာ ဖန်တီးပြီးပါပြီ။",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("📄 ဖိုင်အမည်: ${latestBackupFile!!.name}", fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                            Text("📦 ကုန်ပစ္စည်း: ${products.size} မျိုး", fontSize = 11.sp, color = TextSecondary)
                            Text("🧾 စာရင်းမှတ်တမ်း: ${transactions.size} စောင်", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                    Text(
                        text = "ဤဖိုင်ကို Google Drive၊ Telegram သို့မဟုတ် ဖုန်းသိုလှောင်ခန်းတွင် လုံခြုံစွာ သိမ်းဆည်းထားနိုင်ပါသည်။",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        DatabaseBackupManager.shareBackupFile(context, latestBackupFile!!)
                        showBackupSuccessDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PurpleContainerLight,
                        contentColor = PurplePrimaryDeep
                    )
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ဖိုင်အား ပေးပို့/သိမ်းမည် (Share)", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showBackupSuccessDialog = false },
                    border = BorderStroke(1.dp, DarkOutline)
                ) {
                    Text("ပိတ်မည်", color = TextSecondary)
                }
            }
        )
    }

    // Restore Confirmation Dialog
    if (restorePendingSummary != null && restorePendingContent != null) {
        val summary = restorePendingSummary!!
        AlertDialog(
            onDismissRequest = {
                restorePendingSummary = null
                restorePendingContent = null
            },
            containerColor = DarkSurface,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null, tint = WavePayYellow)
                    Text("ဒေတာ ပြန်လည်သွင်းယူခြင်း (Restore)", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "ရွေးချယ်ထားသော Backup ဖိုင်အတွင်းရှိ အချက်အလက်များ:",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DarkSurfaceVariant,
                        border = BorderStroke(1.dp, DarkOutline.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("🏪 ဆိုင်အမည်: ${summary.shopName}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("📅 Backup ယူခဲ့သည့်အချိန်: ${summary.formattedDate}", fontSize = 11.sp, color = TextSecondary)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = DarkOutline.copy(alpha = 0.4f))
                            Text("📦 ကုန်ပစ္စည်းအရေအတွက်: ${summary.totalProducts} မျိုး", fontSize = 12.sp, color = IncomeColor)
                            Text("🧾 အရောင်း/အဝယ် စာရင်း: ${summary.totalTransactions} စောင်", fontSize = 12.sp, color = IncomeColor)
                            Text("📋 လချုပ် မှတ်တမ်း: ${summary.totalSettlements} လ", fontSize = 12.sp, color = IncomeColor)
                        }
                    }

                    Text("သွင်းယူမည့် ပုံစံရွေးချယ်ပါ (Restore Mode):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)

                    // Option 1: Replace All
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedRestoreMode = RestoreMode.REPLACE }
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedRestoreMode == RestoreMode.REPLACE,
                            onClick = { selectedRestoreMode = RestoreMode.REPLACE },
                            colors = RadioButtonDefaults.colors(selectedColor = PurplePrimary)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text("အကုန် အစားထိုးမည် (Replace All)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("လက်ရှိဒေတာများကို ရှင်းလင်းပြီး Backup အတိုင်း အတိအကျ ပြန်ထားမည်။", fontSize = 10.sp, color = TextMuted)
                        }
                    }

                    // Option 2: Merge
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedRestoreMode = RestoreMode.MERGE }
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedRestoreMode == RestoreMode.MERGE,
                            onClick = { selectedRestoreMode = RestoreMode.MERGE },
                            colors = RadioButtonDefaults.colors(selectedColor = PurplePrimary)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text("ပေါင်းစပ်သွင်းယူမည် (Merge & Keep Current)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("လက်ရှိစာရင်းများကို မဖျက်ဘဲ Backup ထဲမှ ပစ္စည်းများကို ပေါင်းထည့်မည်။", fontSize = 10.sp, color = TextMuted)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val content = restorePendingContent ?: ""
                        val mode = selectedRestoreMode
                        restorePendingContent = null
                        restorePendingSummary = null
                        viewModel.restoreDatabaseFromJson(content, mode) {
                            // Handled by snackbar
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PurpleContainerLight,
                        contentColor = PurplePrimaryDeep
                    )
                ) {
                    Text("အတည်ပြု သွင်းယူမည် (Restore Now)", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        restorePendingContent = null
                        restorePendingSummary = null
                    },
                    border = BorderStroke(1.dp, DarkOutline)
                ) {
                    Text("မလုပ်တော့ပါ", color = TextSecondary)
                }
            }
        )
    }
}

