package com.example.ui.components

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import com.example.ui.theme.SoundingExtendedColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Classifies tank silhouette group by tank number:
 * - T.171 and above (T.171, T.172, ..., T.182, etc.) -> Flat-bottom Towertank with raftered roof,
 *   vertical pipe, roof-side manhole, central valve/vent unit, and side bolted shell manhole.
 * - Preblend tanks (T.101, T.111, T.102, T.112, T.121, T.122, etc.) -> Elevated dished-bottom
 *   tank on steel support columns with domed roof, perimeter railing, and curved gooseneck vent.
 */
internal fun isTowerTank(tankId: String): Boolean {
    val tankNumber = tankId.filter { it.isDigit() }.toIntOrNull() ?: return false
    return tankNumber >= 171
}

/**
 * Renders the T.171+ flat-bottom vertical industrial storage tank:
 * - Circular annular base ring resting flat on a concrete foundation pad (no tall support legs)
 * - Cylindrical brushed steel shell with bright yellow oil fill & bolted circular side manhole
 * - Conical roof with visible radial top rafters pattern, vertical standpipe, circular roof-side
 *   manhole cover, and central breather valve/vent assembly at the roof apex.
 */
internal fun DrawScope.drawTowerTank(
    animatedFill: Float,
    isOverfill: Boolean,
    extras: SoundingExtendedColors,
    colorScheme: ColorScheme
) {
    val w = size.width
    val h = size.height

    val scaleMarginRight = w * 0.16f
    val leftPad = w * 0.05f
    val cylLeft = leftPad
    val cylRight = w - scaleMarginRight
    val cylWidth = (cylRight - cylLeft).coerceAtLeast(32f)
    val centerX = cylLeft + cylWidth / 2f

    val topFittingRoom = h * 0.175f
    val roofHeight = (cylWidth * 0.19f).coerceIn(14.dp.toPx(), 23.dp.toPx())
    val baseRingRoom = 11.dp.toPx()

    val roofApexY = topFittingRoom
    val cylTopY = topFittingRoom + roofHeight
    val floorY = h - 2.dp.toPx()
    val cylBottomY = floorY - baseRingRoom
    val bottomArcDrop = 4.5.dp.toPx()
    val strokePx = 1.7.dp.toPx()

    // 1. Concrete Foundation Pad & Flat-Seated Circular Annular Base Ring (no tall legs)
    val padLeft = cylLeft - 4.5.dp.toPx()
    val padRight = cylRight + 4.5.dp.toPx()
    val padTopY = cylBottomY + 4.dp.toPx()

    // Concrete foundation slab resting flat on floor
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                extras.tankSteelMid.copy(alpha = 0.72f),
                extras.tankSteelDark.copy(alpha = 0.85f)
            ),
            startY = padTopY,
            endY = floorY
        ),
        topLeft = Offset(padLeft, padTopY),
        size = Size(padRight - padLeft, (floorY - padTopY).coerceAtLeast(3.dp.toPx())),
        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
    )
    drawLine(
        color = extras.tankShellStroke.copy(alpha = 0.65f),
        start = Offset(padLeft - 2.dp.toPx(), floorY),
        end = Offset(padRight + 2.dp.toPx(), floorY),
        strokeWidth = 2.dp.toPx(),
        cap = StrokeCap.Round
    )

    // Circular steel base ring / curb flange wrapping the flat tank bottom
    val ringLeft = cylLeft - 2.5.dp.toPx()
    val ringRight = cylRight + 2.5.dp.toPx()
    val ringTopY = cylBottomY - 1.dp.toPx()
    val ringHeight = 5.5.dp.toPx()
    val baseRingPath = Path().apply {
        moveTo(ringLeft, ringTopY)
        quadraticTo(centerX, ringTopY + bottomArcDrop, ringRight, ringTopY)
        lineTo(ringRight, ringTopY + ringHeight)
        quadraticTo(centerX, ringTopY + ringHeight + bottomArcDrop, ringLeft, ringTopY + ringHeight)
        close()
    }
    drawPath(
        path = baseRingPath,
        brush = Brush.horizontalGradient(
            0.0f to extras.tankSteelDark,
            0.28f to extras.tankSteelLight,
            0.52f to extras.tankSteelSheen,
            0.80f to extras.tankSteelMid,
            1.0f to extras.tankSteelDark,
            startX = ringLeft,
            endX = ringRight
        )
    )
    drawPath(
        path = baseRingPath,
        color = extras.tankShellStroke,
        style = Stroke(width = 1.2.dp.toPx(), join = StrokeJoin.Round)
    )

    // 2. Flat-Bottom Vertical Cylindrical Shell Silhouette
    val shellPath = Path().apply {
        moveTo(cylLeft, cylTopY)
        lineTo(cylRight, cylTopY)
        lineTo(cylRight, cylBottomY)
        quadraticTo(centerX, cylBottomY + bottomArcDrop, cylLeft, cylBottomY)
        close()
    }

    // Brushed stainless-steel body fill
    drawPath(
        path = shellPath,
        brush = Brush.horizontalGradient(
            0.0f to extras.tankSteelDark,
            0.16f to extras.tankSteelLight,
            0.30f to extras.tankSteelSheen,
            0.48f to extras.tankSteelLight,
            0.76f to extras.tankSteelMid,
            1.0f to extras.tankSteelDark,
            startX = cylLeft,
            endX = cylRight
        )
    )

    // 3. Bright Yellow Palm Oil Liquid Fill Clipped Cleanly Inside Cylinder Shell
    val scaleBottomY = cylBottomY + bottomArcDrop * 0.45f
    val liquidSpan = (scaleBottomY - cylTopY).coerceAtLeast(36f)
    val liquidTopY = scaleBottomY - (liquidSpan * animatedFill)

    clipPath(shellPath) {
        if (animatedFill > 0.004f) {
            val fillColorTop = if (isOverfill) colorScheme.error else extras.oilFillStart
            val fillColorBottom = extras.oilFillEnd

            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(fillColorTop, fillColorBottom),
                    startY = liquidTopY,
                    endY = cylBottomY + bottomArcDrop
                ),
                topLeft = Offset(cylLeft, liquidTopY),
                size = Size(cylWidth, (cylBottomY + bottomArcDrop - liquidTopY).coerceAtLeast(0f))
            )

            // 3D cylindrical specular highlight & right-edge depth over yellow oil
            drawRect(
                brush = Brush.horizontalGradient(
                    0.0f to Color.Black.copy(alpha = 0.12f),
                    0.18f to Color.White.copy(alpha = 0.10f),
                    0.30f to Color.White.copy(alpha = 0.34f),
                    0.45f to Color.Transparent,
                    0.84f to Color.Black.copy(alpha = 0.06f),
                    1.0f to Color.Black.copy(alpha = 0.18f),
                    startX = cylLeft,
                    endX = cylRight
                ),
                topLeft = Offset(cylLeft, liquidTopY),
                size = Size(cylWidth, (cylBottomY + bottomArcDrop - liquidTopY).coerceAtLeast(0f))
            )

            // 3D elliptical oil meniscus surface
            val meniscusH = (cylWidth * 0.115f).coerceIn(6.5.dp.toPx(), 12.dp.toPx())
            drawOval(
                color = if (isOverfill) {
                    colorScheme.errorContainer.copy(alpha = 0.85f)
                } else {
                    extras.oilSurfaceHighlight.copy(alpha = 0.92f)
                },
                topLeft = Offset(cylLeft, liquidTopY - meniscusH / 2f),
                size = Size(cylWidth, meniscusH)
            )
            drawOval(
                color = if (isOverfill) colorScheme.error else extras.oilSurfaceRim,
                topLeft = Offset(cylLeft, liquidTopY - meniscusH / 2f),
                size = Size(cylWidth, meniscusH),
                style = Stroke(width = 1.2.dp.toPx())
            )
        }

        // Welded horizontal plate course seams along cylindrical shell
        val bodyHeight = (cylBottomY - cylTopY).coerceAtLeast(24f)
        val seamFractions = listOf(0.25f, 0.50f, 0.75f)
        for (frac in seamFractions) {
            val sy = cylTopY + bodyHeight * frac
            val seamPath = Path().apply {
                moveTo(cylLeft, sy)
                quadraticTo(centerX, sy + 3.2.dp.toPx(), cylRight, sy)
            }
            drawPath(
                path = seamPath,
                color = extras.tankShellStroke.copy(alpha = 0.28f),
                style = Stroke(width = 1.dp.toPx())
            )
            drawPath(
                path = seamPath,
                color = Color.White.copy(alpha = 0.18f),
                style = Stroke(width = 0.65.dp.toPx())
            )
        }

        // Vertical sounding dip tape & plumb bob descending to oil surface
        val dipBottomY = liquidTopY.coerceIn(cylTopY + 6.dp.toPx(), scaleBottomY - 4.dp.toPx())
        drawLine(
            color = extras.tankShellStroke.copy(alpha = 0.62f),
            start = Offset(centerX, cylTopY),
            end = Offset(centerX, dipBottomY),
            strokeWidth = 1.3.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 4f), 0f)
        )
        val bobSize = 4.2.dp.toPx()
        val bobPath = Path().apply {
            moveTo(centerX - bobSize * 0.7f, dipBottomY - bobSize)
            lineTo(centerX + bobSize * 0.7f, dipBottomY - bobSize)
            lineTo(centerX, dipBottomY + bobSize * 0.35f)
            close()
        }
        drawPath(path = bobPath, color = extras.tankShellStroke)
    }

    // Outer shell contour stroke
    drawPath(
        path = shellPath,
        color = extras.tankShellStroke,
        style = Stroke(width = strokePx, join = StrokeJoin.Round)
    )

    // Anchor bolt lugs evenly spaced along the circular base ring
    val lugFractions = listOf(0.10f, 0.30f, 0.50f, 0.70f, 0.90f)
    for (f in lugFractions) {
        val lx = ringLeft + (ringRight - ringLeft) * f
        val archT = 1f - (2f * f - 1f) * (2f * f - 1f)
        val ly = ringTopY + archT * bottomArcDrop * 0.75f
        drawRoundRect(
            color = extras.tankShellStroke.copy(alpha = 0.85f),
            topLeft = Offset(lx - 1.2.dp.toPx(), ly + 0.8.dp.toPx()),
            size = Size(2.4.dp.toPx(), 4.dp.toPx()),
            cornerRadius = CornerRadius(0.8.dp.toPx(), 0.8.dp.toPx())
        )
    }

    // 4. Side Shell Bolted Circular Manhole (Manhole Bulat Bermur di Sisi Badan Tangki)
    val sideManholeRadius = (cylWidth * 0.115f).coerceIn(9.5.dp.toPx(), 13.5.dp.toPx())
    val sideManholeCenter = Offset(
        x = cylLeft + cylWidth * 0.27f,
        y = cylBottomY - (cylBottomY - cylTopY) * 0.19f
    )
    // Soft contact shadow behind nozzle flange
    drawCircle(
        color = Color.Black.copy(alpha = 0.22f),
        radius = sideManholeRadius + 1.2.dp.toPx(),
        center = Offset(sideManholeCenter.x + 1.dp.toPx(), sideManholeCenter.y + 1.2.dp.toPx())
    )
    // Outer flange ring
    drawCircle(
        brush = Brush.linearGradient(
            colors = listOf(extras.tankSteelSheen, extras.tankSteelMid, extras.tankSteelDark),
            start = Offset(sideManholeCenter.x - sideManholeRadius, sideManholeCenter.y - sideManholeRadius),
            end = Offset(sideManholeCenter.x + sideManholeRadius, sideManholeCenter.y + sideManholeRadius)
        ),
        radius = sideManholeRadius,
        center = sideManholeCenter
    )
    drawCircle(
        color = extras.tankShellStroke,
        radius = sideManholeRadius,
        center = sideManholeCenter,
        style = Stroke(width = 1.2.dp.toPx())
    )
    // Inner cover plate
    val innerCoverR = sideManholeRadius * 0.64f
    drawCircle(
        brush = Brush.linearGradient(
            colors = listOf(extras.tankSteelLight, extras.tankSteelDark),
            start = Offset(sideManholeCenter.x - innerCoverR, sideManholeCenter.y - innerCoverR),
            end = Offset(sideManholeCenter.x + innerCoverR, sideManholeCenter.y + innerCoverR)
        ),
        radius = innerCoverR,
        center = sideManholeCenter
    )
    drawCircle(
        color = extras.tankShellStroke.copy(alpha = 0.8f),
        radius = innerCoverR,
        center = sideManholeCenter,
        style = Stroke(width = 0.9.dp.toPx())
    )
    // 8 perimeter hex bolts/nuts (mur) around flange ring
    val boltRingR = sideManholeRadius * 0.82f
    val boltRadius = (sideManholeRadius * 0.11f).coerceIn(1.dp.toPx(), 1.6.dp.toPx())
    for (b in 0 until 8) {
        val angleRad = (b * 45.0) * (PI / 180.0)
        val bx = sideManholeCenter.x + (boltRingR * cos(angleRad)).toFloat()
        val by = sideManholeCenter.y + (boltRingR * sin(angleRad)).toFloat()
        drawCircle(
            color = extras.tankShellStroke,
            radius = boltRadius,
            center = Offset(bx, by)
        )
        drawCircle(
            color = extras.tankSteelSheen.copy(alpha = 0.85f),
            radius = boltRadius * 0.42f,
            center = Offset(bx - boltRadius * 0.2f, by - boltRadius * 0.2f)
        )
    }
    // Small davit handle bar across center cover
    drawLine(
        color = extras.tankShellStroke,
        start = Offset(sideManholeCenter.x - innerCoverR * 0.45f, sideManholeCenter.y),
        end = Offset(sideManholeCenter.x + innerCoverR * 0.45f, sideManholeCenter.y),
        strokeWidth = 1.4.dp.toPx(),
        cap = StrokeCap.Round
    )

    // 5. Conical Roof with Structural Top Rafters Pattern (Pola Rangka / Balok Atap)
    val eaveOverhang = 2.dp.toPx()
    val roofLeft = cylLeft - eaveOverhang
    val roofRight = cylRight + eaveOverhang
    val roofEaveArcDrop = 4.2.dp.toPx()

    val roofPath = Path().apply {
        moveTo(roofLeft, cylTopY)
        lineTo(centerX, roofApexY)
        lineTo(roofRight, cylTopY)
        quadraticTo(centerX, cylTopY + roofEaveArcDrop, roofLeft, cylTopY)
        close()
    }
    drawPath(
        path = roofPath,
        brush = Brush.horizontalGradient(
            0.0f to extras.tankSteelDark,
            0.24f to extras.tankSteelLight,
            0.44f to extras.tankSteelSheen,
            0.70f to extras.tankSteelMid,
            1.0f to extras.tankSteelDark,
            startX = roofLeft,
            endX = roofRight
        )
    )
    // Subtle vertical roof shading for 3D cone depth
    drawPath(
        path = roofPath,
        brush = Brush.verticalGradient(
            0.0f to extras.tankSteelSheen.copy(alpha = 0.45f),
            0.65f to Color.Transparent,
            1.0f to extras.tankSteelDark.copy(alpha = 0.26f),
            startY = roofApexY,
            endY = cylTopY + roofEaveArcDrop
        )
    )

    // Radial Top Rafter Beams (Pola Rangka / Balok Atap) radiating from apex hub to circular eave
    clipPath(roofPath) {
        val rafterFractions = listOf(0.10f, 0.24f, 0.38f, 0.50f, 0.62f, 0.76f, 0.90f)
        for (rf in rafterFractions) {
            val endX = roofLeft + (roofRight - roofLeft) * rf
            val arcLift = (1f - (2f * rf - 1f) * (2f * rf - 1f)) * roofEaveArcDrop
            val endY = cylTopY + arcLift * 0.52f
            // Shadow side of rafter beam
            drawLine(
                color = extras.tankShellStroke.copy(alpha = 0.58f),
                start = Offset(centerX, roofApexY + 1.5.dp.toPx()),
                end = Offset(endX, endY),
                strokeWidth = 1.6.dp.toPx(),
                cap = StrokeCap.Round
            )
            // Highlight edge of rafter beam
            drawLine(
                color = extras.tankSteelSheen.copy(alpha = 0.75f),
                start = Offset(centerX + 0.7.dp.toPx(), roofApexY + 1.5.dp.toPx()),
                end = Offset(endX + 0.8.dp.toPx(), endY),
                strokeWidth = 0.8.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        // Concentric purlin ring midway down the conical roof rafters
        val purlinT = 0.52f
        val purlinLeftX = centerX - (centerX - roofLeft) * purlinT
        val purlinRightX = centerX + (roofRight - centerX) * purlinT
        val purlinY = roofApexY + (cylTopY - roofApexY) * purlinT
        val purlinPath = Path().apply {
            moveTo(purlinLeftX, purlinY)
            quadraticTo(centerX, purlinY + 2.4.dp.toPx(), purlinRightX, purlinY)
        }
        drawPath(
            path = purlinPath,
            color = extras.tankShellStroke.copy(alpha = 0.42f),
            style = Stroke(width = 1.dp.toPx())
        )
    }

    // Crisp roof outline + eave curb ring
    drawPath(
        path = roofPath,
        color = extras.tankShellStroke,
        style = Stroke(width = strokePx, join = StrokeJoin.Round)
    )

    // 6. Roof-Top Fittings:
    // 6a. Pipa Tegak (Vertical Standpipe / Gauge Pipe) on left roof shoulder
    val pipeX = cylLeft + cylWidth * 0.21f
    val pipeSlopeT = ((pipeX - roofLeft) / (centerX - roofLeft)).coerceIn(0f, 1f)
    val pipeBaseY = cylTopY - roofHeight * pipeSlopeT + 1.dp.toPx()
    val pipeTopY = (roofApexY - topFittingRoom * 0.62f).coerceAtLeast(5.dp.toPx())
    val pipeW = (cylWidth * 0.068f).coerceIn(5.dp.toPx(), 8.dp.toPx())

    // Vertical pipe body
    drawRect(
        brush = Brush.horizontalGradient(
            0.0f to extras.tankSteelDark,
            0.40f to extras.tankSteelSheen,
            1.0f to extras.tankSteelDark,
            startX = pipeX - pipeW / 2f,
            endX = pipeX + pipeW / 2f
        ),
        topLeft = Offset(pipeX - pipeW / 2f, pipeTopY),
        size = Size(pipeW, (pipeBaseY - pipeTopY).coerceAtLeast(8.dp.toPx()))
    )
    drawRect(
        color = extras.tankShellStroke,
        topLeft = Offset(pipeX - pipeW / 2f, pipeTopY),
        size = Size(pipeW, (pipeBaseY - pipeTopY).coerceAtLeast(8.dp.toPx())),
        style = Stroke(width = 1.1.dp.toPx())
    )
    // Top pipe cap flange & roof base flange collar
    drawRoundRect(
        color = extras.tankSteelLight,
        topLeft = Offset(pipeX - pipeW * 0.85f, pipeTopY - 2.2.dp.toPx()),
        size = Size(pipeW * 1.7f, 2.8.dp.toPx()),
        cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
    )
    drawRoundRect(
        color = extras.tankShellStroke,
        topLeft = Offset(pipeX - pipeW * 0.85f, pipeTopY - 2.2.dp.toPx()),
        size = Size(pipeW * 1.7f, 2.8.dp.toPx()),
        cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx()),
        style = Stroke(width = 1.dp.toPx())
    )
    drawOval(
        color = extras.tankSteelMid,
        topLeft = Offset(pipeX - pipeW * 0.9f, pipeBaseY - 1.5.dp.toPx()),
        size = Size(pipeW * 1.8f, 3.2.dp.toPx())
    )
    drawOval(
        color = extras.tankShellStroke,
        topLeft = Offset(pipeX - pipeW * 0.9f, pipeBaseY - 1.5.dp.toPx()),
        size = Size(pipeW * 1.8f, 3.2.dp.toPx()),
        style = Stroke(width = 0.9.dp.toPx())
    )

    // 6b. Tutup Manhole Bulat di Sisi Atap (Circular Roof-Side Manhole Hatch) on right shoulder
    val roofMhCx = cylRight - cylWidth * 0.23f
    val roofMhSlopeT = ((roofRight - roofMhCx) / (roofRight - centerX)).coerceIn(0f, 1f)
    val roofMhBaseY = cylTopY - roofHeight * roofMhSlopeT + 1.dp.toPx()
    val roofMhW = (cylWidth * 0.22f).coerceIn(16.dp.toPx(), 24.dp.toPx())
    val roofMhH = roofMhW * 0.44f
    val roofMhNeckH = 3.5.dp.toPx()

    // Short cylindrical neck rising from roof slope
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(extras.tankSteelDark, extras.tankSteelLight, extras.tankSteelDark),
            startX = roofMhCx - roofMhW * 0.42f,
            endX = roofMhCx + roofMhW * 0.42f
        ),
        topLeft = Offset(roofMhCx - roofMhW * 0.42f, roofMhBaseY - roofMhNeckH),
        size = Size(roofMhW * 0.84f, roofMhNeckH + 1.dp.toPx())
    )
    drawRect(
        color = extras.tankShellStroke,
        topLeft = Offset(roofMhCx - roofMhW * 0.42f, roofMhBaseY - roofMhNeckH),
        size = Size(roofMhW * 0.84f, roofMhNeckH + 1.dp.toPx()),
        style = Stroke(width = 1.dp.toPx())
    )
    // Round roof manhole cover lid (elliptical perspective)
    val roofMhLidY = roofMhBaseY - roofMhNeckH - roofMhH * 0.45f
    drawOval(
        brush = Brush.linearGradient(
            colors = listOf(extras.tankSteelSheen, extras.tankSteelMid),
            start = Offset(roofMhCx - roofMhW / 2f, roofMhLidY),
            end = Offset(roofMhCx + roofMhW / 2f, roofMhLidY + roofMhH)
        ),
        topLeft = Offset(roofMhCx - roofMhW / 2f, roofMhLidY),
        size = Size(roofMhW, roofMhH)
    )
    drawOval(
        color = extras.tankShellStroke,
        topLeft = Offset(roofMhCx - roofMhW / 2f, roofMhLidY),
        size = Size(roofMhW, roofMhH),
        style = Stroke(width = 1.1.dp.toPx())
    )
    // Small clamping bar / latch on roof manhole lid
    drawLine(
        color = extras.tankShellStroke,
        start = Offset(roofMhCx - roofMhW * 0.30f, roofMhLidY + roofMhH * 0.5f),
        end = Offset(roofMhCx + roofMhW * 0.30f, roofMhLidY + roofMhH * 0.5f),
        strokeWidth = 1.3.dp.toPx(),
        cap = StrokeCap.Round
    )

    // 6c. Unit Valve / Vent di Bagian Tengah Atas (Central Breather Valve / PV-Vent Assembly)
    val ventNeckW = (cylWidth * 0.085f).coerceIn(6.dp.toPx(), 9.5.dp.toPx())
    val ventNeckH = (topFittingRoom * 0.34f).coerceIn(6.dp.toPx(), 10.dp.toPx())
    val ventBodyW = (cylWidth * 0.21f).coerceIn(15.dp.toPx(), 23.dp.toPx())
    val ventBodyH = (topFittingRoom * 0.34f).coerceIn(7.dp.toPx(), 11.dp.toPx())

    val ventNeckTopY = roofApexY - ventNeckH
    val ventBodyTopY = (ventNeckTopY - ventBodyH).coerceAtLeast(3.dp.toPx())

    // Apex mounting flange collar
    drawOval(
        color = extras.tankSteelLight,
        topLeft = Offset(centerX - ventNeckW * 1.15f, roofApexY - 2.dp.toPx()),
        size = Size(ventNeckW * 2.3f, 4.5.dp.toPx())
    )
    drawOval(
        color = extras.tankShellStroke,
        topLeft = Offset(centerX - ventNeckW * 1.15f, roofApexY - 2.dp.toPx()),
        size = Size(ventNeckW * 2.3f, 4.5.dp.toPx()),
        style = Stroke(width = 1.dp.toPx())
    )

    // Vertical valve riser neck
    drawRect(
        brush = Brush.horizontalGradient(
            0.0f to extras.tankSteelDark,
            0.45f to extras.tankSteelSheen,
            1.0f to extras.tankSteelDark,
            startX = centerX - ventNeckW / 2f,
            endX = centerX + ventNeckW / 2f
        ),
        topLeft = Offset(centerX - ventNeckW / 2f, ventNeckTopY),
        size = Size(ventNeckW, ventNeckH + 1.dp.toPx())
    )
    drawRect(
        color = extras.tankShellStroke,
        topLeft = Offset(centerX - ventNeckW / 2f, ventNeckTopY),
        size = Size(ventNeckW, ventNeckH + 1.dp.toPx()),
        style = Stroke(width = 1.1.dp.toPx())
    )

    // Side pressure-relief branch stub on breather valve
    val stubW = ventBodyW * 0.34f
    val stubH = ventBodyH * 0.42f
    drawRoundRect(
        color = extras.tankSteelMid,
        topLeft = Offset(centerX + ventBodyW * 0.35f, ventBodyTopY + ventBodyH * 0.30f),
        size = Size(stubW, stubH),
        cornerRadius = CornerRadius(1.2.dp.toPx(), 1.2.dp.toPx())
    )
    drawRoundRect(
        color = extras.tankShellStroke,
        topLeft = Offset(centerX + ventBodyW * 0.35f, ventBodyTopY + ventBodyH * 0.30f),
        size = Size(stubW, stubH),
        cornerRadius = CornerRadius(1.2.dp.toPx(), 1.2.dp.toPx()),
        style = Stroke(width = 1.dp.toPx())
    )

    // Main breather valve housing body
    drawRoundRect(
        brush = Brush.horizontalGradient(
            0.0f to extras.tankSteelDark,
            0.35f to extras.tankSteelSheen,
            0.70f to extras.tankSteelLight,
            1.0f to extras.tankSteelDark,
            startX = centerX - ventBodyW / 2f,
            endX = centerX + ventBodyW / 2f
        ),
        topLeft = Offset(centerX - ventBodyW / 2f, ventBodyTopY),
        size = Size(ventBodyW, ventBodyH),
        cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx())
    )
    drawRoundRect(
        color = extras.tankShellStroke,
        topLeft = Offset(centerX - ventBodyW / 2f, ventBodyTopY),
        size = Size(ventBodyW, ventBodyH),
        cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx()),
        style = Stroke(width = 1.2.dp.toPx())
    )

    // Top weather hood cap over the breather valve
    val hoodW = ventBodyW * 1.18f
    val hoodH = 3.2.dp.toPx()
    drawRoundRect(
        color = extras.tankSteelLight,
        topLeft = Offset(centerX - hoodW / 2f, (ventBodyTopY - hoodH * 0.75f).coerceAtLeast(1.5.dp.toPx())),
        size = Size(hoodW, hoodH),
        cornerRadius = CornerRadius(1.8.dp.toPx(), 1.8.dp.toPx())
    )
    drawRoundRect(
        color = extras.tankShellStroke,
        topLeft = Offset(centerX - hoodW / 2f, (ventBodyTopY - hoodH * 0.75f).coerceAtLeast(1.5.dp.toPx())),
        size = Size(hoodW, hoodH),
        cornerRadius = CornerRadius(1.8.dp.toPx(), 1.8.dp.toPx()),
        style = Stroke(width = 1.1.dp.toPx())
    )

    // 7. Right-Side Vertical Calibration Graduation Scale (0%..100%)
    drawCalibrationScale(
        cylRight = cylRight,
        cylTopY = cylTopY,
        scaleBottomY = scaleBottomY,
        liquidSpan = liquidSpan,
        liquidTopY = liquidTopY,
        isOverfill = isOverfill,
        extras = extras,
        colorScheme = colorScheme
    )
}

