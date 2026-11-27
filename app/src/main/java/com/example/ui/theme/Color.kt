package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Corporate Executive Brand Palette (Light & Dark Foundation)
val NavyTeal = Color(0xFF073D43)               // Primary foundation: header, dominant nav/accent, main titles
val NavyTealMid = Color(0xFF0A4D54)            // Mid Navy Teal gradient stop
val NavyTealLight = Color(0xFF105D65)          // Elevated Navy Teal gradient stop
val ForestGreen = Color(0xFF477C55)            // Primary CTA buttons, active/success indicators, highlights
val ForestGreenDeep = Color(0xFF396947)        // Deeper Forest Green gradient anchor (high contrast w/ white)
val ForestGreenRich = Color(0xFF569066)        // Subtle lighter Forest Green gradient stop (WCAG AA w/ white)
val Sage = Color(0xFFA8BDAF)                   // Secondary chips/badges, supporting info, soft card accents
val SageSoftContainer = Color(0xFFE6EFE9)      // Soft Sage-tinted chip/indicator background (Light Mode)
val SageDarkContainer = Color(0xFF15443E)      // Harmonized Forest-Sage chip/indicator surface (Dark Mode)
val SoftGrey = Color(0xFFDCE5DF)               // Borders, dividers, supporting surfaces (Light Mode)

// Brand compatibility aliases (used across existing screens & components)
val BrandDeepTeal = NavyTeal                   // #073D43
val BrandDeepTealHover = NavyTealMid           // #0A4D54
val BrandEmeraldMid = NavyTealLight            // #105D65
val BrandLeafGreen = ForestGreen               // #477C55
val BrandLeafGreenBright = ForestGreen         // #477C55
val BrandLeafGreenLightTint = Sage             // #A8BDAF
val BrandLeafGreenSoftBg = SageSoftContainer   // #E6EFE9
val BrandLeafGreenDarkBg = SageDarkContainer   // #15443E

// Primary Action & Executive Accent Tokens
val LimeAccent = ForestGreen                   // #477C55 (Primary action & active status accent)
val LimeBright = Sage                          // #A8BDAF (Refined Sage highlight on dark headers/surfaces)
val LimeDeepReadable = ForestGreen             // #477C55 (WCAG AA readable accent on light surfaces)
val OnLimeInk = Color(0xFFFFFFFF)              // Crisp white text on Forest Green (#477C55)
val SaveGradientGreen = ForestGreenDeep        // #396947 -> #477C55 smooth executive CTA gradient
val HeaderSubtitleSage = Color(0xFFC2D6C8)     // Luminous Sage subtitle on dark Navy Teal headers
val OnlineActiveDot = Color(0xFF7FD198)        // High-visibility active status dot on dark headers

// Light Mode Surfaces (Clean Corporate Executive Canvas + Navy Teal Ink + Soft Grey Borders)
val LightPageCanvas = Color(0xFFF4F7F5)        // Clean off-white canvas harmonized with Soft Grey (#DCE5DF)
val LightCardSurface = Color(0xFFFBFCFB)       // Crisp executive card & navigation surface
val LightCardPureWhite = Color(0xFFFFFFFF)     // Elevated input & metric card white
val LightSurfaceVariant = SoftGrey             // #DCE5DF supporting surfaces
val LightTextPrimary = NavyTeal                // #073D43 primary headings & ink
val LightTextMuted = Color(0xFF476061)         // Muted executive teal-slate body/caption text
val LightOutline = Color(0xFFB5C7BC)           // Input outline bridging Sage (#A8BDAF) & Soft Grey (#DCE5DF)
val LightCardBorder = SoftGrey                 // #DCE5DF borders & dividers
val LightProgressTrack = SoftGrey              // #DCE5DF progress track

// Light Mode Result & Warning
val LightResultSurface = Color(0xFFECF2EE)     // Soft Sage-tinted summary bento surface
val LightResultBorder = Sage                   // #A8BDAF soft executive card border
val LightMetricBorder = SoftGrey               // #DCE5DF metric card border
val LightWarnSurface = Color(0xFFFEF3C7)
val LightWarnBorder = Color(0xFFD97706)
val LightWarnText = Color(0xFF92400E)

// Dark Mode Surfaces (Corporate Executive Dark: Canvas #061F22, Card #0B3032, Text #E8F0EB)
val DarkPageCanvas = Color(0xFF061F22)         // Exact Dark Mode Canvas (#061F22)
val DarkCardSurface = Color(0xFF0B3032)        // Exact Dark Mode Card (#0B3032)
val DarkCardElevated = Color(0xFF103D40)       // Subtle elevated inner input/panel on #0B3032 card
val DarkGlassSurface = Color(0xFF0B3032)       // Card surface (#0B3032)
val DarkGlassSheetSurface = Color(0xFF0B3032)  // Bottom sheet surface (#0B3032)
val DarkTextPrimary = Color(0xFFE8F0EB)        // Exact Dark Mode Text (#E8F0EB)
val DarkTextMuted = Sage                       // #A8BDAF supporting text on #0B3032
val DarkOutline = Color(0xFF2B5B5D)            // Clean executive border on inputs
val DarkCardBorder = Color(0xFF1D4B4E)         // Structured border separating #0B3032 cards from #061F22 canvas
val DarkProgressTrack = Color(0xFF061F22)      // Recessed canvas track inside cards

// Dark Mode Readable Accents & Result (Forest Green #477C55 + Sage #A8BDAF harmony)
val DarkReadableTeal = Color(0xFF88BC97)       // Readable Forest-Sage text accent on #0B3032
val DarkResultSurface = Color(0xFF0F3B3D)      // Elevated executive result bento card
val DarkResultBorder = ForestGreen             // #477C55 highlight border
val DarkMetricBorder = Color(0xFF245759)       // Structured inner metric border
val DarkWarnSurface = Color(0xFF3E2408)
val DarkWarnBorder = Color(0xFFB45309)
val DarkWarnText = Color(0xFFFDE68A)

// Error / Destructive Accents
val RedErrorLight = Color(0xFFB91C1C)
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
