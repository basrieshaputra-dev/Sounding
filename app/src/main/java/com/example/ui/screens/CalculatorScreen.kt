package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.OilBarrel
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.SoundingCalculation
import com.example.ui.SoundingUiState
import com.example.ui.components.ProductPickerBottomSheet
import com.example.ui.components.TankPickerBottomSheet
import com.example.ui.components.VerticalCylinderTankDiagram
import com.example.ui.theme.MonoTypography
import com.example.ui.theme.SaveGradientGreen
import com.example.ui.theme.SoundingThemeExtras
import com.example.util.SoundingFormatters

@Composable
fun CalculatorScreen(
    uiState: SoundingUiState,
    onSelectTank: (Int) -> Unit,
    onSelectProduct: (String) -> Unit,
    onToggleFavoriteProduct: (String) -> Unit,
    onSoundingChanged: (String) -> Unit,
    onTempChanged: (String) -> Unit,
    onDiagramStockKgChanged: (String) -> Unit,
    onDiagramStockKgEditDone: () -> Unit,
    onManualKgChanged: (String) -> Unit,
    onManualNoteChanged: (String) -> Unit,
    onResetInput: () -> Unit,
    onSaveHistory: () -> Unit,
    onCopyWhatsApp: (SoundingCalculation) -> Unit,
    onShareWhatsApp: (SoundingCalculation) -> Unit,
    modifier: Modifier = Modifier
) {
    var showTankPicker by remember { mutableStateOf(false) }
    var showProductPicker by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val extras = SoundingThemeExtras.colors

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Input Parameters Card (Neo-Industrial 20dp Frosted Glass)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = extras.glassCardSurface),
                border = BorderStroke(1.dp, extras.glassBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = if (extras.isDark) 0.dp else 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.section_input_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val hasAnyInput = uiState.soundingInput.isNotEmpty() ||
                            uiState.manualKgInput.isNotEmpty() ||
                            uiState.manualNoteInput.isNotEmpty()
                        if (hasAnyInput) {
                            TextButton(
                                onClick = {
                                    focusManager.clearFocus()
                                    onResetInput()
                                },
                                modifier = Modifier.testTag("reset_input_button"),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = extras.interactiveAccent
                                )
                            ) {
                                Text(
                                    text = stringResource(R.string.reset_input),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }

                    // Tank Picker Trigger
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(R.string.label_tank),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val tank = uiState.selectedTank
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .clickable { showTankPicker = true }
                                .testTag("tank_selector_button"),
                            shape = MaterialTheme.shapes.medium,
                            color = extras.elevatedSurface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OilBarrel,
                                    contentDescription = stringResource(R.string.cd_tank_icon),
                                    tint = extras.interactiveAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = tank?.label ?: "-",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (tank != null) {
                                        Text(
                                            text = if (tank.isManualKg) {
                                                stringResource(R.string.tank_manual_meta)
                                            } else {
                                                stringResource(
                                                    R.string.tank_meta_format,
                                                    SoundingFormatters.formatCompact(tank.tableLevel),
                                                    SoundingFormatters.formatCompact(tank.tRef),
                                                    tank.capacityTon
                                                )
                                            },
                                            style = MonoTypography.badge,
                                            color = if (tank.isManualKg) {
                                                extras.leafAccentReadable
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = stringResource(R.string.cd_dropdown),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Product Picker Trigger
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(R.string.label_product),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .clickable { showProductPicker = true }
                                .testTag("product_selector_button"),
                            shape = MaterialTheme.shapes.medium,
                            color = extras.elevatedSurface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WaterDrop,
                                    contentDescription = null,
                                    tint = extras.leafAccentReadable,
                                    modifier = Modifier.size(20.dp)
                                )
                                val hasProduct = uiState.selectedProduct.isNotEmpty()
                                Text(
                                    text = if (hasProduct) {
                                        uiState.selectedProduct
                                    } else {
                                        stringResource(R.string.placeholder_select_product)
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (hasProduct) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = stringResource(R.string.cd_dropdown),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    val fieldColors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = extras.leafAccent,
                        focusedLabelColor = extras.leafAccentReadable,
                        cursorColor = extras.leafAccentReadable,
                        focusedContainerColor = extras.elevatedSurface,
                        unfocusedContainerColor = extras.elevatedSurface
                    )

                    if (uiState.isManualTankSelected) {
                        // Manual Stock (kg) input for non-calibrated tank (T.50)
                        OutlinedTextField(
                            value = uiState.manualKgInput,
                            onValueChange = onManualKgChanged,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("manual_kg_input"),
                            colors = fieldColors,
                            label = {
                                Text(
                                    text = stringResource(R.string.label_manual_kg),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.placeholder_manual_kg),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            suffix = {
                                Text(
                                    text = "kg",
                                    style = MonoTypography.badge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingIcon = if (uiState.manualKgInput.isNotEmpty()) {
                                {
                                    IconButton(
                                        onClick = { onManualKgChanged("") },
                                        modifier = Modifier.testTag("clear_manual_kg_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = stringResource(R.string.cd_clear_search),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            } else null,
                            textStyle = MonoTypography.subMetric,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next
                            ),
                            shape = MaterialTheme.shapes.medium
                        )

                        OutlinedTextField(
                            value = uiState.manualNoteInput,
                            onValueChange = onManualNoteChanged,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("manual_note_input"),
                            colors = fieldColors,
                            label = {
                                Text(
                                    text = stringResource(R.string.label_manual_note),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.placeholder_manual_note),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            trailingIcon = if (uiState.manualNoteInput.isNotEmpty()) {
                                {
                                    IconButton(
                                        onClick = { onManualNoteChanged("") },
                                        modifier = Modifier.testTag("clear_manual_note_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = stringResource(R.string.cd_clear_search),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            } else null,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { focusManager.clearFocus() }
                            ),
                            shape = MaterialTheme.shapes.medium
                        )
                    } else {
                        // Sounding Level & Temp side-by-side inputs
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            OutlinedTextField(
                                value = uiState.soundingInput,
                                onValueChange = onSoundingChanged,
                                modifier = Modifier
                                    .weight(1.4f)
                                    .testTag("sounding_input"),
                                colors = fieldColors,
                                label = {
                                    Text(
                                        text = stringResource(R.string.label_sounding),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                placeholder = {
                                    Text(
                                        text = stringResource(R.string.placeholder_sounding),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                suffix = {
                                    Text(
                                        text = "cm",
                                        style = MonoTypography.badge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                trailingIcon = if (uiState.soundingInput.isNotEmpty()) {
                                    {
                                        IconButton(
                                            onClick = { onSoundingChanged("") },
                                            modifier = Modifier.testTag("clear_sounding_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = stringResource(R.string.cd_clear_search),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                } else null,
                                textStyle = MonoTypography.subMetric,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                    imeAction = ImeAction.Next
                                ),
                                shape = MaterialTheme.shapes.medium
                            )

                            OutlinedTextField(
                                value = uiState.tempInput,
                                onValueChange = onTempChanged,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("temp_input"),
                                colors = fieldColors,
                                label = {
                                    Text(
                                        text = stringResource(R.string.label_temp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                placeholder = { Text(stringResource(R.string.placeholder_temp)) },
                                suffix = {
                                    Text(
                                        text = "°C",
                                        style = MonoTypography.badge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                textStyle = MonoTypography.subMetric,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { focusManager.clearFocus() }
                                ),
                                shape = MaterialTheme.shapes.medium
                            )
                        }
                    }
                }
            }

            // Calibration / Temperature Warning Banner
            val calc = uiState.calculation
            AnimatedVisibility(
                visible = calc != null && calc.warnings.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                if (calc != null && calc.warnings.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("warning_banner"),
                        shape = MaterialTheme.shapes.medium,
                        color = extras.warningSurface,
                        border = BorderStroke(1.dp, extras.warningBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = stringResource(R.string.cd_warning_icon),
                                tint = extras.warningText,
                                modifier = Modifier
                                    .padding(top = 1.dp)
                                    .size(20.dp)
                            )
                            Text(
                                text = calc.warnings.joinToString(" "),
                                style = MaterialTheme.typography.bodyMedium,
                                color = extras.warningText
                            )
                        }
                    }
                }
            }

            // Live Upright Cylinder Tank Diagram + Result Card + Actions OR Empty Hint
            AnimatedVisibility(
                visible = calc != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                if (calc != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (!calc.isManualKg) {
                            VerticalCylinderTankDiagram(
                                calc = calc,
                                stockKgEditInput = uiState.diagramStockKgInput,
                                onStockKgChanged = onDiagramStockKgChanged,
                                onStockKgEditDone = onDiagramStockKgEditDone
                            )
                        }

                        CalculationResultCard(calc = calc)

                        val savePillShape = RoundedCornerShape(999.dp)
                        val saveGradientBrush = Brush.horizontalGradient(
                            colors = listOf(SaveGradientGreen, extras.limeAccent, extras.primaryCtaEnd)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    focusManager.clearFocus()
                                    onSaveHistory()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 52.dp)
                                    .clip(savePillShape)
                                    .background(saveGradientBrush, savePillShape)
                                    .testTag("save_history_button"),
                                shape = savePillShape,
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = extras.onLimeButton
                                ),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 0.dp,
                                    pressedElevation = 0.dp
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BookmarkAdd,
                                    contentDescription = stringResource(R.string.cd_save_icon),
                                    modifier = Modifier.size(19.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.action_save_history),
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            OutlinedButton(
                                onClick = { onCopyWhatsApp(calc) },
                                modifier = Modifier
                                    .heightIn(min = 52.dp)
                                    .testTag("copy_wa_button"),
                                shape = savePillShape,
                                border = BorderStroke(1.2.dp, extras.interactiveAccent),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = extras.glassCardSurface,
                                    contentColor = extras.interactiveAccent
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = stringResource(R.string.cd_copy_icon),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.action_copy_wa),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }

                            FilledTonalIconButton(
                                onClick = { onShareWhatsApp(calc) },
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(52.dp)
                                    .testTag("share_wa_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = stringResource(R.string.cd_share_icon)
                                )
                            }
                        }
                    }
                }
            }

            if (calc == null) {
                EmptyCalculationCard(
                    isManualMode = uiState.isManualTankSelected,
                    needsProductOnly = !uiState.isManualTankSelected &&
                        uiState.selectedProduct.isEmpty() &&
                        uiState.soundingInput.isNotEmpty()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (showTankPicker) {
        TankPickerBottomSheet(
            tanks = uiState.tanks,
            selectedTankId = uiState.selectedTank?.id,
            onSelect = onSelectTank,
            onDismiss = { showTankPicker = false },
            currentTankStocks = uiState.currentTankStocks
        )
    }

    if (showProductPicker) {
        ProductPickerBottomSheet(
            products = uiState.products,
            favoriteProducts = uiState.favoriteProducts,
            selectedProduct = uiState.selectedProduct,
            onSelect = onSelectProduct,
            onToggleFavorite = onToggleFavoriteProduct,
            onDismiss = { showProductPicker = false }
        )
    }
}

@Composable
private fun CalculationResultCard(calc: SoundingCalculation) {
    val extras = SoundingThemeExtras.colors

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("result_card"),
        shape = RoundedCornerShape(20.dp),
        color = extras.resultSurface,
        border = BorderStroke(1.2.dp, extras.resultBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.result_card_title).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = extras.metricValueInk,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            // Primary Hero Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (calc.isManualKg) {
                    MetricBox(
                        label = stringResource(R.string.metric_stock_kg),
                        value = "${SoundingFormatters.formatIndonesianInteger(calc.stockKg)} kg",
                        modifier = Modifier
                            .weight(1f)
                            .testTag("metric_stock_kg")
                    )
                    MetricBox(
                        label = stringResource(R.string.metric_stock_ton),
                        value = "${SoundingFormatters.formatTon(calc.stockTon)} Ton",
                        modifier = Modifier
                            .weight(1f)
                            .testTag("metric_stock_ton")
                    )
                } else {
                    MetricBox(
                        label = stringResource(R.string.metric_total_volume),
                        value = "${SoundingFormatters.formatIndonesianNumber(calc.totalVol)} L",
                        modifier = Modifier
                            .weight(1f)
                            .testTag("metric_total_volume")
                    )
                    MetricBox(
                        label = stringResource(R.string.metric_stock_kg),
                        value = "${SoundingFormatters.formatIndonesianInteger(calc.stockKg)} kg",
                        modifier = Modifier
                            .weight(1f)
                            .testTag("metric_stock_kg")
                    )
                }
            }

            HorizontalDivider(color = extras.metricCardBorder, thickness = 1.dp)

            // Technical Breakdown Rows
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (calc.isManualKg) {
                    DetailRow(
                        label = stringResource(R.string.detail_input_mode),
                        value = stringResource(R.string.detail_input_mode_manual),
                        emphasize = true
                    )
                    DetailRow(
                        label = stringResource(R.string.detail_product_label),
                        value = calc.product.ifBlank { "-" }
                    )
                    if (calc.note.isNotBlank()) {
                        DetailRow(
                            label = stringResource(R.string.detail_note_label),
                            value = calc.note
                        )
                    }
                } else {
                    val soundingStr = SoundingFormatters.formatCompact(calc.sounding)
                    val tableStr = SoundingFormatters.formatCompact(calc.tableLevel)
                    val highFormula = if (calc.tableLevel < 0) {
                        "$soundingStr - ${SoundingFormatters.formatCompact(-calc.tableLevel)}"
                    } else {
                        "$soundingStr + $tableStr"
                    }

                    DetailRow(
                        label = stringResource(R.string.detail_high_level),
                        value = "${SoundingFormatters.formatFixed(calc.highLevel, 2)} cm ($highFormula)"
                    )
                    DetailRow(
                        label = stringResource(R.string.detail_table_vol, calc.baseHeight),
                        value = "${SoundingFormatters.formatIndonesianNumber(calc.baseVol)} L"
                    )
                    DetailRow(
                        label = stringResource(
                            R.string.detail_fraction,
                            SoundingFormatters.formatFixed(calc.fractionCm, 2),
                            SoundingFormatters.formatCompact(calc.diffPerCm)
                        ),
                        value = "+${SoundingFormatters.formatFixed(calc.fractionVol, 2)} L"
                    )
                    DetailRow(
                        label = stringResource(R.string.detail_total_vol),
                        value = "${SoundingFormatters.formatFixed(calc.totalVol, 2)} L",
                        emphasize = true
                    )
                    DetailRow(
                        label = stringResource(
                            R.string.detail_density,
                            SoundingFormatters.formatCompact(calc.temp)
                        ),
                        value = SoundingFormatters.formatFixed(calc.density, 4)
                    )
                    DetailRow(
                        label = stringResource(
                            R.string.detail_vcf,
                            SoundingFormatters.formatCompact(calc.tRef)
                        ),
                        value = SoundingFormatters.formatFixed(calc.vcf, 7)
                    )
                    DetailRow(
                        label = stringResource(R.string.metric_stock_ton),
                        value = "${SoundingFormatters.formatTon(calc.stockTon)} Ton"
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val extras = SoundingThemeExtras.colors
    val metricTextStyle = when {
        value.length >= 13 -> MonoTypography.heroMetric.copy(fontSize = 16.sp, lineHeight = 22.sp)
        value.length >= 11 -> MonoTypography.heroMetric.copy(fontSize = 18.sp, lineHeight = 24.sp)
        else -> MonoTypography.heroMetric
    }
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = extras.metricCardSurface,
        border = BorderStroke(1.dp, extras.metricCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = metricTextStyle,
                color = extras.metricValueInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    emphasize: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (emphasize) {
                MaterialTheme.typography.labelMedium
            } else {
                MaterialTheme.typography.bodySmall
            },
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = if (emphasize) 1f else 0.82f),
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MonoTypography.detailValue,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun EmptyCalculationCard(
    isManualMode: Boolean = false,
    needsProductOnly: Boolean = false
) {
    val extras = SoundingThemeExtras.colors
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("empty_calculation_hint"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = extras.glassCardSurface),
        border = BorderStroke(1.dp, extras.glassBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = extras.leafContainer,
                modifier = Modifier.size(52.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Straighten,
                    contentDescription = null,
                    tint = extras.leafAccentReadable,
                    modifier = Modifier.padding(13.dp)
                )
            }
            Text(
                text = stringResource(R.string.empty_calc_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(
                    when {
                        isManualMode -> R.string.empty_manual_hint
                        needsProductOnly -> R.string.empty_calc_need_product
                        else -> R.string.empty_calc_hint
                    }
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
