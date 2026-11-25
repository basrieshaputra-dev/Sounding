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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DailyReportMetadata
import com.example.data.model.ReportTimeSlot
import com.example.data.model.TransferLogItem
import com.example.ui.theme.SoundingThemeExtras
import com.example.util.SoundingFormatters
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportJpgReportSheet(
    initialOperatorName: String,
    onShareJpg: (DailyReportMetadata) -> Unit,
    onDownloadJpg: (DailyReportMetadata) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val extras = SoundingThemeExtras.colors

    var dateLabel by remember { mutableStateOf(SoundingFormatters.formatReportDate()) }
    var timeSlot by remember {
        mutableStateOf(defaultTimeSlotForCurrentHour())
    }
    var operatorName by remember { mutableStateOf(initialOperatorName) }
    var nextTransferId by remember { mutableLongStateOf(1L) }
    val transfers = remember { mutableStateListOf<TransferLogItem>() }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = extras.leafAccent,
        focusedLabelColor = extras.leafAccentReadable,
        cursorColor = extras.leafAccentReadable,
        focusedContainerColor = extras.elevatedSurface,
        unfocusedContainerColor = extras.elevatedSurface
    )

    fun buildMetadata(): DailyReportMetadata = DailyReportMetadata(
        dateLabel = dateLabel.trim().ifBlank { SoundingFormatters.formatReportDate() },
        timeSlot = timeSlot,
        operatorName = operatorName.trim(),
        transfers = transfers.toList()
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
                    text = stringResource(R.string.export_jpg_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_export_jpg_sheet")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.cd_close)
                    )
                }
            }

            // Hari & Tanggal
            OutlinedTextField(
                value = dateLabel,
                onValueChange = { dateLabel = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("report_date_input"),
                colors = fieldColors,
                singleLine = true,
                label = { Text(stringResource(R.string.label_report_date)) },
                placeholder = { Text(stringResource(R.string.placeholder_report_date)) },
                shape = MaterialTheme.shapes.medium
            )

            // Waktu: Pagi / Sore / Malam
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.label_report_time),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReportTimeSlot.entries.forEach { slot ->
                        val selected = timeSlot == slot
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .clickable { timeSlot = slot }
                                .testTag("report_time_${slot.name.lowercase()}"),
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
                                    text = slot.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) {
                                        extras.onLeafContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Nama Operator
            OutlinedTextField(
                value = operatorName,
                onValueChange = { operatorName = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("report_operator_input"),
                colors = fieldColors,
                singleLine = true,
                label = { Text(stringResource(R.string.label_report_operator)) },
                placeholder = { Text(stringResource(R.string.placeholder_report_operator)) },
                shape = MaterialTheme.shapes.medium
            )

            HorizontalDivider(color = extras.cardBorder)

            // Catatan Log Transfer (TF / Return) - optional dynamic list
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.label_transfer_section),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = {
                        transfers.add(TransferLogItem(id = nextTransferId++))
                    },
                    modifier = Modifier.testTag("add_transfer_row_button"),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = extras.leafAccentReadable
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.action_add_transfer))
                }
            }

            transfers.forEachIndexed { idx, item ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = extras.elevatedSurface,
                    border = BorderStroke(1.dp, extras.cardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = item.jam,
                                onValueChange = { transfers[idx] = item.copy(jam = it) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("tf_jam_$idx"),
                                colors = fieldColors,
                                singleLine = true,
                                label = { Text(stringResource(R.string.label_tf_jam)) },
                                placeholder = { Text(stringResource(R.string.placeholder_tf_jam)) },
                                shape = MaterialTheme.shapes.small
                            )
                            OutlinedTextField(
                                value = item.qtyKg,
                                onValueChange = { transfers[idx] = item.copy(qtyKg = it) },
                                modifier = Modifier
                                    .weight(1.2f)
                                    .testTag("tf_qty_$idx"),
                                colors = fieldColors,
                                singleLine = true,
                                label = { Text(stringResource(R.string.label_tf_qty)) },
                                placeholder = { Text(stringResource(R.string.placeholder_tf_qty)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = MaterialTheme.shapes.small
                            )
                            IconButton(
                                onClick = { transfers.removeAt(idx) },
                                modifier = Modifier.testTag("remove_tf_$idx")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = stringResource(R.string.cd_delete_item),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        OutlinedTextField(
                            value = item.keterangan,
                            onValueChange = { transfers[idx] = item.copy(keterangan = it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("tf_ket_$idx"),
                            colors = fieldColors,
                            singleLine = true,
                            label = { Text(stringResource(R.string.label_tf_ket)) },
                            placeholder = { Text(stringResource(R.string.placeholder_tf_ket)) },
                            shape = MaterialTheme.shapes.small
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Primary: Bagikan WA (.JPG) + Secondary: Unduh (.JPG)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onShareJpg(buildMetadata()) },
                    modifier = Modifier
                        .weight(1.2f)
                        .heightIn(min = 52.dp)
                        .testTag("confirm_share_jpg_button"),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.action_share_jpg_wa),
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                OutlinedButton(
                    onClick = { onDownloadJpg(buildMetadata()) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp)
                        .testTag("confirm_download_jpg_button"),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.2.dp, extras.interactiveAccent),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = extras.interactiveAccent
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.action_download_jpg),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

private fun defaultTimeSlotForCurrentHour(): ReportTimeSlot {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour in 6..13 -> ReportTimeSlot.PAGI
        hour in 14..18 -> ReportTimeSlot.SORE
        else -> ReportTimeSlot.MALAM
    }
}