/**
 * Renders the Preblend tank (T.101, T.111, T.102, T.112, T.121, T.122, etc.):
 * - Elevated structural steel support columns & curved dished bottom head with discharge pipe
 * - Domed roof with perimeter safety railing, round hinged manhole hatch, and curved gooseneck vent
 */
internal fun DrawScope.drawPreblendTank(
    animatedFill: Float,
    isOverfill: Boolean,
    extras: SoundingExtendedColors,
    colorScheme: ColorScheme
) {
    val w = size.width
    val h = size.height

    val scaleMarginRight = w * 0.16f
    val leftPad = w * 0.05f
    val cylLeft = leftPad
    val cylRight = w - scaleMarginRight
    val cylWidth = (cylRight - cylLeft).coerceAtLeast(32f)
    val centerX = cylLeft + cylWidth / 2f

    val topVentRoom = h * 0.15f
    val domeHeight = (cylWidth * 0.24f).coerceIn(18.dp.toPx(), 30.dp.toPx())
    val bottomDishHeight = (cylWidth * 0.22f).coerceIn(16.dp.toPx(), 26.dp.toPx())
    val supportRoom = h * 0.115f

    val cylTopY = topVentRoom + domeHeight
    val domeApexY = topVentRoom
    val cylBottomY = h - supportRoom - bottomDishHeight
    val bottomApexY = cylBottomY + bottomDishHeight
    val floorY = h - 2.dp.toPx()
    val strokePx = 1.8.dp.toPx()

    // 1. Industrial Steel Structural Support Columns & Saddle Cradle
    val colWidth = (cylWidth * 0.105f).coerceIn(7.dp.toPx(), 12.dp.toPx())
    val leftColX = cylLeft + cylWidth * 0.11f
    val rightColX = cylRight - cylWidth * 0.11f - colWidth
    val colTopY = cylBottomY + bottomDishHeight * 0.18f

    // Concrete floor base line
    drawLine(
        color = extras.tankShellStroke.copy(alpha = 0.45f),
        start = Offset(cylLeft - 2.dp.toPx(), floorY),
        end = Offset(cylRight + 2.dp.toPx(), floorY),
        strokeWidth = 2.dp.toPx(),
        cap = StrokeCap.Round
    )

    // Center bottom discharge pipe & flange elbow underneath the curved dish
    val pipeW = (cylWidth * 0.065f).coerceIn(4.dp.toPx(), 7.dp.toPx())
    val pipeBottomY = (bottomApexY + supportRoom * 0.56f).coerceAtMost(floorY - 3.dp.toPx())
    val dischargePath = Path().apply {
        moveTo(centerX, bottomApexY - 2.dp.toPx())
        lineTo(centerX, pipeBottomY)
        lineTo(cylLeft + cylWidth * 0.25f, pipeBottomY)
    }
    drawPath(
        path = dischargePath,
        color = extras.tankSteelDark,
        style = Stroke(width = pipeW + 1.5.dp.toPx(), cap = StrokeCap.Round)
    )
    drawPath(
        path = dischargePath,
        color = extras.tankSteelLight,
        style = Stroke(width = pipeW * 0.72f, cap = StrokeCap.Round)
    )
    drawRoundRect(
        color = extras.tankSteelDark,
        topLeft = Offset(centerX - pipeW * 1.15f, bottomApexY - 1.dp.toPx()),
        size = Size(pipeW * 2.3f, 3.5.dp.toPx()),
        cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
    )

    // Cross-brace tie beam connecting structural columns
    val braceY = bottomApexY + (floorY - bottomApexY) * 0.42f
    drawLine(
        color = extras.tankSupportSteel.copy(alpha = 0.72f),
        start = Offset(leftColX + colWidth * 0.5f, braceY),
        end = Offset(rightColX + colWidth * 0.5f, braceY),
        strokeWidth = 2.5.dp.toPx()
    )

    // Left & Right structural steel columns with angled saddle gussets
    val leftGusset = Path().apply {
        moveTo(leftColX, colTopY - 2.dp.toPx())
        lineTo(leftColX + colWidth * 1.95f, cylBottomY + bottomDishHeight * 0.68f)
        lineTo(leftColX + colWidth, cylBottomY + bottomDishHeight * 0.92f)
        lineTo(leftColX, colTopY + colWidth * 0.8f)
        close()
    }
    val rightGusset = Path().apply {
        moveTo(rightColX + colWidth, colTopY - 2.dp.toPx())
        lineTo(rightColX - colWidth * 0.95f, cylBottomY + bottomDishHeight * 0.68f)
        lineTo(rightColX, cylBottomY + bottomDishHeight * 0.92f)
        lineTo(rightColX + colWidth, colTopY + colWidth * 0.8f)
        close()
    }
    drawPath(path = leftGusset, color = extras.tankSupportSteel.copy(alpha = 0.88f))
    drawPath(path = rightGusset, color = extras.tankSupportSteel.copy(alpha = 0.88f))

    // Vertical blue-steel I-beam columns
    for (colX in listOf(leftColX, rightColX)) {
        drawRect(
            brush = Brush.horizontalGradient(
                0.0f to extras.tankSupportSteel.copy(alpha = 0.78f),
                0.45f to extras.tankSupportSteel,
                1.0f to extras.tankSupportSteel.copy(alpha = 0.68f),
                startX = colX,
                endX = colX + colWidth
            ),
            topLeft = Offset(colX, colTopY),
            size = Size(colWidth, floorY - colTopY - 2.5.dp.toPx())
        )
        drawRect(
            color = extras.tankShellStroke.copy(alpha = 0.75f),
            topLeft = Offset(colX, colTopY),
            size = Size(colWidth, floorY - colTopY - 2.5.dp.toPx()),
            style = Stroke(width = 1.dp.toPx())
        )
        val platePad = 2.5.dp.toPx()
        drawRoundRect(
            color = extras.tankSupportSteel,
            topLeft = Offset(colX - platePad, floorY - 3.5.dp.toPx()),
            size = Size(colWidth + platePad * 2f, 3.5.dp.toPx()),
            cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
        )
    }

    // 2. Top Safety Railing encircling the upper dome rim
    val railHeight = (topVentRoom * 0.66f).coerceIn(11.dp.toPx(), 18.dp.toPx())
    val railTopY = cylTopY - railHeight
    val railMidY = cylTopY - railHeight * 0.50f
    val railLeftX = cylLeft - 1.5.dp.toPx()
    val railRightX = cylRight + 1.5.dp.toPx()
    val railArchLift = 3.5.dp.toPx()

    val postXs = listOf(
        railLeftX + 2.dp.toPx(),
        cylLeft + cylWidth * 0.28f,
        cylRight - cylWidth * 0.28f,
        railRightX - 2.dp.toPx()
    )
    for (px in postXs) {
        val t = ((px - cylLeft) / cylWidth).coerceIn(0f, 1f)
        val archOffset = (1f - (2f * t - 1f) * (2f * t - 1f)) * railArchLift
        drawLine(
            color = extras.tankRailingStroke,
            start = Offset(px, railTopY - archOffset),
            end = Offset(px, cylTopY + 1.dp.toPx()),
            strokeWidth = 2.8.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = extras.tankRailingColor,
            start = Offset(px, railTopY - archOffset),
            end = Offset(px, cylTopY + 1.dp.toPx()),
            strokeWidth = 1.6.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
    for (ry in listOf(railTopY, railMidY)) {
        val railPath = Path().apply {
            moveTo(railLeftX, ry)
            quadraticTo(centerX, ry - railArchLift * 1.45f, railRightX, ry)
        }
        drawPath(
            path = railPath,
            color = extras.tankRailingStroke,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
        drawPath(
            path = railPath,
            color = extras.tankRailingColor,
            style = Stroke(width = 1.7.dp.toPx(), cap = StrokeCap.Round)
        )
    }

    // 3. Curved Gooseneck Vent Pipe at Top Center of Dome
    val ventBaseX = centerX + cylWidth * 0.05f
    val ventTopY = (domeApexY - topVentRoom * 0.72f).coerceAtLeast(4.dp.toPx())
    val ventCurveRadius = (cylWidth * 0.10f).coerceIn(8.dp.toPx(), 13.dp.toPx())
    val ventTubeW = (cylWidth * 0.085f).coerceIn(6.dp.toPx(), 9.5.dp.toPx())
    val gooseneckPath = Path().apply {
        moveTo(ventBaseX, domeApexY + 4.dp.toPx())
        lineTo(ventBaseX, ventTopY + ventCurveRadius)
        cubicTo(
            ventBaseX, ventTopY - 1.dp.toPx(),
            ventBaseX - ventCurveRadius * 2.0f, ventTopY - 1.dp.toPx(),
            ventBaseX - ventCurveRadius * 2.0f, ventTopY + ventCurveRadius * 1.15f
        )
    }
    drawPath(
        path = gooseneckPath,
        color = extras.tankShellStroke,
        style = Stroke(width = ventTubeW + 2.dp.toPx(), cap = StrokeCap.Butt)
    )
    drawPath(
        path = gooseneckPath,
        brush = Brush.horizontalGradient(
            0.0f to extras.tankSteelMid,
            0.45f to extras.tankSteelSheen,
            1.0f to extras.tankSteelDark,
            startX = ventBaseX - ventCurveRadius * 2.2f,
            endX = ventBaseX + ventTubeW
        ),
        style = Stroke(width = ventTubeW, cap = StrokeCap.Butt)
    )
    val nozzleX = ventBaseX - ventCurveRadius * 2.0f
    val nozzleY = ventTopY + ventCurveRadius * 1.15f
    drawLine(
        color = extras.tankShellStroke,
        start = Offset(nozzleX - ventTubeW * 0.68f, nozzleY),
        end = Offset(nozzleX + ventTubeW * 0.68f, nozzleY),
        strokeWidth = 2.4.dp.toPx(),
        cap = StrokeCap.Round
    )

    // 4. Upright Cylindrical Tank Silhouette with Domed Top & Curved Torispherical Bottom
    val tankInteriorPath = Path().apply {
        moveTo(cylLeft, cylTopY)
        cubicTo(
            cylLeft, domeApexY + domeHeight * 0.16f,
            centerX - cylWidth * 0.30f, domeApexY,
            centerX, domeApexY
        )
        cubicTo(
            centerX + cylWidth * 0.30f, domeApexY,
            cylRight, domeApexY + domeHeight * 0.16f,
            cylRight, cylTopY
        )
        lineTo(cylRight, cylBottomY)
        cubicTo(
            cylRight, cylBottomY + bottomDishHeight * 0.78f,
            centerX + cylWidth * 0.30f, bottomApexY,
            centerX, bottomApexY
        )
        cubicTo(
            centerX - cylWidth * 0.30f, bottomApexY,
            cylLeft, cylBottomY + bottomDishHeight * 0.78f,
            cylLeft, cylBottomY
        )
        close()
    }

    drawPath(
        path = tankInteriorPath,
        brush = Brush.horizontalGradient(
            0.0f to extras.tankSteelDark,
            0.16f to extras.tankSteelLight,
            0.30f to extras.tankSteelSheen,
            0.48f to extras.tankSteelLight,
            0.76f to extras.tankSteelMid,
            1.0f to extras.tankSteelDark,
            startX = cylLeft,
            endX = cylRight
        )
    )

    val domePath = Path().apply {
        moveTo(cylLeft, cylTopY)
        cubicTo(
            cylLeft, domeApexY + domeHeight * 0.16f,
            centerX - cylWidth * 0.30f, domeApexY,
            centerX, domeApexY
        )
        cubicTo(
            centerX + cylWidth * 0.30f, domeApexY,
            cylRight, domeApexY + domeHeight * 0.16f,
            cylRight, cylTopY
        )
        quadraticTo(centerX, cylTopY + 5.dp.toPx(), cylLeft, cylTopY)
        close()
    }
    drawPath(
        path = domePath,
        brush = Brush.verticalGradient(
            0.0f to extras.tankSteelSheen.copy(alpha = 0.55f),
            0.65f to Color.Transparent,
            1.0f to extras.tankSteelDark.copy(alpha = 0.25f),
            startY = domeApexY,
            endY = cylTopY + 5.dp.toPx()
        )
    )

    // 5. Bright Yellow Palm Oil Liquid Fill Clipped Inside Tank Silhouette
    val scaleBottomY = bottomApexY - bottomDishHeight * 0.12f
    val liquidSpan = (scaleBottomY - cylTopY).coerceAtLeast(36f)
    val liquidTopY = scaleBottomY - (liquidSpan * animatedFill)

    clipPath(tankInteriorPath) {
        if (animatedFill > 0.004f) {
            val fillColorTop = if (isOverfill) colorScheme.error else extras.oilFillStart
            val fillColorBottom = extras.oilFillEnd

            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(fillColorTop, fillColorBottom),
                    startY = liquidTopY,
                    endY = bottomApexY
                ),
                topLeft = Offset(cylLeft, liquidTopY),
                size = Size(cylWidth, (bottomApexY - liquidTopY).coerceAtLeast(0f))
            )

            drawRect(
                brush = Brush.horizontalGradient(
                    0.0f to Color.Black.copy(alpha = 0.12f),
                    0.18f to Color.White.copy(alpha = 0.10f),
                    0.30f to Color.White.copy(alpha = 0.34f),
                    0.45f to Color.Transparent,
                    0.84f to Color.Black.copy(alpha = 0.06f),
                    1.0f to Color.Black.copy(alpha = 0.18f),
                    startX = cylLeft,
                    endX = cylRight
                ),
                topLeft = Offset(cylLeft, liquidTopY),
                size = Size(cylWidth, (bottomApexY - liquidTopY).coerceAtLeast(0f))
            )

            val meniscusH = (cylWidth * 0.125f).coerceIn(7.dp.toPx(), 13.dp.toPx())
            drawOval(
                color = if (isOverfill) {
                    colorScheme.errorContainer.copy(alpha = 0.85f)
                } else {
                    extras.oilSurfaceHighlight.copy(alpha = 0.92f)
                },
                topLeft = Offset(cylLeft, liquidTopY - meniscusH / 2f),
                size = Size(cylWidth, meniscusH)
            )
            drawOval(
                color = if (isOverfill) colorScheme.error else extras.oilSurfaceRim,
                topLeft = Offset(cylLeft, liquidTopY - meniscusH / 2f),
                size = Size(cylWidth, meniscusH),
                style = Stroke(width = 1.2.dp.toPx())
            )
        }

        val bodyHeight = (cylBottomY - cylTopY).coerceAtLeast(24f)
        val seamYs = listOf(
            cylTopY,
            cylTopY + bodyHeight * 0.34f,
            cylTopY + bodyHeight * 0.68f,
            cylBottomY
        )
        for ((idx, sy) in seamYs.withIndex()) {
            val isKnuckle = idx == 0 || idx == seamYs.lastIndex
            val seamPath = Path().apply {
                moveTo(cylLeft, sy)
                quadraticTo(centerX, sy + (if (isKnuckle) 5.dp.toPx() else 3.5.dp.toPx()), cylRight, sy)
            }
            drawPath(
                path = seamPath,
                color = extras.tankShellStroke.copy(alpha = if (isKnuckle) 0.48f else 0.28f),
                style = Stroke(width = if (isKnuckle) 1.4.dp.toPx() else 1.dp.toPx())
            )
            drawPath(
                path = seamPath,
                color = Color.White.copy(alpha = if (isKnuckle) 0.30f else 0.18f),
                style = Stroke(width = 0.7.dp.toPx())
            )
        }

        val dipBottomY = liquidTopY.coerceIn(cylTopY + 6.dp.toPx(), scaleBottomY - 4.dp.toPx())
        drawLine(
            color = extras.tankShellStroke.copy(alpha = 0.65f),
            start = Offset(centerX, cylTopY),
            end = Offset(centerX, dipBottomY),
            strokeWidth = 1.4.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 4f), 0f)
        )
        val bobSize = 4.5.dp.toPx()
        val bobPath = Path().apply {
            moveTo(centerX - bobSize * 0.7f, dipBottomY - bobSize)
            lineTo(centerX + bobSize * 0.7f, dipBottomY - bobSize)
            lineTo(centerX, dipBottomY + bobSize * 0.35f)
            close()
        }
        drawPath(path = bobPath, color = extras.tankShellStroke)
    }

    // 6. Outer Stainless-Steel Tank Outline
    drawPath(
        path = tankInteriorPath,
        color = extras.tankShellStroke,
        style = Stroke(width = strokePx)
    )

    // 7. Dome Roof Details (Flange Collar + Hinged Manhole Hatch)
    val flangeW = cylWidth * 0.24f
    val flangeH = (flangeW * 0.34f).coerceIn(5.dp.toPx(), 9.dp.toPx())
    drawOval(
        color = extras.tankSteelLight,
        topLeft = Offset(ventBaseX - flangeW / 2f, domeApexY - flangeH * 0.15f),
        size = Size(flangeW, flangeH)
    )
    drawOval(
        color = extras.tankShellStroke,
        topLeft = Offset(ventBaseX - flangeW / 2f, domeApexY - flangeH * 0.15f),
        size = Size(flangeW, flangeH),
        style = Stroke(width = 1.1.dp.toPx())
    )

    val manholeW = (cylWidth * 0.24f).coerceIn(18.dp.toPx(), 28.dp.toPx())
    val manholeH = manholeW * 0.56f
    val manholeCx = cylLeft + cylWidth * 0.29f
    val manholeCy = cylTopY - domeHeight * 0.36f
    drawOval(
        color = extras.tankSteelDark,
        topLeft = Offset(manholeCx - manholeW / 2f, manholeCy - manholeH / 2f + 1.5.dp.toPx()),
        size = Size(manholeW, manholeH)
    )
    drawOval(
        brush = Brush.linearGradient(
            colors = listOf(extras.tankSteelSheen, extras.tankSteelMid),
            start = Offset(manholeCx - manholeW / 2f, manholeCy - manholeH / 2f),
            end = Offset(manholeCx + manholeW / 2f, manholeCy + manholeH / 2f)
        ),
        topLeft = Offset(manholeCx - manholeW / 2f, manholeCy - manholeH / 2f),
        size = Size(manholeW, manholeH)
    )
    drawOval(
        color = extras.tankShellStroke,
        topLeft = Offset(manholeCx - manholeW / 2f, manholeCy - manholeH / 2f),
        size = Size(manholeW, manholeH),
        style = Stroke(width = 1.1.dp.toPx())
    )
    drawLine(
        color = extras.tankShellStroke,
        start = Offset(manholeCx - manholeW * 0.38f, manholeCy + manholeH * 0.18f),
        end = Offset(manholeCx + manholeW * 0.38f, manholeCy - manholeH * 0.22f),
        strokeWidth = 1.6.dp.toPx(),
        cap = StrokeCap.Round
    )
    drawCircle(
        color = extras.tankSteelSheen,
        radius = 2.5.dp.toPx(),
        center = Offset(manholeCx + manholeW * 0.38f, manholeCy - manholeH * 0.22f)
    )
    drawCircle(
        color = extras.tankShellStroke,
        radius = 2.5.dp.toPx(),
        center = Offset(manholeCx + manholeW * 0.38f, manholeCy - manholeH * 0.22f),
        style = Stroke(width = 1.dp.toPx())
    )

    // 8. Right-Side Vertical Calibration Graduation Scale (0%..100%)
    drawCalibrationScale(
        cylRight = cylRight,
        cylTopY = cylTopY,
        scaleBottomY = scaleBottomY,
        liquidSpan = liquidSpan,
        liquidTopY = liquidTopY,
        isOverfill = isOverfill,
        extras = extras,
        colorScheme = colorScheme
    )
}

