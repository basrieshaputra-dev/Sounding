package com.example

import com.example.data.CalibrationEngine
import com.example.data.local.SoundingLogEntity
import com.example.data.model.DailyReportMetadata
import com.example.data.model.ReportTimeSlot
import com.example.data.model.TransferLogItem
import com.example.util.DailyReportJpgGenerator
import com.example.util.SoundingFormatters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CalibrationEngineTest {

    private fun loadDataset() =
        File("src/main/assets/calibration_data.json").inputStream().use {
            CalibrationEngine.parseDataset(it)
        }

    @Test
    fun parsesAllTanksInExactRequestedOrderWithConsistentSpecs() {
        val dataset = loadDataset()
        assertEquals(19, dataset.tanks.size)
        assertEquals(14, dataset.calibrationTables.size)
        assertEquals(25, dataset.densityTable.size)

        val expectedOrder = listOf(
            "T.101", "T.111", "T.102", "T.112", "T.121", "T.122",
            "T.171", "T.172", "T.173", "T.174", "T.176", "T.177",
            "T.175", "T.178", "T.179", "T.180", "T.181", "T.182",
            "T.50"
        )
        assertEquals(expectedOrder, dataset.tanks.map { it.id })
        assertEquals(expectedOrder, dataset.tanks.map { it.label })

        val block101To122 = setOf("T.101", "T.111", "T.102", "T.112", "T.121", "T.122")
        for (tank in dataset.tanks) {
            assertEquals("", tank.defaultProduct)
            val expectedTRef = if (tank.id in block101To122) 30.0 else 32.0
            assertEquals("Wrong tRef for ${tank.id}", expectedTRef, tank.tRef, 1e-6)
            assertEquals("Wrong isManualKg for ${tank.id}", tank.id == "T.50", tank.isManualKg)
        }
    }

    @Test
    fun calculatesT50ManualKgAndConvertsToTon() {
        val dataset = loadDataset()
        val t50 = dataset.tanks.first { it.id == "T.50" }
        assertTrue(t50.isManualKg)

        val result = CalibrationEngine.calculateManualKg(
            tank = t50,
            product = "RBDPO",
            manualKg = 18500.0,
            note = "CBS Texturizing"
        )

        assertTrue(result.isManualKg)
        assertEquals("T.50", result.tankId)
        assertEquals("RBDPO", result.product)
        assertEquals("CBS Texturizing", result.note)
        assertEquals(18500L, result.stockKg)
        assertEquals(18.5, result.stockTon, 1e-6)
        assertTrue(result.warnings.isEmpty())

        val waText = SoundingFormatters.formatWhatsAppSummary(result)
        assertTrue(waText.contains("T.50"))
        assertTrue(waText.contains("CBS Texturizing"))
        assertTrue(waText.contains("Input Manual"))
        assertTrue(waText.contains("18500 kg (18.5 Ton)"))
    }

    @Test
    fun formatsTonWithSingleDecimalWhileKeepingKgIntact() {
        assertEquals("70.304", SoundingFormatters.formatIndonesianInteger(70304L))
        assertEquals("70.3", SoundingFormatters.formatTon(70304L / 1000.0))
        assertEquals("140.4", SoundingFormatters.formatTon(140.439))
    }

    @Test
    fun buildsDailyReportSnapshotWithExactTwoColumnsAndT50() {
        val logs = listOf(
            SoundingLogEntity(
                id = 3,
                timestampIso = "2026-11-23T09:30:00Z",
                createdAtEpochMs = 3000L,
                tankId = "T.50",
                product = "RBDPO",
                sounding = 0.0,
                tableLevel = 0.0,
                highLevel = 0.0,
                temp = 0.0,
                totalVol = 0.0,
                density = 0.0,
                vcf = 1.0,
                stockKg = 12500L,
                stockTon = 12.5,
                isManualKg = true,
                note = "CBS Texturizing"
            ),
            SoundingLogEntity(
                id = 2,
                timestampIso = "2026-11-23T09:20:00Z",
                createdAtEpochMs = 2000L,
                tankId = "T.171",
                product = "RBDPO",
                sounding = 729.7,
                tableLevel = 31.6,
                highLevel = 761.3,
                temp = 50.0,
                totalVol = 157680.1,
                density = 0.8901,
                vcf = 1.0006264,
                stockKg = 140439L,
                stockTon = 140.439
            ),
            SoundingLogEntity(
                id = 1,
                timestampIso = "2026-11-23T09:10:00Z",
                createdAtEpochMs = 1000L,
                tankId = "T.101",
                product = "RHBDPKO MP 36-42",
                sounding = 401.0,
                tableLevel = -2.5,
                highLevel = 398.5,
                temp = 30.0,
                totalVol = 51000.0,
                density = 0.9100,
                vcf = 1.0,
                stockKg = 46410L,
                stockTon = 46.410
            )
        )

        val metadata = DailyReportMetadata(
            dateLabel = "Senin, 23 Nov 26",
            timeSlot = ReportTimeSlot.PAGI,
            operatorName = "Arif & Basri",
            transfers = listOf(
                TransferLogItem(id = 1L, jam = "09.30", keterangan = "TF T.101 ke T.171", qtyKg = "5000")
            )
        )

        val snapshot = DailyReportJpgGenerator.buildReportSnapshot(logs, metadata)

        assertEquals(
            listOf("T.101", "T.111", "T.102", "T.112", "T.121", "T.122"),
            snapshot.leftRows.map { it.tankId }
        )
        assertEquals(
            listOf("T.171", "T.172", "T.173", "T.174", "T.176", "T.177", "T.50"),
            snapshot.rightRows.map { it.tankId }
        )
        val t50Row = snapshot.rightRows.last()
        assertEquals("T.50", t50Row.tankId)
        assertNotNull(t50Row.log)
        assertEquals(12500L, t50Row.log!!.stockKg)
        assertEquals("CBS Texturizing", t50Row.log!!.note)

        assertEquals(46410L, snapshot.leftTotalKg)
        assertEquals(152939L, snapshot.rightTotalKg)
        assertEquals(199349L, snapshot.grandTotalKg)
        assertEquals(199.349, snapshot.grandTotalTon, 1e-6)
        assertEquals(1, snapshot.transfers.size)
    }

    @Test
    fun calculatesT101WithNegativeTableLevelAndNearestDensityWarning() {
        val dataset = loadDataset()
        val t101 = dataset.tanks.first { it.id == "T.101" }
        assertEquals("T-0101", t101.calTable)
        assertEquals(-2.5, t101.tableLevel, 1e-6)
        assertEquals(30.0, t101.tRef, 1e-6)

        val result = CalibrationEngine.calculate(
            dataset = dataset,
            tank = t101,
            product = "RHBDPKO MP 36-42",
            sounding = 401.0,
            temp = 30.0
        )

        assertEquals(398.5, result.highLevel, 1e-6)
        assertEquals(398, result.baseHeight)
        assertEquals(0.5, result.fractionCm, 1e-6)
        assertEquals(1.0, result.vcf, 1e-9)
        assertEquals(0.9100, result.density, 1e-6)
        assertTrue(result.warnings.any { it.contains("suhu terdekat") })
        assertTrue(result.totalVol > 50_000.0)
        assertTrue(result.stockKg > 45_000L)
    }

    @Test
    fun calculatesT171WithPositiveTableLevelAndFractionalInterpolation() {
        val dataset = loadDataset()
        val t171 = dataset.tanks.first { it.id == "T.171" }
        val result = CalibrationEngine.calculate(
            dataset = dataset,
            tank = t171,
            product = "RBDPO",
            sounding = 729.7,
            temp = 50.0
        )

        assertEquals(761.3, result.highLevel, 1e-6)
        assertEquals(761, result.baseHeight)
        assertEquals(0.3, result.fractionCm, 1e-6)
        assertEquals(157618.0, result.baseVol, 1e-6)
        assertEquals(207.0, result.diffPerCm, 1e-6)
        assertEquals(62.1, result.fractionVol, 1e-6)
        assertEquals(157680.1, result.totalVol, 1e-6)
        assertEquals(1.0006264, result.vcf, 1e-9)
        assertEquals(0.8901, result.density, 1e-6)
        assertFalse(result.warnings.isNotEmpty())
        assertEquals(140439L, result.stockKg)
        assertEquals(140.439, result.stockTon, 1e-6)
        assertEquals(860, result.maxTableHeightCm)
        assertEquals((761.3 / 860.0) * 100.0, result.levelFillPercent, 1e-6)
        assertEquals((761.3 / 860.0).toFloat(), result.levelFillRatio, 1e-4f)

        val waText = SoundingFormatters.formatWhatsAppSummary(result)
        assertTrue(waText.contains("T.171 | RBDPO"))
        assertTrue(waText.contains("140439 kg (140.4 Ton)"))
    }

    @Test
    fun handlesNegativeHighLevelWarning() {
        val dataset = loadDataset()
        val t101 = dataset.tanks.first { it.id == "T.101" }
        val result = CalibrationEngine.calculate(
            dataset = dataset,
            tank = t101,
            product = "RBDPO",
            sounding = 1.0,
            temp = 35.0
        )
        assertEquals(-1.5, result.highLevel, 1e-6)
        assertEquals(0.0, result.totalVol, 1e-6)
        assertEquals(0L, result.stockKg)
        assertEquals(0.0, result.levelFillPercent, 1e-6)
        assertEquals(0f, result.levelFillRatio, 1e-6f)
        assertTrue(result.warnings.any { it.contains("High Level negatif") })
    }

    @Test
    fun parsesCommaAndDotDecimals() {
        assertEquals(729.7, CalibrationEngine.parseDecimalInput("729,7")!!, 1e-9)
        assertEquals(401.0, CalibrationEngine.parseDecimalInput(" 401 ")!!, 1e-9)
        assertEquals(null, CalibrationEngine.parseDecimalInput(""))
        assertEquals(null, CalibrationEngine.parseDecimalInput("."))
    }
}
