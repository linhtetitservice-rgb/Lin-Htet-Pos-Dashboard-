package com.example.model

enum class TransactionType(val myanmarLabel: String, val englishLabel: String) {
    INCOME("ဝင်ငွေ", "Income"),
    EXPENSE("ထွက်ငွေ", "Expense")
}

enum class PaymentMethod(
    val myanmarLabel: String,
    val englishLabel: String,
    val shortCode: String
) {
    KPAY("KBZPay (KPay)", "KPay", "KPAY"),
    WAVE_PAY("WavePay", "WavePay", "WAVE"),
    CASH("ငွေသား", "Cash", "CASH"),
    CB_PAY("CB Pay", "CB Pay", "CBPAY"),
    AYA_PAY("AYA Pay", "AYA Pay", "AYAPAY"),
    OTHER("အခြား", "Other", "OTHER")
}

enum class StockChangeType(val myanmarLabel: String, val englishLabel: String) {
    RESTOCK("Stock ထပ်ဖြည့်ခြင်း", "Restock"),
    SALE("ရောင်းချခြင်း", "Sale"),
    ADJUSTMENT("စာရင်းညှိနှိုင်းခြင်း", "Adjustment")
}

enum class DateRangePreset(val myanmarLabel: String, val englishLabel: String) {
    ALL("အချိန်အားလုံး", "All Time"),
    TODAY("ယနေ့", "Today"),
    YESTERDAY("မနေ့က", "Yesterday"),
    THIS_WEEK("ဒီအပတ်", "This Week"),
    THIS_MONTH("ယခုလ", "This Month"),
    LAST_MONTH("ပြီးခဲ့သည့်လ", "Last Month"),
    CUSTOM("ရက်စွဲရွေးမည် 📅", "Custom Range")
}
