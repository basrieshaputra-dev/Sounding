package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class SoundingExtendedColors(
    val headerGradientStart: Color,
    val headerGradientEnd: Color,
    val leafAccent: Color,
    val leafAccentReadable: Color,
    val leafContainer: Color,
    val onLeafContainer: Color,
    val interactiveAccent: Color,
    val elevatedSurface: Color,
    val resultSurface: Color,
    val resultBorder: Color,
    val metricCardSurface: Color,
    val metricCardBorder: Color,
    val metricValueInk: Color,
    val warningSurface: Color,
    val warningBorder: Color,
    val warningText: Color,
    val oilFillStart: Color,
    val oilFillEnd: Color,
    val oilSurfaceHighlight: Color,
    val oilSurfaceRim: Color,
    val tankShellStroke: Color,
    val tankShellInterior: Color,
    val tankSteelDark: Color,
    val tankSteelMid: Color,
    val tankSteelLight: Color,
    val tankSteelSheen: Color,
    val tankSupportSteel: Color,
    val tankRailingColor: Color,
    val tankRailingStroke: Color,
    val subtleTealTint: Color,
    val cardBorder: Color
)

private val LightExtendedColors = SoundingExtendedColors(
    headerGradientStart = BrandDeepTeal,
    headerGradientEnd = BrandLeafGreen,
    leafAccent = BrandLeafGreen,
    leafAccentReadable = BrandLeafGreen,
    leafContainer = BrandLeafGreenSoftBg,
    onLeafContainer = Color(0xFF214A20),
    interactiveAccent = BrandDeepTeal,
    elevatedSurface = LightCardPureWhite,
    resultSurface = LightResultSurface,
    resultBorder = LightResultBorder,
    metricCardSurface = LightCardPureWhite,
    metricCardBorder = LightMetricBorder,
    metricValueInk = BrandDeepTeal,
    warningSurface = LightWarnSurface,
    warningBorder = LightWarnBorder,
    warningText = LightWarnText,
    oilFillStart = OilYellowBright,
    oilFillEnd = OilYellowDeep,
    oilSurfaceHighlight = OilYellowHighlight,
    oilSurfaceRim = OilYellowRim,
    tankShellStroke = TankSteelOutlineLight,
    tankShellInterior = TankSteelLightTone,
    tankSteelDark = TankSteelDarkLight,
    tankSteelMid = TankSteelMidLight,
    tankSteelLight = TankSteelLightTone,
    tankSteelSheen = TankSteelSheenLight,
    tankSupportSteel = TankSupportBlueLight,
    tankRailingColor = TankRailingSafetyYellow,
    tankRailingStroke = TankRailingOutline,
    subtleTealTint = LightCardPureWhite,
    cardBorder = LightCardBorder
)

private val DarkExtendedColors = SoundingExtendedColors(
    headerGradientStart = BrandDeepTeal,
    headerGradientEnd = BrandLeafGreen,
    leafAccent = BrandLeafGreen,
    leafAccentReadable = BrandLeafGreenBright,
    leafContainer = BrandLeafGreenDarkBg,
    onLeafContainer = BrandLeafGreenLightTint,
    interactiveAccent = DarkReadableTeal,
    elevatedSurface = DarkCardElevated,
    resultSurface = DarkResultSurface,
    resultBorder = DarkResultBorder,
    metricCardSurface = DarkCardElevated,
    metricCardBorder = DarkMetricBorder,
    metricValueInk = DarkReadableTeal,
    warningSurface = DarkWarnSurface,
    warningBorder = DarkWarnBorder,
    warningText = DarkWarnText,
    oilFillStart = OilYellowBright,
    oilFillEnd = OilYellowDeep,
    oilSurfaceHighlight = OilYellowHighlight,
    oilSurfaceRim = OilYellowRim,
    tankShellStroke = TankSteelOutlineDark,
    tankShellInterior = TankSteelMidDark,
    tankSteelDark = TankSteelDarkDark,
    tankSteelMid = TankSteelMidDark,
    tankSteelLight = TankSteelLightDark,
    tankSteelSheen = TankSteelSheenDark,
    tankSupportSteel = TankSupportBlueDark,
    tankRailingColor = OilYellowBright,
    tankRailingStroke = TankRailingOutline,
    subtleTealTint = DarkCardElevated,
    cardBorder = DarkCardBorder
)

val LocalSoundingExtendedColors = staticCompositionLocalOf { LightExtendedColors }

private val LightColorScheme = lightColorScheme(
    primary = BrandDeepTeal,
    onPrimary = Color.White,
    primaryContainer = BrandLeafGreenSoftBg,
    onPrimaryContainer = LightTextPrimary,
    secondary = BrandLeafGreen,
    onSecondary = Color.White,
    secondaryContainer = BrandLeafGreenSoftBg,
    onSecondaryContainer = Color(0xFF1E431D),
    tertiary = BrandLeafGreen,
    onTertiary = Color.White,
    tertiaryContainer = LightWarnSurface,
    onTertiaryContainer = LightWarnText,
    background = LightPageCanvas,
    onBackground = LightTextPrimary,
    surface = LightCardSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextMuted,
    outline = LightOutline,
    outlineVariant = LightCardBorder,
    error = RedErrorLight,
    onError = Color.White,
    errorContainer = RedErrorContainerLight,
    onErrorContainer = RedOnErrorContainerLight
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandDeepTeal,
    onPrimary = Color.White,
    primaryContainer = DarkCardElevated,
    onPrimaryContainer = DarkTextPrimary,
    secondary = BrandLeafGreen,
    onSecondary = Color.White,
    secondaryContainer = BrandLeafGreenDarkBg,
    onSecondaryContainer = BrandLeafGreenLightTint,
    tertiary = BrandLeafGreenBright,
    onTertiary = Color(0xFF0A210A),
    tertiaryContainer = DarkWarnSurface,
    onTertiaryContainer = DarkWarnText,
    background = DarkPageCanvas,
    onBackground = DarkTextPrimary,
    surface = DarkCardSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkCardElevated,
    onSurfaceVariant = DarkTextMuted,
    outline = DarkOutline,
    outlineVariant = DarkCardBorder,
    error = RedErrorDark,
    onError = Color(0xFF450A0A),
    errorContainer = RedErrorContainerDark,
    onErrorContainer = RedOnErrorContainerDark
)

private val SoundingShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

object SoundingThemeExtras {
    val colors: SoundingExtendedColors
        @Composable
        get() = LocalSoundingExtendedColors.current
}

@Composable
fun SoundingTangkiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalSoundingExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SoundingTypography,
            shapes = SoundingShapes,
            content = content
        )
    }
}
