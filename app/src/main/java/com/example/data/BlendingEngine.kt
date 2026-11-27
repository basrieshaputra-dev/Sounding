package com.example.data

import com.example.util.SoundingFormatters
import java.util.Locale
import kotlin.math.abs
import kotlin.math.round

data class ShorteningSpec(
    val id: String,
    val name: String,
    val meltingPointLabel: String,
    val standardRpoPercent: Double,
    val standardRpsPercent: Double,
    val typicalIvMin: Double,
    val typicalIvMax: Double
) {
    val typicalIvMid: Double
        get() = (typicalIvMin + typicalIvMax) / 2.0

    val typicalIvRangeLabel: String
        get() = "${SoundingFormatters.formatFixed(typicalIvMin, 2)} - ${SoundingFormatters.formatFixed(typicalIvMax, 2)}"

    val isPureRpoStandard: Boolean
        get() = standardRpsPercent <= 0.0
}

enum class QuantityInputSource {
    TOTAL,
    RPO,
    RPS
}

data class BlendingQuantities(
    val rpoKg: Double = 0.0,
    val rpsKg: Double = 0.0,
    val totalKg: Double = 0.0
) {
    val rpoTon: Double get() = rpoKg / 1000.0
    val rpsTon: Double get() = rpsKg / 1000.0
    val totalTon: Double get() = totalKg / 1000.0
}

sealed interface PearsonSquareResult {
    data class Computed(
        val rpoPercent: Double,
        val rpsPercent: Double,
        val estimatedIv: Double,
        val isClamped: Boolean
    ) : PearsonSquareResult

    data object IdenticalIvError : PearsonSquareResult
    data object InvalidInputError : PearsonSquareResult
}

object BlendingEngine {
    const val DEFAULT_IV_RPO = 52.0
    const val DEFAULT_IV_RPS = 34.5
    private const val EPSILON = 1e-6

    /**
     * Exact 21-row hardcoded reference table: "RASIO BLENDING SHORTENING"
     * Reference baseline: IV RPO = 52.0, IV RPS = 34.5.
     */
    val SHORTENING_SPECS: List<ShorteningSpec> = listOf(
        ShorteningSpec("sh_33_39", "Shortening 33 - 39", "33 - 39°C", 100.0, 0.0, 51.00, 53.00),
        ShorteningSpec("sh_34_36", "Shortening 34 - 36", "34 - 36°C", 100.0, 0.0, 51.00, 53.00),
        ShorteningSpec("sh_36_39", "Shortening 36 - 39", "36 - 39°C", 98.0, 2.0, 51.50, 52.50),
        ShorteningSpec("sh_38_40", "Shortening 38 - 40", "38 - 40°C", 94.5, 5.5, 50.80, 51.50),
        ShorteningSpec("sh_38_41", "Shortening 38 - 41", "38 - 41°C", 92.0, 8.0, 50.00, 51.00),
        ShorteningSpec("sh_38_42", "Shortening 38 - 42", "38 - 42°C", 86.0, 14.0, 49.00, 50.00),
        ShorteningSpec("sh_39_42", "Shortening 39 - 42", "39 - 42°C", 86.0, 14.0, 49.00, 50.00),
        ShorteningSpec("sh_40_42", "Shortening 40 - 42", "40 - 42°C", 86.0, 14.0, 49.00, 50.00),
        ShorteningSpec("sh_41_43", "Shortening 41 - 43", "41 - 43°C", 84.0, 16.0, 48.50, 49.50),
        ShorteningSpec("sh_42_44", "Shortening 42 - 44", "42 - 44°C", 80.0, 20.0, 48.00, 48.80),
        ShorteningSpec("sh_42_45", "Shortening 42 - 45", "42 - 45°C", 80.0, 20.0, 48.00, 49.00),
        ShorteningSpec("sh_43_45", "Shortening 43 - 45", "43 - 45°C", 80.0, 20.0, 48.00, 49.00),
        ShorteningSpec("sh_44_46", "Shortening 44 - 46", "44 - 46°C", 75.0, 25.0, 47.00, 48.00),
        ShorteningSpec("sh_43_47", "Shortening 43 - 47", "43 - 47°C", 69.0, 31.0, 46.00, 47.00),
        ShorteningSpec("sh_44_47", "Shortening 44 - 47", "44 - 47°C", 69.0, 31.0, 46.00, 47.00),
        ShorteningSpec("sh_45_47", "Shortening 45 - 47", "45 - 47°C", 69.0, 31.0, 46.00, 47.00),
        ShorteningSpec("sh_45_48", "Shortening 45 - 48", "45 - 48°C", 69.0, 31.0, 46.00, 47.00),
        ShorteningSpec("sh_46_48", "Shortening 46 - 48", "46 - 48°C", 69.0, 31.0, 46.00, 47.00),
        ShorteningSpec("sh_48_50", "Shortening 48 - 50", "48 - 50°C", 46.0, 54.0, 42.00, 43.00),
        ShorteningSpec("sh_48_52", "Shortening 48 - 52", "48 - 52°C", 35.0, 65.0, 40.00, 41.00),
        ShorteningSpec("sh_50_52", "Shortening 50 - 52", "50 - 52°C", 35.0, 65.0, 40.00, 41.00)
    )

