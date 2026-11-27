package com.example.data.local

import android.util.Log
import com.example.data.CalibrationEngine
import com.example.data.model.SoundingCalculation
import com.example.util.SoundingFormatters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class SoundingLogRepository(
    private val dao: SoundingLogDao
) {
    val logs: Flow<List<SoundingLogEntity>> = dao.observeAllLogs()
        .map { list ->
            list.map { item ->
                val normalized = CalibrationEngine.normalizeProductName(item.product)
                if (normalized != item.product) item.copy(product = normalized) else item
            }
        }
        .catch { err ->
            if (err is CancellationException) throw err
            Log.e(TAG, "Error observing local Room logs; emitting empty list", err)
            emit(emptyList())
        }

    suspend fun saveCalculation(
        calc: SoundingCalculation,
        shiftLabel: String = "",
        recordedAtEpochMs: Long = System.currentTimeMillis()
    ): Result<SoundingLogEntity> {
        return try {
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
            Result.success(entity.copy(id = generatedId))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed saving calculation to local Room database", e)
            Result.failure(e)
        }
    }

    suspend fun deleteLog(id: Long): Result<Unit> {
        return try {
            dao.deleteLogById(id)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed deleting local Room log id=$id", e)
            Result.failure(e)
        }
    }

    suspend fun clearAll(): Result<Unit> {
        return try {
            dao.clearAllLogs()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed clearing local Room logs", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "SoundingLogRepository"
    }
}
