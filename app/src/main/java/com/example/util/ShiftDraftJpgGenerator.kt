package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.data.model.ShiftDraftEngine
import com.example.data.model.ShiftReportDraft
import com.example.data.model.SoundingPointDraft
import com.example.data.model.TankGroup
import com.example.data.model.TankShiftDraftRow
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object ShiftDraftJpgGenerator {

    private const val COLOR_DEEP_TEAL = 0xFF0A525B.toInt()
    private const val COLOR_LEAF_GREEN = 0xFF498147.toInt()
    private const val COLOR_INK_DARK = 0xFF0C2A2E.toInt()
    private const val COLOR_INK_MUTED = 0xFF426266.toInt()
    private const val COLOR_SOFT_BG = 0xFFF4F8F7.toInt()
    private const val COLOR_ROW_ALT = 0xFFF8FBFA.toInt()
    private const val COLOR_LEAF_SOFT = 0xFFE6F3E5.toInt()
    private const val COLOR_AMBER_SOFT = 0xFFFFF8E1.toInt()
    private const val COLOR_AMBER_BORDER = 0xFFE6C25A.toInt()
    private const val COLOR_BORDER = 0xFFCBDCD7.toInt()
    private const val COLOR_MINT_TON = 0xFFD8F3DC.toInt()

    fun renderToJpegBytes(
        context: Context,
        draft: ShiftReportDraft
    ): ByteArray {
        val bitmap = renderDraftBitmap(context, draft)
        return try {
            ByteArrayOutputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 94, out)
                out.toByteArray()
            }
        } finally {
            bitmap.recycle()
        }
    }

    fun writeDraftToCacheFile(context: Context, jpegBytes: ByteArray): File {
        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(dir, "draft_laporan_shift_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { it.write(jpegBytes) }
        return file
    }

    private fun renderDraftBitmap(
        context: Context,
        draft: ShiftReportDraft
    ): Bitmap {
        val sansRegular = ResourcesCompat.getFont(context, R.font.plus_jakarta_sans) ?: Typeface.SANS_SERIF
        val sansBold = Typeface.create(sansRegular, Typeface.BOLD)
        val monoRegular = ResourcesCompat.getFont(context, R.font.jetbrains_mono) ?: Typeface.MONOSPACE
        val monoBold = Typeface.create(monoRegular, Typeface.BOLD)

        val preblendRows = draft.tankRows.filter { it.group == TankGroup.PREBLEND }
        val towerRows = draft.tankRows.filter { it.group == TankGroup.TOWERTANK }
        val activeTransfers = draft.transfers.filter {
            it.jam.isNotBlank() || it.keterangan.isNotBlank() || it.qtyKg.isNotBlank()
        }
        val hasNb = draft.nbNote.isNotBlank()

        val width = 1680
        val margin = 44f
        val contentW = width - margin * 2
        val colGap = 24f
        val halfColW = (contentW - colGap) / 2f

        // Pre-calculate exact heights
        val topStripeH = 14f
        val headerH = 136f
        val metaH = 78f
        val sectionTitleH = 42f

        val stokAwalRows = maxOf(preblendRows.size, towerRows.size)
        val stokAwalRowH = 42f
        val stokAwalBoxH = 44f + stokAwalRows * stokAwalRowH + 44f

        val tfHeadH = 44f
        val tfSubHeadH = 36f
        val tfRowH = 42f
        val tfBoxH = tfHeadH + tfSubHeadH + maxOf(activeTransfers.size, 1) * tfRowH

        val nbLines = if (hasNb) wrapTextLines(draft.nbNote.trim(), sansRegular, 18f, contentW - 44f) else emptyList()
        val nbBoxH = if (hasNb) (46f + maxOf(nbLines.size, 1) * 26f + 16f) else 0f

        val stokAkhirRows = maxOf(preblendRows.size, towerRows.size)
        val stokAkhirRowH = 46f
        val stokAkhirBoxH = 44f + stokAkhirRows * stokAkhirRowH + 46f

        val grandTotalH = 92f

        // Detail table heights (2 rows per tank: S. Awal + S. Akhir, 34f each = 68f per tank)
        val detailSubHeadH = 42f
        val detailColHeadH = 42f
        val detailTankBlockH = 72f
        val detailPreblendH = detailSubHeadH + detailColHeadH + preblendRows.size * detailTankBlockH
        val detailTowerH = detailSubHeadH + detailColHeadH + towerRows.size * detailTankBlockH

        val footerH = 44f

        val totalHeight = (
            topStripeH + 22f + headerH + 14f + metaH + 24f +
                sectionTitleH + 12f + stokAwalBoxH + 20f +
                tfBoxH + (if (hasNb) 18f + nbBoxH else 0f) + 20f +
                stokAkhirBoxH + 18f + grandTotalH + 28f +
                sectionTitleH + 12f + detailPreblendH + 20f + detailTowerH +
                22f + footerH + 18f
            ).toInt()

        val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_BORDER
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_BORDER
            strokeWidth = 1.2f
        }
        val softFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_SOFT_BG }

        // Top corporate stripe
        canvas.drawRect(
            0f, 0f, width.toFloat(), topStripeH,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(0f, 0f, width.toFloat(), 0f, COLOR_DEEP_TEAL, COLOR_LEAF_GREEN, Shader.TileMode.CLAMP)
            }
        )

        var y = topStripeH + 22f

        // Corporate Header + Logo
        val logoSize = 96f
        val logoRect = RectF(width - margin - logoSize, y + 8f, width - margin, y + 8f + logoSize)
        canvas.drawRoundRect(logoRect, 18f, 18f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })
        canvas.drawRoundRect(logoRect, 18f, 18f, borderPaint)
        BitmapFactory.decodeResource(context.resources, R.drawable.ic_company_logo)?.let { bmp ->
            val pad = 12f
            canvas.drawBitmap(
                bmp,
                null,
                RectF(logoRect.left + pad, logoRect.top + pad, logoRect.right - pad, logoRect.bottom - pad),
                Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            )
            bmp.recycle()
        }

        canvas.drawText(
            "LAPORAN SHIFT HARIAN — PREBLEND & TOWERTANK",
            margin,
            y + 38f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_DEEP_TEAL
                typeface = sansBold
                textSize = 36f
            }
        )
        canvas.drawText(
            "FAC - SPECIALTY FATS - TEXTURIZING  •  PT Permata Hijau Palm Oleo – Belawan",
            margin,
            y + 72f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_LEAF_GREEN
                typeface = sansBold
                textSize = 20f
            }
        )
        canvas.drawText(
            "Draft Laporan Terpadu (Ringkasan Stok & TF + Tabel Detail S. Awal / S. Akhir) • App By. Basri syaputra ID 11536",
            margin,
            y + 104f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_INK_MUTED
                typeface = sansRegular
                textSize = 17f
            }
        )
        y += headerH + 14f

        // Shift Identity Strip
        val metaRect = RectF(margin, y, width - margin, y + metaH)
        canvas.drawRoundRect(metaRect, 14f, 14f, softFillPaint)
        canvas.drawRoundRect(metaRect, 14f, 14f, borderPaint)

        val metaColW = contentW / 3f
        val metaLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_MUTED
            typeface = sansBold
            textSize = 14.5f
        }
        val metaValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK_DARK
            typeface = sansBold
            textSize = 22f
        }
        drawMetaCell(canvas, margin + 20f, y, metaColW - 28f, "HARI & TANGGAL", draft.dateLabel.ifBlank { "-" }, metaLabelPaint, metaValPaint)
        drawMetaCell(canvas, margin + metaColW + 20f, y, metaColW - 28f, "SHIFT", draft.shift.label, metaLabelPaint, metaValPaint)
        drawMetaCell(canvas, margin + metaColW * 2 + 20f, y, metaColW - 28f, "OPERATOR", draft.operatorName.ifBlank { "-" }, metaLabelPaint, metaValPaint)
        canvas.drawLine(margin + metaColW, y + 14f, margin + metaColW, y + metaH - 14f, divPaint)
        canvas.drawLine(margin + metaColW * 2, y + 14f, margin + metaColW * 2, y + metaH - 14f, divPaint)
        y += metaH + 24f

        // ============================================================
        // BAGIAN 1: RINGKASAN STOK & TF
        // ============================================================
        drawSectionBanner(canvas, margin, y, contentW, sectionTitleH, "BAGIAN 1 — RINGKASAN STOK & TRANSFER (TF / RETURN)", COLOR_DEEP_TEAL, sansBold)
        y += sectionTitleH + 12f

        // 1A: Stok Awal (2 columns side by side)
        drawStokAwalBox(
            canvas = canvas,
            x = margin,
            y = y,
            colW = halfColW,
            title = "STOK AWAL — PREBLEND (T.101 – T.122)",
            accentColor = COLOR_DEEP_TEAL,
            rows = preblendRows,
            padRows = stokAwalRows,
            rowH = stokAwalRowH,
            sansBold = sansBold,
            monoBold = monoBold,
            borderPaint = borderPaint,
            divPaint = divPaint
        )
        drawStokAwalBox(
            canvas = canvas,
            x = margin + halfColW + colGap,
            y = y,
            colW = halfColW,
            title = "STOK AWAL — TOWERTANK & T.50 (T.171 – T.50)",
            accentColor = COLOR_LEAF_GREEN,
            rows = towerRows,
            padRows = stokAwalRows,
            rowH = stokAwalRowH,
            sansBold = sansBold,
            monoBold = monoBold,
            borderPaint = borderPaint,
            divPaint = divPaint
        )
        y += stokAwalBoxH + 20f

        // 1B: Daftar Transfer / Return (TF)
        val tfRect = RectF(margin, y, width - margin, y + tfBoxH)
        canvas.drawRoundRect(tfRect, 14f, 14f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })
        drawCardHeader(canvas, margin, y, contentW, tfHeadH, "DAFTAR TRANSFER / RETURN (TF)", COLOR_DEEP_TEAL, sansBold)

        val tfSubTop = y + tfHeadH
        canvas.drawRect(margin, tfSubTop, width - margin, tfSubTop + tfSubHeadH, softFillPaint)
        canvas.drawLine(margin, tfSubTop + tfSubHeadH, width - margin, tfSubTop + tfSubHeadH, divPaint)

        val jamW = 240f
        val qtyW = 280f
        val descX = margin + jamW
        val qtyRightX = width - margin - 20f
        val thPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_INK_MUTED; typeface = sansBold; textSize = 15.5f }
        val thRight = Paint(thPaint).apply { textAlign = Paint.Align.RIGHT }

        canvas.drawText("JAM", margin + 18f, tfSubTop + 24f, thPaint)
        canvas.drawText("URAIAN TF / RETURN", descX + 14f, tfSubTop + 24f, thPaint)
        canvas.drawText("QTY (KG)", qtyRightX, tfSubTop + 24f, thRight)

        var tfY = tfSubTop + tfSubHeadH
        if (activeTransfers.isEmpty()) {
            canvas.drawText(
                "- Tidak ada catatan transfer / return pada shift ini -",
                width / 2f,
                tfY + 27f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = COLOR_INK_MUTED
                    typeface = sansRegular
                    textSize = 16.5f
                    textAlign = Paint.Align.CENTER
                }
            )
        } else {
            val jamPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_INK_DARK; typeface = monoBold; textSize = 16.5f }
            val descPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_INK_DARK; typeface = sansRegular; textSize = 17f }
            val qtyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_LEAF_GREEN; typeface = monoBold; textSize = 17.5f; textAlign = Paint.Align.RIGHT }
            activeTransfers.forEachIndexed { idx, tf ->
                if (idx % 2 == 1) {
                    canvas.drawRect(margin, tfY, width - margin, tfY + tfRowH, Paint().apply { color = COLOR_ROW_ALT })
                }
                if (idx > 0) canvas.drawLine(margin, tfY, width - margin, tfY, divPaint)
                canvas.drawText(tf.jam.ifBlank { "-" }, margin + 18f, tfY + 27f, jamPaint)
                val maxDescW = (width - margin - qtyW - descX - 24f).coerceAtLeast(120f)
                val clippedDesc = TextUtils.ellipsize("→  ${tf.keterangan.ifBlank { "-" }}", descPaint, maxDescW, TextUtils.TruncateAt.END).toString()
                canvas.drawText(clippedDesc, descX + 14f, tfY + 27f, descPaint)

                val qtyDisplay = tf.qtyKg.trim().let { raw ->
                    if (raw.isEmpty()) "-" else {
                        val parsed = ShiftDraftEngine.parseQuantityKg(raw)
                        if (parsed > 0L) "${SoundingFormatters.formatIndonesianInteger(parsed)} kg" else "$raw kg"
                    }
                }
                canvas.drawText(qtyDisplay, qtyRightX, tfY + 27f, qtyPaint)
                tfY += tfRowH
            }
        }
        canvas.drawRoundRect(tfRect, 14f, 14f, borderPaint)
        y += tfBoxH

        // 1C: Catatan Nb. (optional)
        if (hasNb) {
            y += 18f
            val nbRect = RectF(margin, y, width - margin, y + nbBoxH)
            canvas.drawRoundRect(nbRect, 14f, 14f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_AMBER_SOFT })
            canvas.drawRoundRect(
                nbRect, 14f, 14f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_AMBER_BORDER; style = Paint.Style.STROKE; strokeWidth = 2f }
            )
            canvas.drawText(
                "CATATAN (Nb.)",
                margin + 20f,
                y + 28f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_DEEP_TEAL; typeface = sansBold; textSize = 15.5f }
            )
            val nbTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_INK_DARK; typeface = sansRegular; textSize = 18f }
            var lineY = y + 56f
            for (line in nbLines) {
                canvas.drawText(line, margin + 20f, lineY, nbTextPaint)
                lineY += 26f
            }
            y += nbBoxH
        }

        y += 20f

        // 1D: Rekap Stok Akhir (2 columns side by side)
        drawStokAkhirBox(
            canvas = canvas,
            x = margin,
            y = y,
            colW = halfColW,
            title = "REKAP STOK AKHIR — PREBLEND (T.101 – T.122)",
            accentColor = COLOR_DEEP_TEAL,
            rows = preblendRows,
            padRows = stokAkhirRows,
            rowH = stokAkhirRowH,
            sansRegular = sansRegular,
            sansBold = sansBold,
            monoBold = monoBold,
            borderPaint = borderPaint,
            divPaint = divPaint
        )
        drawStokAkhirBox(
            canvas = canvas,
            x = margin + halfColW + colGap,
            y = y,
            colW = halfColW,
            title = "REKAP STOK AKHIR — TOWERTANK & T.50 (T.171 – T.50)",
            accentColor = COLOR_LEAF_GREEN,
            rows = towerRows,
            padRows = stokAkhirRows,
            rowH = stokAkhirRowH,
            sansRegular = sansRegular,
            sansBold = sansBold,
            monoBold = monoBold,
            borderPaint = borderPaint,
            divPaint = divPaint
        )
        y += stokAkhirBoxH + 18f

        // Grand Total Summary Banner
        val totalAwalKg = ShiftDraftEngine.totalInitialKg(draft.tankRows)
        val totalAkhirKg = ShiftDraftEngine.totalFinalKg(draft.tankRows)
        val totalRect = RectF(margin, y, width - margin, y + grandTotalH)
        canvas.drawRoundRect(
            totalRect, 14f, 14f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(totalRect.left, totalRect.top, totalRect.right, totalRect.bottom, COLOR_DEEP_TEAL, COLOR_LEAF_GREEN, Shader.TileMode.CLAMP)
            }
        )
        canvas.drawText(
            "TOTAL REKAP STOK SHIFT",
            margin + 24f,
            y + 34f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(230, 255, 255, 255); typeface = sansBold; textSize = 16.5f }
        )
        canvas.drawText(
            "Total Stok Awal: ${SoundingFormatters.formatIndonesianInteger(totalAwalKg)} kg (${SoundingFormatters.formatTon(totalAwalKg / 1000.0)} Ton)",
            margin + 24f,
            y + 66f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(220, 255, 255, 255); typeface = monoRegular; textSize = 17f }
        )
        canvas.drawText(
            "Stok Akhir: ${SoundingFormatters.formatIndonesianInteger(totalAkhirKg)} kg",
            width - margin - 24f,
            y + 42f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; typeface = monoBold; textSize = 30f; textAlign = Paint.Align.RIGHT }
        )
        canvas.drawText(
            "${SoundingFormatters.formatTon(totalAkhirKg / 1000.0)} Ton",
            width - margin - 24f,
            y + 74f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_MINT_TON; typeface = monoBold; textSize = 21f; textAlign = Paint.Align.RIGHT }
        )
        y += grandTotalH + 28f

        // ============================================================
        // BAGIAN 2: TABEL DETAIL S. AWAL / S. AKHIR
        // ============================================================
        drawSectionBanner(canvas, margin, y, contentW, sectionTitleH, "BAGIAN 2 — TABEL DETAIL SOUNDING (S. AWAL & S. AKHIR)", COLOR_LEAF_GREEN, sansBold)
        y += sectionTitleH + 12f

        drawDetailTable(
            canvas = canvas,
            x = margin,
            y = y,
            tableW = contentW,
            title = "DETAIL PREBLEND (T.101 – T.122)",
            accentColor = COLOR_DEEP_TEAL,
            rows = preblendRows,
            subHeadH = detailSubHeadH,
            colHeadH = detailColHeadH,
            blockH = detailTankBlockH,
            sansRegular = sansRegular,
            sansBold = sansBold,
            monoRegular = monoRegular,
            monoBold = monoBold,
            borderPaint = borderPaint,
            divPaint = divPaint
        )
        y += detailPreblendH + 20f

        drawDetailTable(
            canvas = canvas,
            x = margin,
            y = y,
            tableW = contentW,
            title = "DETAIL TOWERTANK & MANUAL (T.171 – T.50)",
            accentColor = COLOR_LEAF_GREEN,
            rows = towerRows,
            subHeadH = detailSubHeadH,
            colHeadH = detailColHeadH,
            blockH = detailTankBlockH,
            sansRegular = sansRegular,
            sansBold = sansBold,
            monoRegular = monoRegular,
            monoBold = monoBold,
            borderPaint = borderPaint,
            divPaint = divPaint
        )
        y += detailTowerH + 22f

        // Footer
        val generatedAt = SoundingFormatters.formatDisplayDateTime(System.currentTimeMillis())
        canvas.drawText(
            "Dicetak: $generatedAt  •  Sounding Preblend & Towertank • FAC - SPECIALTY FATS - TEXTURIZING • App By. Basri syaputra ID 11536",
            width / 2f,
            y + 24f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_INK_MUTED
                typeface = sansRegular
                textSize = 15.5f
                textAlign = Paint.Align.CENTER
            }
        )

        return bitmap
    }

    private fun drawSectionBanner(
        canvas: Canvas,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        title: String,
        color: Int,
        sansBold: Typeface
    ) {
        val rect = RectF(x, y, x + w, y + h)
        canvas.drawRoundRect(rect, 10f, 10f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
        canvas.drawText(
            title,
            x + 18f,
            y + 27f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = Color.WHITE
                typeface = sansBold
                textSize = 19f
                letterSpacing = 0.03f
            }
        )
    }

    private fun drawCardHeader(
        canvas: Canvas,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        title: String,
        color: Int,
        sansBold: Typeface
    ) {
        val headPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        canvas.drawRoundRect(RectF(x, y, x + w, y + h), 14f, 14f, headPaint)
        canvas.drawRect(x, y + h - 16f, x + w, y + h, headPaint)
        canvas.drawText(
            title,
            x + 16f,
            y + 28f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = Color.WHITE
                typeface = sansBold
                textSize = 17.5f
            }
        )
    }

    private fun drawMetaCell(
        canvas: Canvas,
        x: Float,
        top: Float,
        maxW: Float,
        label: String,
        value: String,
        labelPaint: Paint,
        valPaint: Paint
    ) {
        canvas.drawText(label, x, top + 26f, labelPaint)
        val clipped = TextUtils.ellipsize(value, TextPaint(valPaint), maxW, TextUtils.TruncateAt.END).toString()
        canvas.drawText(clipped, x, top + 57f, valPaint)
    }

    private fun drawStokAwalBox(
        canvas: Canvas,
        x: Float,
        y: Float,
        colW: Float,
        title: String,
        accentColor: Int,
        rows: List<TankShiftDraftRow>,
        padRows: Int,
        rowH: Float,
        sansBold: Typeface,
        monoBold: Typeface,
        borderPaint: Paint,
        divPaint: Paint
    ) {
        val headerH = 44f
        val footerH = 44f
        val totalH = headerH + padRows * rowH + footerH
        val rect = RectF(x, y, x + colW, y + totalH)
        canvas.drawRoundRect(rect, 14f, 14f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })
        drawCardHeader(canvas, x, y, colW, headerH, title, accentColor, sansBold)

        val tankPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor; typeface = monoBold; textSize = 18f }
        val qtyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_INK_DARK; typeface = monoBold; textSize = 18f; textAlign = Paint.Align.RIGHT }
        val mutedQtyPaint = Paint(qtyPaint).apply { color = COLOR_INK_MUTED }

        var curY = y + headerH
        for (i in 0 until padRows) {
            if (i % 2 == 1) canvas.drawRect(x, curY, x + colW, curY + rowH, Paint().apply { color = COLOR_ROW_ALT })
            canvas.drawLine(x, curY, x + colW, curY, divPaint)
            val row = rows.getOrNull(i)
            if (row != null) {
                canvas.drawText(row.tankId, x + 18f, curY + 27f, tankPaint)
                val qtyLabel = ShiftDraftEngine.formatQuantityDisplay(row.sAwal.quantityKg)
                val display = if (qtyLabel == "-" || qtyLabel.equals("Empty", true)) qtyLabel else "$qtyLabel kg"
                canvas.drawText(display, x + colW - 18f, curY + 27f, if (qtyLabel == "-") mutedQtyPaint else qtyPaint)
            }
            curY += rowH
        }

        // Subtotal
        val subtotalKg = ShiftDraftEngine.totalInitialKg(rows)
        val subRect = RectF(x, curY, x + colW, curY + footerH)
        canvas.drawRoundRect(subRect, 14f, 14f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_SOFT_BG })
        canvas.drawRect(x, curY, x + colW, curY + 14f, Paint().apply { color = COLOR_SOFT_BG })
        canvas.drawLine(x, curY, x + colW, curY, divPaint)
        canvas.drawText(
            "SUBTOTAL AWAL",
            x + 18f,
            curY + 28f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_INK_MUTED; typeface = sansBold; textSize = 15.5f }
        )
        canvas.drawText(
            "${SoundingFormatters.formatIndonesianInteger(subtotalKg)} kg (${SoundingFormatters.formatTon(subtotalKg / 1000.0)} Ton)",
            x + colW - 18f,
            curY + 28f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor; typeface = monoBold; textSize = 16.5f; textAlign = Paint.Align.RIGHT }
        )
        canvas.drawRoundRect(rect, 14f, 14f, borderPaint)
    }

    private fun drawStokAkhirBox(
        canvas: Canvas,
        x: Float,
        y: Float,
        colW: Float,
        title: String,
        accentColor: Int,
        rows: List<TankShiftDraftRow>,
        padRows: Int,
        rowH: Float,
        sansRegular: Typeface,
        sansBold: Typeface,
        monoBold: Typeface,
        borderPaint: Paint,
        divPaint: Paint
    ) {
        val headerH = 44f
        val footerH = 46f
        val totalH = headerH + padRows * rowH + footerH
        val rect = RectF(x, y, x + colW, y + totalH)
        canvas.drawRoundRect(rect, 14f, 14f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })
        drawCardHeader(canvas, x, y, colW, headerH, title, accentColor, sansBold)

        val tankPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor; typeface = monoBold; textSize = 18f }
        val qtyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_INK_DARK; typeface = monoBold; textSize = 18f }
        val prodPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_INK_MUTED; typeface = sansRegular; textSize = 16f }

        var curY = y + headerH
        for (i in 0 until padRows) {
            if (i % 2 == 1) canvas.drawRect(x, curY, x + colW, curY + rowH, Paint().apply { color = COLOR_ROW_ALT })
            canvas.drawLine(x, curY, x + colW, curY, divPaint)
            val row = rows.getOrNull(i)
            if (row != null) {
                canvas.drawText(row.tankId, x + 16f, curY + 29f, tankPaint)
                val qtyLabel = ShiftDraftEngine.formatQuantityDisplay(row.sAkhir.quantityKg)
                val qtyDisplay = if (qtyLabel == "-" || qtyLabel.equals("Empty", true)) qtyLabel else "$qtyLabel kg"
                canvas.drawText(qtyDisplay, x + 112f, curY + 29f, qtyPaint)

                val descParts = listOf(row.oilType.trim(), row.summaryNote.trim()).filter { it.isNotEmpty() }
                val descText = descParts.joinToString(" • ").ifEmpty { "-" }
                val descX = x + 266f
                val maxDescW = (x + colW - 14f - descX).coerceAtLeast(80f)
                val clipped = TextUtils.ellipsize(descText, prodPaint, maxDescW, TextUtils.TruncateAt.END).toString()
                canvas.drawText(clipped, descX, curY + 29f, prodPaint)
            }
            curY += rowH
        }

        val subtotalKg = ShiftDraftEngine.totalFinalKg(rows)
        val subRect = RectF(x, curY, x + colW, curY + footerH)
        canvas.drawRoundRect(subRect, 14f, 14f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_SOFT_BG })
        canvas.drawRect(x, curY, x + colW, curY + 14f, Paint().apply { color = COLOR_SOFT_BG })
        canvas.drawLine(x, curY, x + colW, curY, divPaint)
        canvas.drawText(
            "SUBTOTAL AKHIR",
            x + 16f,
            curY + 29f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_INK_MUTED; typeface = sansBold; textSize = 15.5f }
        )
        canvas.drawText(
            "${SoundingFormatters.formatIndonesianInteger(subtotalKg)} kg (${SoundingFormatters.formatTon(subtotalKg / 1000.0)} Ton)",
            x + colW - 16f,
            curY + 29f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor; typeface = monoBold; textSize = 16.5f; textAlign = Paint.Align.RIGHT }
        )
        canvas.drawRoundRect(rect, 14f, 14f, borderPaint)
    }

    private fun drawDetailTable(
        canvas: Canvas,
        x: Float,
        y: Float,
        tableW: Float,
        title: String,
        accentColor: Int,
        rows: List<TankShiftDraftRow>,
        subHeadH: Float,
        colHeadH: Float,
        blockH: Float,
        sansRegular: Typeface,
        sansBold: Typeface,
        monoRegular: Typeface,
        monoBold: Typeface,
        borderPaint: Paint,
        divPaint: Paint
    ) {
        val totalH = subHeadH + colHeadH + rows.size * blockH
        val rect = RectF(x, y, x + tableW, y + totalH)
        canvas.drawRoundRect(rect, 14f, 14f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })
        drawCardHeader(canvas, x, y, tableW, subHeadH, title, accentColor, sansBold)

        // 12 Column proportions summing to tableW (1592f)
        // No Tangki | Jenis Oil | Baris | Sondingan | M.U | Jml M.U | Volume/L | F. Koreksi | Density | Temp | Quantity | Keterangan
        val weights = floatArrayOf(82f, 152f, 80f, 108f, 76f, 106f, 128f, 126f, 96f, 70f, 142f, 426f)
        val totalWeight = weights.sum()
        val colWidths = FloatArray(weights.size) { i -> tableW * (weights[i] / totalWeight) }
        val colLefts = FloatArray(weights.size)
        var accX = x
        for (i in weights.indices) {
            colLefts[i] = accX
            accX += colWidths[i]
        }

        val colTop = y + subHeadH
        canvas.drawRect(x, colTop, x + tableW, colTop + colHeadH, Paint().apply { color = COLOR_SOFT_BG })
        canvas.drawLine(x, colTop + colHeadH, x + tableW, colTop + colHeadH, divPaint)

        val headers = arrayOf(
            "No Tank", "Jenis Oil", "Sond.", "Sondingan", "M.U",
            "Jml M.U", "Vol (L)", "F. Koreksi", "Density", "Temp", "Quantity", "Keterangan"
        )
        val thPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_INK_MUTED; typeface = sansBold; textSize = 14f }
        for (i in headers.indices) {
            if (i > 0) canvas.drawLine(colLefts[i], colTop, colLefts[i], y + totalH, divPaint)
            canvas.drawText(headers[i], colLefts[i] + 8f, colTop + 26f, thPaint)
        }

        val tankPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor; typeface = monoBold; textSize = 17f }
        val oilPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_INK_DARK; typeface = sansBold; textSize = 15f }
        val statusAwalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_INK_MUTED; typeface = sansBold; textSize = 13.5f }
        val statusAkhirPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_LEAF_GREEN; typeface = sansBold; textSize = 13.5f }
        val monoPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_INK_DARK; typeface = monoRegular; textSize = 14.5f }
        val qtyBoldPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_INK_DARK; typeface = monoBold; textSize = 15f }
        val notePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_INK_MUTED; typeface = sansRegular; textSize = 14.5f }
        val deltaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_DEEP_TEAL; typeface = monoBold; textSize = 13.5f }

        var rowTop = colTop + colHeadH
        val subRowH = blockH / 2f

        rows.forEachIndexed { idx, row ->
            if (idx % 2 == 1) {
                canvas.drawRect(x, rowTop, x + tableW, rowTop + blockH, Paint().apply { color = COLOR_ROW_ALT })
            }
            if (idx > 0) {
                canvas.drawLine(x, rowTop, x + tableW, rowTop, Paint().apply { color = COLOR_BORDER; strokeWidth = 1.6f })
            }
            // Faint horizontal divider between S. Awal and S. Akhir across numeric columns (2..10)
            canvas.drawLine(
                colLefts[2], rowTop + subRowH,
                colLefts[11], rowTop + subRowH,
                Paint().apply { color = COLOR_BORDER; strokeWidth = 0.8f }
            )

            // Col 0: Tank ID (vertically centered across 2 sub-rows)
            canvas.drawText(row.tankId, colLefts[0] + 8f, rowTop + blockH * 0.56f, tankPaint)

            // Col 1: Jenis Oil
            val oilClipped = TextUtils.ellipsize(row.oilType.ifBlank { "-" }, oilPaint, colWidths[1] - 14f, TextUtils.TruncateAt.END).toString()
            canvas.drawText(oilClipped, colLefts[1] + 8f, rowTop + blockH * 0.56f, oilPaint)

            // Row 1: S. Awal
            drawDetailSubRow(
                canvas = canvas,
                colLefts = colLefts,
                colWidths = colWidths,
                baselineY = rowTop + 23f,
                statusLabel = "S. Awal",
                statusPaint = statusAwalPaint,
                point = row.sAwal,
                monoPaint = monoPaint,
                qtyPaint = qtyBoldPaint
            )

            // Row 2: S. Akhir
            drawDetailSubRow(
                canvas = canvas,
                colLefts = colLefts,
                colWidths = colWidths,
                baselineY = rowTop + subRowH + 23f,
                statusLabel = "S. Akhir",
                statusPaint = statusAkhirPaint,
                point = row.sAkhir,
                monoPaint = monoPaint,
                qtyPaint = qtyBoldPaint
            )

            // Col 11: Keterangan + optional delta summary
            val noteX = colLefts[11] + 10f
            val noteMaxW = colWidths[11] - 16f
            val deltaKg = ShiftDraftEngine.computeRowDeltaKg(row)
            val mainNote = row.detailNote.trim().ifBlank { row.summaryNote.trim() }

            if (deltaKg != null) {
                val sign = if (deltaKg > 0) "+" else "-"
                val deltaText = "Selisih: $sign${SoundingFormatters.formatIndonesianInteger(kotlin.math.abs(deltaKg))} kg"
                val clippedDelta = TextUtils.ellipsize(deltaText, deltaPaint, noteMaxW, TextUtils.TruncateAt.END).toString()
                canvas.drawText(clippedDelta, noteX, rowTop + 24f, deltaPaint)
                if (mainNote.isNotEmpty()) {
                    val clippedNote = TextUtils.ellipsize(mainNote, notePaint, noteMaxW, TextUtils.TruncateAt.END).toString()
                    canvas.drawText(clippedNote, noteX, rowTop + subRowH + 22f, notePaint)
                }
            } else {
                val displayNote = mainNote.ifEmpty { "-" }
                val clippedNote = TextUtils.ellipsize(displayNote, notePaint, noteMaxW, TextUtils.TruncateAt.END).toString()
                canvas.drawText(clippedNote, noteX, rowTop + blockH * 0.55f, notePaint)
            }

            rowTop += blockH
        }

        // Re-draw vertical dividers cleanly over row backgrounds
        for (i in 1 until colLefts.size) {
            canvas.drawLine(colLefts[i], colTop, colLefts[i], y + totalH, divPaint)
        }
        canvas.drawRoundRect(rect, 14f, 14f, borderPaint)
    }

    private fun drawDetailSubRow(
        canvas: Canvas,
        colLefts: FloatArray,
        colWidths: FloatArray,
        baselineY: Float,
        statusLabel: String,
        statusPaint: Paint,
        point: SoundingPointDraft,
        monoPaint: TextPaint,
        qtyPaint: TextPaint
    ) {
        canvas.drawText(statusLabel, colLefts[2] + 8f, baselineY, statusPaint)
        val vals = arrayOf(
            point.soundingCm.ifBlank { "-" },
            point.tableLevelCm.ifBlank { "-" },
            point.highLevelCm.ifBlank { "-" },
            point.volumeL.ifBlank { "-" },
            point.vcf.ifBlank { "-" },
            point.density.ifBlank { "-" },
            point.tempC.ifBlank { "-" },
            ShiftDraftEngine.formatQuantityDisplay(point.quantityKg)
        )
        for (idx in vals.indices) {
            val colIdx = idx + 3
            val paint = if (colIdx == 10) qtyPaint else monoPaint
            val clipped = TextUtils.ellipsize(vals[idx], paint, colWidths[colIdx] - 12f, TextUtils.TruncateAt.END).toString()
            canvas.drawText(clipped, colLefts[colIdx] + 8f, baselineY, paint)
        }
    }

    private fun wrapTextLines(text: String, typeface: Typeface, textSize: Float, maxWidth: Float): List<String> {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            this.textSize = textSize
        }
        val result = mutableListOf<String>()
        for (paragraph in text.lines()) {
            if (paragraph.isBlank()) {
                result.add("")
                continue
            }
            val words = paragraph.split(" ")
            val current = StringBuilder()
            for (word in words) {
                val candidate = if (current.isEmpty()) word else "$current $word"
                if (paint.measureText(candidate) <= maxWidth) {
                    current.clear()
                    current.append(candidate)
                } else {
                    if (current.isNotEmpty()) result.add(current.toString())
                    current.clear()
                    current.append(word)
                }
            }
            if (current.isNotEmpty()) result.add(current.toString())
        }
        return result
    }
}
