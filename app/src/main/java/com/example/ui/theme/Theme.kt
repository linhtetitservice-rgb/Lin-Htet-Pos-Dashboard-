package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PurplePrimary,
    onPrimary = PurplePrimaryDark,
    primaryContainer = DarkSurfaceVariant,
    onPrimaryContainer = PurpleContainerLight,
    secondary = SecondaryLavender,
    onSecondary = Color(0xFF332D41),
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = PurpleContainerLight,
    tertiary = TertiaryPink,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = ExpenseColor,
    onError = Color(0xFF601410),
    errorContainer = ExpenseContainer,
    onErrorContainer = ExpenseColor
)

@Composable
fun MyanmarShopTheme(
    darkTheme: Boolean = true, // Default to Sophisticated Dark
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
