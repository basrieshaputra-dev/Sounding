package com.example.data.model

import com.example.data.CalibrationEngine
import com.example.data.local.SoundingLogEntity
import com.example.util.SoundingFormatters
import java.util.Calendar

enum class DraftSubTab {
    SUMMARY_TF,
    DETAIL_TABLE
}

enum class TankGroup(val label: String) {
    PREBLEND("PREBLEND"),
    TOWERTANK("TOWERTANK & T.50")
}

data class SoundingPointDraft(
    val soundingCm: String = "",
    val tableLevelCm: String = "",
    val highLevelCm: String = "",
    val volumeL: String = "",
    val vcf: String = "",
    val density: String = "",
    val tempC: String = "",
    val quantityKg: String = "",
    val timestampLabel: String = "",
    val sourceLogId: Long? = null
) {
    val isEmptyOrDash: Boolean
        get() {
            val trimmed = quantityKg.trim()
            return trimmed.isEmpty() || trimmed == "-" || trimmed.equals("empty", ignoreCase = true)
        }
}

data class TankShiftDraftRow(
    val tankId: String,
    val group: TankGroup,
    val oilType: String = "",
    val sAwal: SoundingPointDraft = SoundingPointDraft(),
    val sAkhir: SoundingPointDraft = SoundingPointDraft(),
    val summaryNote: String = "",
    val detailNote: String = "",
    val isManuallyEdited: Boolean = false
)

data class ShiftReportDraft(
    val dateLabel: String = "",
    val shift: ReportTimeSlot = ReportTimeSlot.PAGI,
    val operatorName: String = "",
    val nbNote: String = "",
    val transfers: List<TransferLogItem> = emptyList(),
    val tankRows: List<TankShiftDraftRow> = emptyList()
)

object ShiftDraftEngine {

    val PREBLEND_TANKS = listOf("T.101", "T.111", "T.102", "T.112", "T.121", "T.122")
    val TOWERTANK_TANKS = listOf("T.171", "T.172", "T.173", "T.174", "T.176", "T.177", "T.50")
    val ALL_STANDARD_TANKS = PREBLEND_TANKS + TOWERTANK_TANKS

    fun createInitialDraft(
        logs: List<SoundingLogEntity>,
        defaultOperator: String,
        nowEpochMs: Long = System.currentTimeMillis()
    ): ShiftReportDraft {
        val refEpoch = logs.firstOrNull()?.createdAtEpochMs?.takeIf { it > 0L } ?: nowEpochMs
        val initialRows = ALL_STANDARD_TANKS.map { tankId ->
            val group = if (tankId in PREBLEND_TANKS) TankGroup.PREBLEND else TankGroup.TOWERTANK
            buildTankRowFromLogs(tankId = tankId, group = group, tankLogsNewestFirst = logs.filter { it.tankId == tankId })
        }
        return ShiftReportDraft(
            dateLabel = SoundingFormatters.formatReportDate(refEpoch),
            shift = defaultShiftForEpoch(refEpoch),
            operatorName = defaultOperator,
            nbNote = "",
            transfers = emptyList(),
            tankRows = initialRows
        )
    }

    /**
     * Merges the latest History logs into an existing draft.
     * - If [forceOverwriteManual] is false, rows with `isManuallyEdited == true` are preserved.
     * - Always ensures all 13 standard tanks exist in canonical logbook order.
     */
    fun syncDraftWithHistory(
        current: ShiftReportDraft,
        logs: List<SoundingLogEntity>,
        defaultOperator: String = "",
        forceOverwriteManual: Boolean = false
    ): ShiftReportDraft {
        val existingByTank = current.tankRows.associateBy { it.tankId }
        val logsByTank = logs.groupBy { it.tankId }

        val updatedRows = ALL_STANDARD_TANKS.map { tankId ->
            val group = if (tankId in PREBLEND_TANKS) TankGroup.PREBLEND else TankGroup.TOWERTANK
            val existing = existingByTank[tankId]
            val tankLogs = logsByTank[tankId].orEmpty()

            if (existing != null && existing.isManuallyEdited && !forceOverwriteManual) {
                existing.copy(group = group)
            } else {
                buildTankRowFromLogs(tankId = tankId, group = group, tankLogsNewestFirst = tankLogs)
            }
        }

        val refEpoch = logs.firstOrNull()?.createdAtEpochMs?.takeIf { it > 0L } ?: System.currentTimeMillis()
        val resolvedDate = current.dateLabel.ifBlank { SoundingFormatters.formatReportDate(refEpoch) }
        val resolvedOperator = current.operatorName.ifBlank { defaultOperator }

        return current.copy(
            dateLabel = resolvedDate,
            operatorName = resolvedOperator,
            tankRows = updatedRows
        )
    }

