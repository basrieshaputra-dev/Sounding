package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.SoundingLogEntity
import com.example.ui.SoundingUiState
import com.example.ui.theme.MonoTypography
import com.example.ui.theme.SoundingThemeExtras
import com.example.util.SoundingFormatters

@Composable
fun HistoryScreen(
    uiState: SoundingUiState,
    onOpenExportJpg: () -> Unit,
    onExportCsv: () -> Unit,
    onCopyLogWa: (SoundingLogEntity) -> Unit,
    onDeleteLog: (SoundingLogEntity) -> Unit,
    onClearAllLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showClearDialog by remember { mutableStateOf(false) }
    val extras = SoundingThemeExtras.colors

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Summary & Export Header Card
            item(key = "summary_header") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("history_summary_card"),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, extras.cardBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                                        stringResource(R.string.history_count_suffix, uiState.logs.size),
                                    style = MonoTypography.badge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (uiState.logs.isNotEmpty()) {
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
                                .heightIn(min = 50.dp)
                                .testTag("export_jpg_button"),
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
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
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Secondary Option: Export CSV
                        OutlinedButton(
                            onClick = onExportCsv,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("export_csv_button"),
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(1.dp, extras.interactiveAccent),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = extras.interactiveAccent
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
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            if (uiState.logs.isEmpty()) {
                item(key = "empty_history") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("empty_history_card"),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, extras.cardBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 36.dp),
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
                                text = stringResource(R.string.empty_history_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(
                    items = uiState.logs,
                    key = { it.id }
                ) { log ->
                    SoundingLogCard(
                        log = log,
                        onCopyWa = { onCopyLogWa(log) },
                        onDelete = { onDeleteLog(log) }
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
                Text(stringResource(R.string.confirm_clear_message, uiState.logs.size))
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
private fun SoundingLogCard(
    log: SoundingLogEntity,
    onCopyWa: () -> Unit,
    onDelete: () -> Unit
) {
    val extras = SoundingThemeExtras.colors
    val formattedTime = remember(log.createdAtEpochMs) {
        SoundingFormatters.formatDisplayDateTime(log.createdAtEpochMs)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("log_item_card_${log.id}"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, extras.cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                        shape = MaterialTheme.shapes.small,
                        color = extras.leafContainer
                    ) {
                        Text(
                            text = log.tankId,
                            style = MonoTypography.detailValue,
                            color = extras.onLeafContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
                        modifier = Modifier.testTag("copy_log_wa_${log.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = stringResource(R.string.cd_copy_icon),
                            tint = extras.interactiveAccent,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_log_${log.id}")
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

            Text(
                text = formattedTime,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

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
