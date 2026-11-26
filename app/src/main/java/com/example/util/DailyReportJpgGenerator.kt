package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.data.CalibrationEngine
import com.example.data.local.SoundingLogEntity
import com.example.data.model.DailyReportMetadata
import com.example.data.model.TransferLogItem
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

data class ReportTankRow(
    val tankId: String,
    val log: SoundingLogEntity?
)

data class DailyReportSnapshot(
    val metadata: DailyReportMetadata,
    val leftRows: List<ReportTankRow>,
    val rightRows: List<ReportTankRow>,
    val extraRows: List<ReportTankRow>,
    val transfers: List<TransferLogItem>,
    val leftTotalKg: Long,
    val rightTotalKg: Long,
    val extraTotalKg: Long,
    val grandTotalKg: Long,
    val earliestLogEpochMs: Long? = null,
    val latestLogEpochMs: Long? = null
) {
    val grandTotalTon: Double get() = grandTotalKg / 1000.0
}

object DailyReportJpgGenerator {

    val LEFT_COLUMN_TANKS = listOf("T.101", "T.111", "T.102", "T.112", "T.121", "T.122")
    val RIGHT_COLUMN_TANKS = listOf("T.171", "T.172", "T.173", "T.174", "T.176", "T.177", "T.50")

    private const val COLOR_DEEP_TEAL = 0xFF0A525B.toInt()
    private const val COLOR_LEAF_GREEN = 0xFF498147.toInt()
    private const val COLOR_INK_DARK = 0xFF0C2A2E.toInt()
    private const val COLOR_INK_MUTED = 0xFF426266.toInt()
    private const val COLOR_SOFT_BG = 0xFFF4F8F7.toInt()
    private const val COLOR_ROW_ALT = 0xFFF8FBFA.toInt()
    private const val COLOR_LEAF_SOFT = 0xFFE3F1E2.toInt()
    private const val COLOR_BORDER = 0xFFCBDCD7.toInt()
    private const val COLOR_MINT_TON = 0xFFD8F3DC.toInt()

    fun buildReportSnapshot(
        logs: List<SoundingLogEntity>,
        metadata: DailyReportMetadata
    ): DailyReportSnapshot {
        // logs are already ordered newest-first; keep latest entry per tankId
        val latestByTank = LinkedHashMap<String, SoundingLogEntity>()
        for (log in logs) {
            if (!latestByTank.containsKey(log.tankId)) {
                val normalizedProduct = CalibrationEngine.normalizeProductName(log.product)
                latestByTank[log.tankId] = if (normalizedProduct != log.product) {
                    log.copy(product = normalizedProduct)
                } else {
                    log
                }
            }
        }

        val leftRows = LEFT_COLUMN_TANKS.map { id -> ReportTankRow(id, latestByTank[id]) }
        val rightRows = RIGHT_COLUMN_TANKS.map { id -> ReportTankRow(id, latestByTank[id]) }

        val standardIds = (LEFT_COLUMN_TANKS + RIGHT_COLUMN_TANKS).toSet()
        val extraRows = latestByTank
            .filterKeys { it !in standardIds }
            .map { (id, log) -> ReportTankRow(id, log) }

        val leftTotalKg = leftRows.sumOf { it.log?.stockKg ?: 0L }
        val rightTotalKg = rightRows.sumOf { it.log?.stockKg ?: 0L }
        val extraTotalKg = extraRows.sumOf { it.log?.stockKg ?: 0L }

        val activeTransfers = metadata.transfers.filter {
            it.jam.isNotBlank() || it.keterangan.isNotBlank() || it.qtyKg.isNotBlank()
        }

        val recordedEpochs = latestByTank.values
            .map { it.createdAtEpochMs }
            .filter { it > 0L }

        return DailyReportSnapshot(
            metadata = metadata,
            leftRows = leftRows,
            rightRows = rightRows,
            extraRows = extraRows,
            transfers = activeTransfers,
            leftTotalKg = leftTotalKg,
            rightTotalKg = rightTotalKg,
            extraTotalKg = extraTotalKg,
            grandTotalKg = leftTotalKg + rightTotalKg + extraTotalKg,
            earliestLogEpochMs = recordedEpochs.minOrNull(),
            latestLogEpochMs = recordedEpochs.maxOrNull()
        )
    }

