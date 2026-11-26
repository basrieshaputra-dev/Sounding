package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.CalibrationEngine
import com.example.data.local.SoundingLogEntity
import com.example.data.model.ShiftDraftEngine
import com.example.data.model.SoundingPointDraft
import com.example.data.model.TankShiftDraftRow
import com.example.data.model.TransferLogItem
import com.example.ui.theme.BrandDeepTeal
import com.example.ui.theme.BrandLeafGreen
import com.example.ui.theme.MonoTypography
import com.example.ui.theme.SoundingThemeExtras
import com.example.util.SoundingFormatters

enum class TankEditFocus {
    S_AWAL,
    S_AKHIR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TankShiftEditBottomSheet(
    row: TankShiftDraftRow,
    initialFocus: TankEditFocus,
    tankHistoryLogs: List<SoundingLogEntity>,
    onSave: (TankShiftDraftRow) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val extras = SoundingThemeExtras.colors

    var activeTab by remember(row.tankId, initialFocus) { mutableStateOf(initialFocus) }
    var oilType by remember(row.tankId) { mutableStateOf(row.oilType) }
    var summaryNote by remember(row.tankId) { mutableStateOf(row.summaryNote) }
    var detailNote by remember(row.tankId) { mutableStateOf(row.detailNote) }
    var sAwal by remember(row.tankId) { mutableStateOf(row.sAwal) }
    var sAkhir by remember(row.tankId) { mutableStateOf(row.sAkhir) }
    var showHistoryPicker by remember { mutableStateOf(false) }

    val activePoint = if (activeTab == TankEditFocus.S_AWAL) sAwal else sAkhir
    fun updateActivePoint(transform: (SoundingPointDraft) -> SoundingPointDraft) {
        if (activeTab == TankEditFocus.S_AWAL) {
            sAwal = transform(sAwal)
        } else {
            sAkhir = transform(sAkhir)
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = extras.leafAccent,
        focusedLabelColor = extras.leafAccentReadable,
        cursorColor = extras.leafAccentReadable,
        focusedContainerColor = extras.elevatedSurface,
        unfocusedContainerColor = extras.elevatedSurface
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.draft_edit_tank_title, row.tankId),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_tank_edit_sheet")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.cd_close)
                    )
                }
            }

            // S. Awal / S. Akhir Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    TankEditFocus.S_AWAL to stringResource(R.string.draft_edit_tab_awal),
                    TankEditFocus.S_AKHIR to stringResource(R.string.draft_edit_tab_akhir)
                ).forEach { (focus, label) ->
                    val selected = activeTab == focus
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .clickable { activeTab = focus }
                            .testTag("draft_edit_mode_${focus.name.lowercase()}"),
                        shape = MaterialTheme.shapes.medium,
                        color = if (selected) extras.leafContainer else extras.elevatedSurface,
                        border = BorderStroke(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) extras.leafAccent else MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = if (selected) extras.onLeafContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Quick Actions: Fill from History | Isi Empty | Isi -
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (tankHistoryLogs.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { showHistoryPicker = !showHistoryPicker },
                        modifier = Modifier
                            .weight(1.3f)
                            .heightIn(min = 48.dp)
                            .testTag("draft_pick_history_button"),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        border = BorderStroke(1.dp, extras.interactiveAccent),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = extras.interactiveAccent)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.History,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.draft_edit_pick_history, tankHistoryLogs.size),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        updateActivePoint {
                            SoundingPointDraft(
                                soundingCm = "-",
                                tableLevelCm = "-",
                                highLevelCm = "-",
                                volumeL = "-",
                                vcf = "-",
                                density = "-",
                                tempC = "-",
                                quantityKg = "Empty"
                            )
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("draft_set_empty_button"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Text(stringResource(R.string.draft_edit_set_empty), style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick = {
                        updateActivePoint {
                            SoundingPointDraft(
                                soundingCm = "-",
                                tableLevelCm = "-",
                                highLevelCm = "-",
                                volumeL = "-",
                                vcf = "-",
                                density = "-",
                                tempC = "-",
                                quantityKg = "-"
                            )
                        }
                    },
                    modifier = Modifier
                        .weight(0.75f)
                        .heightIn(min = 48.dp)
                        .testTag("draft_set_dash_button"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Text(stringResource(R.string.draft_edit_set_dash), style = MaterialTheme.typography.labelMedium)
                }
            }

            // Expandable History Picker for this tank
            if (showHistoryPicker && tankHistoryLogs.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = extras.elevatedSurface,
                    border = BorderStroke(1.dp, extras.interactiveAccent.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tankHistoryLogs.forEach { log ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .clickable {
                                        val pt = ShiftDraftEngine.toSoundingPoint(log)
                                        updateActivePoint { pt }
                                        val normProd = CalibrationEngine.normalizeProductName(log.product)
                                        if (normProd.isNotBlank()) oilType = normProd
                                        if (log.note.isNotBlank() && summaryNote.isBlank()) {
                                            summaryNote = log.note.trim()
                                        }
                                        showHistoryPicker = false
                                    },
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, extras.cardBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${SoundingFormatters.formatIndonesianInteger(log.stockKg)} kg • ${CalibrationEngine.normalizeProductName(log.product)}",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        val meta = if (log.createdAtEpochMs > 0L) {
                                            SoundingFormatters.formatDisplayDateTime(log.createdAtEpochMs)
                                        } else {
                                            log.timestampIso
                                        }
                                        Text(
                                            text = meta,
                                            style = MonoTypography.badge,
                                            color = extras.interactiveAccent
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = extras.leafAccentReadable,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Primary Stock & Oil fields
            OutlinedTextField(
                value = activePoint.quantityKg,
                onValueChange = { v -> updateActivePoint { it.copy(quantityKg = v) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("draft_edit_qty_input"),
                colors = fieldColors,
                singleLine = true,
                label = { Text(stringResource(R.string.draft_edit_qty)) },
                shape = MaterialTheme.shapes.medium
            )

            OutlinedTextField(
                value = oilType,
                onValueChange = { oilType = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("draft_edit_oil_input"),
                colors = fieldColors,
                singleLine = true,
                label = { Text(stringResource(R.string.draft_edit_oil_type)) },
                shape = MaterialTheme.shapes.medium
            )

            // Telemetry rows for Detail Table
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = activePoint.soundingCm,
                    onValueChange = { v -> updateActivePoint { it.copy(soundingCm = v) } },
                    modifier = Modifier.weight(1f),
                    colors = fieldColors,
                    singleLine = true,
                    label = { Text(stringResource(R.string.draft_edit_sondingan)) },
                    shape = MaterialTheme.shapes.small
                )
                OutlinedTextField(
                    value = activePoint.tableLevelCm,
                    onValueChange = { v -> updateActivePoint { it.copy(tableLevelCm = v) } },
                    modifier = Modifier.weight(1f),
                    colors = fieldColors,
                    singleLine = true,
                    label = { Text(stringResource(R.string.draft_edit_mu)) },
                    shape = MaterialTheme.shapes.small
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = activePoint.highLevelCm,
                    onValueChange = { v -> updateActivePoint { it.copy(highLevelCm = v) } },
                    modifier = Modifier.weight(1f),
                    colors = fieldColors,
                    singleLine = true,
                    label = { Text(stringResource(R.string.draft_edit_jml_mu)) },
                    shape = MaterialTheme.shapes.small
                )
                OutlinedTextField(
                    value = activePoint.volumeL,
                    onValueChange = { v -> updateActivePoint { it.copy(volumeL = v) } },
                    modifier = Modifier.weight(1f),
                    colors = fieldColors,
                    singleLine = true,
                    label = { Text(stringResource(R.string.draft_edit_volume)) },
                    shape = MaterialTheme.shapes.small
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = activePoint.vcf,
                    onValueChange = { v -> updateActivePoint { it.copy(vcf = v) } },
                    modifier = Modifier.weight(1f),
                    colors = fieldColors,
                    singleLine = true,
                    label = { Text(stringResource(R.string.draft_edit_vcf)) },
                    shape = MaterialTheme.shapes.small
                )
                OutlinedTextField(
                    value = activePoint.density,
                    onValueChange = { v -> updateActivePoint { it.copy(density = v) } },
                    modifier = Modifier.weight(1f),
                    colors = fieldColors,
                    singleLine = true,
                    label = { Text(stringResource(R.string.draft_edit_density)) },
                    shape = MaterialTheme.shapes.small
                )
                OutlinedTextField(
                    value = activePoint.tempC,
                    onValueChange = { v -> updateActivePoint { it.copy(tempC = v) } },
                    modifier = Modifier.weight(0.75f),
                    colors = fieldColors,
                    singleLine = true,
                    label = { Text(stringResource(R.string.draft_edit_temp)) },
                    shape = MaterialTheme.shapes.small
                )
            }

            HorizontalDivider(color = extras.cardBorder)

            OutlinedTextField(
                value = summaryNote,
                onValueChange = { summaryNote = it },
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors,
                singleLine = true,
                label = { Text(stringResource(R.string.draft_edit_summary_note)) },
                placeholder = { Text(stringResource(R.string.draft_edit_summary_note_hint)) },
                shape = MaterialTheme.shapes.medium
            )

            OutlinedTextField(
                value = detailNote,
                onValueChange = { detailNote = it },
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors,
                singleLine = true,
                label = { Text(stringResource(R.string.draft_edit_detail_note)) },
                placeholder = { Text(stringResource(R.string.draft_edit_detail_note_hint)) },
                shape = MaterialTheme.shapes.medium
            )

            Button(
                onClick = {
                    onSave(
                        row.copy(
                            oilType = oilType.trim(),
                            sAwal = sAwal,
                            sAkhir = sAkhir,
                            summaryNote = summaryNote.trim(),
                            detailNote = detailNote.trim(),
                            isManuallyEdited = true
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .heightIn(min = 52.dp)
                    .testTag("draft_save_tank_button"),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandLeafGreen,
                    contentColor = Color.White
                )
            ) {
                Icon(imageVector = Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.draft_action_save),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
fun TransferRowEditDialog(
    initialItem: TransferLogItem?,
    onSave: (jam: String, keterangan: String, qtyKg: String) -> Unit,
    onDismiss: () -> Unit
) {
    val extras = SoundingThemeExtras.colors
    var jam by remember(initialItem?.id) { mutableStateOf(initialItem?.jam.orEmpty()) }
    var keterangan by remember(initialItem?.id) { mutableStateOf(initialItem?.keterangan.orEmpty()) }
    var qtyKg by remember(initialItem?.id) { mutableStateOf(initialItem?.qtyKg.orEmpty()) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = extras.leafAccent,
        focusedLabelColor = extras.leafAccentReadable,
        cursorColor = extras.leafAccentReadable
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(
                    if (initialItem == null) R.string.draft_tf_dialog_add_title else R.string.draft_tf_dialog_edit_title
                ),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = jam,
                    onValueChange = { jam = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("draft_tf_jam_input"),
                    colors = fieldColors,
                    singleLine = true,
                    label = { Text(stringResource(R.string.draft_tf_label_jam)) },
                    placeholder = { Text(stringResource(R.string.draft_tf_hint_jam)) }
                )
                OutlinedTextField(
                    value = keterangan,
                    onValueChange = { keterangan = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("draft_tf_desc_input"),
                    colors = fieldColors,
                    singleLine = true,
                    label = { Text(stringResource(R.string.draft_tf_label_desc)) },
                    placeholder = { Text(stringResource(R.string.draft_tf_hint_desc)) }
                )
                OutlinedTextField(
                    value = qtyKg,
                    onValueChange = { qtyKg = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("draft_tf_qty_input"),
                    colors = fieldColors,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    label = { Text(stringResource(R.string.draft_tf_label_qty)) },
                    placeholder = { Text(stringResource(R.string.draft_tf_hint_qty)) }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(jam.trim(), keterangan.trim(), qtyKg.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = BrandLeafGreen, contentColor = Color.White),
                modifier = Modifier.testTag("draft_tf_save_button")
            ) {
                Text(stringResource(R.string.draft_action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportDraftJpgBottomSheet(
    onShareJpg: () -> Unit,
    onDownloadJpg: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val extras = SoundingThemeExtras.colors

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.draft_export_sheet_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Filled.Close, contentDescription = stringResource(R.string.cd_close))
                }
            }

            Text(
                text = stringResource(R.string.draft_export_sheet_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onShareJpg,
                    modifier = Modifier
                        .weight(1.15f)
                        .heightIn(min = 52.dp)
                        .testTag("confirm_share_draft_jpg"),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandDeepTeal, contentColor = Color.White)
                ) {
                    Icon(imageVector = Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.action_share_jpg_wa), style = MaterialTheme.typography.labelLarge)
                }

                OutlinedButton(
                    onClick = onDownloadJpg,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp)
                        .testTag("confirm_download_draft_jpg"),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.2.dp, extras.interactiveAccent),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = extras.interactiveAccent)
                ) {
                    Icon(imageVector = Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.action_download_jpg), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
