package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.firebase.ShiftReportCloudDoc
import com.example.data.local.SoundingLogEntity
import com.example.data.model.DraftSubTab
import com.example.data.model.ShiftDraftEngine
import com.example.data.model.ShiftReportDraft
import com.example.data.model.TankGroup
import com.example.data.model.TankShiftDraftRow
import com.example.data.model.TransferLogItem
import com.example.ui.DateFilterOption
import com.example.ui.FILTER_ALL
import com.example.ui.components.DraftDetailTableSection
import com.example.ui.components.TankEditFocus
import com.example.ui.components.TankShiftEditBottomSheet
import com.example.ui.components.TransferRowEditDialog
import com.example.ui.theme.BrandDeepTeal
import com.example.ui.theme.BrandLeafGreen
import com.example.ui.theme.MonoTypography
import com.example.ui.theme.SoundingThemeExtras
import com.example.util.SoundingFormatters

@Composable
fun DraftReportScreen(
    draft: ShiftReportDraft,
    historyLogs: List<SoundingLogEntity>,
    cloudShiftReports: List<ShiftReportCloudDoc> = emptyList(),
    availableDraftDates: List<DateFilterOption> = emptyList(),
    availableDraftAccounts: List<String> = emptyList(),
    selectedDraftDateFilter: String = FILTER_ALL,
    selectedDraftAccountFilter: String = FILTER_ALL,
    isViewingCloudReport: Boolean = false,
    onSelectDraftDateFilter: (String) -> Unit = {},
    onSelectDraftAccountFilter: (String) -> Unit = {},
    onOpenCloudShiftReport: (ShiftReportCloudDoc) -> Unit = {},
    onReturnToActiveDraft: () -> Unit = {},
    onSaveDraftToCloud: () -> Unit = {},
    onDateChanged: (String) -> Unit,
    onOperatorChanged: (String) -> Unit,
    onNbChanged: (String) -> Unit,
    onUpdateTankRow: (TankShiftDraftRow) -> Unit,
    onSaveTransferRow: (id: Long?, jam: String, keterangan: String, qtyKg: String) -> Unit,
    onDeleteTransferRow: (Long) -> Unit,
    onSyncFromHistory: () -> Unit,
    onResetDraft: () -> Unit,
    onOpenExportDraftJpg: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extras = SoundingThemeExtras.colors
    var selectedSubTab by remember { mutableStateOf(DraftSubTab.SUMMARY_TF) }
    var editingTankTarget by remember { mutableStateOf<Pair<TankShiftDraftRow, TankEditFocus>?>(null) }
    var editingTransferDialog by remember { mutableStateOf<Pair<Boolean, TransferLogItem?>>(false to null) }
    var showResetConfirm by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("draft_report_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 820.dp)
        ) {
            // =================================================================
            // 1. HEADER IDENTITAS LAPORAN (Always visible above both sub-tabs)
            // =================================================================
            ReportIdentityStickyHeader(
                draft = draft,
                isViewingCloudReport = isViewingCloudReport,
                onDateChanged = onDateChanged,
                onOperatorChanged = onOperatorChanged,
                onSyncFromHistory = onSyncFromHistory,
                onSaveToCloud = onSaveDraftToCloud,
                onReturnToActiveDraft = onReturnToActiveDraft,
                onRequestReset = { showResetConfirm = true },
                onExportJpg = onOpenExportDraftJpg
            )

            // 2 Sub-Tab Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    DraftSubTab.SUMMARY_TF to stringResource(R.string.draft_subtab_summary),
                    DraftSubTab.DETAIL_TABLE to stringResource(R.string.draft_subtab_detail)
                ).forEach { (tab, label) ->
                    val selected = selectedSubTab == tab
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .clickable { selectedSubTab = tab }
                            .testTag(
                                if (tab == DraftSubTab.SUMMARY_TF) "draft_subtab_summary" else "draft_subtab_detail"
                            ),
                        shape = MaterialTheme.shapes.medium,
                        color = if (selected) BrandDeepTeal else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) BrandDeepTeal else extras.cardBorder
                        ),
                        shadowElevation = if (selected) 2.dp else 0.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // =================================================================
            // Sub-Tab Body Content
            // =================================================================
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "cloud_report_monitor") {
                    CloudReportMonitorCard(
                        cloudReports = cloudShiftReports,
                        availableDates = availableDraftDates,
                        availableAccounts = availableDraftAccounts,
                        selectedDateKey = selectedDraftDateFilter,
                        selectedAccount = selectedDraftAccountFilter,
                        onSelectDate = onSelectDraftDateFilter,
                        onSelectAccount = onSelectDraftAccountFilter,
                        onOpenReport = onOpenCloudShiftReport
                    )
                }

                when (selectedSubTab) {
                    DraftSubTab.SUMMARY_TF -> {
                        item(key = "stok_awal") {
                            StokAwalCard(
                                rows = draft.tankRows,
                                onEditTank = { row ->
                                    editingTankTarget = row to TankEditFocus.S_AWAL
                                }
                            )
                        }
                        item(key = "transfers") {
                            DraftTransferCard(
                                transfers = draft.transfers,
                                onAdd = { editingTransferDialog = true to null },
                                onEdit = { item -> editingTransferDialog = true to item },
                                onDelete = onDeleteTransferRow
                            )
                        }
                        item(key = "nb_note") {
                            DraftNbCard(
                                nbNote = draft.nbNote,
                                onNbChanged = onNbChanged
                            )
                        }
                        item(key = "stok_akhir") {
                            StokAkhirCard(
                                rows = draft.tankRows,
                                onEditTank = { row ->
                                    editingTankTarget = row to TankEditFocus.S_AKHIR
                                }
                            )
                        }
                    }

                    DraftSubTab.DETAIL_TABLE -> {
                        item(key = "detail_table") {
                            DraftDetailTableSection(
                                rows = draft.tankRows,
                                onEditTank = { row, focus ->
                                    editingTankTarget = row to focus
                                }
                            )
                        }
                    }
                }

                // Bottom Export Draft (.JPG) CTA
                item(key = "export_draft_cta") {
                    Button(
                        onClick = onOpenExportDraftJpg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 14.dp)
                            .heightIn(min = 52.dp)
                            .testTag("draft_export_jpg_button"),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandLeafGreen,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Image,
                            contentDescription = null,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.draft_action_export_jpg),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }

    // Tank S. Awal / S. Akhir / Detail Editor Sheet
    editingTankTarget?.let { (targetRow, focus) ->
        val freshRow = draft.tankRows.firstOrNull { it.tankId == targetRow.tankId } ?: targetRow
        val tankLogs = remember(historyLogs, freshRow.tankId) {
            historyLogs.filter { it.tankId == freshRow.tankId }
        }
        TankShiftEditBottomSheet(
            row = freshRow,
            initialFocus = focus,
            tankHistoryLogs = tankLogs,
            onSave = { updated ->
                onUpdateTankRow(updated)
                editingTankTarget = null
            },
            onDismiss = { editingTankTarget = null }
        )
    }

    // Transfer Add / Edit Dialog
    if (editingTransferDialog.first) {
        TransferRowEditDialog(
            initialItem = editingTransferDialog.second,
            onSave = { jam, ket, qty ->
                onSaveTransferRow(editingTransferDialog.second?.id, jam, ket, qty)
                editingTransferDialog = false to null
            },
            onDismiss = { editingTransferDialog = false to null }
        )
    }

    // Reset Draft Confirmation
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text(stringResource(R.string.draft_reset_confirm_title)) },
            text = { Text(stringResource(R.string.draft_reset_confirm_msg)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetConfirm = false
                        onResetDraft()
                    }
                ) {
                    Text(
                        text = stringResource(R.string.draft_action_reset),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }
}

