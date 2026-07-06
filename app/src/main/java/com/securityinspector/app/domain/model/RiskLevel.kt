package com.securityinspector.app.domain.model

import kotlinx.serialization.Serializable

/**
 * Overall device risk classification derived from the aggregate security score.
 * This is a heuristic classification, not a certification of device safety.
 */
@Serializable
enum class RiskLevel(val label: String, val scoreRange: IntRange) {
    SAFE("Safe", 90..100),
    LOW("Low", 75..89),
    MEDIUM("Medium", 50..74),
    HIGH("High", 25..49),
    CRITICAL("Critical", 0..24);

    companion object {
        fun fromScore(score: Int): RiskLevel {
            val clamped = score.coerceIn(0, 100)
            return entries.firstOrNull { clamped in it.scoreRange } ?: CRITICAL
        }
    }
}
