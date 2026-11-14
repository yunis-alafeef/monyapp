package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

enum class AppThemeMode(val titleAr: String) {
    SYSTEM("تلقائي حسب الجهاز"),
    LIGHT("فاتح ساطع"),
    DARK("داكن فاخر")
}

enum class AppThemePalette(
    val titleAr: String,
    val subtitleAr: String,
    val previewPrimaryHex: Long,
    val previewAccentHex: Long,
    val prefersBrightSurface: Boolean = false
) {
    ROYAL_EMERALD("الملكي الزمردي", "كحلي وزمردي كلاسيكي", 0xFF0B1B32L, 0xFF10B981L),
    VIBRANT_AZURE("الأزرق الساطع", "سماوي مشرق وحديث", 0xFF0284C7L, 0xFF06B6D4L, prefersBrightSurface = true),
    IMPERIAL_TEAL("الفيروزي العصري", "تيل عميق وأخضر نعناعي", 0xFF0F766EL, 0xFF14B8A6L),
    AMETHYST_LUX("الأرجواني الفاخر", "نيلي ملكي وبنفسجي ساطع", 0xFF4F46E5L, 0xFF8B5CF6L, prefersBrightSurface = true),
    SUNSET_RUBY("الياقوتي الدافئ", "قرمزي جريء وذهبي دافئ", 0xFFBE123CL, 0xFFF59E0BL, prefersBrightSurface = true),
    MIDNIGHT_GOLD("الليلي الذهبي", "فحمي عميق ولمسات ذهبية", 0xFF111827L, 0xFFEAB308L),
    CUSTOM("ثيم مخصص", "ألوان من اختيارك", 0xFF059669L, 0xFFD97706L)
}

data class CustomThemePreset(
    val id: String,
    val nameAr: String,
    val primaryHex: Long,
    val accentHex: Long,
    val darkModePreferred: Boolean = false
)

@Immutable
data class HisabatiExtendedColors(
    val heroStart: Color,
    val heroMid: Color,
    val heroEnd: Color,
    val positive: Color,
    val negative: Color,
    val warning: Color,
    val accent: Color,
    val info: Color,
    val dividerSubtle: Color
)

val LocalHisabatiColors = staticCompositionLocalOf {
    HisabatiExtendedColors(
        heroStart = NavyDeep,
        heroMid = NavyRich,
        heroEnd = EmeraldPrimary,
        positive = EmeraldPrimary,
        negative = PayableCoral,
        warning = WarningAmber,
        accent = GoldAccent,
        info = InfoSlate,
        dividerSubtle = LightOutline
    )
}

val HisabatiShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