private fun DrawScope.drawCalibrationScale(
    cylRight: Float,
    cylTopY: Float,
    scaleBottomY: Float,
    liquidSpan: Float,
    liquidTopY: Float,
    isOverfill: Boolean,
    extras: SoundingExtendedColors,
    colorScheme: ColorScheme
) {
    val scaleX = cylRight + 6.dp.toPx()
    drawLine(
        color = colorScheme.outline,
        start = Offset(scaleX, cylTopY),
        end = Offset(scaleX, scaleBottomY),
        strokeWidth = 1.2.dp.toPx(),
        cap = StrokeCap.Round
    )
    val totalSteps = 8
    for (step in 0..totalSteps) {
        val frac = step.toFloat() / totalSteps
        val tickY = scaleBottomY - (liquidSpan * frac)
        val isMajor = step % 2 == 0
        val tickLen = if (isMajor) 8.dp.toPx() else 4.5.dp.toPx()
        drawLine(
            color = if (isMajor) extras.tankShellStroke else colorScheme.outline,
            start = Offset(scaleX, tickY),
            end = Offset(scaleX + tickLen, tickY),
            strokeWidth = if (isMajor) 1.5.dp.toPx() else 1.dp.toPx(),
            cap = StrokeCap.Round
        )
    }

    // Active live level pointer & golden-yellow indicator dot
    drawLine(
        color = extras.tankShellStroke,
        start = Offset(cylRight - 3.dp.toPx(), liquidTopY),
        end = Offset(scaleX + 6.dp.toPx(), liquidTopY),
        strokeWidth = 1.8.dp.toPx(),
        cap = StrokeCap.Round
    )
    drawCircle(
        color = if (isOverfill) colorScheme.error else extras.oilFillEnd,
        radius = 4.2.dp.toPx(),
        center = Offset(scaleX, liquidTopY)
    )
    drawCircle(
        color = if (isOverfill) colorScheme.error else extras.oilSurfaceRim,
        radius = 4.2.dp.toPx(),
        center = Offset(scaleX, liquidTopY),
        style = Stroke(width = 1.dp.toPx())
    )
    drawCircle(
        color = colorScheme.surface,
        radius = 1.6.dp.toPx(),
        center = Offset(scaleX, liquidTopY)
    )
}
