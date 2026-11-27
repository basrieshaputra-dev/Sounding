package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import com.example.ui.theme.MonoTypography
import com.example.ui.theme.SoundingThemeExtras
import com.example.util.SoundingFormatters

private val FIELD_GRID_PREBLEND_ORDER = listOf("T.101", "T.111", "T.121", "T.102", "T.112", "T.122")
private val FIELD_GRID_TOWERTANK_ORDER = listOf("T.171", "T.172", "T.173", "T.174", "T.176", "T.177", "T.50")

private val DEFAULT_TANK_CAPACITIES_TON = mapOf(
    "T.101" to 30,
    "T.111" to 30,
    "T.121" to 30,
    "T.102" to 30,
    "T.112" to 30,
    "T.122" to 30,
    "T.171" to 60,
    "T.172" to 60,
    "T.173" to 60,
    "T.174" to 60,
    "T.176" to 60,
    "T.177" to 60,
    "T.50" to 50
)

private val DraftCardShape = RoundedCornerShape(20.dp)
private val FieldTankCellShape = RoundedCornerShape(16.dp)
private val DraftCtaShape = RoundedCornerShape(16.dp)

@Composable
fun DraftReportScreen(
    draft: ShiftReportDraft,
    historyLogs: List<SoundingLogEntity>,
    tankCapacitiesTon: Map<String, Int> = emptyMap(),
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

    val resolvedCapacities = remember(tankCapacitiesTon) {
        if (tankCapacitiesTon.isEmpty()) DEFAULT_TANK_CAPACITIES_TON
        else DEFAULT_TANK_CAPACITIES_TON + tankCapacitiesTon
    }

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

            // 2. Segmented Sub-Tab Switcher (Sage & Warm Sand pill track)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                color = extras.innerContainer,
                border = BorderStroke(1.dp, extras.cardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        DraftSubTab.SUMMARY_TF to stringResource(R.string.draft_subtab_summary),
                        DraftSubTab.DETAIL_TABLE to stringResource(R.string.draft_subtab_detail)
                    ).forEach { (tab, label) ->
                        val selected = selectedSubTab == tab
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedSubTab = tab }
                                .testTag(
                                    if (tab == DraftSubTab.SUMMARY_TF) "draft_subtab_summary" else "draft_subtab_detail"
                                ),
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) extras.sagePrimary else Color.Transparent,
                            shadowElevation = if (selected && !extras.isDark) 1.dp else 0.dp
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
                                    color = if (selected) {
                                        extras.onSagePrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
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
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
                                capacitiesTon = resolvedCapacities,
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
                                capacitiesTon = resolvedCapacities,
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

                // Bottom Export Draft (.JPG) CTA — 52dp height, 16dp radius, SagePrimary
                item(key = "export_draft_cta") {
                    Button(
                        onClick = onOpenExportDraftJpg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 16.dp)
                            .heightIn(min = 52.dp)
                            .testTag("draft_export_jpg_button"),
                        shape = DraftCtaShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = extras.sagePrimary,
                            contentColor = extras.onSagePrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = if (extras.isDark) 0.dp else 2.dp
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Image,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
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
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        focusedBorderColor = extras.emeraldAccent,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedLabelColor = extras.emeraldAccent,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        cursorColor = extras.emeraldAccent,
        focusedContainerColor = extras.innerContainer,
        unfocusedContainerColor = extras.innerContainer
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 10.dp)
            .testTag("draft_identity_header"),
        shape = DraftCardShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, extras.cardBorder),
        shadowElevation = if (extras.isDark) 0.dp else 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isViewingCloudReport) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = extras.sageContainer
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
                                tint = extras.onSageContainer,
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
                                color = extras.onSageContainer,
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
                                color = extras.sagePrimary
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
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onSyncFromHistory,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("draft_sync_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Sync,
                            contentDescription = stringResource(R.string.cd_sync_history),
                            modifier = Modifier.size(15.dp),
                            tint = extras.sagePrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.draft_action_sync_history),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = extras.sagePrimary
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
                            tint = extras.sagePrimary,
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
                            tint = extras.sagePrimary,
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
                    shape = RoundedCornerShape(14.dp)
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
                    shape = RoundedCornerShape(14.dp)
                )
            }
        }
    }
}

