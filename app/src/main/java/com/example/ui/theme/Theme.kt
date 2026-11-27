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
    val sagePrimary: Color,
    val onSagePrimary: Color,
    val sageContainer: Color,
    val onSageContainer: Color,
    val amberAccent: Color,
    val onAmberAccent: Color,
    val amberTextReadable: Color,
    val amberContainer: Color,
    val emeraldAccent: Color,
    val onEmeraldAccent: Color,
    val tealAccent: Color,
    val innerContainer: Color,
    val emptyValueInk: Color,
    val logoBadgeSurface: Color,
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
    headerGradientStart = EmeraldHeaderStart,
    headerGradientMid = EmeraldHeaderMid,
    headerGradientEnd = EmeraldHeaderEnd,
    headerSubtitleAccent = HeaderSubtitleSage,
    onlineIndicatorDot = EmeraldFresh,
    executiveActionSurface = EmeraldDeepPrimary,
    primaryCtaEnd = TealFresh,
    navSelectedIcon = LightOnEmeraldContainer,
    navSelectedText = EmeraldDeepPrimary,
    navIndicator = LightEmeraldContainer,
    sagePrimary = EmeraldDeepPrimary,
    onSagePrimary = OnEmeraldWhite,
    sageContainer = LightEmeraldContainer,
    onSageContainer = LightOnEmeraldContainer,
    amberAccent = AmberGold,
    onAmberAccent = OnAmberDeepInk,
    amberTextReadable = AmberReadableLight,
    amberContainer = AmberSoftContainerLight,
    emeraldAccent = EmeraldFresh,
    onEmeraldAccent = OnEmeraldWhite,
    tealAccent = TealFresh,
    innerContainer = LightInputSurface,
    emptyValueInk = LightInkEmpty,
    logoBadgeSurface = LightPureWhiteCard,
    leafAccent = EmeraldFresh,
    leafAccentReadable = EmeraldDeepPrimary,
    leafContainer = LightEmeraldContainer,
    onLeafContainer = LightOnEmeraldContainer,
    limeAccent = EmeraldFresh,
    limeBright = LimeBright,
    onLimeButton = OnEmeraldWhite,
    glassCardSurface = LightPureWhiteCard,
    glassSheetSurface = LightCleanCanvas,
    glassBorder = LightBorderCrisp,
    glassHighlightTop = Color.White.copy(alpha = 0.92f),
    progressTrack = LightInputSurface,
    interactiveAccent = EmeraldDeepPrimary,
    elevatedSurface = LightInputSurface,
    resultSurface = LightResultSurface,
    resultBorder = LightResultBorder,
    metricCardSurface = LightPureWhiteCard,
    metricCardBorder = LightBorderCrisp,
    metricValueInk = AmberReadableLight,
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
    subtleTealTint = LightInputSurface,
    cardBorder = LightBorderCrisp
)

private val DarkExtendedColors = SoundingExtendedColors(
    isDark = true,
    headerGradientStart = DarkHeaderStart,
    headerGradientMid = DarkHeaderMid,
    headerGradientEnd = DarkHeaderEnd,
    headerSubtitleAccent = HeaderSubtitleSage,
    onlineIndicatorDot = EmeraldFresh,
    executiveActionSurface = EmeraldFresh,
    primaryCtaEnd = TealFresh,
    navSelectedIcon = EmeraldFresh,
    navSelectedText = TealFresh,
    navIndicator = DarkEmeraldContainer,
    sagePrimary = EmeraldFresh,
    onSagePrimary = OnEmeraldDarkInk,
    sageContainer = DarkEmeraldContainer,
    onSageContainer = DarkOnEmeraldContainer,
    amberAccent = AmberGoldBright,
    onAmberAccent = OnAmberDeepInk,
    amberTextReadable = AmberGoldBright,
    amberContainer = AmberSoftContainerDark,
    emeraldAccent = EmeraldFresh,
    onEmeraldAccent = OnEmeraldDarkInk,
    tealAccent = TealFresh,
    innerContainer = DarkInputSurface,
    emptyValueInk = DarkInkEmpty,
    logoBadgeSurface = LightPureWhiteCard,
    leafAccent = EmeraldFresh,
    leafAccentReadable = TealFresh,
    leafContainer = DarkEmeraldContainer,
    onLeafContainer = DarkOnEmeraldContainer,
    limeAccent = EmeraldFresh,
    limeBright = LimeBright,
    onLimeButton = OnEmeraldDarkInk,
    glassCardSurface = DarkElevatedCard,
    glassSheetSurface = DarkAltCanvas,
    glassBorder = DarkBorderCrisp,
    glassHighlightTop = TealFresh.copy(alpha = 0.08f),
    progressTrack = DarkInputSurface,
    interactiveAccent = TealFresh,
    elevatedSurface = DarkInputSurface,
    resultSurface = DarkResultSurface,
    resultBorder = DarkResultBorder,
    metricCardSurface = DarkInputSurface,
    metricCardBorder = DarkBorderCrisp,
    metricValueInk = AmberGoldBright,
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
    subtleTealTint = DarkInputSurface,
    cardBorder = DarkBorderCrisp
)

val LocalSoundingExtendedColors = staticCompositionLocalOf { LightExtendedColors }

private val LightColorScheme = lightColorScheme(
    primary = EmeraldDeepPrimary,
    onPrimary = OnEmeraldWhite,
    primaryContainer = LightEmeraldContainer,
    onPrimaryContainer = LightOnEmeraldContainer,
    secondary = TealFresh,
    onSecondary = OnEmeraldWhite,
    secondaryContainer = LightEmeraldContainer,
    onSecondaryContainer = LightOnEmeraldContainer,
    tertiary = AmberGold,
    onTertiary = OnAmberDeepInk,
    tertiaryContainer = AmberSoftContainerLight,
    onTertiaryContainer = AmberReadableLight,
    background = LightCleanCanvas,
    onBackground = LightInkCrisp,
    surface = LightPureWhiteCard,
    onSurface = LightInkCrisp,
    surfaceVariant = LightInputSurface,
    onSurfaceVariant = LightInkMuted,
    outline = LightOutlineStrong,
    outlineVariant = LightBorderCrisp,
    error = RedErrorLight,
    onError = Color.White,
    errorContainer = RedErrorContainerLight,
    onErrorContainer = RedOnErrorContainerLight
)

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldFresh,
    onPrimary = OnEmeraldDarkInk,
    primaryContainer = DarkEmeraldContainer,
    onPrimaryContainer = DarkOnEmeraldContainer,
    secondary = TealFresh,
    onSecondary = OnEmeraldDarkInk,
    secondaryContainer = DarkEmeraldContainer,
    onSecondaryContainer = DarkOnEmeraldContainer,
    tertiary = AmberGoldBright,
    onTertiary = OnAmberDeepInk,
    tertiaryContainer = AmberSoftContainerDark,
    onTertiaryContainer = AmberGoldBright,
    background = DarkDeepCanvas,
    onBackground = DarkInkCrisp,
    surface = DarkElevatedCard,
    onSurface = DarkInkCrisp,
    surfaceVariant = DarkInputSurface,
    onSurfaceVariant = DarkInkMuted,
    outline = DarkOutlineStrong,
    outlineVariant = DarkBorderCrisp,
    error = RedErrorDark,
    onError = Color(0xFF450A0A),
    errorContainer = RedErrorContainerDark,
    onErrorContainer = RedOnErrorContainerDark
)

private val SoundingShapes = Shapes(
    small = RoundedCornerShape(12.dp),
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
