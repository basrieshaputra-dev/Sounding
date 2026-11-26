package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
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
    stockKgEditInput: String?,
    onStockKgChanged: (String) -> Unit,
    onStockKgEditDone: () -> Unit,
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
    val tableVolDisplay = SoundingFormatters.formatIndonesianNumber(calc.baseVol)
    val stockKgDisplay = SoundingFormatters.formatIndonesianInteger(calc.stockKg)
    val tempDisplay = SoundingFormatters.formatCompact(calc.temp)

    val accessibilitySummary = stringResource(
        R.string.cd_tank_diagram,
        calc.tankId,
        tableVolDisplay,
        stockKgDisplay,
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
                        CylinderCanvasWithTankBadge(
                            animatedFill = animatedFill,
                            tankIdLabel = calc.tankId,
                            isOverfill = isOverfill,
                            modifier = Modifier
                                .width(158.dp)
                                .height(206.dp)
                        )
                        TelemetryReadoutsColumn(
                            calc = calc,
                            tableVolDisplay = tableVolDisplay,
                            stockKgEditInput = stockKgEditInput,
                            onStockKgChanged = onStockKgChanged,
                            onStockKgEditDone = onStockKgEditDone,
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
                        CylinderCanvasWithTankBadge(
                            animatedFill = animatedFill,
                            tankIdLabel = calc.tankId,
                            isOverfill = isOverfill,
                            modifier = Modifier
                                .width(tankWidth)
                                .height(tankHeight)
                        )

                        TelemetryReadoutsColumn(
                            calc = calc,
                            tableVolDisplay = tableVolDisplay,
                            stockKgEditInput = stockKgEditInput,
                            onStockKgChanged = onStockKgChanged,
                            onStockKgEditDone = onStockKgEditDone,
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
private fun CylinderCanvasWithTankBadge(
    animatedFill: Float,
    tankIdLabel: String,
    isOverfill: Boolean,
    modifier: Modifier = Modifier
) {
    val extras = SoundingThemeExtras.colors
    val colorScheme = MaterialTheme.colorScheme
    val towerStyle = isTowerTank(tankIdLabel)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag(if (towerStyle) "tank_canvas_tower" else "tank_canvas_preblend")
        ) {
            if (towerStyle) {
                drawTowerTank(
                    animatedFill = animatedFill,
                    isOverfill = isOverfill,
                    extras = extras,
                    colorScheme = colorScheme
                )
            } else {
                drawPreblendTank(
                    animatedFill = animatedFill,
                    isOverfill = isOverfill,
                    extras = extras,
                    colorScheme = colorScheme
                )
            }
        }

        // Crisp high-contrast active tank number pill centered over cylinder body
        Surface(
            modifier = Modifier
                .padding(end = 18.dp, top = 8.dp)
                .testTag("diagram_tank_id_badge"),
            shape = RoundedCornerShape(999.dp),
            color = extras.elevatedSurface.copy(alpha = 0.94f),
            border = BorderStroke(
                width = 1.2.dp,
                color = if (isOverfill) extras.warningBorder else extras.oilSurfaceRim
            ),
            shadowElevation = 2.dp
        ) {
            Text(
                text = tankIdLabel,
                style = MonoTypography.detailValue,
                color = if (isOverfill) extras.warningText else colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun TelemetryReadoutsColumn(
    calc: SoundingCalculation,
    tableVolDisplay: String,
    stockKgEditInput: String?,
    onStockKgChanged: (String) -> Unit,
    onStockKgEditDone: () -> Unit,
    tempDisplay: String,
    isOverfill: Boolean,
    modifier: Modifier = Modifier
) {
    val extras = SoundingThemeExtras.colors

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Volume Tabel (L)
        DiagramMetricTile(
            icon = Icons.Default.Opacity,
            iconTint = extras.interactiveAccent,
            label = stringResource(R.string.diagram_label_table_vol),
            value = "$tableVolDisplay L",
            subLabel = "${calc.baseHeight} cm",
            valueTag = "diagram_table_vol_value"
        )

        // 2. Editable Total Stock (kg) synced bi-directionally with Sounding Level
        EditableTotalStockTile(
            stockKg = calc.stockKg,
            stockKgEditInput = stockKgEditInput,
            onStockKgChanged = onStockKgChanged,
            onStockKgEditDone = onStockKgEditDone,
            isOverfill = isOverfill
        )

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
private fun EditableTotalStockTile(
    stockKg: Long,
    stockKgEditInput: String?,
    onStockKgChanged: (String) -> Unit,
    onStockKgEditDone: () -> Unit,
    isOverfill: Boolean
) {
    val extras = SoundingThemeExtras.colors
    val colorScheme = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }
    var isFocused by remember { mutableStateOf(false) }
    var wasFocused by remember { mutableStateOf(false) }

    val displayDigits = stockKgEditInput ?: stockKg.coerceAtLeast(0L).toString()
    val thousandsTransformation = remember { IndonesianThousandsVisualTransformation() }
    val editCd = stringResource(R.string.cd_edit_total_stock)

    val borderColor = when {
        isFocused -> extras.leafAccent
        isOverfill -> extras.warningBorder
        else -> extras.interactiveAccent.copy(alpha = 0.55f)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { focusRequester.requestFocus() }
            ),
        shape = MaterialTheme.shapes.medium,
        color = extras.elevatedSurface,
        border = BorderStroke(if (isFocused) 1.6.dp else 1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 11.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Scale,
                    contentDescription = null,
                    tint = extras.oilSurfaceRim,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stringResource(R.string.diagram_label_total_stock),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isFocused) extras.leafAccentReadable else colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = editCd,
                    tint = if (isFocused) extras.leafAccentReadable else extras.interactiveAccent,
                    modifier = Modifier.size(13.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 28.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                BasicTextField(
                    value = displayDigits,
                    onValueChange = onStockKgChanged,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            isFocused = focusState.isFocused
                            if (focusState.isFocused) {
                                wasFocused = true
                            } else if (wasFocused) {
                                wasFocused = false
                                onStockKgEditDone()
                            }
                        }
                        .semantics { contentDescription = editCd }
                        .testTag("diagram_total_stock_input"),
                    textStyle = MonoTypography.subMetric.copy(
                        color = if (isOverfill) extras.warningText else colorScheme.onSurface
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(extras.leafAccentReadable),
                    visualTransformation = thousandsTransformation,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            onStockKgEditDone()
                        }
                    ),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (displayDigits.isEmpty()) {
                                Text(
                                    text = "0",
                                    style = MonoTypography.subMetric,
                                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                )
                            }
                            innerTextField()
                        }
                    }
                )
                Text(
                    text = "kg",
                    style = MonoTypography.badge,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private class IndonesianThousandsVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (raw.isEmpty()) {
            return TransformedText(AnnotatedString(""), OffsetMapping.Identity)
        }

        val sb = StringBuilder(raw.length + raw.length / 3)
        val origToTrans = IntArray(raw.length + 1)
        val transToOrigList = ArrayList<Int>(raw.length + raw.length / 3 + 1)
        transToOrigList.add(0)

        for (i in raw.indices) {
            if (i > 0 && (raw.length - i) % 3 == 0) {
                sb.append('.')
                transToOrigList.add(i)
            }
            origToTrans[i] = sb.length
            sb.append(raw[i])
            transToOrigList.add(i + 1)
        }
        origToTrans[raw.length] = sb.length

        val transToOrig = transToOrigList.toIntArray()
        val transformed = sb.toString()

        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return origToTrans[offset.coerceIn(0, raw.length)]
            }

            override fun transformedToOriginal(offset: Int): Int {
                return transToOrig[offset.coerceIn(0, transformed.length)]
            }
        }
        return TransformedText(AnnotatedString(transformed), mapping)
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