private fun orderFieldGroupRows(
    rows: List<TankShiftDraftRow>,
    group: TankGroup,
    preferredOrder: List<String>
): List<TankShiftDraftRow> {
    val groupRows = rows.filter { it.group == group }
    val orderIndex = preferredOrder.withIndex().associate { it.value to it.index }
    return groupRows.sortedBy { orderIndex[it.tankId] ?: Int.MAX_VALUE }
}

private fun parseTankStockKg(rawQty: String): Long? {
    val trimmed = rawQty.trim()
    if (trimmed.isEmpty() || trimmed == "-" || trimmed.equals("empty", ignoreCase = true)) {
        return null
    }
    return ShiftDraftEngine.parseQuantityKg(trimmed)
}

@Composable
private fun StokAwalCard(
    rows: List<TankShiftDraftRow>,
    capacitiesTon: Map<String, Int>,
    onEditTank: (TankShiftDraftRow) -> Unit
) {
    val extras = SoundingThemeExtras.colors
    val preblend = remember(rows) { orderFieldGroupRows(rows, TankGroup.PREBLEND, FIELD_GRID_PREBLEND_ORDER) }
    val tower = remember(rows) { orderFieldGroupRows(rows, TankGroup.TOWERTANK, FIELD_GRID_TOWERTANK_ORDER) }
    val maxCount = maxOf(preblend.size, tower.size)
    val totalAwalKg = ShiftDraftEngine.totalInitialKg(rows)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("draft_stok_awal_card"),
        shape = DraftCardShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, extras.cardBorder),
        shadowElevation = if (extras.isDark) 0.dp else 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${SoundingFormatters.formatIndonesianInteger(totalAwalKg)} kg",
                        style = MonoTypography.subMetric.copy(fontWeight = FontWeight.Bold),
                        color = extras.amberTextReadable
                    )
                    Text(
                        text = "${SoundingFormatters.formatTon(totalAwalKg / 1000.0)} Ton",
                        style = MonoTypography.fieldTon,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            DualColumnFieldGridHeader()

            for (i in 0 until maxCount) {
                val left = preblend.getOrNull(i)
                val right = tower.getOrNull(i)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (left != null) {
                        FieldTankStockCell(
                            row = left,
                            rawQuantity = left.sAwal.quantityKg,
                            capacityTon = capacitiesTon[left.tankId] ?: 30,
                            showProductLabel = left.oilType.isNotBlank(),
                            testTag = "draft_awal_${left.tankId}",
                            onClick = { onEditTank(left) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    if (right != null) {
                        FieldTankStockCell(
                            row = right,
                            rawQuantity = right.sAwal.quantityKg,
                            capacityTon = capacitiesTon[right.tankId] ?: 60,
                            showProductLabel = right.oilType.isNotBlank(),
                            testTag = "draft_awal_${right.tankId}",
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
private fun DualColumnFieldGridHeader() {
    val extras = SoundingThemeExtras.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(10.dp),
            color = extras.sageContainer
        ) {
            Text(
                text = "PREBLEND",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.6.sp
                ),
                color = extras.onSageContainer,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(10.dp),
            color = extras.sageContainer
        ) {
            Text(
                text = "TOWERTANK & T.50",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.6.sp
                ),
                color = extras.onSageContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
    }
}

@Composable
private fun FieldTankStockCell(
    row: TankShiftDraftRow,
    rawQuantity: String,
    capacityTon: Int,
    showProductLabel: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extras = SoundingThemeExtras.colors
    val qtyDisplay = ShiftDraftEngine.formatQuantityDisplay(rawQuantity)
    val parsedKg = parseTankStockKg(rawQuantity)
    val isFilled = parsedKg != null && parsedKg > 0L
    val fillRatio = if (isFilled && capacityTon > 0) {
        (parsedKg.toDouble() / (capacityTon * 1000.0)).toFloat().coerceIn(0.05f, 1f)
    } else {
        0f
    }

    val containerColor = if (isFilled) {
        MaterialTheme.colorScheme.surface
    } else {
        extras.innerContainer
    }
    val cellBorder = if (isFilled) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    } else {
        BorderStroke(1.dp, extras.cardBorder.copy(alpha = 0.7f))
    }
    val pillBg = if (isFilled) {
        extras.sagePrimary
    } else {
        extras.sagePrimary.copy(alpha = if (extras.isDark) 0.28f else 0.72f)
    }
    val pillInk = if (isFilled) {
        extras.onSagePrimary
    } else {
        if (extras.isDark) extras.onSageContainer else extras.onSagePrimary
    }

    val productSummary = remember(row.oilType, row.summaryNote) {
        val oil = row.oilType.trim()
        val note = row.summaryNote.trim()
        when {
            oil.isNotEmpty() -> oil
            note.isNotEmpty() -> note
            else -> ""
        }
    }

    Surface(
        modifier = modifier
            .heightIn(min = 68.dp)
            .clip(FieldTankCellShape)
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = FieldTankCellShape,
        color = containerColor,
        border = cellBorder,
        shadowElevation = if (isFilled && !extras.isDark) 1.dp else 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Top Row: Tank Code Sage Pill Badge + Short Product Label
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = pillBg
                ) {
                    Text(
                        text = row.tankId,
                        style = MonoTypography.badge.copy(
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = pillInk,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.5.dp)
                    )
                }

                if (isFilled && showProductLabel && productSummary.isNotEmpty()) {
                    Text(
                        text = productSummary,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }

            // Value Row: Tabular Figures kg + Ton
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = if (isFilled) "$qtyDisplay kg" else qtyDisplay,
                    style = MonoTypography.fieldStock.copy(
                        fontSize = if (isFilled) 13.5.sp else 13.sp,
                        fontWeight = if (isFilled) FontWeight.ExtraBold else FontWeight.SemiBold
                    ),
                    color = if (isFilled) MaterialTheme.colorScheme.onSurface else extras.emptyValueInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (isFilled && parsedKg != null) {
                    Text(
                        text = "${SoundingFormatters.formatTon(parsedKg / 1000.0)} T",
                        style = MonoTypography.fieldTon.copy(fontSize = 10.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            // Subtle Mini Fill Progress Bar at the bottom of the cell
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (isFilled) extras.innerContainer else extras.cardBorder.copy(alpha = 0.4f)
                    )
            ) {
                if (isFilled && fillRatio > 0f) {
                    val barColor = if (fillRatio >= 0.96f) extras.warningBorder else extras.sagePrimary
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fillRatio)
                            .clip(RoundedCornerShape(999.dp))
                            .background(barColor)
                    )
                }
            }
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
        shape = DraftCardShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, extras.cardBorder),
        shadowElevation = if (extras.isDark) 0.dp else 2.dp
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
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    border = BorderStroke(1.dp, extras.sagePrimary.copy(alpha = 0.65f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = extras.sagePrimary)
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.draft_action_add_tf), style = MaterialTheme.typography.labelMedium)
                }
            }

            if (transfers.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = extras.innerContainer
                ) {
                    Text(
                        text = stringResource(R.string.draft_tf_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
            } else {
                transfers.forEachIndexed { idx, tf ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("draft_tf_row_$idx"),
                        shape = RoundedCornerShape(14.dp),
                        color = extras.innerContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = tf.jam.ifBlank { "-" },
                                        style = MonoTypography.badge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    val qtyDisplay = tf.qtyKg.trim().let { raw ->
                                        if (raw.isEmpty()) "-" else "${ShiftDraftEngine.formatQuantityDisplay(raw)} kg"
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = extras.sageContainer
                                    ) {
                                        Text(
                                            text = "Qty: $qtyDisplay",
                                            style = MonoTypography.badge.copy(
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = extras.onSageContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
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
                                    tint = extras.sagePrimary,
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
        shape = DraftCardShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, extras.cardBorder),
        shadowElevation = if (extras.isDark) 0.dp else 2.dp
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
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedBorderColor = extras.emeraldAccent,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = extras.emeraldAccent,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    cursorColor = extras.emeraldAccent,
                    focusedContainerColor = extras.innerContainer,
                    unfocusedContainerColor = extras.innerContainer
                ),
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
private fun StokAkhirCard(
    rows: List<TankShiftDraftRow>,
    capacitiesTon: Map<String, Int>,
    onEditTank: (TankShiftDraftRow) -> Unit
) {
    val extras = SoundingThemeExtras.colors
    val preblend = remember(rows) { orderFieldGroupRows(rows, TankGroup.PREBLEND, FIELD_GRID_PREBLEND_ORDER) }
    val tower = remember(rows) { orderFieldGroupRows(rows, TankGroup.TOWERTANK, FIELD_GRID_TOWERTANK_ORDER) }
    val maxCount = maxOf(preblend.size, tower.size)
    val totalAkhirKg = ShiftDraftEngine.totalFinalKg(rows)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("draft_stok_akhir_card"),
        shape = DraftCardShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, extras.cardBorder),
        shadowElevation = if (extras.isDark) 0.dp else 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                        color = extras.amberTextReadable
                    )
                    Text(
                        text = "${SoundingFormatters.formatTon(totalAkhirKg / 1000.0)} Ton",
                        style = MonoTypography.fieldTon,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            DualColumnFieldGridHeader()

            for (i in 0 until maxCount) {
                val left = preblend.getOrNull(i)
                val right = tower.getOrNull(i)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (left != null) {
                        FieldTankStockCell(
                            row = left,
                            rawQuantity = left.sAkhir.quantityKg,
                            capacityTon = capacitiesTon[left.tankId] ?: 30,
                            showProductLabel = true,
                            testTag = "draft_akhir_${left.tankId}",
                            onClick = { onEditTank(left) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    if (right != null) {
                        FieldTankStockCell(
                            row = right,
                            rawQuantity = right.sAkhir.quantityKg,
                            capacityTon = capacitiesTon[right.tankId] ?: 60,
                            showProductLabel = true,
                            testTag = "draft_akhir_${right.tankId}",
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
        shape = DraftCardShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, extras.cardBorder),
        shadowElevation = if (extras.isDark) 0.dp else 2.dp
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
                        tint = extras.sagePrimary,
                        modifier = Modifier.size(19.dp)
                    )
                    Text(
                        text = stringResource(R.string.draft_cloud_monitor_title),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = extras.sageContainer
                    ) {
                        Text(
                            text = cloudReports.size.toString(),
                            style = MonoTypography.badge,
                            color = extras.onSageContainer,
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
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { onOpenReport(report) }
                                    .testTag("cloud_report_${report.id}"),
                                shape = RoundedCornerShape(16.dp),
                                color = extras.innerContainer,
                                border = BorderStroke(1.dp, extras.cardBorder)
                            ) {
                                Column(
                                    modifier = Modifier.padding(11.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(999.dp),
                                            color = extras.sageContainer
                                        ) {
                                            Text(
                                                text = report.displayAccountName.ifBlank { "-" },
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = extras.onSageContainer,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
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
                                        color = extras.sagePrimary,
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
        selected && usePrimaryFill -> extras.sagePrimary
        selected -> extras.sageContainer
        else -> extras.innerContainer
    }
    val textColor = when {
        selected && usePrimaryFill -> extras.onSagePrimary
        selected -> extras.onSageContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier
            .heightIn(min = 36.dp)
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(999.dp),
        color = bgColor,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
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