    fun buildTankRowFromLogs(
        tankId: String,
        group: TankGroup,
        tankLogsNewestFirst: List<SoundingLogEntity>
    ): TankShiftDraftRow {
        if (tankLogsNewestFirst.isEmpty()) {
            return TankShiftDraftRow(
                tankId = tankId,
                group = group,
                oilType = "",
                sAwal = SoundingPointDraft(quantityKg = "-"),
                sAkhir = SoundingPointDraft(quantityKg = "-"),
                summaryNote = "",
                detailNote = "",
                isManuallyEdited = false
            )
        }

        // Chronological order: earliest log is S Awal, latest log is S Akhir
        val chronological = tankLogsNewestFirst.sortedWith(
            compareBy<SoundingLogEntity> { it.createdAtEpochMs }.thenBy { it.id }
        )
        val earliest = chronological.first()
        val latest = chronological.last()

        val sAwalPoint = toSoundingPoint(earliest)
        val sAkhirPoint = toSoundingPoint(latest)

        val resolvedOil = CalibrationEngine.normalizeProductName(
            latest.product.ifBlank { earliest.product }
        )
        val resolvedSummaryNote = latest.note.trim().ifBlank { earliest.note.trim() }
        val resolvedDetailNote = when {
            chronological.size == 1 -> "Tidak ada pergerakan"
            earliest.stockKg == latest.stockKg -> "Tidak ada pergerakan"
            else -> ""
        }

        return TankShiftDraftRow(
            tankId = tankId,
            group = group,
            oilType = resolvedOil,
            sAwal = sAwalPoint,
            sAkhir = sAkhirPoint,
            summaryNote = resolvedSummaryNote,
            detailNote = resolvedDetailNote,
            isManuallyEdited = false
        )
    }

    fun toSoundingPoint(log: SoundingLogEntity): SoundingPointDraft {
        val timeText = if (log.createdAtEpochMs > 0L) {
            SoundingFormatters.formatLocalTime(log.createdAtEpochMs)
        } else {
            ""
        }
        val qtyFormatted = SoundingFormatters.formatIndonesianInteger(log.stockKg)
        if (log.isManualKg) {
            return SoundingPointDraft(
                soundingCm = "-",
                tableLevelCm = "-",
                highLevelCm = "-",
                volumeL = "-",
                vcf = "-",
                density = "-",
                tempC = "-",
                quantityKg = qtyFormatted,
                timestampLabel = timeText,
                sourceLogId = log.id
            )
        }
        return SoundingPointDraft(
            soundingCm = SoundingFormatters.formatCompact(log.sounding),
            tableLevelCm = SoundingFormatters.formatCompact(log.tableLevel),
            highLevelCm = SoundingFormatters.formatCompact(log.highLevel),
            volumeL = SoundingFormatters.formatIndonesianNumber(log.totalVol),
            vcf = SoundingFormatters.formatFixed(log.vcf, 6).trimEnd('0').trimEnd('.'),
            density = SoundingFormatters.formatFixed(log.density, 4),
            tempC = SoundingFormatters.formatCompact(log.temp),
            quantityKg = qtyFormatted,
            timestampLabel = timeText,
            sourceLogId = log.id
        )
    }

    /**
     * Parses Indonesian-formatted or plain numeric kg strings (e.g. "63.974", "63974", "19.840")
     * while returning 0L for "-", "Empty", or blank values.
     */
    fun parseQuantityKg(raw: String): Long {
        val trimmed = raw.trim()
        if (trimmed.isEmpty() || trimmed == "-" || trimmed.equals("empty", ignoreCase = true)) {
            return 0L
        }
        // Strip unit suffix if present
        val cleaned = trimmed
            .replace("kg", "", ignoreCase = true)
            .replace(" ", "")
        // If string uses dots as thousand separators (e.g. 63.974 or 121.718), strip dots
        val dotParts = cleaned.split('.')
        val normalized = if (dotParts.size >= 2 && dotParts.drop(1).all { it.length == 3 && it.all(Char::isDigit) }) {
            cleaned.replace(".", "").replace(",", ".")
        } else {
            cleaned.replace(",", ".")
        }
        return normalized.toDoubleOrNull()?.let { kotlin.math.round(it).toLong() }?.coerceAtLeast(0L) ?: 0L
    }

    fun formatQuantityDisplay(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return "-"
        if (trimmed == "-") return "-"
        if (trimmed.equals("empty", ignoreCase = true)) return "Empty"
        val parsed = parseQuantityKg(trimmed)
        return if (parsed > 0L || trimmed == "0") {
            SoundingFormatters.formatIndonesianInteger(parsed)
        } else {
            trimmed
        }
    }

    fun totalInitialKg(rows: List<TankShiftDraftRow>, group: TankGroup? = null): Long {
        return rows
            .filter { group == null || it.group == group }
            .sumOf { parseQuantityKg(it.sAwal.quantityKg) }
    }

    fun totalFinalKg(rows: List<TankShiftDraftRow>, group: TankGroup? = null): Long {
        return rows
            .filter { group == null || it.group == group }
            .sumOf { parseQuantityKg(it.sAkhir.quantityKg) }
    }

    fun computeRowDeltaKg(row: TankShiftDraftRow): Long? {
        val startBlank = row.sAwal.isEmptyOrDash
        val endBlank = row.sAkhir.isEmptyOrDash
        if (startBlank && endBlank) return null
        val startKg = parseQuantityKg(row.sAwal.quantityKg)
        val endKg = parseQuantityKg(row.sAkhir.quantityKg)
        val diff = endKg - startKg
        return if (diff == 0L) null else diff
    }

    fun defaultShiftForEpoch(epochMs: Long): ReportTimeSlot {
        val hour = Calendar.getInstance().apply { timeInMillis = epochMs }.get(Calendar.HOUR_OF_DAY)
        return when {
            hour in 6..13 -> ReportTimeSlot.PAGI
            hour in 14..18 -> ReportTimeSlot.SORE
            else -> ReportTimeSlot.MALAM
        }
    }
}
