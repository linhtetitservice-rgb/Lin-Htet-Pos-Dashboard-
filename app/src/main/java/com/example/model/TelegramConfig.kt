package com.example.model

data class TelegramConfig(
    val botToken: String = "",
    val chatId: String = "",
    val shopName: String = "ရွှေမင်းသမီး ကုန်စုံဆိုင်",
    val shopPhone: String = "09-123456789",
    val shopAddress: String = "ရန်ကုန်မြို့",
    val currencySymbol: String = "Ks"
) {
    val isConfigured: Boolean
        get() = botToken.isNotBlank() && chatId.isNotBlank()
}
