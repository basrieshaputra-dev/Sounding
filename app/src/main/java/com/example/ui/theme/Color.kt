package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// =============================================================================
// E - TXT High-Contrast Executive Palette
// - Dark Mode: Deep Blue-Charcoal (#0B1412 / #101C19) with elevated cards
//   (#162622) and crisp input boxes (#1D322D).
// - Light Mode: Clean Crisp White (#F8FAF9 canvas / #FFFFFF cards / #EFF4F2 inputs)
//   with distinct borders & subtle elevation.
// - Brand Accents harmonized with the E - TXT woven emblem:
//   * Warm Amber / Gold (#F59E0B / #FBBF24) for hero result numbers, active
//     tonnage & temperature quick-select pills, and key highlights.
//   * Fresh Emerald / Teal (#10B981 / #14B8A6) for ONLINE • REALTIME & IN SPEC
//     badges, active icons, and bottom navigation indicator.
// =============================================================================

// Brand Core Accents (harmonized with E - TXT logo)
val AmberGold = Color(0xFFF59E0B)              // Active preset pills & warm gold accents (Light)
val AmberGoldBright = Color(0xFFFBBF24)        // Luminous gold numbers & active pills (Dark)
val AmberReadableLight = Color(0xFFB45309)     // High-contrast rich amber text on #FFFFFF cards
val OnAmberDeepInk = Color(0xFF1A1202)         // Sharp espresso-black ink on #F59E0B / #FBBF24
val AmberSoftContainerLight = Color(0xFFFEF3C7)
val AmberSoftContainerDark = Color(0xFF332609)

val EmeraldFresh = Color(0xFF10B981)           // Fresh emerald status badges, active icons, nav
val TealFresh = Color(0xFF14B8A6)              // Fresh teal highlight & secondary accent
val EmeraldDeepPrimary = Color(0xFF059669)     // Crisp readable emerald CTA / accent on Light Mode
val EmeraldDarker = Color(0xFF047857)          // Deep emerald gradient anchor
val EmeraldHeaderStart = Color(0xFF0A2E28)     // Deep teal-emerald header start (Light)
val EmeraldHeaderMid = Color(0xFF0D4A3F)       // Rich teal-emerald header mid (Light)
val EmeraldHeaderEnd = Color(0xFF0F6B5B)       // Fresh teal header end (Light)
val OnEmeraldWhite = Color(0xFFFFFFFF)         // Crisp white text on Light Emerald CTAs
val OnEmeraldDarkInk = Color(0xFF042219)       // Sharp dark ink on bright #10B981 / #14B8A6 (Dark)

// Light Mode Core Tokens — Clean Crisp White & Structured Contrast
val LightCleanCanvas = Color(0xFFF8FAF9)       // Main background canvas (#F8FAF9)
val LightPureWhiteCard = Color(0xFFFFFFFF)     // Primary card surface (#FFFFFF)
val LightInputSurface = Color(0xFFEFF4F2)      // Input fields & inner containers
val LightBorderCrisp = Color(0xFFD0DDD8)       // Clear card & input divider/border
val LightOutlineStrong = Color(0xFFB5C7C0)     // Stronger border for inputs & unselected chips
val LightEmeraldContainer = Color(0xFFD1FAE5)  // Fresh emerald soft pill / nav indicator (Light)
val LightOnEmeraldContainer = Color(0xFF065F46)// Deep readable emerald ink (Light)
val LightInkCrisp = Color(0xFF0F1E1B)          // Sharp primary text & numbers
val LightInkMuted = Color(0xFF4B615B)          // Secondary / supporting text
val LightInkEmpty = Color(0xFF8CA099)          // Dimmed empty / "-" value ink

