package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.SoundingCalculation
import com.example.ui.theme.MonoTypography
import com.example.ui.theme.SoundingThemeExtras
import com.example.util.SoundingFormatters

@Composable
fun VerticalCylinderTankDiagram(
    calc: SoundingCalculation,
    modifier: Modifier = Modifier
) {
    val animatedFill by animateFloatAsState(
        targetValue = calc.levelFillRatio.coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessLow
        ),
        label = "cylinder_liquid_level"
    )

    val extras = SoundingThemeExtras.colors
    val colorScheme = MaterialTheme.colorScheme
    val isOverfill = calc.levelFillPercent > 100.0
    val percentDisplay = SoundingFormatters.formatFixed(calc.levelFillPercent, 1)
    val highLevelDisplay = SoundingFormatters.formatFixed(calc.highLevel, 2)
    val tempDisplay = SoundingFormatters.formatCompact(calc.temp)

    val accessibilitySummary = stringResource(
        R.string.cd_tank_diagram,
        calc.tankId,
        percentDisplay,
        highLevelDisplay,
        tempDisplay
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("vertical_tank_diagram_card")
            .semantics { contentDescription = accessibilitySummary },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        border = BorderStroke(1.dp, extras.cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Diagram Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.diagram_card_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${calc.tankLabel} • ${stringResource(R.string.diagram_max_height_format, calc.maxTableHeightCm)}",
                        style = MonoTypography.badge,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (isOverfill) extras.warningSurface else extras.leafContainer,
                    border = if (isOverfill) BorderStroke(1.dp, extras.warningBorder) else null
                ) {
                    Text(
                        text = if (isOverfill) {
                            stringResource(R.string.diagram_overfill_badge)
                        } else {
                            stringResource(R.string.gauge_table_label, calc.calTableId)
                        },
                        style = MonoTypography.badge,
                        color = if (isOverfill) extras.warningText else extras.onLeafContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Responsive Body: Upright Cylindrical Tank + Side Telemetry Callouts
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isVeryNarrow = maxWidth < 310.dp
                val tankWidth = when {
                    maxWidth < 340.dp -> 128.dp
                    maxWidth < 440.dp -> 150.dp
                    else -> 172.dp
                }
                val tankHeight = if (maxWidth < 340.dp) 200.dp else 224.dp

                if (isVeryNarrow) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CylinderCanvasWithPercentBadge(
                            animatedFill = animatedFill,
                            percentDisplay = percentDisplay,
                            isOverfill = isOverfill,
                            modifier = Modifier
                                .width(158.dp)
                                .height(206.dp)
                        )
                        TelemetryReadoutsColumn(
                            calc = calc,
                            animatedFill = animatedFill,
                            highLevelDisplay = highLevelDisplay,
                            percentDisplay = percentDisplay,
                            tempDisplay = tempDisplay,
                            isOverfill = isOverfill,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CylinderCanvasWithPercentBadge(
                            animatedFill = animatedFill,
                            percentDisplay = percentDisplay,
                            isOverfill = isOverfill,
                            modifier = Modifier
                                .width(tankWidth)
                                .height(tankHeight)
                        )

                        TelemetryReadoutsColumn(
                            calc = calc,
                            animatedFill = animatedFill,
                            highLevelDisplay = highLevelDisplay,
                            percentDisplay = percentDisplay,
                            tempDisplay = tempDisplay,
                            isOverfill = isOverfill,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CylinderCanvasWithPercentBadge(
    animatedFill: Float,
    percentDisplay: String,
    isOverfill: Boolean,
    modifier: Modifier = Modifier
) {
    val extras = SoundingThemeExtras.colors
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Reserve right margin for calibration scale and top/bottom for gooseneck pipe, railing & support structure
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

            // 1. Industrial Steel Structural Support Columns & Saddle Cradle (matching reference Photo 1)
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
            // Discharge flange collar under dish
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

            // Left & Right structural steel columns with angled saddle gussets cradling the curved bottom
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
                // Flanged anchor base plate on floor
                val platePad = 2.5.dp.toPx()
                drawRoundRect(
                    color = extras.tankSupportSteel,
                    topLeft = Offset(colX - platePad, floorY - 3.5.dp.toPx()),
                    size = Size(colWidth + platePad * 2f, 3.5.dp.toPx()),
                    cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
                )
            }

            // 2. Top Safety Railing encircling the upper dome rim (matching reference Photo 2)
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
            // Vertical railing stanchions
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
            // Top and middle arched perimeter safety rails
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

            // 3. Curved Gooseneck Vent Pipe at Top Center of Dome (matching reference Photo 2)
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
            // Gooseneck metallic outer outline + brushed steel core + top highlight
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
            // Downward nozzle lip at gooseneck opening
            val nozzleX = ventBaseX - ventCurveRadius * 2.0f
            val nozzleY = ventTopY + ventCurveRadius * 1.15f
            drawLine(
                color = extras.tankShellStroke,
                start = Offset(nozzleX - ventTubeW * 0.68f, nozzleY),
                end = Offset(nozzleX + ventTubeW * 0.68f, nozzleY),
                strokeWidth = 2.4.dp.toPx(),
                cap = StrokeCap.Round
            )

            // 4. Real Upright Cylindrical Tank Silhouette with Domed Top & Curved Torispherical Bottom
            val tankInteriorPath = Path().apply {
                moveTo(cylLeft, cylTopY)
                // Domed top roof (smooth elliptical/torispherical dome)
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
                // Right vertical cylinder wall
                lineTo(cylRight, cylBottomY)
                // Curved torispherical / dished bottom head (no flat bottom!)
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

            // Brushed Silvery-Gray Stainless Steel Tank Body Shading
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

            // Soft dome top lighting to accentuate 3D spherical roof curvature
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

            // 5. Bright Yellow Palm Oil Liquid Fill (#FACC15 / #EAB308) clipped cleanly inside tank silhouette
            val scaleBottomY = bottomApexY - bottomDishHeight * 0.12f
            val liquidSpan = (scaleBottomY - cylTopY).coerceAtLeast(36f)
            val liquidTopY = scaleBottomY - (liquidSpan * animatedFill)

            clipPath(tankInteriorPath) {
                if (animatedFill > 0.004f) {
                    val fillColorTop = if (isOverfill) colorScheme.error else extras.oilFillStart
                    val fillColorBottom = if (isOverfill) extras.oilFillEnd else extras.oilFillEnd

                    // Main bright yellow oil volume filling from curved bottom dish up to liquidTopY
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(fillColorTop, fillColorBottom),
                            startY = liquidTopY,
                            endY = bottomApexY
                        ),
                        topLeft = Offset(cylLeft, liquidTopY),
                        size = Size(cylWidth, (bottomApexY - liquidTopY).coerceAtLeast(0f))
                    )

                    // 3D cylindrical specular highlight & right-edge depth over the yellow oil
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

                    // 3D elliptical oil meniscus surface
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

                // Subtle horizontal circumferential weld/stiffener seam rings (matching Photo 1 steel shell bands)
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

                // Vertical sounding dip tape line descending from dome center to yellow oil surface
                val dipBottomY = liquidTopY.coerceIn(cylTopY + 6.dp.toPx(), scaleBottomY - 4.dp.toPx())
                drawLine(
                    color = extras.tankShellStroke.copy(alpha = 0.65f),
                    start = Offset(centerX, cylTopY),
                    end = Offset(centerX, dipBottomY),
                    strokeWidth = 1.4.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 4f), 0f)
                )
                // Plumb bob indicator at oil surface
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

            // 7. Dome Roof Details (matching reference Photo 2):
            // 7a. Central circular flange collar at base of gooseneck vent pipe
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

            // 7b. Round Hinged Manhole Hatch with clamping bar & handwheel on dome shoulder
            val manholeW = (cylWidth * 0.24f).coerceIn(18.dp.toPx(), 28.dp.toPx())
            val manholeH = manholeW * 0.56f
            val manholeCx = cylLeft + cylWidth * 0.29f
            val manholeCy = cylTopY - domeHeight * 0.36f
            // Manhole base collar rim
            drawOval(
                color = extras.tankSteelDark,
                topLeft = Offset(manholeCx - manholeW / 2f, manholeCy - manholeH / 2f + 1.5.dp.toPx()),
                size = Size(manholeW, manholeH)
            )
            // Manhole lid cover
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
            // Manhole cross-bar clamp + mini handwheel knob
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

            // 8. Right-side Vertical Calibration Graduation Scale (0%..100%)
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

        // Crisp high-contrast percentage pill centered over cylinder body
        Surface(
            modifier = Modifier.padding(end = 18.dp, top = 8.dp),
            shape = RoundedCornerShape(999.dp),
            color = extras.elevatedSurface.copy(alpha = 0.94f),
            border = BorderStroke(
                width = 1.2.dp,
                color = if (isOverfill) extras.warningBorder else extras.oilSurfaceRim
            ),
            shadowElevation = 2.dp
        ) {
            Text(
                text = "$percentDisplay%",
                style = MonoTypography.detailValue,
                color = if (isOverfill) extras.warningText else colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun TelemetryReadoutsColumn(
    calc: SoundingCalculation,
    animatedFill: Float,
    highLevelDisplay: String,
    percentDisplay: String,
    tempDisplay: String,
    isOverfill: Boolean,
    modifier: Modifier = Modifier
) {
    val extras = SoundingThemeExtras.colors
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. High Level (cm)
        DiagramMetricTile(
            icon = Icons.Default.Height,
            iconTint = extras.interactiveAccent,
            label = stringResource(R.string.diagram_label_high_level),
            value = "$highLevelDisplay cm",
            subLabel = "/ ${calc.maxTableHeightCm} cm",
            valueTag = "diagram_high_level_value"
        )

        // 2. Persentase Isi (%) + Synchronized Bright Yellow Oil Progress Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = extras.elevatedSurface,
            border = BorderStroke(1.dp, extras.cardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 11.dp, vertical = 9.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Opacity,
                        contentDescription = null,
                        tint = extras.oilSurfaceRim,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.diagram_label_fill_percent),
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = "$percentDisplay%",
                    style = MonoTypography.subMetric,
                    color = if (isOverfill) extras.warningText else colorScheme.onSurface,
                    modifier = Modifier.testTag("diagram_fill_percent_value")
                )
                LinearProgressIndicator(
                    progress = { animatedFill.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = if (isOverfill) colorScheme.error else extras.oilFillEnd,
                    trackColor = colorScheme.surfaceVariant
                )
            }
        }

        // 3. Suhu (°C)
        DiagramMetricTile(
            icon = Icons.Default.DeviceThermostat,
            iconTint = extras.leafAccentReadable,
            label = stringResource(R.string.diagram_label_temp),
            value = "$tempDisplay °C",
            subLabel = stringResource(
                R.string.diagram_ref_temp_format,
                SoundingFormatters.formatCompact(calc.tRef)
            ),
            valueTag = "diagram_temp_value"
        )
    }
}

@Composable
private fun DiagramMetricTile(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    subLabel: String,
    valueTag: String
) {
    val extras = SoundingThemeExtras.colors
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = extras.subtleTealTint,
        border = BorderStroke(1.dp, extras.cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 11.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = value,
                    style = MonoTypography.subMetric,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag(valueTag)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = subLabel,
                    style = MonoTypography.badge,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
