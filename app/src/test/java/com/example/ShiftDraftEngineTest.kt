package com.example

import com.example.data.local.SoundingLogEntity
import com.example.data.model.ReportTimeSlot
import com.example.data.model.ShiftDraftEngine
import com.example.data.model.ShiftReportDraft
import com.example.data.model.SoundingPointDraft
import com.example.data.model.TankGroup
import com.example.data.model.TransferLogItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShiftDraftEngineTest {

    @Test
    fun syncsHistoryLogsIntoSAwalAndSAkhirForAll13StandardTanks() {
        val t172Early = SoundingLogEntity(
            id = 1,
            timestampIso = "2026-11-25T23:00:00Z",
            createdAtEpochMs = 1_000_000L,
            tankId = "T.172",
            product = "RBDPO",
            sounding = 226.3,
            tableLevel = 33.9,
            highLevel = 260.2,
            temp = 55.0,
            totalVol = 53836.0,
            density = 0.8868,
            vcf = 1.0008004,
            stockKg = 47742L,
            stockTon = 47.742
        )
        val t172Late = SoundingLogEntity(
            id = 2,
            timestampIso = "2026-11-26T05:35:00Z",
            createdAtEpochMs = 2_000_000L,
            tankId = "T.172",
            product = "RBDPO",
            sounding = 508.4,
            tableLevel = 33.9,
            highLevel = 542.3,
            temp = 55.0,
            totalVol = 112277.0,
            density = 0.8868,
            vcf = 1.0008004,
            stockKg = 99567L,
            stockTon = 99.567,
            note = "belum sampel"
        )
        val t171Single = SoundingLogEntity(
            id = 3,
            timestampIso = "2026-11-26T00:10:00Z",
            createdAtEpochMs = 1_500_000L,
            tankId = "T.171",
            product = "RBDPO",
            sounding = 630.5,
            tableLevel = 31.6,
            highLevel = 662.1,
            temp = 55.0,
            totalVol = 137255.0,
            density = 0.8868,
            vcf = 1.0008004,
            stockKg = 121718L,
            stockTon = 121.718
        )
        val t50Manual = SoundingLogEntity(
            id = 4,
            timestampIso = "2026-11-26T00:15:00Z",
            createdAtEpochMs = 1_600_000L,
            tankId = "T.50",
            product = "CBS",
            sounding = 0.0,
            tableLevel = 0.0,
            highLevel = 0.0,
            temp = 0.0,
            totalVol = 0.0,
            density = 0.0,
            vcf = 1.0,
            stockKg = 19840L,
            stockTon = 19.840,
            isManualKg = true,
            note = "flushing"
        )

        // Newest-first as returned by Room DAO
        val draft = ShiftDraftEngine.createInitialDraft(
            logs = listOf(t172Late, t50Manual, t171Single, t172Early),
            defaultOperator = "Arif, Basri"
        )

        assertEquals(13, draft.tankRows.size)
        assertEquals(
            listOf("T.101", "T.111", "T.102", "T.112", "T.121", "T.122"),
            draft.tankRows.filter { it.group == TankGroup.PREBLEND }.map { it.tankId }
        )
        assertEquals(
            listOf("T.171", "T.172", "T.173", "T.174", "T.176", "T.177", "T.50"),
            draft.tankRows.filter { it.group == TankGroup.TOWERTANK }.map { it.tankId }
        )

        // T.172 has 2 logs -> earliest is S Awal, latest is S Akhir
        val row172 = draft.tankRows.first { it.tankId == "T.172" }
        assertEquals("47.742", row172.sAwal.quantityKg)
        assertEquals("226.3", row172.sAwal.soundingCm)
        assertEquals("99.567", row172.sAkhir.quantityKg)
        assertEquals("508.4", row172.sAkhir.soundingCm)
        assertEquals("belum sampel", row172.summaryNote)
        assertEquals(51825L, ShiftDraftEngine.computeRowDeltaKg(row172))

        // T.171 has 1 log -> S Awal == S Akhir, detailNote = "Tidak ada pergerakan"
        val row171 = draft.tankRows.first { it.tankId == "T.171" }
        assertEquals("121.718", row171.sAwal.quantityKg)
        assertEquals("121.718", row171.sAkhir.quantityKg)
        assertEquals("Tidak ada pergerakan", row171.detailNote)
        assertNull(ShiftDraftEngine.computeRowDeltaKg(row171))

        // T.50 manual kg
        val row50 = draft.tankRows.first { it.tankId == "T.50" }
        assertEquals("19.840", row50.sAkhir.quantityKg)
        assertEquals("-", row50.sAkhir.soundingCm)
        assertEquals("flushing", row50.summaryNote)

        // Totals
        assertEquals(47742L + 121718L + 19840L, ShiftDraftEngine.totalInitialKg(draft.tankRows))
        assertEquals(99567L + 121718L + 19840L, ShiftDraftEngine.totalFinalKg(draft.tankRows))
    }

    @Test
    fun preservesManualEditsLikeEmptyAndSerializesDraftWithMoshi() {
        val initial = ShiftDraftEngine.createInitialDraft(
            logs = emptyList(),
            defaultOperator = "Arif, Basri"
        )
        val editedRows = initial.tankRows.map { row ->
            if (row.tankId == "T.102") {
                row.copy(
                    sAwal = SoundingPointDraft(quantityKg = "47.336"),
                    sAkhir = SoundingPointDraft(quantityKg = "Empty"),
                    oilType = "SH 36-39",
                    summaryNote = "Empty",
                    isManuallyEdited = true
                )
            } else {
                row
            }
        }
        val editedDraft = initial.copy(
            dateLabel = "Rabu, 25 Nov 2026",
            shift = ReportTimeSlot.MALAM,
            nbNote = "* Return Mt2 ke 177, Masuk Laporan Pagi, Qty: 1.521 kg",
            transfers = listOf(
                TransferLogItem(1L, "24.00 - 05.35", "TF Rpo 185 ke 172", "51.825")
            ),
            tankRows = editedRows
        )

        // Sync without force preserves manual edit on T.102
        val afterPassiveSync = ShiftDraftEngine.syncDraftWithHistory(
            current = editedDraft,
            logs = emptyList(),
            forceOverwriteManual = false
        )
        val t102 = afterPassiveSync.tankRows.first { it.tankId == "T.102" }
        assertEquals("Empty", ShiftDraftEngine.formatQuantityDisplay(t102.sAkhir.quantityKg))
        assertEquals(0L, ShiftDraftEngine.parseQuantityKg(t102.sAkhir.quantityKg))
        assertEquals(47336L, ShiftDraftEngine.parseQuantityKg(t102.sAwal.quantityKg))
        assertEquals(-47336L, ShiftDraftEngine.computeRowDeltaKg(t102))

        // Moshi round-trip serialization works cleanly
        val adapter = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
            .adapter(ShiftReportDraft::class.java)
        val json = adapter.toJson(afterPassiveSync)
        val restored = adapter.fromJson(json)
        assertNotNull(restored)
        assertEquals("Rabu, 25 Nov 2026", restored!!.dateLabel)
        assertEquals(ReportTimeSlot.MALAM, restored.shift)
        assertEquals(1, restored.transfers.size)
        assertEquals("Empty", restored.tankRows.first { it.tankId == "T.102" }.sAkhir.quantityKg)
        assertTrue(restored.nbNote.contains("1.521"))
    }
}
