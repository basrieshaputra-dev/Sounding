package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sounding_logs")
data class SoundingLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestampIso: String,
    val createdAtEpochMs: Long,
    val shift: String = "",
    val tankId: String,
    val product: String,
    val sounding: Double,
    val tableLevel: Double,
    val highLevel: Double,
    val temp: Double,
    val totalVol: Double,
    val density: Double,
    val vcf: Double,
    val stockKg: Long,
    val stockTon: Double,
    val isManualKg: Boolean = false,
    val note: String = ""
)
