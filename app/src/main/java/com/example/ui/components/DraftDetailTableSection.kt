package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.ShiftDraftEngine
import com.example.data.model.SoundingPointDraft
import com.example.data.model.TankGroup
import com.example.data.model.TankShiftDraftRow
import com.example.ui.theme.BrandDeepTeal
import com.example.ui.theme.BrandLeafGreen
import com.example.ui.theme.MonoTypography
import com.example.ui.theme.SoundingThemeExtras
import com.example.util.SoundingFormatters
import kotlin.math.abs

private enum class DetailGroupFilter {
    ALL,
    PREBLEND,
    TOWERTANK
}

@Composable
fun DraftDetailTableSection(
    rows: List<TankShiftDraftRow>,
    onEditTank: (TankShiftDraftRow, TankEditFocus) -> Unit,
    modifier: Modifier = Modifier
) {
    val extras = SoundingThemeExtras.colors
    var groupFilter by remember { mutableStateOf(DetailGroupFilter.ALL) }

    val filteredRows = remember(rows, groupFilter) {
        when (groupFilter) {
            DetailGroupFilter.ALL -> rows
            DetailGroupFilter.PREBLEND -> rows.filter { it.group == TankGroup.PREBLEND }
            DetailGroupFilter.TOWERTANK -> rows.filter { it.group == TankGroup.TOWERTANK }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Group Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                DetailGroupFilter.ALL to stringResource(R.string.draft_group_all),
                DetailGroupFilter.PREBLEND to stringResource(R.string.draft_group_preblend),
                DetailGroupFilter.TOWERTANK to stringResource(R.string.draft_group_towertank)
            ).forEach { (filter, label) ->
                val selected = groupFilter == filter
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .clickable { groupFilter = filter }
                        .testTag("draft_filter_${filter.name.lowercase()}"),
                    shape = RoundedCornerShape(10.dp),
                    color = if (selected) extras.leafContainer else extras.elevatedSurface,
                    border = BorderStroke(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) extras.leafAccent else extras.cardBorder
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (selected) extras.onLeafContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Text(
            text = stringResource(R.string.draft_detail_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Horizontally scrollable detail table matching factory logbook columns
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("draft_detail_table_card"),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, extras.cardBorder),
            tonalElevation = 1.dp
        ) {
            val hScroll = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(hScroll)
            ) {
                // Header Row
                Row(
                    modifier = Modifier
                        .background(BrandDeepTeal)
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TableHeaderCell("No Tank", 72.dp)
                    TableHeaderCell("Jenis Oil", 116.dp)
                    TableHeaderCell("Sond.", 66.dp)
                    TableHeaderCell("Sondingan", 86.dp)
                    TableHeaderCell("M.U", 64.dp)
                    TableHeaderCell("Jml M.U", 84.dp)
                    TableHeaderCell("Volume (L)", 102.dp)
                    TableHeaderCell("F. Koreksi", 104.dp)
                    TableHeaderCell("Density", 78.dp)
                    TableHeaderCell("Temp", 58.dp)
                    TableHeaderCell("Quantity", 104.dp)
                    TableHeaderCell("Keterangan", 180.dp)
                }

                filteredRows.forEachIndexed { idx, row ->
                    val altBg = if (idx % 2 == 1) extras.elevatedSurface else MaterialTheme.colorScheme.surface
                    val deltaKg = ShiftDraftEngine.computeRowDeltaKg(row)

                    Column(
                        modifier = Modifier
                            .background(altBg)
                            .clickable { onEditTank(row, TankEditFocus.S_AKHIR) }
                            .testTag("draft_detail_row_${row.tankId}")
                    ) {
                        if (idx > 0) {
                            HorizontalDivider(color = extras.cardBorder, thickness = 1.2.dp)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Col 1: Tank ID
                            Box(
                                modifier = Modifier
                                    .width(72.dp)
                                    .padding(horizontal = 6.dp, vertical = 8.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (row.group == TankGroup.PREBLEND) {
                                        extras.resultSurface
                                    } else {
                                        extras.leafContainer
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        if (row.group == TankGroup.PREBLEND) extras.interactiveAccent else extras.leafAccent
                                    )
                                ) {
                                    Text(
                                        text = row.tankId,
                                        style = MonoTypography.badge.copy(fontWeight = FontWeight.Bold),
                                        color = if (row.group == TankGroup.PREBLEND) {
                                            extras.interactiveAccent
                                        } else {
                                            extras.onLeafContainer
                                        },
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            // Col 2: Jenis Oil
                            Text(
                                text = row.oilType.ifBlank { "-" },
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .width(116.dp)
                                    .padding(horizontal = 6.dp)
                            )

                            // Cols 3..11: Stacked S. Awal & S. Akhir rows
                            Column {
                                DetailPointRow(
                                    statusLabel = "S. Awal",
                                    statusColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    point = row.sAwal,
                                    onClick = { onEditTank(row, TankEditFocus.S_AWAL) }
                                )
                                HorizontalDivider(
                                    color = extras.cardBorder.copy(alpha = 0.65f),
                                    modifier = Modifier.width(746.dp)
                                )
                                DetailPointRow(
                                    statusLabel = "S. Akhir",
                                    statusColor = BrandLeafGreen,
                                    point = row.sAkhir,
                                    onClick = { onEditTank(row, TankEditFocus.S_AKHIR) }
                                )
                            }

                            // Col 12: Keterangan + optional delta badge
                            Column(
                                modifier = Modifier
                                    .width(180.dp)
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                if (deltaKg != null) {
                                    val sign = if (deltaKg > 0L) "+" else "-"
                                    Text(
                                        text = "Δ $sign${SoundingFormatters.formatIndonesianInteger(abs(deltaKg))} kg",
                                        style = MonoTypography.badge.copy(fontWeight = FontWeight.Bold),
                                        color = extras.interactiveAccent,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                val noteText = row.detailNote.trim().ifBlank { row.summaryNote.trim() }.ifBlank { "-" }
                                Text(
                                    text = noteText,
                                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 15.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
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

@Composable
private fun TableHeaderCell(text: String, width: Dp) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 11.5.sp
        ),
        color = Color.White,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .width(width)
            .padding(horizontal = 6.dp)
    )
}

@Composable
private fun DetailPointRow(
    statusLabel: String,
    statusColor: Color,
    point: SoundingPointDraft,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .height(34.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = statusLabel,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = statusColor,
            maxLines = 1,
            modifier = Modifier
                .width(66.dp)
                .padding(horizontal = 6.dp)
        )
        MonoCell(point.soundingCm.ifBlank { "-" }, 86.dp)
        MonoCell(point.tableLevelCm.ifBlank { "-" }, 64.dp)
        MonoCell(point.highLevelCm.ifBlank { "-" }, 84.dp)
        MonoCell(point.volumeL.ifBlank { "-" }, 102.dp)
        MonoCell(point.vcf.ifBlank { "-" }, 104.dp)
        MonoCell(point.density.ifBlank { "-" }, 78.dp)
        MonoCell(point.tempC.ifBlank { "-" }, 58.dp)
        MonoCell(
            text = ShiftDraftEngine.formatQuantityDisplay(point.quantityKg),
            width = 104.dp,
            bold = true
        )
    }
}

@Composable
private fun MonoCell(
    text: String,
    width: Dp,
    bold: Boolean = false
) {
    Text(
        text = text,
        style = MonoTypography.badge.copy(
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            fontSize = 12.sp
        ),
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .width(width)
            .padding(horizontal = 6.dp)
    )
}
