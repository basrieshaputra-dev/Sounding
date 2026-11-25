package com.example.util

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

    fun formatIsoUtc(epochMs: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return sdf.format(Date(epochMs))
    }

    fun formatDisplayDateTime(epochMs: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yy HH.mm", idLocale)
        return sdf.format(Date(epochMs))
    }

    fun formatReportDate(epochMs: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("EEEE, dd MMM yy", idLocale)
        return sdf.format(Date(epochMs))
    }

    fun formatProductAndNote(product: String, note: String): String {
        val p = product.trim()
        val n = note.trim()
        return when {
            p.isNotEmpty() && n.isNotEmpty() -> "$p ($n)"
            p.isNotEmpty() -> p
            n.isNotEmpty() -> n
            else -> "-"
        }
    }

    fun formatWhatsAppSummary(calc: SoundingCalculation): String {
        if (calc.isManualKg) {
            val desc = formatProductAndNote(calc.product, calc.note)
            return "${calc.tankId} | $desc | Input Manual | Stok: ${calc.stockKg} kg (${formatTon(calc.stockTon)} Ton)"
        }
        return buildString {
            append(calc.tankId)
            append(" | ")
            append(calc.product)
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

    fun formatLogWhatsAppSummary(log: SoundingLogEntity): String {
        if (log.isManualKg) {
            val desc = formatProductAndNote(log.product, log.note)
            return "${log.tankId} | $desc | Input Manual | Stok: ${log.stockKg} kg (${formatTon(log.stockKg / 1000.0)} Ton)"
        }
        return buildString {
            append(log.tankId)
            append(" | ")
            append(log.product)
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
            append(" Ton)")
        }
    }

    fun buildCsvContent(logs: List<SoundingLogEntity>): String {
        val header =
            "Waktu,Tangki,Produk,Catatan,Mode,Sounding_Cm,Table_Level_Cm,High_Level_Cm,Temp_C,Volume_Liter,Density,VCF,Total_Stock_Kg,Total_Stock_Ton"
        val sb = StringBuilder()
        sb.append(header)
        for (l in logs) {
            sb.append('\n')
            sb.append(csvEscape(l.timestampIso)).append(',')
            sb.append(csvEscape(l.tankId)).append(',')
            sb.append('"').append(l.product.replace("\"", "\"\"")).append('"').append(',')
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
