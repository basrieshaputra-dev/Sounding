package com.example.data.local

import com.example.data.model.SoundingCalculation
import com.example.util.SoundingFormatters
import kotlinx.coroutines.flow.Flow

class SoundingLogRepository(
    private val dao: SoundingLogDao
) {
    val logs: Flow<List<SoundingLogEntity>> = dao.observeAllLogs()

    suspend fun saveCalculation(calc: SoundingCalculation): SoundingLogEntity {
        val now = System.currentTimeMillis()
        val entity = SoundingLogEntity(
            timestampIso = SoundingFormatters.formatIsoUtc(now),
            createdAtEpochMs = now,
            shift = "",
            tankId = calc.tankId,
            product = calc.product,
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
