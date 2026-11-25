package com.example.data

import android.content.Context
import com.example.data.model.CalibrationDataset
import com.example.data.model.CalibrationEntry
import com.example.data.model.CalibrationTable
import com.example.data.model.SoundingCalculation
import com.example.data.model.TankSpec
import com.squareup.moshi.JsonReader
import okio.buffer
import okio.source
import java.io.InputStream
import java.util.Locale
import kotlin.math.floor
import kotlin.math.roundToLong

object CalibrationEngine {

    private const val ASSET_FILE_NAME = "calibration_data.json"
    private const val THERMAL_EXPANSION_COEFF = 0.0000348

    @Volatile
    private var cachedDataset: CalibrationDataset? = null

    fun loadFromAssets(context: Context): CalibrationDataset {
        cachedDataset?.let { return it }
        return synchronized(this) {
            cachedDataset ?: context.assets.open(ASSET_FILE_NAME).use { stream ->
                parseDataset(stream)
            }.also { cachedDataset = it }
        }
    }

    fun parseDataset(inputStream: InputStream): CalibrationDataset {
        val tanks = mutableListOf<TankSpec>()
        val calTables = LinkedHashMap<String, CalibrationTable>()
        val densityTable = LinkedHashMap<String, Map<Int, Double>>()

        JsonReader.of(inputStream.source().buffer()).use { reader ->
            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "tanks" -> readTanks(reader, tanks)
                    "calibrationTables" -> readCalibrationTables(reader, calTables)
                    "densityTable" -> readDensityTable(reader, densityTable)
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
        }

        return CalibrationDataset(
            tanks = tanks,
            calibrationTables = calTables,
            densityTable = densityTable,
            productNames = densityTable.keys.toList()
        )
    }

    private fun readTanks(reader: JsonReader, out: MutableList<TankSpec>) {
        reader.beginArray()
        while (reader.hasNext()) {
            var id = ""
            var label = ""
            var calTable = ""
            var tableLevel = 0.0
            var tRef = 30.0
            var capacityTon = 0
            var defaultProduct = ""
            var isManualKg = false

            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "id" -> id = reader.nextString()
                    "label" -> label = reader.nextString()
                    "calTable" -> calTable = reader.nextString()
                    "tableLevel" -> tableLevel = reader.nextDouble()
                    "tRef" -> tRef = reader.nextDouble()
                    "capacityTon" -> capacityTon = reader.nextInt()
                    "defaultProduct" -> defaultProduct = reader.nextString()
                    "isManualKg" -> isManualKg = reader.nextBoolean()
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
            out.add(
                TankSpec(
                    id = id,
                    label = label,
                    calTable = calTable,
                    tableLevel = tableLevel,
                    tRef = tRef,
                    capacityTon = capacityTon,
                    defaultProduct = defaultProduct,
                    isManualKg = isManualKg || calTable.isBlank()
                )
            )
        }
        reader.endArray()
    }

    private fun readCalibrationTables(
        reader: JsonReader,
        out: MutableMap<String, CalibrationTable>
    ) {
        reader.beginObject()
        while (reader.hasNext()) {
            val tableId = reader.nextName()
            var capacityLabel = ""
            var tableLevel = 0.0
            val entries = HashMap<Int, CalibrationEntry>(1024)
            var maxVol = 0.0

            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "isi_bersih" -> {
                        if (reader.peek() == JsonReader.Token.NULL) {
                            reader.nextNull<Any>()
                        } else {
                            capacityLabel = reader.nextString()
                        }
                    }
                    "tinggi_meja" -> {
                        if (reader.peek() == JsonReader.Token.NULL) {
                            reader.nextNull<Any>()
                        } else {
                            tableLevel = reader.nextDouble()
                        }
                    }
                    "entries" -> {
                        reader.beginObject()
                        while (reader.hasNext()) {
                            val heightKey = reader.nextName().toIntOrNull()
                            reader.beginArray()
                            val vol = if (reader.hasNext()) reader.nextDouble() else 0.0
                            val diff = if (reader.hasNext()) reader.nextDouble() else 0.0
                            while (reader.hasNext()) reader.skipValue()
                            reader.endArray()

                            if (heightKey != null) {
                                entries[heightKey] = CalibrationEntry(vol, diff)
                                if (vol > maxVol) maxVol = vol
                            }
                        }
                        reader.endObject()
                    }
                    else -> reader.skipValue()
                }
            }
            reader.endObject()