private fun buildThemeBundle(
    palette: AppThemePalette,
    darkTheme: Boolean,
    customPrimaryHex: Long,
    customAccentHex: Long
): Pair<ColorScheme, HisabatiExtendedColors> {
    val (primaryBase, secondaryBase, tertiaryBase) = when (palette) {
        AppThemePalette.ROYAL_EMERALD -> Triple(NavyDeep, EmeraldPrimary, GoldAccent)
        AppThemePalette.VIBRANT_AZURE -> Triple(Color(0xFF0369A1), Color(0xFF06B6D4), Color(0xFFF59E0B))
        AppThemePalette.IMPERIAL_TEAL -> Triple(Color(0xFF0F766E), Color(0xFF10B981), Color(0xFFF97316))
        AppThemePalette.AMETHYST_LUX -> Triple(Color(0xFF4338CA), Color(0xFF8B5CF6), Color(0xFFF59E0B))
        AppThemePalette.SUNSET_RUBY -> Triple(Color(0xFF9F1239), Color(0xFFEA580C), Color(0xFFFBBF24))
        AppThemePalette.MIDNIGHT_GOLD -> Triple(Color(0xFF1E293B), Color(0xFFD97706), Color(0xFF10B981))
        AppThemePalette.CUSTOM -> Triple(
            Color(customPrimaryHex),
            Color(customAccentHex),
            lerp(Color(customPrimaryHex), Color(customAccentHex), 0.5f)
        )
    }

    val colorScheme = if (!darkTheme) {
        val bg = lerp(Color(0xFFF8FAFC), primaryBase, 0.03f)
        val surfaceVariant = lerp(Color(0xFFEFF4FA), primaryBase, 0.06f)
        val outline = lerp(Color(0xFFDCE4EF), primaryBase, 0.14f)
        lightColorScheme(
            primary = primaryBase,
            onPrimary = Color.White,
            primaryContainer = lerp(Color.White, secondaryBase, 0.18f),
            onPrimaryContainer = lerp(Color.Black, primaryBase, 0.85f),
            secondary = secondaryBase,
            onSecondary = Color.White,
            secondaryContainer = lerp(Color.White, primaryBase, 0.14f),
            onSecondaryContainer = primaryBase,
            tertiary = tertiaryBase,
            onTertiary = Color(0xFF111827),
            tertiaryContainer = lerp(Color.White, tertiaryBase, 0.22f),
            onTertiaryContainer = Color(0xFF111827),
            error = PayableCoral,
            onError = Color.White,
            errorContainer = PayableSoft,
            onErrorContainer = PayableDarkContainer,
            background = bg,
            onBackground = Color(0xFF0F172A),
            surface = Color.White,
            onSurface = Color(0xFF0F172A),
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = Color(0xFF475569),
            outline = outline,
            outlineVariant = outline.copy(alpha = 0.72f)
        )
    } else {
        val darkBg = lerp(Color(0xFF060B14), primaryBase, 0.10f)
        val darkSurface = lerp(Color(0xFF0C1524), primaryBase, 0.12f)
        val darkVariant = lerp(Color(0xFF152238), primaryBase, 0.16f)
        val darkOutline = lerp(Color(0xFF233554), secondaryBase, 0.20f)
        val brightPrimary = lerp(secondaryBase, Color.White, 0.18f)
        val brightSecondary = lerp(tertiaryBase, Color.White, 0.15f)
        darkColorScheme(
            primary = brightPrimary,
            onPrimary = Color(0xFF04131B),
            primaryContainer = lerp(Color(0xFF0A192F), secondaryBase, 0.30f),
            onPrimaryContainer = lerp(Color.White, secondaryBase, 0.25f),
            secondary = brightSecondary,
            onSecondary = Color(0xFF0A1320),
            secondaryContainer = darkVariant,
            onSecondaryContainer = Color(0xFFF1F5F9),
            tertiary = lerp(primaryBase, Color.White, 0.35f),
            onTertiary = Color.White,
            error = Color(0xFFFB7185),
            onError = PayableDarkContainer,
            errorContainer = PayableDarkContainer,
            onErrorContainer = PayableSoft,
            background = darkBg,
            onBackground = Color(0xFFF1F5F9),
            surface = darkSurface,
            onSurface = Color(0xFFF1F5F9),
            surfaceVariant = darkVariant,
            onSurfaceVariant = Color(0xFF9FB0C7),
            outline = darkOutline,
            outlineVariant = darkOutline.copy(alpha = 0.75f)
        )
    }

    val heroStart = if (darkTheme) lerp(Color(0xFF060C18), primaryBase, 0.45f) else lerp(Color(0xFF091426), primaryBase, 0.78f)
    val heroMid = lerp(primaryBase, secondaryBase, 0.26f)
    val heroEnd = lerp(secondaryBase, tertiaryBase, 0.18f)

    val extended = HisabatiExtendedColors(
        heroStart = heroStart,
        heroMid = heroMid,
        heroEnd = heroEnd,
        positive = if (darkTheme) Color(0xFF34D399) else EmeraldPrimary,
        negative = if (darkTheme) Color(0xFFFB7185) else PayableCoral,
        warning = if (darkTheme) Color(0xFFFBBF24) else WarningAmber,
        accent = if (darkTheme) lerp(tertiaryBase, Color.White, 0.16f) else secondaryBase,
        info = if (darkTheme) Color(0xFF60A5FA) else InfoSlate,
        dividerSubtle = colorScheme.outlineVariant
    )
    return colorScheme to extended
}

object HisabatiTheme {
    val colors: HisabatiExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalHisabatiColors.current
}

@Composable
fun HisabatiTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    themePalette: AppThemePalette = AppThemePalette.ROYAL_EMERALD,
    customPrimaryHex: Long = 0xFF059669L,
    customAccentHex: Long = 0xFFD97706L,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val (colorScheme, extendedColors) = buildThemeBundle(
        palette = themePalette,
        darkTheme = darkTheme,
        customPrimaryHex = customPrimaryHex,
        customAccentHex = customAccentHex
    )

    // Enforce Arabic RTL layout across all screens without exception
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalHisabatiColors provides extendedColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = HisabatiTypography,
            shapes = HisabatiShapes,
            content = content
        )
    }
}
