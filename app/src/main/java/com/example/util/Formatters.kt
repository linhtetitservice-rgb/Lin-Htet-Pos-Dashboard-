package com.example.util

import com.example.model.PaymentMethod
import com.example.model.TransactionType
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Formatters {
    private val currencyFormatter = DecimalFormat("#,###")
    private val decimalFormatter = DecimalFormat("#,##0.00")
    private val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val dateTimeFormatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val monthYearFormatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private val monthKeyFormatter = SimpleDateFormat("yyyy-MM", Locale.getDefault())

    fun formatKyat(amount: Double, includeSuffix: Boolean = true): String {
        val formatted = currencyFormatter.format(amount)
        return if (includeSuffix) "$formatted Ks" else formatted
    }

    fun formatNumber(number: Number): String {
        return currencyFormatter.format(number)
    }

    fun formatDate(millis: Long): String {
        return dateFormatter.format(Date(millis))
    }

    fun formatDateTime(millis: Long): String {
        return dateTimeFormatter.format(Date(millis))
    }

    fun formatTime(millis: Long): String {
        return timeFormatter.format(Date(millis))
    }

    fun formatMonthYear(millis: Long): String {
        return monthYearFormatter.format(Date(millis))
    }

    fun formatMonthKey(millis: Long): String {
        return monthKeyFormatter.format(Date(millis))
    }

    fun getMonthKey(year: Int, monthZeroIndexed: Int): String {
        return String.format(Locale.US, "%04d-%02d", year, monthZeroIndexed + 1)
    }

    fun getStartAndEndOfMonth(year: Int, monthZeroIndexed: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, monthZeroIndexed)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis

        return Pair(start, end)
    }

    fun getStartAndEndOfToday(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis

        return Pair(start, end)
    }

    fun getStartAndEndOfYesterday(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis

        return Pair(start, end)
    }

    fun getStartAndEndOfWeek(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        cal.add(Calendar.DAY_OF_WEEK, 6)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis

        return Pair(start, end)
    }

    fun getStartAndEndOfLastMonth(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -1)
        return getStartAndEndOfMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
    }

    fun isSameDay(millis1: Long, millis2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = millis1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = millis2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun getDayGroupingLabel(millis: Long): String {
        val now = System.currentTimeMillis()
        val (todayStart, todayEnd) = getStartAndEndOfToday()
        val (yesterdayStart, yesterdayEnd) = getStartAndEndOfYesterday()

        return when {
            millis in todayStart..todayEnd -> "ယနေ့ • ${formatDate(millis)}"
            millis in yesterdayStart..yesterdayEnd -> "မနေ့က • ${formatDate(millis)}"
            else -> formatDate(millis)
        }
    }
}
