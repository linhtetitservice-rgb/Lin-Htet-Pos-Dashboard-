package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.model.MonthlySettlement
import com.example.model.TelegramConfig
import com.example.model.TransactionRecord
import com.example.model.TransactionType
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DocumentExporter {

    private const val PDF_PAGE_WIDTH = 595 // A4 standard width at 72dpi
    private const val PDF_PAGE_HEIGHT = 842 // A4 standard height at 72dpi

    data class ExportResult(
        val file: File,
        val mimeType: String,
        val filename: String
    )

    /**
     * Generate a multi-page PDF document for monthly settlement & transactions
     */
    fun generateMonthlyReportPdf(
        context: Context,
        monthLabel: String,
        settlement: MonthlySettlement,
        transactions: List<TransactionRecord>,
        config: TelegramConfig
    ): File {
        val pdfDocument = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PDF_PAGE_WIDTH, PDF_PAGE_HEIGHT, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val margin = 36f
        val contentWidth = PDF_PAGE_WIDTH - (margin * 2)
        var currentY = margin

        // Helper to start a new page
        fun checkAndStartNewPage(requiredHeight: Float) {
            if (currentY + requiredHeight > PDF_PAGE_HEIGHT - margin - 30f) {
                // Draw footer for current page
                drawPageFooter(canvas, pageNumber, config.shopName)
                pdfDocument.finishPage(page)

                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PDF_PAGE_WIDTH, PDF_PAGE_HEIGHT, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                currentY = margin

                // Draw compact header for subsequent pages
                drawSubsequentPageHeader(canvas, monthLabel, config.shopName, margin, contentWidth)
                currentY += 45f
            }
        }

        // 1. Draw Brand Header
        currentY = drawReportHeader(canvas, monthLabel, config, margin, contentWidth, currentY)
        currentY += 15f

        // 2. Draw Financial Overview Cards
        currentY = drawFinancialSummaryCards(canvas, settlement, margin, contentWidth, currentY)
        currentY += 15f

        // 3. Draw Payment Channels & Inventory Valuation Box
        currentY = drawPaymentAndStockSection(canvas, settlement, margin, contentWidth, currentY)
        currentY += 20f

        // 4. Draw Transactions Section Title
        paint.color = Color.rgb(20, 20, 35)
        textPaint.color = Color.rgb(30, 30, 45)
        textPaint.textSize = 13f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Monthly Transactions (${transactions.size} Records) / လစဥ် အရောင်းအဝယ် စာရင်းများ", margin, currentY, textPaint)
        currentY += 10f

        // 5. Draw Transactions Table Header
        currentY = drawTableHeader(canvas, margin, contentWidth, currentY)

        // 6. Draw Table Rows
        val sortedTransactions = transactions.sortedByDescending { it.dateMillis }
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        for ((index, tx) in sortedTransactions.withIndex()) {
            checkAndStartNewPage(24f)

            val rowBgColor = if (index % 2 == 0) Color.rgb(248, 249, 252) else Color.WHITE
            paint.color = rowBgColor
            canvas.drawRect(margin, currentY, margin + contentWidth, currentY + 20f, paint)

            val isIncome = tx.type == TransactionType.INCOME
            val amtColor = if (isIncome) Color.rgb(22, 101, 52) else Color.rgb(185, 28, 28)
            val typePrefix = if (isIncome) "+ " else "- "

            textPaint.textSize = 8.5f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textPaint.color = Color.rgb(80, 80, 90)

            // Col 1: Date & Time
            val dateStr = dateFormat.format(Date(tx.dateMillis))
            canvas.drawText(dateStr, margin + 4f, currentY + 13f, textPaint)

            // Col 2: Category & Note
            textPaint.color = Color.rgb(20, 20, 30)
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val desc = if (tx.note.isNotBlank()) "${tx.category} (${tx.note})" else tx.category
            val trimmedDesc = if (desc.length > 28) desc.take(26) + "..." else desc
            canvas.drawText(trimmedDesc, margin + 105f, currentY + 13f, textPaint)

            // Col 3: Channel
            textPaint.color = Color.rgb(70, 70, 85)
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(tx.paymentMethod.name, margin + 275f, currentY + 13f, textPaint)

            // Col 4: Type
            val typeText = if (isIncome) "INCOME" else "EXPENSE"
            textPaint.color = amtColor
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(typeText, margin + 355f, currentY + 13f, textPaint)

            // Col 5: Amount (MMK)
            val amountStr = "$typePrefix${Formatters.formatNumber(tx.amount)} Ks"
            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(amountStr, margin + contentWidth - 6f, currentY + 13f, textPaint)
            textPaint.textAlign = Paint.Align.LEFT

            // Row Bottom divider line
            paint.color = Color.rgb(230, 232, 240)
            paint.strokeWidth = 0.5f
            canvas.drawLine(margin, currentY + 20f, margin + contentWidth, currentY + 20f, paint)

            currentY += 20f
        }

        // Check space for signature
        checkAndStartNewPage(80f)
        currentY += 20f

        // Draw Signatures
        drawSignatureBlock(canvas, margin, contentWidth, currentY)

        // Draw footer on last page
        drawPageFooter(canvas, pageNumber, config.shopName)
        pdfDocument.finishPage(page)

        // Write to cache file
        val reportsDir = File(context.cacheDir, "reports").apply { if (!exists()) mkdirs() }
        val filename = "Statement_${monthLabel.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"
        val pdfFile = File(reportsDir, filename)

        FileOutputStream(pdfFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return pdfFile
    }

    /**
     * Generate a crisp high-res PNG image summary (Card style) for instant sharing to chat/social
     */
    fun generateMonthlyReportPng(
        context: Context,
        monthLabel: String,
        settlement: MonthlySettlement,
        transactions: List<TransactionRecord>,
        config: TelegramConfig
    ): File {
        val width = 1080
        val topTransactions = transactions.sortedByDescending { it.dateMillis }.take(8)
        val height = 1550 + (topTransactions.size * 55)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Background
        paint.color = Color.rgb(15, 17, 28) // Deep Dark Theme
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        var y = 60f
        val margin = 50f
        val cardWidth = width - (margin * 2)

        // Header Background Banner
        val headerRect = RectF(margin, y, margin + cardWidth, y + 170f)
        paint.color = Color.rgb(30, 26, 60)
        canvas.drawRoundRect(headerRect, 30f, 30f, paint)

        // Header Border
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = Color.rgb(100, 75, 180)
        canvas.drawRoundRect(headerRect, 30f, 30f, paint)
        paint.style = Paint.Style.FILL

        // Shop Name
        textPaint.color = Color.rgb(255, 255, 255)
        textPaint.textSize = 34f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(config.shopName.ifBlank { "ဆိုင်စာရင်း" }, margin + 30f, y + 55f, textPaint)

        // Subtitle
        textPaint.color = Color.rgb(180, 160, 240)
        textPaint.textSize = 24f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Monthly Financial Statement / လချုပ် စာရင်းချုပ်", margin + 30f, y + 95f, textPaint)

        // Period & Date
        textPaint.color = Color.rgb(200, 200, 220)
        textPaint.textSize = 20f
        val genDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        canvas.drawText("ကာလ: $monthLabel  •  ထုတ်ယူချိန်: $genDate", margin + 30f, y + 135f, textPaint)

        y += 200f

        // Hero Net Profit Card
        val isProfitable = settlement.netProfit >= 0
        val profitCardBg = if (isProfitable) Color.rgb(18, 45, 30) else Color.rgb(55, 20, 25)
        val profitBorderColor = if (isProfitable) Color.rgb(34, 197, 94) else Color.rgb(239, 68, 68)

        val profitCardRect = RectF(margin, y, margin + cardWidth, y + 230f)
        paint.color = profitCardBg
        canvas.drawRoundRect(profitCardRect, 30f, 30f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.color = profitBorderColor
        canvas.drawRoundRect(profitCardRect, 30f, 30f, paint)
        paint.style = Paint.Style.FILL

        textPaint.color = if (isProfitable) Color.rgb(74, 222, 128) else Color.rgb(248, 113, 113)
        textPaint.textSize = 24f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val profitTitle = if (isProfitable) "အသားတင် အမြတ် (NET PROFIT)" else "အသားတင် အရှုံး (NET LOSS)"
        canvas.drawText(profitTitle, margin + 30f, y + 55f, textPaint)

        // Profit Big Value
        textPaint.color = Color.WHITE
        textPaint.textSize = 58f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val profitValStr = "${Formatters.formatNumber(settlement.netProfit)} MMK"
        canvas.drawText(profitValStr, margin + 30f, y + 125f, textPaint)

        // Income vs Expense Subrow
        textPaint.textSize = 22f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.color = Color.rgb(180, 220, 190)
        canvas.drawText("ဝင်ငွေ: +${Formatters.formatKyat(settlement.totalIncome)}", margin + 30f, y + 185f, textPaint)

        textPaint.color = Color.rgb(255, 180, 180)
        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("ထွက်ငွေ: -${Formatters.formatKyat(settlement.totalExpense)}", margin + cardWidth - 30f, y + 185f, textPaint)
        textPaint.textAlign = Paint.Align.LEFT

        y += 260f

        // 3-Column Payment Badges (KPay, Wave, Cash)
        val colWidth = (cardWidth - 40f) / 3f

        // KPay Box
        drawStatBoxPng(
            canvas, margin, y, colWidth, 120f,
            "KBZPay", Formatters.formatKyat(settlement.kpayTotal),
            Color.rgb(15, 30, 60), Color.rgb(59, 130, 246)
        )

        // WavePay Box
        drawStatBoxPng(
            canvas, margin + colWidth + 20f, y, colWidth, 120f,
            "WavePay", Formatters.formatKyat(settlement.waveTotal),
            Color.rgb(60, 45, 10), Color.rgb(234, 179, 8)
        )

        // Cash Box
        drawStatBoxPng(
            canvas, margin + (colWidth + 20f) * 2, y, colWidth, 120f,
            "ငွေသား (Cash)", Formatters.formatKyat(settlement.cashTotal),
            Color.rgb(20, 45, 30), Color.rgb(34, 197, 94)
        )

        y += 150f

        // Inventory Stock Valuation Card
        val stockCardRect = RectF(margin, y, margin + cardWidth, y + 180f)
        paint.color = Color.rgb(25, 28, 45)
        canvas.drawRoundRect(stockCardRect, 24f, 24f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        paint.color = Color.rgb(70, 75, 110)
        canvas.drawRoundRect(stockCardRect, 24f, 24f, paint)
        paint.style = Paint.Style.FILL

        textPaint.color = Color.rgb(220, 220, 240)
        textPaint.textSize = 24f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("📦 လကုန် ဆိုင်လက်ကျန် ကုန်ပစ္စည်းတန်ဖိုး", margin + 25f, y + 45f, textPaint)

        textPaint.textSize = 20f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.color = Color.rgb(160, 165, 190)
        canvas.drawText("ကုန်ပစ္စည်းဝယ်ယူစရိတ်:", margin + 25f, y + 90f, textPaint)
        textPaint.textAlign = Paint.Align.RIGHT
        textPaint.color = Color.WHITE
        canvas.drawText(Formatters.formatKyat(settlement.totalStockPurchaseCost), margin + cardWidth - 25f, y + 90f, textPaint)
        textPaint.textAlign = Paint.Align.LEFT

        textPaint.color = Color.rgb(160, 165, 190)
        canvas.drawText("လက်ကျန် Stock ဝယ်ရင်းတန်ဖိုး:", margin + 25f, y + 130f, textPaint)
        textPaint.textAlign = Paint.Align.RIGHT
        textPaint.color = Color.rgb(100, 200, 255)
        canvas.drawText(Formatters.formatKyat(settlement.endingStockValueCost), margin + cardWidth - 25f, y + 130f, textPaint)
        textPaint.textAlign = Paint.Align.LEFT

        textPaint.color = Color.rgb(160, 165, 190)
        canvas.drawText("လက်ကျန် Stock ရောင်းဈေးတန်ဖိုး:", margin + 25f, y + 165f, textPaint)
        textPaint.textAlign = Paint.Align.RIGHT
        textPaint.color = Color.rgb(74, 222, 128)
        canvas.drawText(Formatters.formatKyat(settlement.endingStockValueRetail), margin + cardWidth - 25f, y + 165f, textPaint)
        textPaint.textAlign = Paint.Align.LEFT

        y += 210f

        // Recent Transactions Header
        textPaint.color = Color.WHITE
        textPaint.textSize = 26f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("လတ်တလော အရောင်းအဝယ် စာရင်းများ (${transactions.size} ခု အနက် နမူနာ)", margin, y + 10f, textPaint)
        y += 35f

        val dateFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())

        for (tx in topTransactions) {
            val txRect = RectF(margin, y, margin + cardWidth, y + 48f)
            paint.color = Color.rgb(25, 28, 42)
            canvas.drawRoundRect(txRect, 14f, 14f, paint)

            val isIncome = tx.type == TransactionType.INCOME
            val tagColor = if (isIncome) Color.rgb(34, 197, 94) else Color.rgb(239, 68, 68)

            // Small indicator bar on left
            paint.color = tagColor
            canvas.drawRoundRect(RectF(margin, y, margin + 8f, y + 48f), 6f, 6f, paint)

            textPaint.color = Color.rgb(180, 180, 200)
            textPaint.textSize = 18f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(dateFormat.format(Date(tx.dateMillis)), margin + 20f, y + 32f, textPaint)

            textPaint.color = Color.WHITE
            textPaint.textSize = 19f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val desc = if (tx.note.isNotBlank()) "${tx.category} - ${tx.note}" else tx.category
            val trimmedDesc = if (desc.length > 25) desc.take(23) + "..." else desc
            canvas.drawText(trimmedDesc, margin + 170f, y + 32f, textPaint)

            // Method tag
            textPaint.color = Color.rgb(140, 145, 175)
            textPaint.textSize = 16f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(tx.paymentMethod.name, margin + 540f, y + 32f, textPaint)

            // Amount
            textPaint.textAlign = Paint.Align.RIGHT
            textPaint.color = tagColor
            textPaint.textSize = 20f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val amt = "${if (isIncome) "+" else "-"}${Formatters.formatNumber(tx.amount)} Ks"
            canvas.drawText(amt, margin + cardWidth - 20f, y + 32f, textPaint)
            textPaint.textAlign = Paint.Align.LEFT

            y += 56f
        }

        // Footer & Signature notice
        y += 20f
        textPaint.color = Color.rgb(130, 135, 160)
        textPaint.textSize = 18f
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("Generated by ဆိုင်စာရင်း (Shop Manager) • Official Digital Statement", width / 2f, y + 30f, textPaint)
        textPaint.textAlign = Paint.Align.LEFT

        // Save Bitmap to PNG
        val reportsDir = File(context.cacheDir, "reports").apply { if (!exists()) mkdirs() }
        val filename = "Statement_${monthLabel.replace(" ", "_")}_${System.currentTimeMillis()}.png"
        val pngFile = File(reportsDir, filename)

        try {
            FileOutputStream(pngFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        } finally {
            bitmap.recycle()
        }

        return pngFile
    }

    // Helper functions for PDF drawing
    private fun drawReportHeader(
        canvas: Canvas,
        monthLabel: String,
        config: TelegramConfig,
        margin: Float,
        contentWidth: Float,
        startY: Float
    ): Float {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Top Accent Banner
        paint.color = Color.rgb(88, 28, 135) // Deep Purple Primary
        canvas.drawRoundRect(RectF(margin, startY, margin + contentWidth, startY + 68f), 8f, 8f, paint)

        textPaint.color = Color.WHITE
        textPaint.textSize = 16f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val shopTitle = config.shopName.ifBlank { "ဆိုင်စာရင်း (Shop Manager)" }
        canvas.drawText(shopTitle, margin + 14f, startY + 26f, textPaint)

        textPaint.textSize = 10f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.color = Color.rgb(230, 220, 255)
        val contact = listOfNotNull(
            if (config.shopPhone.isNotBlank()) "Ph: ${config.shopPhone}" else null,
            if (config.shopAddress.isNotBlank()) config.shopAddress else null
        ).joinToString(" | ")
        if (contact.isNotBlank()) {
            canvas.drawText(contact, margin + 14f, startY + 42f, textPaint)
        }

        val genDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        canvas.drawText("Monthly Financial Settlement Report (လချုပ် အစီရင်ခံစာ) • Generated: $genDate", margin + 14f, startY + 58f, textPaint)

        // Statement Period Box on Right
        textPaint.textAlign = Paint.Align.RIGHT
        textPaint.textSize = 14f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.color = Color.rgb(255, 235, 100)
        canvas.drawText(monthLabel, margin + contentWidth - 14f, startY + 36f, textPaint)
        textPaint.textAlign = Paint.Align.LEFT

        return startY + 68f
    }

    private fun drawFinancialSummaryCards(
        canvas: Canvas,
        settlement: MonthlySettlement,
        margin: Float,
        contentWidth: Float,
        startY: Float
    ): Float {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val cardWidth = (contentWidth - 16f) / 3f
        val cardHeight = 62f

        // Card 1: Total Income
        drawStatCardPdf(
            canvas, margin, startY, cardWidth, cardHeight,
            "Total Revenue (ဝင်ငွေ)", "+${Formatters.formatKyat(settlement.totalIncome)}",
            Color.rgb(240, 253, 244), Color.rgb(22, 101, 52), Color.rgb(187, 247, 208)
        )

        // Card 2: Total Expense
        drawStatCardPdf(
            canvas, margin + cardWidth + 8f, startY, cardWidth, cardHeight,
            "Total Expenses (ထွက်ငွေ)", "-${Formatters.formatKyat(settlement.totalExpense)}",
            Color.rgb(254, 242, 242), Color.rgb(185, 28, 28), Color.rgb(254, 202, 202)
        )

        // Card 3: Net Profit / Loss
        val isProfitable = settlement.netProfit >= 0
        val profitTitle = if (isProfitable) "Net Profit (အမြတ်)" else "Net Loss (အရှုံး)"
        val profitVal = Formatters.formatKyat(settlement.netProfit)
        val profitBg = if (isProfitable) Color.rgb(236, 253, 245) else Color.rgb(255, 241, 242)
        val profitColor = if (isProfitable) Color.rgb(6, 95, 70) else Color.rgb(159, 18, 57)
        val profitBorder = if (isProfitable) Color.rgb(167, 243, 208) else Color.rgb(254, 205, 211)

        drawStatCardPdf(
            canvas, margin + (cardWidth + 8f) * 2, startY, cardWidth, cardHeight,
            profitTitle, profitVal, profitBg, profitColor, profitBorder
        )

        return startY + cardHeight
    }

    private fun drawStatCardPdf(
        canvas: Canvas,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        title: String,
        value: String,
        bgColor: Int,
        textColor: Int,
        borderColor: Int
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.color = bgColor
        canvas.drawRoundRect(RectF(x, y, x + w, y + h), 6f, 6f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = borderColor
        canvas.drawRoundRect(RectF(x, y, x + w, y + h), 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        textPaint.color = Color.rgb(75, 85, 99)
        textPaint.textSize = 8.5f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(title, x + 8f, y + 18f, textPaint)

        textPaint.color = textColor
        textPaint.textSize = 12f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(value, x + 8f, y + 42f, textPaint)
    }

    private fun drawPaymentAndStockSection(
        canvas: Canvas,
        settlement: MonthlySettlement,
        margin: Float,
        contentWidth: Float,
        startY: Float
    ): Float {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val boxWidth = (contentWidth - 10f) / 2f
        val boxHeight = 76f

        // Left: Payment Channels Breakdown
        paint.color = Color.rgb(248, 250, 252)
        canvas.drawRoundRect(RectF(margin, startY, margin + boxWidth, startY + boxHeight), 6f, 6f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.8f
        paint.color = Color.rgb(226, 232, 240)
        canvas.drawRoundRect(RectF(margin, startY, margin + boxWidth, startY + boxHeight), 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        textPaint.color = Color.rgb(30, 41, 59)
        textPaint.textSize = 9.5f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Payment Methods / ငွေပေးချေမှု လမ်းကြောင်းများ", margin + 8f, startY + 16f, textPaint)

        textPaint.textSize = 8.5f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.color = Color.rgb(71, 85, 105)

        canvas.drawText("• KBZPay (KPay): ${Formatters.formatKyat(settlement.kpayTotal)}", margin + 8f, startY + 34f, textPaint)
        canvas.drawText("• WavePay: ${Formatters.formatKyat(settlement.waveTotal)}", margin + 8f, startY + 48f, textPaint)
        canvas.drawText("• Cash (ငွေသား): ${Formatters.formatKyat(settlement.cashTotal)}", margin + 8f, startY + 62f, textPaint)

        // Right: Inventory Valuation
        val rightX = margin + boxWidth + 10f
        paint.color = Color.rgb(248, 250, 252)
        canvas.drawRoundRect(RectF(rightX, startY, rightX + boxWidth, startY + boxHeight), 6f, 6f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.8f
        paint.color = Color.rgb(226, 232, 240)
        canvas.drawRoundRect(RectF(rightX, startY, rightX + boxWidth, startY + boxHeight), 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        textPaint.color = Color.rgb(30, 41, 59)
        textPaint.textSize = 9.5f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Inventory Valuation / ဆိုင်လက်ကျန် Stock တန်ဖိုး", rightX + 8f, startY + 16f, textPaint)

        textPaint.textSize = 8.5f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.color = Color.rgb(71, 85, 105)
        canvas.drawText("• Stock Purchase Cost: ${Formatters.formatKyat(settlement.totalStockPurchaseCost)}", rightX + 8f, startY + 34f, textPaint)
        canvas.drawText("• Ending Stock (Cost Value): ${Formatters.formatKyat(settlement.endingStockValueCost)}", rightX + 8f, startY + 48f, textPaint)
        canvas.drawText("• Ending Stock (Retail Value): ${Formatters.formatKyat(settlement.endingStockValueRetail)}", rightX + 8f, startY + 62f, textPaint)

        return startY + boxHeight
    }

    private fun drawTableHeader(
        canvas: Canvas,
        margin: Float,
        contentWidth: Float,
        startY: Float
    ): Float {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.color = Color.rgb(226, 232, 240)
        canvas.drawRoundRect(RectF(margin, startY, margin + contentWidth, startY + 22f), 4f, 4f, paint)

        textPaint.color = Color.rgb(30, 41, 59)
        textPaint.textSize = 9f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        canvas.drawText("DATE & TIME", margin + 4f, startY + 15f, textPaint)
        canvas.drawText("ITEM / DESCRIPTION", margin + 105f, startY + 15f, textPaint)
        canvas.drawText("CHANNEL", margin + 275f, startY + 15f, textPaint)
        canvas.drawText("TYPE", margin + 355f, startY + 15f, textPaint)

        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("AMOUNT (MMK)", margin + contentWidth - 6f, startY + 15f, textPaint)
        textPaint.textAlign = Paint.Align.LEFT

        return startY + 22f
    }

    private fun drawSubsequentPageHeader(
        canvas: Canvas,
        monthLabel: String,
        shopName: String,
        margin: Float,
        contentWidth: Float
    ) {
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)

        textPaint.color = Color.rgb(88, 28, 135)
        textPaint.textSize = 10f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("$shopName • $monthLabel Financial Statement (Continued)", margin, 24f, textPaint)

        linePaint.color = Color.rgb(203, 213, 225)
        linePaint.strokeWidth = 0.8f
        canvas.drawLine(margin, 28f, margin + contentWidth, 28f, linePaint)
    }

    private fun drawPageFooter(canvas: Canvas, pageNumber: Int, shopName: String) {
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        textPaint.color = Color.rgb(148, 163, 184)
        textPaint.textSize = 8f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        val footerText = "Generated by ဆိုင်စာရင်း (Shop Manager) • Page $pageNumber"
        canvas.drawText(footerText, 36f, PDF_PAGE_HEIGHT - 18f, textPaint)
    }

    private fun drawSignatureBlock(canvas: Canvas, margin: Float, contentWidth: Float, startY: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.color = Color.rgb(100, 116, 139)
        paint.strokeWidth = 1f

        // Prepared by line
        val line1X = margin + 30f
        val line1W = 160f
        canvas.drawLine(line1X, startY + 30f, line1X + line1W, startY + 30f, paint)

        textPaint.color = Color.rgb(51, 65, 85)
        textPaint.textSize = 9f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("စာရင်းစစ်ဆေးသူ (Prepared By)", line1X + 10f, startY + 44f, textPaint)

        // Approved by line
        val line2X = margin + contentWidth - 190f
        val line2W = 160f
        canvas.drawLine(line2X, startY + 30f, line2X + line2W, startY + 30f, paint)
        canvas.drawText("ဆိုင်ပိုင်ရှင် အတည်ပြုလက်မှတ် (Authorized)", line2X, startY + 44f, textPaint)
    }

    private fun drawStatBoxPng(
        canvas: Canvas,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        title: String,
        amount: String,
        bgColor: Int,
        accentColor: Int
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        val rect = RectF(x, y, x + w, y + h)
        paint.color = bgColor
        canvas.drawRoundRect(rect, 20f, 20f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = accentColor.copy(alpha = 0.5f)
        canvas.drawRoundRect(rect, 20f, 20f, paint)
        paint.style = Paint.Style.FILL

        textPaint.color = accentColor
        textPaint.textSize = 19f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(title, x + 20f, y + 42f, textPaint)

        textPaint.color = Color.WHITE
        textPaint.textSize = 24f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(amount, x + 20f, y + 88f, textPaint)
    }

    private fun Int.copy(alpha: Float): Int {
        val a = (alpha * 255).toInt().coerceIn(0, 255)
        return Color.argb(a, Color.red(this), Color.green(this), Color.blue(this))
    }

    /**
     * Share or view the exported file via Android Intent
     */
    fun shareDocument(context: Context, file: File, mimeType: String, title: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "$title - ဆိုင်စာရင်း အစီရင်ခံစာ")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    /**
     * Open or view the document directly
     */
    fun viewDocument(context: Context, file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to share intent if no dedicated viewer app exists
            shareDocument(context, file, mimeType, "ဆိုင်စာရင်း အစီရင်ခံစာ")
        }
    }
}