            val sortedKeys = entries.keys.sorted()
            out[tableId] = CalibrationTable(
                id = tableId,
                capacityLabel = capacityLabel,
                tableLevel = tableLevel,
                entries = entries,
                sortedKeys = sortedKeys,
                maxVolumeL = maxVol
            )
        }
        reader.endObject()
    }

    private fun readDensityTable(
        reader: JsonReader,
        out: MutableMap<String, Map<Int, Double>>
    ) {
        reader.beginObject()
        while (reader.hasNext()) {
            val product = reader.nextName()
            val temps = HashMap<Int, Double>(96)
            reader.beginObject()
            while (reader.hasNext()) {
                val tempKey = reader.nextName().toIntOrNull()
                val densityVal = reader.nextDouble()
                if (tempKey != null) {
                    temps[tempKey] = densityVal
                }
            }
            reader.endObject()
            out[product] = temps
        }
        reader.endObject()
    }

    fun parseDecimalInput(raw: String): Double? {
        val cleaned = raw.trim().replace(',', '.')
        if (cleaned.isEmpty() || cleaned == "-" || cleaned == "." || cleaned == "-.") {
            return null
        }
        return cleaned.toDoubleOrNull()?.takeIf { it.isFinite() }
    }

    fun calculateManualKg(
        tank: TankSpec,
        product: String,
        manualKg: Double,
        note: String
    ): SoundingCalculation {
        val safeKg = manualKg.coerceAtLeast(0.0).roundToLong()
        val stockTon = safeKg / 1000.0
        return SoundingCalculation(
            tankId = tank.id,
            tankLabel = tank.label,
            calTableId = "",
            capacityTon = tank.capacityTon,
            maxTableHeightCm = 0,
            maxTableVolL = 0.0,
            product = product.trim(),
            note = note.trim(),
            isManualKg = true,
            sounding = 0.0,
            tableLevel = 0.0,
            highLevel = 0.0,
            baseHeight = 0,
            baseVol = 0.0,
            fractionCm = 0.0,
            diffPerCm = 0.0,
            fractionVol = 0.0,
            totalVol = 0.0,
            temp = 0.0,
            tRef = tank.tRef,
            vcf = 1.0,
            density = 0.0,
            stockKg = safeKg,
            stockTon = stockTon,
            warnings = emptyList(),
            fillRatio = 0f,
            levelFillRatio = 0f,
            levelFillPercent = 0.0
        )
    }

    fun calculate(
        dataset: CalibrationDataset,
        tank: TankSpec,
        product: String,
        sounding: Double,
        temp: Double
    ): SoundingCalculation {
        val highLevel = sounding + tank.tableLevel
        val table = dataset.calibrationTables[tank.calTable]
        val entries = table?.entries.orEmpty()
        val sortedKeys = table?.sortedKeys.orEmpty()

        var baseVol = 0.0
        var diffPerCm = 0.0
        var baseHeight = 0
        var fractionCm = 0.0
        val warnings = mutableListOf<String>()

        if (highLevel < 0.0) {
            warnings.add(
                String.format(
                    Locale.US,
                    "High Level negatif (%.2f cm), volume dianggap 0 L.",
                    highLevel
                )
            )
        } else {
            baseHeight = floor(highLevel).toInt()
            fractionCm = highLevel - baseHeight

            val exactEntry = entries[baseHeight]
            if (exactEntry != null) {
                baseVol = exactEntry.baseVol
                diffPerCm = exactEntry.diffPerCm
            } else if (sortedKeys.isNotEmpty()) {
                val minKey = sortedKeys.first()
                val maxKey = sortedKeys.last()
                if (baseHeight < minKey) {
                    warnings.add("Tinggi di bawah batas minimum tabel kalibrasi ($minKey cm).")
                } else {
                    var matched = minKey
                    for (k in sortedKeys) {
                        if (k <= baseHeight) matched = k else break
                    }
                    val matchedEntry = entries[matched]
                    baseVol = matchedEntry?.baseVol ?: 0.0
                    diffPerCm = matchedEntry?.diffPerCm ?: 0.0
                    if (baseHeight > maxKey) {
                        warnings.add("Tinggi melebihi batas maksimum tabel ($maxKey cm).")
                    }
                }
            }
        }

        val fractionVol = fractionCm * diffPerCm
        val totalVol = baseVol + fractionVol

        val prodDensities = dataset.densityTable[product].orEmpty()
        val tempInt = floor(temp).toInt()
        var density = 0.0
        val exactDensity = prodDensities[tempInt]
        if (exactDensity != null) {
            density = exactDensity
        } else if (prodDensities.isNotEmpty()) {
            val tKeys = prodDensities.keys.sorted()
            var nearest = tKeys.first()
            for (tk in tKeys) {
                if (tk <= tempInt) nearest = tk else break
            }
            density = prodDensities[nearest] ?: 0.0
            warnings.add("Suhu ${tempInt}°C memakai density suhu terdekat (${nearest}°C).")
        }

        val vcf = ((temp - tank.tRef) * THERMAL_EXPANSION_COEFF) + 1.0
        val stockKg = (totalVol * vcf * density).roundToLong()
        val stockTon = stockKg / 1000.0

        val maxTableHeight = sortedKeys.lastOrNull() ?: 900
        val maxTableVol = table?.maxVolumeL ?: 1.0
        val fillRatio = if (maxTableVol > 0.0) {
            (totalVol / maxTableVol).toFloat().coerceIn(0f, 1f)
        } else {
            0f
        }
        val levelFillPercent = if (maxTableHeight > 0) {
            ((highLevel / maxTableHeight.toDouble()) * 100.0).coerceAtLeast(0.0)
        } else {
            0.0
        }
        val levelFillRatio = (levelFillPercent / 100.0).toFloat().coerceIn(0f, 1f)

        return SoundingCalculation(
            tankId = tank.id,
            tankLabel = tank.label,
            calTableId = tank.calTable,
            capacityTon = tank.capacityTon,
            maxTableHeightCm = maxTableHeight,
            maxTableVolL = maxTableVol,
            product = product,
            sounding = sounding,
            tableLevel = tank.tableLevel,
            highLevel = highLevel,
            baseHeight = baseHeight,
            baseVol = baseVol,
            fractionCm = fractionCm,
            diffPerCm = diffPerCm,
            fractionVol = fractionVol,
            totalVol = totalVol,
            temp = temp,
            tRef = tank.tRef,
            vcf = vcf,
            density = density,
            stockKg = stockKg,
            stockTon = stockTon,
            warnings = warnings,
            fillRatio = fillRatio,
            levelFillRatio = levelFillRatio,
            levelFillPercent = levelFillPercent
        )
    }
}