    fun renderToJpegBytes(
        context: Context,
        logs: List<SoundingLogEntity>,
        metadata: DailyReportMetadata
    ): ByteArray {
        val snapshot = buildReportSnapshot(logs, metadata)
        val bitmap = renderReportBitmap(context, snapshot)
        return try {
            ByteArrayOutputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                out.toByteArray()
            }
        } finally {
            bitmap.recycle()
        }
    }

    fun writeReportToCacheFile(context: Context, jpegBytes: ByteArray): File {
        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(dir, "laporan_sounding_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { it.write(jpegBytes) }
        return file
    }

    private fun renderReportBitmap(
        context: Context,
        snapshot: DailyReportSnapshot
    ): Bitmap {
        val sansRegular = ResourcesCompat.getFont(context, R.font.plus_jakarta_sans) ?: Typeface.SANS_SERIF
        val sansBold = Typeface.create(sansRegular, Typeface.BOLD)
        val monoRegular = ResourcesCompat.getFont(context, R.font.jetbrains_mono) ?: Typeface.MONOSPACE
        val monoBold = Typeface.create(monoRegular, Typeface.BOLD)

        val width = 1240
        val margin = 44f
        val colGap = 24f
        val colWidth = (width - margin * 2 - colGap) / 2f

        val topStripeH = 14f
        val headerH = 142f
        val metaCardH = 78f
        val colHeaderH = 48f
        val tankRowH = 88f
        val subtotalRowH = 48f

        val maxTankRows = maxOf(snapshot.leftRows.size, snapshot.rightRows.size)
        val mainGridH = colHeaderH + (maxTankRows * tankRowH) + subtotalRowH

        val extraSectionH = if (snapshot.extraRows.isNotEmpty()) {
            val extraPairs = (snapshot.extraRows.size + 1) / 2
            20f + 42f + (extraPairs * tankRowH)
        } else {
            0f
        }

        val transferHeaderH = 46f
        val transferSubHeadH = 38f
        val transferRowH = 44f
        val transferBodyRows = maxOf(snapshot.transfers.size, 1)
        val transferSectionH = transferHeaderH + transferSubHeadH + (transferBodyRows * transferRowH)

        val grandTotalH = 108f
        val footerH = 48f

        val totalHeight = (
            topStripeH + 24f + headerH + 16f + metaCardH + 22f +
                mainGridH + extraSectionH + 24f + transferSectionH + 24f +
                grandTotalH + 18f + footerH + 20f
            ).toInt()

        val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Clean pure white report background
        canvas.drawColor(Color.WHITE)

        // Top brand gradient stripe
        val stripePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), 0f,
                COLOR_DEEP_TEAL, COLOR_LEAF_GREEN,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), topStripeH, stripePaint)

        var y = topStripeH + 24f

        // Header block with 3 bold lines + company logo on right
        val logoSize = 96f
        val logoRect = RectF(
            width - margin - logoSize,
            y + 8f,
            width - margin,
            y + 8f + logoSize
        )
        val logoBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_BORDER
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(logoRect, 18f, 18f, logoBgPaint)
        canvas.drawRoundRect(logoRect, 18f, 18f, borderPaint)

        val logoBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_company_logo)
        if (logoBitmap != null) {
            val pad = 12f
            val dst = RectF(
                logoRect.left + pad,
                logoRect.top + pad,
                logoRect.right - pad,
                logoRect.bottom - pad
            )
            canvas.drawBitmap(logoBitmap, null, dst, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            logoBitmap.recycle()
        }

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DEEP_TEAL
            typeface = sansBold
            textSize = 36f
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_LEAF_GREEN
            typeface = sansBold
            textSize = 21f
            letterSpacing = 0.05f
        }
        val creditPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_MUTED
            typeface = sansRegular
            textSize = 18f
        }

        canvas.drawText("SOUNDING PREBLEND & TOWERTANK", margin, y + 40f, titlePaint)
        canvas.drawText("FAC - SPECIALTY FATS - TEXTURIZING", margin, y + 74f, subtitlePaint)
        canvas.drawText("Laporan Harian Stok Tangki • App By. Basri syaputra ID 11536", margin, y + 106f, creditPaint)

        y += headerH + 6f

        // Metadata info card (Hari & Tanggal | Waktu | Operator)
        val metaRect = RectF(margin, y, width - margin, y + metaCardH)
        val softFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_SOFT_BG }
        canvas.drawRoundRect(metaRect, 14f, 14f, softFillPaint)
        canvas.drawRoundRect(metaRect, 14f, 14f, borderPaint)

        val metaLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_MUTED
            typeface = sansBold
            textSize = 15f
        }
        val metaValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_DARK
            typeface = sansBold
            textSize = 21f
        }

        val metaColW = (width - margin * 2) / 3f
        val dateText = snapshot.metadata.dateLabel.ifBlank {
            SoundingFormatters.formatReportDate(snapshot.latestLogEpochMs ?: System.currentTimeMillis())
        }
        val timeRange = SoundingFormatters.formatTimeRange(
            earliestEpochMs = snapshot.earliestLogEpochMs,
            latestEpochMs = snapshot.latestLogEpochMs
        )
        val timeText = if (timeRange.isNotEmpty()) {
            "${snapshot.metadata.timeSlot.label} ($timeRange)"
        } else {
            snapshot.metadata.timeSlot.label
        }
        val operatorText = snapshot.metadata.operatorName.ifBlank { "-" }

        drawMetaCell(canvas, margin + 20f, y, metaColW - 24f, "HARI & TANGGAL", dateText, metaLabelPaint, metaValPaint)
        drawMetaCell(canvas, margin + metaColW + 20f, y, metaColW - 24f, "WAKTU", timeText, metaLabelPaint, metaValPaint)
        drawMetaCell(canvas, margin + metaColW * 2 + 20f, y, metaColW - 24f, "OPERATOR", operatorText, metaLabelPaint, metaValPaint)

        // Vertical dividers inside meta card
        val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_BORDER
            strokeWidth = 1.5f
        }
        canvas.drawLine(margin + metaColW, y + 14f, margin + metaColW, y + metaCardH - 14f, divPaint)
        canvas.drawLine(margin + metaColW * 2, y + 14f, margin + metaColW * 2, y + metaCardH - 14f, divPaint)

        y += metaCardH + 22f

        // 2-Column Tank Table
        val leftX = margin
        val rightX = margin + colWidth + colGap

        drawTankColumnTable(
            canvas = canvas,
            x = leftX,
            y = y,
            colWidth = colWidth,
            headerTitle = "PREBLEND (T.101 – T.122)",
            headerColor = COLOR_DEEP_TEAL,
            rows = snapshot.leftRows,
            padToRowCount = maxTankRows,
            subtotalKg = snapshot.leftTotalKg,
            colHeaderH = colHeaderH,
            rowH = tankRowH,
            subtotalH = subtotalRowH,
            sansRegular = sansRegular,
            sansBold = sansBold,
            monoRegular = monoRegular,
            monoBold = monoBold
        )

        drawTankColumnTable(
            canvas = canvas,
            x = rightX,
            y = y,
            colWidth = colWidth,
            headerTitle = "TOWERTANK & MANUAL (T.171 – T.50)",
            headerColor = COLOR_LEAF_GREEN,
            rows = snapshot.rightRows,
            padToRowCount = maxTankRows,
            subtotalKg = snapshot.rightTotalKg,
            colHeaderH = colHeaderH,
            rowH = tankRowH,
            subtotalH = subtotalRowH,
            sansRegular = sansRegular,
            sansBold = sansBold,
            monoRegular = monoRegular,
            monoBold = monoBold
        )

        y += mainGridH

        // Optional Extra Recorded Tanks (if operator logged T.175, T.178..T.182)
        if (snapshot.extraRows.isNotEmpty()) {
            y += 20f
            val extraPairs = (snapshot.extraRows.size + 1) / 2
            val extraBoxH = 42f + extraPairs * tankRowH
            val extraRect = RectF(margin, y, width - margin, y + extraBoxH)
            canvas.drawRoundRect(extraRect, 14f, 14f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })

            val extraHeadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_DEEP_TEAL }
            canvas.drawRoundRect(RectF(margin, y, width - margin, y + 42f), 14f, 14f, extraHeadPaint)
            canvas.drawRect(margin, y + 24f, width - margin, y + 42f, extraHeadPaint)
            canvas.drawText(
                "TANGKI TAMBAHAN TERCATAT",
                margin + 18f,
                y + 28f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    typeface = sansBold
                    textSize = 18f
                }
            )

            var ey = y + 42f
            for (idx in 0 until snapshot.extraRows.size step 2) {
                val leftItem = snapshot.extraRows[idx]
                val rightItem = snapshot.extraRows.getOrNull(idx + 1)
                drawSingleTankRow(
                    canvas, margin, ey, colWidth + colGap / 2f, tankRowH, leftItem,
                    idx % 4 == 2, sansRegular, sansBold, monoRegular, monoBold
                )
                if (rightItem != null) {
                    drawSingleTankRow(
                        canvas, margin + colWidth + colGap / 2f, ey, colWidth + colGap / 2f, tankRowH, rightItem,
                        idx % 4 == 2, sansRegular, sansBold, monoRegular, monoBold
                    )
                }
                ey += tankRowH
            }
            canvas.drawRoundRect(extraRect, 14f, 14f, borderPaint)
            y += extraBoxH
        }

        y += 24f

        // Middle Section: Catatan Log Transfer (TF / Return)
        val tfRect = RectF(margin, y, width - margin, y + transferSectionH)
        canvas.drawRoundRect(tfRect, 14f, 14f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })

        val tfHeadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_DEEP_TEAL }
        canvas.drawRoundRect(RectF(margin, y, width - margin, y + transferHeaderH), 14f, 14f, tfHeadPaint)
        canvas.drawRect(margin, y + 26f, width - margin, y + transferHeaderH, tfHeadPaint)
        canvas.drawText(
            "CATATAN LOG TRANSFER (TF / RETURN)",
            margin + 18f,
            y + 30f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                typeface = sansBold
                textSize = 19f
            }
        )

        val tfTableTop = y + transferHeaderH
        canvas.drawRect(margin, tfTableTop, width - margin, tfTableTop + transferSubHeadH, softFillPaint)
        canvas.drawLine(margin, tfTableTop + transferSubHeadH, width - margin, tfTableTop + transferSubHeadH, divPaint)

        val jamColW = 170f
        val qtyColW = 240f
        val ketColX = margin + jamColW
        val qtyColX = width - margin - qtyColW

        val thPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_MUTED
            typeface = sansBold
            textSize = 16f
        }
        val thRightPaint = Paint(thPaint).apply { textAlign = Paint.Align.RIGHT }

        canvas.drawText("JAM", margin + 18f, tfTableTop + 25f, thPaint)
        canvas.drawText("KETERANGAN TF / RETURN", ketColX + 14f, tfTableTop + 25f, thPaint)
        canvas.drawText("QTY (KG)", width - margin - 18f, tfTableTop + 25f, thRightPaint)

        var rowY = tfTableTop + transferSubHeadH
        if (snapshot.transfers.isEmpty()) {
            val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_INK_MUTED
                typeface = sansRegular
                textSize = 17f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(
                "- Tidak ada catatan log transfer (TF / Return) -",
                width / 2f,
                rowY + 29f,
                emptyPaint
            )
        } else {
            val cellMonoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_INK_DARK
                typeface = monoBold
                textSize = 17f
            }
            val cellTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_INK_DARK
                typeface = sansRegular
                textSize = 17f
            }
            val cellQtyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_LEAF_GREEN
                typeface = monoBold
                textSize = 18f
                textAlign = Paint.Align.RIGHT
            }

            snapshot.transfers.forEachIndexed { idx, tf ->
                if (idx % 2 == 1) {
                    canvas.drawRect(margin, rowY, width - margin, rowY + transferRowH, Paint().apply { color = COLOR_ROW_ALT })
                }
                if (idx > 0) {
                    canvas.drawLine(margin, rowY, width - margin, rowY, divPaint)
                }
                canvas.drawText(tf.jam.ifBlank { "-" }, margin + 18f, rowY + 28f, cellMonoPaint)
                val maxKetWidth = (qtyColX - ketColX - 24f).coerceAtLeast(100f)
                val clippedKet = TextUtils.ellipsize(
                    tf.keterangan.ifBlank { "-" },
                    cellTextPaint,
                    maxKetWidth,
                    TextUtils.TruncateAt.END
                ).toString()
                canvas.drawText(clippedKet, ketColX + 14f, rowY + 28f, cellTextPaint)

                val qtyDisplay = tf.qtyKg.trim().let { raw ->
                    val parsed = raw.replace(",", ".").toDoubleOrNull()
                    when {
                        parsed != null -> "${SoundingFormatters.formatIndonesianNumber(parsed)} kg"
                        raw.isNotEmpty() -> "$raw kg"
                        else -> "-"
                    }
                }
                canvas.drawText(qtyDisplay, width - margin - 18f, rowY + 28f, cellQtyPaint)
                rowY += transferRowH
            }
        }
        canvas.drawRoundRect(tfRect, 14f, 14f, borderPaint)

        y += transferSectionH + 24f

        // Bottom Section: Grand Total Summary Card
        val totalRect = RectF(margin, y, width - margin, y + grandTotalH)
        val totalGradPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                totalRect.left, totalRect.top, totalRect.right, totalRect.bottom,
                COLOR_DEEP_TEAL, COLOR_LEAF_GREEN,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(totalRect, 16f, 16f, totalGradPaint)

        val totalLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(225, 255, 255, 255)
            typeface = sansBold
            textSize = 17f
            letterSpacing = 0.04f
        }
        val totalSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(215, 255, 255, 255)
            typeface = monoRegular
            textSize = 16f
        }
        val grandKgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = monoBold
            textSize = 36f
            textAlign = Paint.Align.RIGHT
        }
        val grandTonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_MINT_TON
            typeface = monoBold
            textSize = 24f
            textAlign = Paint.Align.RIGHT
        }

        canvas.drawText("TOTAL STOK KESELURUHAN", margin + 26f, y + 42f, totalLabelPaint)
        val breakdownLine = "Preblend: ${SoundingFormatters.formatIndonesianInteger(snapshot.leftTotalKg)} kg   •   " +
            "Towertank & T.50: ${SoundingFormatters.formatIndonesianInteger(snapshot.rightTotalKg)} kg"
        canvas.drawText(breakdownLine, margin + 26f, y + 76f, totalSubPaint)

        canvas.drawText(
            "${SoundingFormatters.formatIndonesianInteger(snapshot.grandTotalKg)} kg",
            width - margin - 26f,
            y + 50f,
            grandKgPaint
        )
        canvas.drawText(
            "${SoundingFormatters.formatTon(snapshot.grandTotalTon)} Ton",
            width - margin - 26f,
            y + 86f,
            grandTonPaint
        )

        y += grandTotalH + 18f

        // Footer credit + timestamp reference
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_MUTED
            typeface = sansRegular
            textSize = 15f
            textAlign = Paint.Align.CENTER
        }
        val refTimeText = if (snapshot.latestLogEpochMs != null) {
            val latestFull = SoundingFormatters.formatDisplayDateTime(snapshot.latestLogEpochMs)
            "Ref. Sounding Terakhir: $latestFull  •  Sounding Preblend & Towertank • App By. Basri syaputra ID 11536"
        } else {
            "Sounding Preblend & Towertank • FAC - SPECIALTY FATS - TEXTURIZING • App By. Basri syaputra ID 11536"
        }
        canvas.drawText(
            refTimeText,
            width / 2f,
            y + 24f,
            footerPaint
        )

        return bitmap
    }

    private fun drawMetaCell(
        canvas: Canvas,
        x: Float,
        cardTop: Float,
        maxWidth: Float,
        label: String,
        value: String,
        labelPaint: Paint,
        valPaint: Paint
    ) {
        canvas.drawText(label, x, cardTop + 26f, labelPaint)
        val tp = TextPaint(valPaint)
        val ellipsized = TextUtils.ellipsize(value, tp, maxWidth, TextUtils.TruncateAt.END).toString()
        canvas.drawText(ellipsized, x, cardTop + 56f, valPaint)
    }

    private fun drawTankColumnTable(
        canvas: Canvas,
        x: Float,
        y: Float,
        colWidth: Float,
        headerTitle: String,
        headerColor: Int,
        rows: List<ReportTankRow>,
        padToRowCount: Int,
        subtotalKg: Long,
        colHeaderH: Float,
        rowH: Float,
        subtotalH: Float,
        sansRegular: Typeface,
        sansBold: Typeface,
        monoRegular: Typeface,
        monoBold: Typeface
    ) {
        val totalH = colHeaderH + (padToRowCount * rowH) + subtotalH
        val boxRect = RectF(x, y, x + colWidth, y + totalH)

        canvas.drawRoundRect(boxRect, 14f, 14f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })

        // Header bar
        val headPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = headerColor }
        canvas.drawRoundRect(RectF(x, y, x + colWidth, y + colHeaderH), 14f, 14f, headPaint)
        canvas.drawRect(x, y + colHeaderH - 16f, x + colWidth, y + colHeaderH, headPaint)
        canvas.drawText(
            headerTitle,
            x + 16f,
            y + 31f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                typeface = sansBold
                textSize = 18f
            }
        )

        var curY = y + colHeaderH
        for (i in 0 until padToRowCount) {
            val row = rows.getOrNull(i)
            if (row != null) {
                drawSingleTankRow(
                    canvas = canvas,
                    x = x,
                    y = curY,
                    rowWidth = colWidth,
                    rowH = rowH,
                    row = row,
                    altBackground = i % 2 == 1,
                    sansRegular = sansRegular,
                    sansBold = sansBold,
                    monoRegular = monoRegular,
                    monoBold = monoBold
                )
            } else {
                // Empty spacer row so left & right columns align evenly at subtotal line
                val bg = Paint().apply { color = if (i % 2 == 1) COLOR_ROW_ALT else Color.WHITE }
                canvas.drawRect(x, curY, x + colWidth, curY + rowH, bg)
                canvas.drawLine(x, curY, x + colWidth, curY, Paint().apply { color = COLOR_BORDER; strokeWidth = 1f })
            }
            curY += rowH
        }

        // Subtotal footer row
        val subRect = RectF(x, curY, x + colWidth, curY + subtotalH)
        canvas.drawRoundRect(subRect, 14f, 14f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_SOFT_BG })
        canvas.drawRect(x, curY, x + colWidth, curY + 14f, Paint().apply { color = COLOR_SOFT_BG })
        canvas.drawLine(x, curY, x + colWidth, curY, Paint().apply { color = COLOR_BORDER; strokeWidth = 1.5f })

        canvas.drawText(
            "SUBTOTAL",
            x + 16f,
            curY + 30f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_INK_MUTED
                typeface = sansBold
                textSize = 16f
            }
        )
        canvas.drawText(
            "${SoundingFormatters.formatIndonesianInteger(subtotalKg)} kg (${SoundingFormatters.formatTon(subtotalKg / 1000.0)} Ton)",
            x + colWidth - 16f,
            curY + 31f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = headerColor
                typeface = monoBold
                textSize = 17f
                textAlign = Paint.Align.RIGHT
            }
        )

        canvas.drawRoundRect(
            boxRect,
            14f,
            14f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_BORDER
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
        )
    }

    private fun drawSingleTankRow(
        canvas: Canvas,
        x: Float,
        y: Float,
        rowWidth: Float,
        rowH: Float,
        row: ReportTankRow,
        altBackground: Boolean,
        sansRegular: Typeface,
        sansBold: Typeface,
        monoRegular: Typeface,
        monoBold: Typeface
    ) {
        val rowBg = Paint().apply { color = if (altBackground) COLOR_ROW_ALT else Color.WHITE }
        canvas.drawRect(x, y, x + rowWidth, y + rowH, rowBg)
        canvas.drawLine(x, y, x + rowWidth, y, Paint().apply { color = COLOR_BORDER; strokeWidth = 1f })

        val log = row.log
        val isManual = row.tankId == "T.50" || (log?.isManualKg == true)

        // Tank ID pill badge
        val badgeW = 84f
        val badgeH = 38f
        val badgeRect = RectF(x + 12f, y + (rowH - badgeH) / 2f, x + 12f + badgeW, y + (rowH + badgeH) / 2f)
        canvas.drawRoundRect(
            badgeRect,
            9f,
            9f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isManual) COLOR_LEAF_SOFT else COLOR_SOFT_BG
            }
        )
        canvas.drawRoundRect(
            badgeRect,
            9f,
            9f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isManual) COLOR_LEAF_GREEN else COLOR_DEEP_TEAL
                style = Paint.Style.STROKE
                strokeWidth = 1.5f
            }
        )
        val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isManual) COLOR_LEAF_GREEN else COLOR_DEEP_TEAL
            typeface = monoBold
            textSize = 18f
            textAlign = Paint.Align.CENTER
        }
        val textBounds = Rect()
        badgeTextPaint.getTextBounds(row.tankId, 0, row.tankId.length, textBounds)
        canvas.drawText(
            row.tankId,
            badgeRect.centerX(),
            badgeRect.centerY() + textBounds.height() / 2f - 1f,
            badgeTextPaint
        )

        // Right-aligned Stock kg + Ton
        val stockAreaW = 164f
        val stockRightX = x + rowWidth - 14f

        val kgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (log != null) COLOR_INK_DARK else COLOR_INK_MUTED
            typeface = monoBold
            textSize = 19f
            textAlign = Paint.Align.RIGHT
        }
        val tonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (log != null) COLOR_LEAF_GREEN else COLOR_INK_MUTED
            typeface = monoBold
            textSize = 15f
            textAlign = Paint.Align.RIGHT
        }

        if (log != null) {
            canvas.drawText(
                "${SoundingFormatters.formatIndonesianInteger(log.stockKg)} kg",
                stockRightX,
                y + 36f,
                kgPaint
            )
            canvas.drawText(
                "${SoundingFormatters.formatTon(log.stockKg / 1000.0)} Ton",
                stockRightX,
                y + 62f,
                tonPaint
            )
        } else {
            canvas.drawText("-", stockRightX, y + 49f, kgPaint)
        }

        // Middle Product, Telemetry & Saved Date/Time info
        val midX = badgeRect.right + 12f
        val midMaxW = (stockRightX - stockAreaW - midX).coerceAtLeast(80f)

        val prodPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (log != null) COLOR_INK_DARK else COLOR_INK_MUTED
            typeface = sansBold
            textSize = 17f
        }
        val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_MUTED
            typeface = monoRegular
            textSize = 13.5f
        }
        val timeStampPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DEEP_TEAL
            typeface = monoRegular
            textSize = 12.5f
        }

        val prodText = if (log != null) {
            SoundingFormatters.formatProductAndNote(log.product, log.note)
        } else {
            if (isManual) "Belum diisi (Manual kg)" else "Belum sounding"
        }
        val clippedProd = TextUtils.ellipsize(prodText, prodPaint, midMaxW, TextUtils.TruncateAt.END).toString()
        canvas.drawText(clippedProd, midX, y + 29f, prodPaint)

        val detailText = when {
            log == null -> "-"
            log.isManualKg -> "Input Manual (kg)"
            else -> "High: ${SoundingFormatters.formatFixed(log.highLevel, 1)} cm • ${SoundingFormatters.formatCompact(log.temp)}°C"
        }
        val clippedDetail = TextUtils.ellipsize(detailText, metaPaint, midMaxW, TextUtils.TruncateAt.END).toString()
        canvas.drawText(clippedDetail, midX, y + 51f, metaPaint)

        if (log != null && log.createdAtEpochMs > 0L) {
            val savedAt = SoundingFormatters.formatDisplayDateTime(log.createdAtEpochMs)
            val clippedTime = TextUtils.ellipsize(savedAt, timeStampPaint, midMaxW, TextUtils.TruncateAt.END).toString()
            canvas.drawText(clippedTime, midX, y + 72f, timeStampPaint)
        }
    }
}