// Dark Mode Core Tokens — Deep Blue-Charcoal (#0B1412 / #101C19) & Elevated Surfaces
val DarkDeepCanvas = Color(0xFF0B1412)         // Main background canvas (#0B1412)
val DarkAltCanvas = Color(0xFF101C19)          // Secondary canvas / bottom sheets / nav bar (#101C19)
val DarkElevatedCard = Color(0xFF162622)       // Elevated card surface (#162622)
val DarkInputSurface = Color(0xFF1D322D)       // Input boxes & inner containers (#1D322D)
val DarkBorderCrisp = Color(0xFF28423B)        // Subtle crisp border separating cards & boxes
val DarkOutlineStrong = Color(0xFF33544B)      // Clear outline for inputs & unselected chips
val DarkHeaderStart = Color(0xFF0D1D1A)        // Deep charcoal-teal header start (Dark)
val DarkHeaderMid = Color(0xFF122823)          // Deep charcoal-teal header mid (Dark)
val DarkHeaderEnd = Color(0xFF15322C)          // Deep charcoal-teal header end (Dark)
val DarkEmeraldContainer = Color(0xFF12382E)   // Fresh dark emerald active nav / badge container
val DarkOnEmeraldContainer = Color(0xFF6EE7B7) // Luminous mint-emerald ink on dark container
val DarkInkCrisp = Color(0xFFF1F7F5)           // Ultra-sharp primary text & numeric values (Dark)
val DarkInkMuted = Color(0xFF9BB4AD)           // Secondary / supporting text (Dark)
val DarkInkEmpty = Color(0xFF5D766F)           // Dimmed empty / "-" ink (Dark)

// Named Aliases (kept for backward compatibility across existing screens & JPG generators)
val WarmSandCanvas = LightCleanCanvas
val WarmOffWhiteCard = LightPureWhiteCard
val SoftSandContainer = LightInputSurface
val WarmBorderSubtle = LightBorderCrisp
val SagePrimary = EmeraldDeepPrimary
val SagePrimaryDeep = EmeraldHeaderStart
val SagePrimarySoft = TealFresh
val OnSageWhite = OnEmeraldWhite
val SageHighlightContainer = LightEmeraldContainer
val SageOnHighlight = LightOnEmeraldContainer
val WarmInkPrimary = LightInkCrisp
val WarmInkSecondary = LightInkMuted
val WarmInkEmpty = LightInkEmpty

val CharcoalCanvas = DarkDeepCanvas
val CharcoalSurface = DarkElevatedCard
val CharcoalInnerContainer = DarkInputSurface
val CharcoalBorderSubtle = DarkBorderCrisp
val SageMintPrimary = EmeraldFresh
val OnSageMintInk = OnEmeraldDarkInk
val SageMintHeaderStart = DarkHeaderStart
val SageMintHeaderMid = DarkHeaderMid
val SageMintHeaderEnd = DarkHeaderEnd
val DarkSageHighlight = DarkEmeraldContainer
val DarkSageOnHighlight = DarkOnEmeraldContainer
val CharcoalInkPrimary = DarkInkCrisp
val CharcoalInkSecondary = DarkInkMuted
val CharcoalInkEmpty = DarkInkEmpty

val NavyTeal = EmeraldHeaderStart
val NavyTealMid = EmeraldHeaderMid
val NavyTealLight = TealFresh
val ForestGreen = EmeraldDeepPrimary
val ForestGreenDeep = EmeraldDarker
val ForestGreenRich = EmeraldFresh
val Sage = Color(0xFF5EEAD4)
val SageSoftContainer = LightEmeraldContainer
val SageDarkContainer = DarkEmeraldContainer
val SoftGrey = LightBorderCrisp

val BrandDeepTeal = EmeraldHeaderStart
val BrandDeepTealHover = EmeraldDeepPrimary
val BrandEmeraldMid = TealFresh
val BrandLeafGreen = EmeraldFresh
val BrandLeafGreenBright = EmeraldFresh
val BrandLeafGreenLightTint = TealFresh
val BrandLeafGreenSoftBg = LightEmeraldContainer
val BrandLeafGreenDarkBg = DarkEmeraldContainer

