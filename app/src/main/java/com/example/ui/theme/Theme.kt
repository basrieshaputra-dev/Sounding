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
    val isDark: Boolean,
    val headerGradientStart: Color,
    val headerGradientMid: Color,
    val headerGradientEnd: Color,
    val headerSubtitleAccent: Color,
    val onlineIndicatorDot: Color,
    val executiveActionSurface: Color,
    val primaryCtaEnd: Color,
    val navSelectedIcon: Color,
    val navSelectedText: Color,
    val navIndicator: Color,
    val leafAccent: Color,
    val leafAccentReadable: Color,
    val leafContainer: Color,
    val onLeafContainer: Color,
    val limeAccent: Color,
    val limeBright: Color,
    val onLimeButton: Color,
    val glassCardSurface: Color,
    val glassSheetSurface: Color,
    val glassBorder: Color,
    val glassHighlightTop: Color,
    val progressTrack: Color,
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
    isDark = false,
    headerGradientStart = NavyTeal,
    headerGradientMid = NavyTealMid,
    headerGradientEnd = NavyTealLight,
    headerSubtitleAccent = HeaderSubtitleSage,
    onlineIndicatorDot = OnlineActiveDot,
    executiveActionSurface = NavyTeal,
    primaryCtaEnd = ForestGreenRich,
    navSelectedIcon = NavyTeal,
    navSelectedText = NavyTeal,
    navIndicator = SageSoftContainer,
    leafAccent = ForestGreen,
    leafAccentReadable = ForestGreen,
    leafContainer = SageSoftContainer,
    onLeafContainer = NavyTeal,
    limeAccent = ForestGreen,
    limeBright = Sage,
    onLimeButton = OnLimeInk,
    glassCardSurface = LightCardPureWhite,
    glassSheetSurface = LightPageCanvas,
    glassBorder = SoftGrey,
    glassHighlightTop = Color.White.copy(alpha = 0.85f),
    progressTrack = LightProgressTrack,
    interactiveAccent = NavyTeal,
    elevatedSurface = LightCardPureWhite,
    resultSurface = LightResultSurface,
    resultBorder = LightResultBorder,
    metricCardSurface = LightCardPureWhite,
    metricCardBorder = LightMetricBorder,
    metricValueInk = NavyTeal,
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
    isDark = true,
    headerGradientStart = DarkPageCanvas,
    headerGradientMid = NavyTeal,
    headerGradientEnd = DarkCardSurface,
    headerSubtitleAccent = HeaderSubtitleSage,
    onlineIndicatorDot = OnlineActiveDot,
    executiveActionSurface = ForestGreen,
    primaryCtaEnd = ForestGreenRich,
    navSelectedIcon = DarkTextPrimary,
    navSelectedText = Sage,
    navIndicator = ForestGreen.copy(alpha = 0.34f),
    leafAccent = ForestGreen,
    leafAccentReadable = DarkReadableTeal,
    leafContainer = SageDarkContainer,
    onLeafContainer = DarkTextPrimary,
    limeAccent = ForestGreen,
    limeBright = Sage,
    onLimeButton = OnLimeInk,
    glassCardSurface = DarkCardSurface,
    glassSheetSurface = DarkGlassSheetSurface,
    glassBorder = DarkCardBorder,
    glassHighlightTop = Sage.copy(alpha = 0.10f),
    progressTrack = DarkProgressTrack,
    interactiveAccent = Sage,
    elevatedSurface = DarkCardElevated,
    resultSurface = DarkResultSurface,
    resultBorder = DarkResultBorder,
    metricCardSurface = DarkCardElevated,
    metricCardBorder = DarkMetricBorder,
    metricValueInk = DarkTextPrimary,
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
    primary = NavyTeal,
    onPrimary = Color.White,
    primaryContainer = SageSoftContainer,
    onPrimaryContainer = LightTextPrimary,
    secondary = ForestGreen,
    onSecondary = Color.White,
    secondaryContainer = SageSoftContainer,
    onSecondaryContainer = NavyTeal,
    tertiary = Sage,
    onTertiary = NavyTeal,
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
    primary = ForestGreen,
    onPrimary = Color.White,
    primaryContainer = DarkCardElevated,
    onPrimaryContainer = DarkTextPrimary,
    secondary = Sage,
    onSecondary = NavyTeal,
    secondaryContainer = SageDarkContainer,
    onSecondaryContainer = DarkTextPrimary,
    tertiary = DarkReadableTeal,
    onTertiary = DarkPageCanvas,
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
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
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