    fun parseDecimal(raw: String): Double? {
        return try {
            val normalized = raw.trim().replace(',', '.')
            if (normalized.isEmpty()) return null
            val parsed = normalized.toDoubleOrNull() ?: return null
            if (!parsed.isFinite() || parsed < 0.0) null else parsed
        } catch (_: Throwable) {
            null
        }
    }

    fun calculateEstimatedIv(
        rpoPercent: Double,
        rpsPercent: Double,
        ivRpo: Double,
        ivRps: Double
    ): Double {
        return try {
            if (!rpoPercent.isFinite() || !rpsPercent.isFinite() || !ivRpo.isFinite() || !ivRps.isFinite()) {
                return 0.0
            }
            val result = ((rpoPercent * ivRpo) + (rpsPercent * ivRps)) / 100.0
            if (result.isFinite()) result else 0.0
        } catch (_: Throwable) {
            0.0
        }
    }

    fun isWithinTypicalIv(estimatedIv: Double, spec: ShorteningSpec): Boolean {
        return try {
            estimatedIv.isFinite() &&
                estimatedIv >= (spec.typicalIvMin - EPSILON) &&
                estimatedIv <= (spec.typicalIvMax + EPSILON)
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Pearson Square auto-correction targeting the midpoint of Typical IV.
     * Prevents division by zero when IV_RPO == IV_RPS and guarantees rpo + rps == 100.0%.
     */
    fun calculatePearsonSquareRatio(
        targetIv: Double,
        ivRpo: Double,
        ivRps: Double
    ): PearsonSquareResult {
        return try {
            if (!targetIv.isFinite() || !ivRpo.isFinite() || !ivRps.isFinite() || ivRpo <= 0.0 || ivRps <= 0.0) {
                return PearsonSquareResult.InvalidInputError
            }
            val denom = ivRpo - ivRps
            if (abs(denom) < EPSILON) {
                return PearsonSquareResult.IdenticalIvError
            }

            val rawRpoPercent = ((targetIv - ivRps) / denom) * 100.0
            if (!rawRpoPercent.isFinite()) {
                return PearsonSquareResult.InvalidInputError
            }

            val isClamped = rawRpoPercent < 0.0 || rawRpoPercent > 100.0
            val clampedRpo = rawRpoPercent.coerceIn(0.0, 100.0)
            val roundedRpo = (round(clampedRpo * 10.0) / 10.0).coerceIn(0.0, 100.0)
            val roundedRps = (round((100.0 - roundedRpo) * 10.0) / 10.0).coerceIn(0.0, 100.0)
            val estimatedIv = calculateEstimatedIv(roundedRpo, roundedRps, ivRpo, ivRps)

            PearsonSquareResult.Computed(
                rpoPercent = roundedRpo,
                rpsPercent = roundedRps,
                estimatedIv = estimatedIv,
                isClamped = isClamped
            )
        } catch (_: Throwable) {
            PearsonSquareResult.InvalidInputError
        }
    }

    /**
     * 3-way flexible realtime quantity calculation (Total <-> RPO <-> RPS)
     * with strict division-by-zero protection.
     */
    fun calculateQuantities(
        source: QuantityInputSource,
        inputValueKg: Double,
        rpoPercent: Double,
        rpsPercent: Double
    ): BlendingQuantities {
        return try {
            if (!inputValueKg.isFinite() || inputValueKg <= 0.0 ||
                !rpoPercent.isFinite() || !rpsPercent.isFinite()
            ) {
                return BlendingQuantities()
            }
            val safeRpoPct = rpoPercent.coerceIn(0.0, 100.0)
            val safeRpsPct = rpsPercent.coerceIn(0.0, 100.0)

            when (source) {
                QuantityInputSource.TOTAL -> {
                    val total = inputValueKg
                    val rpoKg = total * (safeRpoPct / 100.0)
                    val rpsKg = total * (safeRpsPct / 100.0)
                    BlendingQuantities(
                        rpoKg = sanitizeKg(rpoKg),
                        rpsKg = sanitizeKg(rpsKg),
                        totalKg = sanitizeKg(total)
                    )
                }

                QuantityInputSource.RPO -> {
                    val rpoFraction = safeRpoPct / 100.0
                    if (rpoFraction <= EPSILON) {
                        return BlendingQuantities(
                            rpoKg = sanitizeKg(inputValueKg),
                            rpsKg = 0.0,
                            totalKg = sanitizeKg(inputValueKg)
                        )
                    }
                    val total = inputValueKg / rpoFraction
                    val rpsKg = (total - inputValueKg).coerceAtLeast(0.0)
                    BlendingQuantities(
                        rpoKg = sanitizeKg(inputValueKg),
                        rpsKg = sanitizeKg(rpsKg),
                        totalKg = sanitizeKg(total)
                    )
                }

                QuantityInputSource.RPS -> {
                    val rpsFraction = safeRpsPct / 100.0
                    if (rpsFraction <= EPSILON) {
                        // 100% RPO product — RPS input is not applicable; avoid division by zero
                        return BlendingQuantities()
                    }
                    val total = inputValueKg / rpsFraction
                    val rpoKg = (total - inputValueKg).coerceAtLeast(0.0)
                    BlendingQuantities(
                        rpoKg = sanitizeKg(rpoKg),
                        rpsKg = sanitizeKg(inputValueKg),
                        totalKg = sanitizeKg(total)
                    )
                }
            }
        } catch (_: Throwable) {
            BlendingQuantities()
        }
    }

    private fun sanitizeKg(value: Double): Double {
        return if (value.isFinite() && value > 0.0) value else 0.0
    }

    /**
     * Clean decimal string for synced input fields (no grouping separators so user can edit freely).
     */
    fun formatInputKg(value: Double): String {
        return try {
            if (!value.isFinite() || value <= 0.0) return ""
            val rounded2 = round(value * 100.0) / 100.0
            val asLong = rounded2.toLong()
            if (abs(rounded2 - asLong) < 1e-6) {
                asLong.toString()
            } else {
                String.format(Locale.US, "%.2f", rounded2).trimEnd('0').trimEnd('.')
            }
        } catch (_: Throwable) {
            ""
        }
    }

    fun formatPercent(value: Double): String {
        return try {
            if (!value.isFinite()) return "0.0%"
            String.format(Locale.US, "%.1f%%", value)
        } catch (_: Throwable) {
            "0.0%"
        }
    }

    fun formatKgDisplay(value: Double): String {
        return try {
            if (!value.isFinite() || value <= 0.0) return "0"
            SoundingFormatters.formatIndonesianNumber(round(value * 10.0) / 10.0)
        } catch (_: Throwable) {
            "0"
        }
    }

    fun formatTonDisplay(valueKg: Double): String {
        return try {
            if (!valueKg.isFinite() || valueKg <= 0.0) return "0.00"
            String.format(Locale.US, "%.2f", valueKg / 1000.0)
        } catch (_: Throwable) {
            "0.00"
        }
    }

    fun formatWhatsAppSummary(
        spec: ShorteningSpec,
        rpoPercent: Double,
        rpsPercent: Double,
        isAutoCorrected: Boolean,
        ivRpo: Double,
        ivRps: Double,
        estimatedIv: Double,
        inSpec: Boolean,
        quantities: BlendingQuantities,
        operatorName: String = "",
        timestampEpochMs: Long = System.currentTimeMillis()
    ): String {
        return try {
            val statusLabel = if (inSpec) "MASUK SPEC TYPICAL IV" else "DI LUAR SPEC TYPICAL IV"
            val ratioMode = if (isAutoCorrected) "Auto-Koreksi (Pearson Square)" else "Rasio Standar Tabel"
            val timeLabel = runCatching {
                SoundingFormatters.formatDisplayDateTime(timestampEpochMs)
            }.getOrDefault("-")
            buildString {
                appendLine("*HASIL BLENDING SHORTENING*")
                appendLine("FAC - Specialty Fats - Texturizing")
                appendLine("━━━━━━━━━━━━━━━━━━━━")
                appendLine("Produk : ${spec.name}")
                appendLine("Mode Rasio : $ratioMode")
                appendLine("Rasio Blend : RPO ${formatPercent(rpoPercent)} | RPS ${formatPercent(rpsPercent)}")
                appendLine("IV Aktual : RPO ${SoundingFormatters.formatFixed(ivRpo, 2)} | RPS ${SoundingFormatters.formatFixed(ivRps, 2)}")
                appendLine("Estimasi IV : ${SoundingFormatters.formatFixed(estimatedIv, 2)} (Target ${spec.typicalIvRangeLabel})")
                appendLine("Status IV : $statusLabel")
                appendLine("━━━━━━━━━━━━━━━━━━━━")
                appendLine("• Kebutuhan RPO : ${formatKgDisplay(quantities.rpoKg)} kg (${formatTonDisplay(quantities.rpoKg)} Ton)")
                appendLine("• Kebutuhan RPS : ${formatKgDisplay(quantities.rpsKg)} kg (${formatTonDisplay(quantities.rpsKg)} Ton)")
                appendLine("• Total Batch : ${formatKgDisplay(quantities.totalKg)} kg (${formatTonDisplay(quantities.totalKg)} Ton)")
                if (operatorName.isNotBlank()) {
                    appendLine("Akun : $operatorName")
                }
                append("Waktu : $timeLabel")
            }
        } catch (_: Throwable) {
            "Hasil Blending ${spec.name}: RPO ${formatPercent(rpoPercent)}, RPS ${formatPercent(rpsPercent)}"
        }
    }
}
