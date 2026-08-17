package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PaymentMethod
import com.example.ui.theme.AYAPayPurple
import com.example.ui.theme.AYAPayPurpleContainer
import com.example.ui.theme.CBPayRed
import com.example.ui.theme.CBPayRedContainer
import com.example.ui.theme.CashGreen
import com.example.ui.theme.CashGreenContainer
import com.example.ui.theme.DarkOutline
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ExpenseColor
import com.example.ui.theme.ExpenseContainer
import com.example.ui.theme.IncomeColor
import com.example.ui.theme.IncomeContainer
import com.example.ui.theme.KPayBlue
import com.example.ui.theme.KPayBlueContainer
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningColor
import com.example.ui.theme.WarningContainer
import com.example.ui.theme.WavePayYellow
import com.example.ui.theme.WavePayYellowContainer

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    containerColor: Color = DarkSurface,
    contentColor: Color = PurplePrimary,
    borderColor: Color = DarkOutline.copy(alpha = 0.4f),
    modifier: Modifier = Modifier,
    tag: String = ""
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier.testTag(tag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(contentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                fontSize = 20.sp
            )

            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun PaymentBadge(
    method: PaymentMethod,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label) = when (method) {
        PaymentMethod.KPAY -> Triple(KPayBlueContainer, KPayBlue, "KBZPay")
        PaymentMethod.WAVE_PAY -> Triple(WavePayYellowContainer, WavePayYellow, "WavePay")
        PaymentMethod.CASH -> Triple(CashGreenContainer, CashGreen, "Cash (ငွေသား)")
        PaymentMethod.CB_PAY -> Triple(CBPayRedContainer, CBPayRed, "CB Pay")
        PaymentMethod.AYA_PAY -> Triple(AYAPayPurpleContainer, AYAPayPurple, "AYA Pay")
        PaymentMethod.OTHER -> Triple(DarkSurfaceVariant, TextSecondary, "အခြား")
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 6.dp else 10.dp,
                vertical = if (compact) 2.dp else 4.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(textColor)
            )
            Text(
                text = if (compact) method.name.take(4) else label,
                fontSize = if (compact) 10.sp else 12.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

@Composable
fun StockBadge(
    stockQuantity: Int,
    lowThreshold: Int,
    unit: String,
    modifier: Modifier = Modifier
) {
    val isOut = stockQuantity <= 0
    val isLow = stockQuantity <= lowThreshold && !isOut

    val (bgColor, textColor, label) = when {
        isOut -> Triple(ExpenseContainer, ExpenseColor, "Stock ကုန်ပြီ")
        isLow -> Triple(WarningContainer, WarningColor, "သတိပေး Stock: $stockQuantity $unit")
        else -> Triple(DarkSurface, IncomeColor, "Stock: $stockQuantity $unit")
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (isLow || isOut) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(12.dp)
                )
            }
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        }
    }
}
