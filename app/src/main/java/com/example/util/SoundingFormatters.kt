package com.example.util

import com.example.data.CalibrationEngine
import com.example.data.local.SoundingLogEntity
import com.example.data.model.SoundingCalculation
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

object SoundingFormatters {

    private val idLocale = Locale.forLanguageTag("id-ID")
    private val idSymbols = DecimalFormatSymbols(idLocale).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }

    fun formatIndonesianInteger(value: Long): String {
        val df = DecimalFormat("#,##0", idSymbols)
        return df.format(value)
    }

    fun formatIndonesianNumber(value: Double): String {
        val isWhole = abs(value - value.toLong()) < 1e-9
        return if (isWhole) {
            formatIndonesianInteger(value.toLong())
        } else {
            val df = DecimalFormat("#,##0.##", idSymbols)
            df.format(value)
        }
    }

    fun formatFixed(value: Double, decimals: Int): String {
        return String.format(Locale.US, "%.${decimals}f", value)
    }

    fun formatTon(tonValue: Double): String {
        return formatFixed(tonValue, 1)
    }

    fun formatCompact(value: Double): String {
        val asLong = value.toLong()
        return if (abs(value - asLong) < 1e-9) {
            asLong.toString()
        } else {
            String.format(Locale.US, "%.4f", value).trimEnd('0').trimEnd('.')
        }
    }

    private val shortWeekdaysId = arrayOf("Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab")
    private val fullWeekdaysId = arrayOf("Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu")
    private val shortMonthsId = arrayOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")

    fun formatIsoUtc(epochMs: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return sdf.format(Date(epochMs))
    }

    fun formatLocalDate(
        epochMs: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val cal = java.util.Calendar.getInstance(timeZone).apply { timeInMillis = epochMs }
        val dayName = shortWeekdaysId[(cal.get(java.util.Calendar.DAY_OF_WEEK) - 1).coerceIn(0, 6)]
        val dayOfMonth = cal.get(java.util.Calendar.DAY_OF_MONTH)
        val monthName = shortMonthsId[cal.get(java.util.Calendar.MONTH).coerceIn(0, 11)]
        val year = cal.get(java.util.Calendar.YEAR)
        return "$dayName, $dayOfMonth $monthName $year"
    }

    fun formatLocalTime(
        epochMs: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val cal = java.util.Calendar.getInstance(timeZone).apply { timeInMillis = epochMs }
        val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = cal.get(java.util.Calendar.MINUTE)
        return String.format(Locale.US, "%02d:%02d", hour, minute)
    }

    fun formatDisplayDateTime(
        epochMs: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        return "${formatLocalDate(epochMs, timeZone)} • ${formatLocalTime(epochMs, timeZone)}"
    }

    fun formatReportDate(
        epochMs: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val cal = java.util.Calendar.getInstance(timeZone).apply { timeInMillis = epochMs }
        val dayName = fullWeekdaysId[(cal.get(java.util.Calendar.DAY_OF_WEEK) - 1).coerceIn(0, 6)]
        val dayOfMonth = cal.get(java.util.Calendar.DAY_OF_MONTH)
        val monthName = shortMonthsId[cal.get(java.util.Calendar.MONTH).coerceIn(0, 11)]
        val year = cal.get(java.util.Calendar.YEAR)
        return "$dayName, $dayOfMonth $monthName $year"
    }

    fun formatTimeRange(
        earliestEpochMs: Long?,
        latestEpochMs: Long?,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        if (earliestEpochMs == null || latestEpochMs == null) return ""
        val start = formatLocalTime(earliestEpochMs, timeZone)
        val end = formatLocalTime(latestEpochMs, timeZone)
        return if (start == end) start else "$start – $end"
    }

    fun formatProductAndNote(product: String, note: String): String {
        val p = CalibrationEngine.normalizeProductName(product)
        val n = note.trim()
        return when {
            p.isNotEmpty() && n.isNotEmpty() -> "$p ($n)"
            p.isNotEmpty() -> p
            n.isNotEmpty() -> n
            else -> "-"
        }
    }

    fun formatWhatsAppSummary(calc: SoundingCalculation): String {
        val productLabel = CalibrationEngine.normalizeProductName(calc.product)
        if (calc.isManualKg) {
            val desc = formatProductAndNote(productLabel, calc.note)
            return "${calc.tankId} | $desc | Input Manual | Stok: ${calc.stockKg} kg (${formatTon(calc.stockTon)} Ton)"
        }
        return buildString {
            append(calc.tankId)
            append(" | ")
            append(productLabel)
            append(" | Sounding: ")
            append(formatCompact(calc.sounding))
            append(" cm | High: ")
            append(formatFixed(calc.highLevel, 2))
            append(" cm | Suhu: ")
            append(formatCompact(calc.temp))
            append("°C | Vol: ")
            append(formatFixed(calc.totalVol, 1))
            append(" L | Stok: ")
            append(calc.stockKg)
            append(" kg (")
            append(formatTon(calc.stockTon))
            append(" Ton)")
        }
    }

    fun formatLogWhatsAppSummary(
        log: SoundingLogEntity,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val productLabel = CalibrationEngine.normalizeProductName(log.product)
        val timeStamp = if (log.createdAtEpochMs > 0L) {
            formatDisplayDateTime(log.createdAtEpochMs, timeZone)
        } else {
            log.timestampIso
        }
        if (log.isManualKg) {
            val desc = formatProductAndNote(productLabel, log.note)
            return "${log.tankId} | $desc | Input Manual | Stok: ${log.stockKg} kg (${formatTon(log.stockKg / 1000.0)} Ton) | $timeStamp"
        }
        return buildString {
            append(log.tankId)
            append(" | ")
            append(productLabel)
            append(" | Sounding: ")
            append(formatCompact(log.sounding))
            append(" cm | High: ")
            append(formatFixed(log.highLevel, 2))
            append(" cm | Suhu: ")
            append(formatCompact(log.temp))
            append("°C | Vol: ")
            append(formatFixed(log.totalVol, 1))
            append(" L | Stok: ")
            append(log.stockKg)
            append(" kg (")
            append(formatTon(log.stockKg / 1000.0))
            append(" Ton) | ")
            append(timeStamp)
        }
    }

    fun buildCsvContent(
        logs: List<SoundingLogEntity>,
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val header =
            "Tanggal,Jam,Waktu_Lengkap,Tangki,Produk,Catatan,Mode,Sounding_Cm,Table_Level_Cm,High_Level_Cm,Temp_C,Volume_Liter,Density,VCF,Total_Stock_Kg,Total_Stock_Ton"
        val sb = StringBuilder()
        sb.append(header)
        for (l in logs) {
            sb.append('\n')
            val dateLabel = if (l.createdAtEpochMs > 0L) formatLocalDate(l.createdAtEpochMs, timeZone) else l.timestampIso
            val timeLabel = if (l.createdAtEpochMs > 0L) formatLocalTime(l.createdAtEpochMs, timeZone) else ""
            val fullTimestamp = if (l.createdAtEpochMs > 0L) formatDisplayDateTime(l.createdAtEpochMs, timeZone) else l.timestampIso
            sb.append(csvEscape(dateLabel)).append(',')
            sb.append(csvEscape(timeLabel)).append(',')
            sb.append(csvEscape(fullTimestamp)).append(',')
            sb.append(csvEscape(l.tankId)).append(',')
            val productLabel = CalibrationEngine.normalizeProductName(l.product)
            sb.append('"').append(productLabel.replace("\"", "\"\"")).append('"').append(',')
            sb.append('"').append(l.note.replace("\"", "\"\"")).append('"').append(',')
            sb.append(if (l.isManualKg) "Manual_Kg" else "Kalibrasi").append(',')
            sb.append(if (l.isManualKg) "" else formatCompact(l.sounding)).append(',')
            sb.append(if (l.isManualKg) "" else formatCompact(l.tableLevel)).append(',')
            sb.append(if (l.isManualKg) "" else formatFixed(l.highLevel, 2)).append(',')
            sb.append(if (l.isManualKg) "" else formatCompact(l.temp)).append(',')
            sb.append(if (l.isManualKg) "" else formatFixed(l.totalVol, 2)).append(',')
            sb.append(if (l.isManualKg) "" else formatCompact(l.density)).append(',')
            sb.append(if (l.isManualKg) "" else formatFixed(l.vcf, 7)).append(',')
            sb.append(l.stockKg).append(',')
            sb.append(formatTon(l.stockKg / 1000.0))
        }
        return sb.toString()
    }

    private fun csvEscape(raw: String): String {
        return if (raw.contains(',') || raw.contains('"') || raw.contains('\n')) {
            "\"${raw.replace("\"", "\"\"")}\""
        } else {
            raw
        }
    }
}