// Action & Header Status Tokens
val LimeAccent = EmeraldFresh
val LimeBright = Color(0xFF34D399)
val LimeDeepReadable = EmeraldDeepPrimary
val OnLimeInk = OnEmeraldWhite
val SaveGradientGreen = EmeraldDarker
val HeaderSubtitleSage = Color(0xFFA7F3D0)
val OnlineActiveDot = Color(0xFF10B981)

// Light Mode Surface Aliases
val LightPageCanvas = LightCleanCanvas
val LightCardSurface = LightPureWhiteCard
val LightCardPureWhite = LightPureWhiteCard
val LightSurfaceVariant = LightInputSurface
val LightTextPrimary = LightInkCrisp
val LightTextMuted = LightInkMuted
val LightOutline = LightOutlineStrong
val LightCardBorder = LightBorderCrisp
val LightProgressTrack = LightInputSurface

// Light Mode Result & Warning
val LightResultSurface = Color(0xFFECFDF5)
val LightResultBorder = Color(0xFFA7F3D0)
val LightMetricBorder = LightBorderCrisp
val LightWarnSurface = Color(0xFFFEF3C7)
val LightWarnBorder = Color(0xFFF59E0B)
val LightWarnText = Color(0xFF92400E)

// Dark Mode Surface Aliases
val DarkPageCanvas = DarkDeepCanvas
val DarkCardSurface = DarkElevatedCard
val DarkCardElevated = DarkInputSurface
val DarkGlassSurface = DarkElevatedCard
val DarkGlassSheetSurface = DarkAltCanvas
val DarkTextPrimary = DarkInkCrisp
val DarkTextMuted = DarkInkMuted
val DarkOutline = DarkOutlineStrong
val DarkCardBorder = DarkBorderCrisp
val DarkProgressTrack = DarkInputSurface

// Dark Mode Readable Accents & Result
val DarkReadableTeal = TealFresh
val DarkResultSurface = Color(0xFF122621)
val DarkResultBorder = Color(0xFF235245)
val DarkMetricBorder = DarkBorderCrisp
val DarkWarnSurface = Color(0xFF362409)
val DarkWarnBorder = Color(0xFFF59E0B)
val DarkWarnText = Color(0xFFFDE68A)

// Error / Destructive Accents
val RedErrorLight = Color(0xFFDC2626)
val RedErrorContainerLight = Color(0xFFFEE2E2)
val RedOnErrorContainerLight = Color(0xFF991B1B)
val RedErrorDark = Color(0xFFFCA5A5)
val RedErrorContainerDark = Color(0xFF450A0A)
val RedOnErrorContainerDark = Color(0xFFFECACA)

// Real Industrial Tank Visual & Bright Palm Oil Yellow (#FACC15 / #EAB308) — Preserved
val OilYellowBright = Color(0xFFFACC15)
val OilYellowDeep = Color(0xFFEAB308)
val OilYellowHighlight = Color(0xFFFEF08A)
val OilYellowRim = Color(0xFFCA8A04)

val TankSteelDarkLight = Color(0xFF64748B)
val TankSteelMidLight = Color(0xFF94A3B8)
val TankSteelLightTone = Color(0xFFCBD5E1)
val TankSteelSheenLight = Color(0xFFF1F5F9)
val TankSteelOutlineLight = Color(0xFF475569)

val TankSteelDarkDark = Color(0xFF475569)
val TankSteelMidDark = Color(0xFF7B8FA3)
val TankSteelLightDark = Color(0xFFB4C2D0)
val TankSteelSheenDark = Color(0xFFE2E8F0)
val TankSteelOutlineDark = Color(0xFF94A3B8)

val TankSupportBlueLight = Color(0xFF1D6296)
val TankSupportBlueDark = Color(0xFF3889C4)
val TankRailingSafetyYellow = Color(0xFFEAB308)
val TankRailingOutline = Color(0xFF713F12)
