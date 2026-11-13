package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

enum class AppThemeMode(val titleAr: String) {
    SYSTEM("تلقائي حسب الجهاز"),
    LIGHT("الوضع الفاتح"),
    DARK("الوضع الداكن")
}

private val HisabatiLightColorScheme = lightColorScheme(
    primary = NavyDeep,
    onPrimary = Color.White,
    primaryContainer = EmeraldSoft,
    onPrimaryContainer = NavyDeep,
    secondary = EmeraldPrimary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = NavyRich,
    tertiary = GoldAccent,
    onTertiary = NavyDeep,
    tertiaryContainer = GoldSoft,
    onTertiaryContainer = NavyDeep,
    error = PayableCoral,
    onError = Color.White,
    errorContainer = PayableSoft,
    onErrorContainer = PayableDarkContainer,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightOutline,
    outlineVariant = Color(0xFFE5ECF4)
)

private val HisabatiDarkColorScheme = darkColorScheme(
    primary = EmeraldBright,
    onPrimary = NavyDeep,
    primaryContainer = EmeraldDarkContainer,
    onPrimaryContainer = EmeraldSoft,
    secondary = GoldAccent,
    onSecondary = NavyDeep,
    secondaryContainer = NavyCardDark,
    onSecondaryContainer = DarkTextPrimary,
    tertiary = InfoSlate,
    onTertiary = Color.White,
    error = Color(0xFFFB7185),
    onError = PayableDarkContainer,
    errorContainer = PayableDarkContainer,
    onErrorContainer = PayableSoft,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkOutline,
    outlineVariant = NavyBorderDark
)

val HisabatiShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun HisabatiTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val colorScheme = if (darkTheme) HisabatiDarkColorScheme else HisabatiLightColorScheme

    // Enforce Arabic RTL layout across all screens without exception
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = HisabatiTypography,
            shapes = HisabatiShapes,
            content = content
        )
    }
}
