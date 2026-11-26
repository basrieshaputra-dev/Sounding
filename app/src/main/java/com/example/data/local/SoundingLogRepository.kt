package com.example.data.local

import com.example.data.CalibrationEngine
import com.example.data.model.SoundingCalculation
import com.example.util.SoundingFormatters
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SoundingLogRepository(
    private val dao: SoundingLogDao
) {
    val logs: Flow<List<SoundingLogEntity>> = dao.observeAllLogs().map { list ->
        list.map { item ->
            val normalized = CalibrationEngine.normalizeProductName(item.product)
            if (normalized != item.product) item.copy(product = normalized) else item
        }
    }

    suspend fun saveCalculation(
        calc: SoundingCalculation,
        shiftLabel: String = "",
        recordedAtEpochMs: Long = System.currentTimeMillis()
    ): SoundingLogEntity {
        val entity = SoundingLogEntity(
            timestampIso = SoundingFormatters.formatIsoUtc(recordedAtEpochMs),
            createdAtEpochMs = recordedAtEpochMs,
            shift = shiftLabel,
            tankId = calc.tankId,
            product = CalibrationEngine.normalizeProductName(calc.product),
            sounding = calc.sounding,
            tableLevel = calc.tableLevel,
            highLevel = calc.highLevel,
            temp = calc.temp,
            totalVol = calc.totalVol,
            density = calc.density,
            vcf = calc.vcf,
            stockKg = calc.stockKg,
            stockTon = calc.stockTon,
            isManualKg = calc.isManualKg,
            note = calc.note
        )
        val generatedId = dao.insertLog(entity)
        return entity.copy(id = generatedId)
    }

    suspend fun deleteLog(id: Long) {
        dao.deleteLogById(id)
    }

    suspend fun clearAll() {
        dao.clearAllLogs()
    }
}