@Composable
private fun ReportIdentityStickyHeader(
    draft: ShiftReportDraft,
    isViewingCloudReport: Boolean,
    onDateChanged: (String) -> Unit,
    onOperatorChanged: (String) -> Unit,
    onSyncFromHistory: () -> Unit,
    onSaveToCloud: () -> Unit,
    onReturnToActiveDraft: () -> Unit,
    onRequestReset: () -> Unit,
    onExportJpg: () -> Unit
) {
    val extras = SoundingThemeExtras.colors
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = extras.leafAccent,
        focusedLabelColor = extras.leafAccentReadable,
        cursorColor = extras.leafAccentReadable,
        focusedContainerColor = extras.elevatedSurface,
        unfocusedContainerColor = extras.elevatedSurface
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp)
            .testTag("draft_identity_header"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, extras.cardBorder),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isViewingCloudReport) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    color = extras.leafContainer,
                    border = BorderStroke(1.dp, extras.leafAccent.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CloudDone,
                                contentDescription = null,
                                tint = extras.onLeafContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            val detailMeta = listOf(
                                draft.operatorName.ifBlank { "-" },
                                draft.timeLabel.trim()
                            ).filter { it.isNotEmpty() }.joinToString(" • ")
                            Text(
                                text = stringResource(
                                    R.string.draft_viewing_other_banner,
                                    draft.dateLabel.ifBlank { "-" },
                                    detailMeta
                                ),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = extras.onLeafContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        TextButton(
                            onClick = onReturnToActiveDraft,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("draft_return_active_button")
                        ) {
                            Text(
                                text = stringResource(R.string.draft_action_back_today),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = extras.interactiveAccent
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.draft_header_identity_title),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = extras.interactiveAccent
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onSyncFromHistory,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("draft_sync_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Sync,
                            contentDescription = stringResource(R.string.cd_sync_history),
                            modifier = Modifier.size(15.dp),
                            tint = extras.leafAccentReadable
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = stringResource(R.string.draft_action_sync_history),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = extras.leafAccentReadable
                        )
                    }
                    IconButton(
                        onClick = onSaveToCloud,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("draft_save_cloud_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CloudUpload,
                            contentDescription = stringResource(R.string.draft_action_save_cloud),
                            tint = extras.leafAccentReadable,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    IconButton(
                        onClick = onRequestReset,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("draft_reset_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.RestartAlt,
                            contentDescription = stringResource(R.string.draft_action_reset),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    IconButton(
                        onClick = onExportJpg,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("draft_header_export_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Image,
                            contentDescription = stringResource(R.string.draft_action_export_jpg),
                            tint = extras.interactiveAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Hari & Tanggal + Operator side by side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = draft.dateLabel,
                    onValueChange = onDateChanged,
                    modifier = Modifier
                        .weight(1.1f)
                        .testTag("draft_date_input"),
                    colors = fieldColors,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    label = { Text(stringResource(R.string.label_report_date), style = MaterialTheme.typography.labelSmall) },
                    shape = MaterialTheme.shapes.small
                )
                OutlinedTextField(
                    value = draft.operatorName,
                    onValueChange = onOperatorChanged,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("draft_operator_input"),
                    colors = fieldColors,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    label = { Text(stringResource(R.string.draft_label_operator), style = MaterialTheme.typography.labelSmall) },
                    placeholder = { Text(stringResource(R.string.draft_placeholder_operator), style = MaterialTheme.typography.labelSmall) },
                    shape = MaterialTheme.shapes.small
                )
            }
        }
    }
}

@Composable
private fun StokAwalCard(
    rows: List<TankShiftDraftRow>,
    onEditTank: (TankShiftDraftRow) -> Unit
) {
    val extras = SoundingThemeExtras.colors
    val preblend = rows.filter { it.group == TankGroup.PREBLEND }
    val tower = rows.filter { it.group == TankGroup.TOWERTANK }
    val maxCount = maxOf(preblend.size, tower.size)
    val totalAwalKg = ShiftDraftEngine.totalInitialKg(rows)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("draft_stok_awal_card"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, extras.cardBorder),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.draft_section_stok_awal),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.draft_section_stok_awal_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${SoundingFormatters.formatIndonesianInteger(totalAwalKg)} kg",
                    style = MonoTypography.subMetric.copy(fontWeight = FontWeight.Bold),
                    color = extras.interactiveAccent
                )
            }

            // Column group labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "PREBLEND",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = extras.interactiveAccent,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "TOWERTANK & T.50",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = extras.leafAccentReadable,
                    modifier = Modifier.weight(1f)
                )
            }

            for (i in 0 until maxCount) {
                val left = preblend.getOrNull(i)
                val right = tower.getOrNull(i)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (left != null) {
                        StokAwalCell(
                            row = left,
                            accentColor = extras.interactiveAccent,
                            onClick = { onEditTank(left) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    if (right != null) {
                        StokAwalCell(
                            row = right,
                            accentColor = extras.leafAccentReadable,
                            onClick = { onEditTank(right) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun StokAwalCell(
    row: TankShiftDraftRow,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extras = SoundingThemeExtras.colors
    val qtyText = ShiftDraftEngine.formatQuantityDisplay(row.sAwal.quantityKg)
    Surface(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .testTag("draft_awal_${row.tankId}"),
        shape = MaterialTheme.shapes.small,
        color = extras.elevatedSurface,
        border = BorderStroke(1.dp, extras.cardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = row.tankId,
                style = MonoTypography.badge.copy(fontWeight = FontWeight.Bold),
                color = accentColor
            )
            Text(
                text = qtyText,
                style = MonoTypography.subMetric.copy(fontSize = 13.5.sp, fontWeight = FontWeight.Bold),
                color = if (qtyText == "-") MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DraftTransferCard(
    transfers: List<TransferLogItem>,
    onAdd: () -> Unit,
    onEdit: (TransferLogItem) -> Unit,
    onDelete: (Long) -> Unit
) {
    val extras = SoundingThemeExtras.colors

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("draft_transfers_card"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, extras.cardBorder),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.draft_section_tf),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = onAdd,
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .testTag("draft_add_transfer_button"),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    border = BorderStroke(1.dp, extras.leafAccent),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = extras.leafAccentReadable)
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.draft_action_add_tf), style = MaterialTheme.typography.labelMedium)
                }
            }

            if (transfers.isEmpty()) {
                Text(
                    text = stringResource(R.string.draft_tf_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            } else {
                transfers.forEachIndexed { idx, tf ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("draft_tf_row_$idx"),
                        shape = MaterialTheme.shapes.small,
                        color = extras.elevatedSurface,
                        border = BorderStroke(1.dp, extras.cardBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 10.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = tf.jam.ifBlank { "-" },
                                        style = MonoTypography.badge.copy(fontWeight = FontWeight.Bold),
                                        color = extras.interactiveAccent
                                    )
                                    val qtyDisplay = tf.qtyKg.trim().let { raw ->
                                        if (raw.isEmpty()) "-" else "${ShiftDraftEngine.formatQuantityDisplay(raw)} kg"
                                    }
                                    Text(
                                        text = "Qty: $qtyDisplay",
                                        style = MonoTypography.badge.copy(fontWeight = FontWeight.Bold),
                                        color = extras.leafAccentReadable
                                    )
                                }
                                Text(
                                    text = tf.keterangan.ifBlank { "-" },
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(
                                onClick = { onEdit(tf) },
                                modifier = Modifier.testTag("draft_tf_edit_$idx")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = stringResource(R.string.cd_edit_item),
                                    tint = extras.interactiveAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { onDelete(tf.id) },
                                modifier = Modifier.testTag("draft_tf_delete_$idx")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteOutline,
                                    contentDescription = stringResource(R.string.cd_delete_item),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DraftNbCard(
    nbNote: String,
    onNbChanged: (String) -> Unit
) {
    val extras = SoundingThemeExtras.colors
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("draft_nb_card"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, extras.cardBorder),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.draft_section_nb),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            OutlinedTextField(
                value = nbNote,
                onValueChange = onNbChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("draft_nb_input"),
                minLines = 2,
                maxLines = 4,
                placeholder = { Text(stringResource(R.string.draft_placeholder_nb)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = extras.leafAccent,
                    focusedLabelColor = extras.leafAccentReadable,
                    cursorColor = extras.leafAccentReadable,
                    focusedContainerColor = extras.elevatedSurface,
                    unfocusedContainerColor = extras.elevatedSurface
                ),
                shape = MaterialTheme.shapes.medium
            )
        }
    }
}

@Composable
private fun StokAkhirCard(
    rows: List<TankShiftDraftRow>,
    onEditTank: (TankShiftDraftRow) -> Unit
) {
    val extras = SoundingThemeExtras.colors
    val totalAkhirKg = ShiftDraftEngine.totalFinalKg(rows)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("draft_stok_akhir_card"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, extras.cardBorder),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.draft_section_stok_akhir),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.draft_section_stok_akhir_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${SoundingFormatters.formatIndonesianInteger(totalAkhirKg)} kg",
                        style = MonoTypography.subMetric.copy(fontWeight = FontWeight.Bold),
                        color = extras.leafAccentReadable
                    )
                    Text(
                        text = "${SoundingFormatters.formatTon(totalAkhirKg / 1000.0)} Ton",
                        style = MonoTypography.badge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            rows.forEachIndexed { idx, row ->
                val qtyLabel = ShiftDraftEngine.formatQuantityDisplay(row.sAkhir.quantityKg)
                val accent = if (row.group == TankGroup.PREBLEND) extras.interactiveAccent else extras.leafAccentReadable
                val descParts = listOf(row.oilType.trim(), row.summaryNote.trim()).filter { it.isNotEmpty() }
                val descText = descParts.joinToString(" • ").ifEmpty { "-" }

                if (idx > 0) HorizontalDivider(color = extras.cardBorder.copy(alpha = 0.7f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable { onEditTank(row) }
                        .padding(vertical = 8.dp, horizontal = 4.dp)
                        .testTag("draft_akhir_${row.tankId}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(7.dp),
                        color = extras.elevatedSurface,
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = row.tankId,
                            style = MonoTypography.badge.copy(fontWeight = FontWeight.Bold),
                            color = accent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = qtyLabel,
                        style = MonoTypography.subMetric.copy(fontWeight = FontWeight.Bold),
                        color = if (qtyLabel == "-") MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.width(86.dp)
                    )

                    Text(
                        text = descText,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.cd_edit_item),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CloudReportMonitorCard(
    cloudReports: List<ShiftReportCloudDoc>,
    availableDates: List<DateFilterOption>,
    availableAccounts: List<String>,
    selectedDateKey: String,
    selectedAccount: String,
    onSelectDate: (String) -> Unit,
    onSelectAccount: (String) -> Unit,
    onOpenReport: (ShiftReportCloudDoc) -> Unit
) {
    val extras = SoundingThemeExtras.colors
    var expanded by remember { mutableStateOf(true) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("draft_cloud_monitor_card"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, extras.cardBorder),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Groups,
                        contentDescription = null,
                        tint = extras.interactiveAccent,
                        modifier = Modifier.size(19.dp)
                    )
                    Text(
                        text = stringResource(R.string.draft_cloud_monitor_title),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = extras.leafContainer
                    ) {
                        Text(
                            text = cloudReports.size.toString(),
                            style = MonoTypography.badge,
                            color = extras.onLeafContainer,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (expanded) {
                // Date filter chips
                if (availableDates.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DraftFilterChip(
                            label = stringResource(R.string.filter_date_all),
                            selected = selectedDateKey == FILTER_ALL,
                            usePrimaryFill = true,
                            onClick = { onSelectDate(FILTER_ALL) },
                            testTag = "draft_monitor_date_all"
                        )
                        availableDates.forEach { opt ->
                            DraftFilterChip(
                                label = opt.label,
                                selected = selectedDateKey == opt.dateKey,
                                usePrimaryFill = true,
                                onClick = { onSelectDate(opt.dateKey) },
                                testTag = "draft_monitor_date_${opt.dateKey}"
                            )
                        }
                    }
                }

                // Account filter chips
                if (availableAccounts.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DraftFilterChip(
                            label = stringResource(R.string.filter_account_all),
                            selected = selectedAccount == FILTER_ALL,
                            usePrimaryFill = false,
                            onClick = { onSelectAccount(FILTER_ALL) },
                            testTag = "draft_monitor_account_all"
                        )
                        availableAccounts.forEach { account ->
                            DraftFilterChip(
                                label = account,
                                selected = selectedAccount.equals(account, ignoreCase = true),
                                usePrimaryFill = false,
                                onClick = { onSelectAccount(account) },
                                testTag = "draft_monitor_account_$account"
                            )
                        }
                    }
                }

                if (cloudReports.isEmpty()) {
                    Text(
                        text = stringResource(R.string.draft_cloud_monitor_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        items(
                            items = cloudReports,
                            key = { it.id.ifBlank { "${it.dateKey}_${it.displayAccountName}_${it.updatedAtEpochMs}" } }
                        ) { report ->
                            val timeText = report.timeLabel.ifBlank {
                                if (report.updatedAtEpochMs > 0L) {
                                    SoundingFormatters.formatLocalTime(report.updatedAtEpochMs)
                                } else {
                                    ""
                                }
                            }
                            Surface(
                                modifier = Modifier
                                    .width(214.dp)
                                    .clickable { onOpenReport(report) }
                                    .testTag("cloud_report_${report.id}"),
                                shape = MaterialTheme.shapes.medium,
                                color = extras.elevatedSurface,
                                border = BorderStroke(1.dp, extras.cardBorder)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = extras.leafContainer
                                        ) {
                                            Text(
                                                text = report.displayAccountName.ifBlank { "-" },
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = extras.onLeafContainer,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        if (timeText.isNotEmpty()) {
                                            Text(
                                                text = timeText,
                                                style = MonoTypography.badge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Text(
                                        text = report.dateLabel.ifBlank { report.dateKey },
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Text(
                                        text = "Akhir: ${SoundingFormatters.formatIndonesianInteger(report.totalAkhirKg)} kg • ${report.transferCount} TF",
                                        style = MonoTypography.badge,
                                        color = extras.interactiveAccent,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DraftFilterChip(
    label: String,
    selected: Boolean,
    usePrimaryFill: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val extras = SoundingThemeExtras.colors
    val bgColor = when {
        selected && usePrimaryFill -> extras.executiveActionSurface
        selected -> extras.leafContainer
        else -> extras.elevatedSurface
    }
    val borderColor = when {
        selected && usePrimaryFill -> extras.executiveActionSurface
        selected -> extras.leafAccent
        else -> extras.cardBorder
    }
    val textColor = when {
        selected && usePrimaryFill -> Color.White
        selected -> extras.onLeafContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier
            .heightIn(min = 34.dp)
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(999.dp),
        color = bgColor,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = borderColor
        )
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = textColor
            )
        }
    }
}
