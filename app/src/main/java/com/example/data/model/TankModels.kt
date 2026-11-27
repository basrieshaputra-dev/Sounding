package com.example.data.model

data class TankSpec(
    val id: String,
    val label: String,
    val calTable: String,
    val tableLevel: Double,
    val tRef: Double,
    val capacityTon: Int,
    val defaultProduct: String,
    val isManualKg: Boolean = false
)

data class CalibrationEntry(
    val baseVol: Double,
    val diffPerCm: Double
)

data class CalibrationTable(
    val id: String,
    val capacityLabel: String,
    val tableLevel: Double,
    val entries: Map<Int, CalibrationEntry>,
    val sortedKeys: List<Int>,
    val maxVolumeL: Double
)

data class CalibrationDataset(
    val tanks: List<TankSpec>,
    val calibrationTables: Map<String, CalibrationTable>,
    val densityTable: Map<String, Map<Int, Double>>,
    val productNames: List<String>
)

data class SoundingCalculation(
    val tankId: String,
    val tankLabel: String,
    val calTableId: String,
    val capacityTon: Int,
    val maxTableHeightCm: Int,
    val maxTableVolL: Double,
    val product: String,
    val note: String = "",
    val isManualKg: Boolean = false,
    val sounding: Double,
    val tableLevel: Double,
    val highLevel: Double,
    val baseHeight: Int,
    val baseVol: Double,
    val fractionCm: Double,
    val diffPerCm: Double,
    val fractionVol: Double,
    val totalVol: Double,
    val temp: Double,
    val tRef: Double,
    val vcf: Double,
    val density: Double,
    val stockKg: Long,
    val stockTon: Double,
    val warnings: List<String>,
    val fillRatio: Float,
    val levelFillRatio: Float,
    val levelFillPercent: Double
)

data class TransferLogItem(
    val id: Long,
    val jam: String = "",
    val keterangan: String = "",
    val qtyKg: String = ""
)

data class DailyReportMetadata(
    val dateLabel: String,
    val timeLabel: String = "",
    val operatorName: String,
    val transfers: List<TransferLogItem>
)
