package com.example

import com.example.data.BlendingEngine
import com.example.data.PearsonSquareResult
import com.example.data.QuantityInputSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlendingEngineTest {

    @Test
    fun shorteningTable_containsAll21AccurateSpecs() {
        val specs = BlendingEngine.SHORTENING_SPECS
        assertEquals(21, specs.size)

        specs.forEach { spec ->
            assertEquals(
                "RPO + RPS must equal 100.0 for ${spec.name}",
                100.0,
                spec.standardRpoPercent + spec.standardRpsPercent,
                1e-6
            )
            assertTrue(
                "Typical IV min < max for ${spec.name}",
                spec.typicalIvMin < spec.typicalIvMax
            )
            val defaultIv = BlendingEngine.calculateEstimatedIv(
                rpoPercent = spec.standardRpoPercent,
                rpsPercent = spec.standardRpsPercent,
                ivRpo = BlendingEngine.DEFAULT_IV_RPO,
                ivRps = BlendingEngine.DEFAULT_IV_RPS
            )
            assertTrue(
                "Default IV ($defaultIv) should be in spec (${spec.typicalIvRangeLabel}) for ${spec.name}",
                BlendingEngine.isWithinTypicalIv(defaultIv, spec)
            )
        }

        // Spot-check edge rows
        val first = specs.first()
        assertEquals("Shortening 33 - 39", first.name)
        assertEquals(100.0, first.standardRpoPercent, 1e-6)
        assertEquals(0.0, first.standardRpsPercent, 1e-6)
        assertTrue(first.isPureRpoStandard)

        val last = specs.last()
        assertEquals("Shortening 50 - 52", last.name)
        assertEquals(35.0, last.standardRpoPercent, 1e-6)
        assertEquals(65.0, last.standardRpsPercent, 1e-6)
        assertEquals(40.00, last.typicalIvMin, 1e-6)
        assertEquals(41.00, last.typicalIvMax, 1e-6)
    }

    @Test
    fun pearsonSquareAutoCorrection_bringsOutOfSpecIvBackIntoTypicalRange() {
        val spec = BlendingEngine.SHORTENING_SPECS.first { it.name == "Shortening 42 - 45" }
        val actualIvRpo = 54.0
        val actualIvRps = 33.0

        val standardEstimated = BlendingEngine.calculateEstimatedIv(
            rpoPercent = spec.standardRpoPercent,
            rpsPercent = spec.standardRpsPercent,
            ivRpo = actualIvRpo,
            ivRps = actualIvRps
        )
        assertFalse(BlendingEngine.isWithinTypicalIv(standardEstimated, spec))

        val result = BlendingEngine.calculatePearsonSquareRatio(
            targetIv = spec.typicalIvMid,
            ivRpo = actualIvRpo,
            ivRps = actualIvRps
        )
        assertTrue(result is PearsonSquareResult.Computed)
        val computed = result as PearsonSquareResult.Computed
        assertFalse(computed.isClamped)
        assertEquals(100.0, computed.rpoPercent + computed.rpsPercent, 1e-6)
        assertTrue(BlendingEngine.isWithinTypicalIv(computed.estimatedIv, spec))
        assertEquals(48.5, computed.estimatedIv, 0.05)
    }

    @Test
    fun pearsonSquare_preventsDivisionByZeroWhenIvEqual() {
        val identical = BlendingEngine.calculatePearsonSquareRatio(
            targetIv = 48.5,
            ivRpo = 45.0,
            ivRps = 45.0
        )
        assertTrue(identical is PearsonSquareResult.IdenticalIvError)

        val invalid = BlendingEngine.calculatePearsonSquareRatio(
            targetIv = 48.5,
            ivRpo = 0.0,
            ivRps = 34.5
        )
        assertTrue(invalid is PearsonSquareResult.InvalidInputError)
    }

    @Test
    fun threeWayQuantitySolver_computesAccuratelyFromTotalRpoAndRps() {
        // 80% RPO / 20% RPS
        val fromTotal = BlendingEngine.calculateQuantities(
            source = QuantityInputSource.TOTAL,
            inputValueKg = 20_000.0,
            rpoPercent = 80.0,
            rpsPercent = 20.0
        )
        assertEquals(16_000.0, fromTotal.rpoKg, 1e-4)
        assertEquals(4_000.0, fromTotal.rpsKg, 1e-4)
        assertEquals(20_000.0, fromTotal.totalKg, 1e-4)

        val fromRpo = BlendingEngine.calculateQuantities(
            source = QuantityInputSource.RPO,
            inputValueKg = 16_000.0,
            rpoPercent = 80.0,
            rpsPercent = 20.0
        )
        assertEquals(16_000.0, fromRpo.rpoKg, 1e-4)
        assertEquals(4_000.0, fromRpo.rpsKg, 1e-4)
        assertEquals(20_000.0, fromRpo.totalKg, 1e-4)

        val fromRps = BlendingEngine.calculateQuantities(
            source = QuantityInputSource.RPS,
            inputValueKg = 4_000.0,
            rpoPercent = 80.0,
            rpsPercent = 20.0
        )
        assertEquals(16_000.0, fromRps.rpoKg, 1e-4)
        assertEquals(4_000.0, fromRps.rpsKg, 1e-4)
        assertEquals(20_000.0, fromRps.totalKg, 1e-4)
    }

    @Test
    fun threeWayQuantitySolver_handlesZeroPercentSafelyWithoutNaNOrCrash() {
        val pureRpoFromRps = BlendingEngine.calculateQuantities(
            source = QuantityInputSource.RPS,
            inputValueKg = 5_000.0,
            rpoPercent = 100.0,
            rpsPercent = 0.0
        )
        assertEquals(0.0, pureRpoFromRps.totalKg, 1e-6)
        assertEquals(0.0, pureRpoFromRps.rpoKg, 1e-6)
        assertEquals(0.0, pureRpoFromRps.rpsKg, 1e-6)

        val pureRpoFromTotal = BlendingEngine.calculateQuantities(
            source = QuantityInputSource.TOTAL,
            inputValueKg = 15_000.0,
            rpoPercent = 100.0,
            rpsPercent = 0.0
        )
        assertEquals(15_000.0, pureRpoFromTotal.rpoKg, 1e-6)
        assertEquals(0.0, pureRpoFromTotal.rpsKg, 1e-6)
        assertEquals(15_000.0, pureRpoFromTotal.totalKg, 1e-6)

        val negativeInput = BlendingEngine.calculateQuantities(
            source = QuantityInputSource.TOTAL,
            inputValueKg = -100.0,
            rpoPercent = 75.0,
            rpsPercent = 25.0
        )
        assertEquals(0.0, negativeInput.totalKg, 1e-6)
    }
}
