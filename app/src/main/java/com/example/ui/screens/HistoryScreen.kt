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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.firebase.CurrentTankStockDoc
import com.example.data.firebase.HistoryRecordItem
import com.example.data.local.SoundingLogEntity
import com.example.ui.DateFilterOption
import com.example.ui.FILTER_ALL
import com.example.ui.SoundingUiState
import com.example.ui.theme.MonoTypography
import com.example.ui.theme.SoundingThemeExtras
import com.example.util.SoundingFormatters

@Composable
fun HistoryScreen(
    uiState: SoundingUiState,
    onSelectDateFilter: (String) -> Unit,
    onSelectAccountFilter: (String) -> Unit,
    onOpenExportJpg: () -> Unit,
    onExportCsv: () -> Unit,
    onCopyLogWa: (SoundingLogEntity) -> Unit,
    onDeleteRecord: (HistoryRecordItem) -> Unit,
    onClearAllLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showClearDialog by remember { mutableStateOf(false) }
    val extras = SoundingThemeExtras.colors
    val records = uiState.filteredHistoryRecords

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Live Realtime Tank Stock Strip (current_tank_stocks)
            item(key = "realtime_tank_stocks") {
                RealtimeTankStocksCard(
                    stocks = uiState.currentTankStocks,
                    isOnlineRealtime = uiState.isOnlineRealtime
                )
            }

            // 2. Filter Card (Tanggal & Akun)
            item(key = "history_filter") {
                HistoryFilterCard(
                    availableDates = uiState.availableHistoryDates,
                    availableAccounts = uiState.availableHistoryAccounts,
                    selectedDateKey = uiState.historyDateFilter,
                    selectedAccount = uiState.historyAccountFilter,
                    onSelectDate = onSelectDateFilter,
                    onSelectAccount = onSelectAccountFilter
                )
            }

            // 3. Summary & Export Header Card
            item(key = "summary_header") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("history_summary_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, extras.cardBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (extras.isDark) 0.dp else 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.history_total_label),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${SoundingFormatters.formatTon(uiState.totalRecordedTon)} Ton",
                                    style = MonoTypography.heroMetric,
                                    color = extras.metricValueInk,
                                    modifier = Modifier.testTag("total_recorded_ton")
                                )
                                Text(
                                    text = "${SoundingFormatters.formatIndonesianInteger(uiState.totalRecordedKg)} kg • " +
                                        stringResource(R.string.history_count_suffix, records.size),
                                    style = MonoTypography.badge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (uiState.historyRecords.isNotEmpty()) {
                                IconButton(
                                    onClick = { showClearDialog = true },
                                    modifier = Modifier.testTag("clear_all_logs_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteSweep,
                                        contentDescription = stringResource(R.string.action_clear_all),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = extras.cardBorder)

                        // Primary CTA: Export Laporan (.JPG)
                        Button(
                            onClick = onOpenExportJpg,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .testTag("export_jpg_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = extras.sagePrimary,
                                contentColor = extras.onSagePrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = stringResource(R.string.cd_export_jpg),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.action_export_jpg),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Secondary Option: Export CSV
                        OutlinedButton(
                            onClick = onExportCsv,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 50.dp)
                                .testTag("export_csv_button"),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, extras.sagePrimary.copy(alpha = 0.55f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = extras.sagePrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = stringResource(R.string.cd_export_csv),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.action_export_csv),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            if (records.isEmpty()) {
                item(key = "empty_history") {
                    val isFiltered = uiState.historyDateFilter != FILTER_ALL ||
                        uiState.historyAccountFilter != FILTER_ALL
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("empty_history_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, extras.cardBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (extras.isDark) 0.dp else 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = extras.leafContainer,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = extras.leafAccentReadable,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                            Text(
                                text = stringResource(R.string.empty_history_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(
                                    if (isFiltered) R.string.filter_empty_result else R.string.empty_history_desc
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(
                    items = records,
                    key = { it.stableKey }
                ) { record ->
                    val canDelete = record.cloudDocId == null ||
                        record.authorUid == uiState.currentUserUid
                    SoundingLogCard(
                        record = record,
                        canDelete = canDelete,
                        onCopyWa = { onCopyLogWa(record.entity) },
                        onDelete = { onDeleteRecord(record) }
                    )
                }
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.confirm_clear_title)) },
            text = {
                Text(stringResource(R.string.confirm_clear_message, uiState.historyRecords.size))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        onClearAllLogs()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("confirm_clear_all_button")
                ) {
                    Text(stringResource(R.string.dialog_confirm_delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearDialog = false },
                    modifier = Modifier.testTag("cancel_clear_all_button")
                ) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }
}

@Composable
private fun RealtimeTankStocksCard(
    stocks: List<CurrentTankStockDoc>,
    isOnlineRealtime: Boolean
) {
    val extras = SoundingThemeExtras.colors

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("realtime_tank_stocks_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, extras.cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = if (extras.isDark) 0.dp else 2.dp)
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
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = extras.interactiveAccent,
                        modifier = Modifier.size(19.dp)
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.realtime_stocks_title),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.realtime_stocks_subtitle),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (isOnlineRealtime) extras.sageContainer else extras.innerContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isOnlineRealtime) extras.emeraldAccent else extras.amberAccent
                                )
                        )
                        Text(
                            text = "${stocks.size} Tangki",
                            style = MonoTypography.badge,
                            color = if (isOnlineRealtime) extras.onSageContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (stocks.isEmpty()) {
                Text(
                    text = stringResource(R.string.realtime_stocks_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(items = stocks, key = { it.id.ifBlank { it.tankId } }) { stock ->
                        val timeText = stock.timeLabel.ifBlank {
                            if (stock.updatedAtEpochMs > 0L) {
                                SoundingFormatters.formatLocalTime(stock.updatedAtEpochMs)
                            } else {
                                ""
                            }
                        }
                        val dateText = stock.dateLabel.ifBlank {
                            if (stock.updatedAtEpochMs > 0L) {
                                SoundingFormatters.formatLocalDate(stock.updatedAtEpochMs)
                            } else {
                                ""
                            }
                        }
                        Surface(
                            modifier = Modifier
                                .width(172.dp)
                                .testTag("realtime_stock_${stock.tankId}"),
                            shape = RoundedCornerShape(16.dp),
                            color = extras.innerContainer,
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
                                        shape = RoundedCornerShape(999.dp),
                                        color = extras.sagePrimary
                                    ) {
                                        Text(
                                            text = stock.tankId,
                                            style = MonoTypography.badge.copy(fontWeight = FontWeight.Bold),
                                            color = extras.onSagePrimary,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                    if (timeText.isNotEmpty()) {
                                        Text(
                                            text = timeText,
                                            style = MonoTypography.badge.copy(fontWeight = FontWeight.Bold),
                                            color = extras.leafAccentReadable
                                        )
                                    }
                                }

                                Text(
                                    text = stock.product.ifBlank { "-" },
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = "${SoundingFormatters.formatIndonesianInteger(stock.totalStockKg)} kg",
                                    style = MonoTypography.subMetric.copy(fontSize = 14.sp),
                                    color = extras.metricValueInk,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = "${SoundingFormatters.formatTon(stock.tonase)} Ton • ${SoundingFormatters.formatCompact(stock.temp)}°C",
                                    style = MonoTypography.badge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                val metaText = listOf(stock.displayAccountName.trim(), dateText)
                                    .filter { it.isNotEmpty() }
                                    .joinToString(" • ")
                                if (metaText.isNotEmpty()) {
                                    Text(
                                        text = metaText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
private fun HistoryFilterCard(
    availableDates: List<DateFilterOption>,
    availableAccounts: List<String>,
    selectedDateKey: String,
    selectedAccount: String,
    onSelectDate: (String) -> Unit,
    onSelectAccount: (String) -> Unit
) {
    val extras = SoundingThemeExtras.colors

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_filter_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, extras.cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = if (extras.isDark) 0.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                    tint = extras.interactiveAccent,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.filter_section_title),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Date filter horizontal chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterPillChip(
                    label = stringResource(R.string.filter_date_all),
                    selected = selectedDateKey == FILTER_ALL,
                    usePrimaryFill = true,
                    onClick = { onSelectDate(FILTER_ALL) },
                    testTag = "history_date_filter_all"
                )
                availableDates.forEach { opt ->
                    FilterPillChip(
                        label = opt.label,
                        selected = selectedDateKey == opt.dateKey,
                        usePrimaryFill = true,
                        onClick = { onSelectDate(opt.dateKey) },
                        testTag = "history_date_filter_${opt.dateKey}"
                    )
                }
            }

            // Account filter horizontal chips
            if (availableAccounts.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterPillChip(
                        label = stringResource(R.string.filter_account_all),
                        selected = selectedAccount == FILTER_ALL,
                        usePrimaryFill = false,
                        onClick = { onSelectAccount(FILTER_ALL) },
                        testTag = "history_account_filter_all"
                    )
                    availableAccounts.forEach { account ->
                        FilterPillChip(
                            label = account,
                            selected = selectedAccount.equals(account, ignoreCase = true),
                            usePrimaryFill = false,
                            onClick = { onSelectAccount(account) },
                            testTag = "history_account_filter_$account"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterPillChip(
    label: String,
    selected: Boolean,
    usePrimaryFill: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val extras = SoundingThemeExtras.colors
    val containerColor = when {
        selected && usePrimaryFill -> extras.sagePrimary
        selected -> extras.sageContainer
        else -> extras.innerContainer
    }
    val contentColor = when {
        selected && usePrimaryFill -> extras.onSagePrimary
        selected -> extras.onSageContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier
            .heightIn(min = 38.dp)
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(999.dp),
        color = containerColor,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = contentColor
            )
        }
    }
}

@Composable
private fun SoundingLogCard(
    record: HistoryRecordItem,
    canDelete: Boolean,
    onCopyWa: () -> Unit,
    onDelete: () -> Unit
) {
    val log = record.entity
    val extras = SoundingThemeExtras.colors
    val formattedTime = remember(log.createdAtEpochMs) {
        SoundingFormatters.formatDisplayDateTime(log.createdAtEpochMs)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("log_item_card_${record.stableKey}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, extras.cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = if (extras.isDark) 0.dp else 2.dp)
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
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = extras.sagePrimary
                    ) {
                        Text(
                            text = log.tankId,
                            style = MonoTypography.detailValue,
                            color = extras.onSagePrimary,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = SoundingFormatters.formatProductAndNote(log.product, log.note),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onCopyWa,
                        modifier = Modifier.testTag("copy_log_wa_${record.stableKey}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = stringResource(R.string.cd_copy_icon),
                            tint = extras.interactiveAccent,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    if (canDelete) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.testTag("delete_log_${record.stableKey}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = stringResource(R.string.cd_delete_item),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Full Timestamp + Account + Cloud sync metadata row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = extras.elevatedSurface,
                    border = BorderStroke(1.dp, extras.cardBorder),
                    modifier = Modifier.testTag("log_timestamp_${record.stableKey}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = extras.interactiveAccent,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = formattedTime,
                            style = MonoTypography.badge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (record.displayAccountName.isNotBlank()) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = extras.subtleTealTint
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = extras.metricValueInk,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = record.displayAccountName,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = extras.metricValueInk
                            )
                        }
                    }
                }

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (record.isCloudSynced) extras.leafContainer else extras.elevatedSurface
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = if (record.isCloudSynced) Icons.Default.CloudDone else Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = if (record.isCloudSynced) extras.onLeafContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = stringResource(
                                if (record.isCloudSynced) R.string.badge_cloud_synced else R.string.badge_local_only
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (record.isCloudSynced) extras.onLeafContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text(
                text = if (log.isManualKg) {
                    stringResource(R.string.tank_manual_meta)
                } else {
                    "Sounding: ${SoundingFormatters.formatCompact(log.sounding)} cm " +
                        "(High: ${SoundingFormatters.formatFixed(log.highLevel, 1)} cm) • " +
                        "Suhu: ${SoundingFormatters.formatCompact(log.temp)}°C • " +
                        "Vol: ${SoundingFormatters.formatFixed(log.totalVol, 1)} L"
                },
                style = MonoTypography.badge,
                color = if (log.isManualKg) {
                    extras.leafAccentReadable
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            HorizontalDivider(color = extras.cardBorder)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Stok: ${SoundingFormatters.formatIndonesianInteger(log.stockKg)} kg",
                    style = MonoTypography.subMetric,
                    color = extras.metricValueInk
                )
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = extras.subtleTealTint
                ) {
                    Text(
                        text = "${SoundingFormatters.formatTon(log.stockKg / 1000.0)} Ton",
                        style = MonoTypography.detailValue,
                        color = extras.metricValueInk,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
