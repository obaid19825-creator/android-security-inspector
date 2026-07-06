package com.securityinspector.app.domain.usecase

import com.securityinspector.app.domain.model.RiskLevel
import com.securityinspector.app.domain.model.ScanResult
import com.securityinspector.app.domain.repository.SecurityScanRepository
import javax.inject.Inject

/**
 * Runs all security checks, fetches device info, computes an aggregate score,
 * persists the result, and returns it.
 *
 * Scoring model: starts at 100 and subtracts each non-positive HEURISTIC or
 * INFORMATIONAL finding's severity weight, floored at 0. This is a simple,
 * transparent, additive heuristic — it is intentionally not a black box, and
 * the report screen lists exactly which findings contributed to the score.
 */
class RunSecurityScanUseCase @Inject constructor(
    private val repository: SecurityScanRepository
) {
    suspend operator fun invoke(): ScanResult {
        val deviceInfo = repository.getDeviceInfo()
        val findings = repository.runSecurityChecks()
        val installedApps = repository.getInstalledApps()

        val deduction = findings
            .filterNot { it.isPositiveFinding }
            .sumOf { it.severity.weight }

        val score = (100 - deduction).coerceIn(0, 100)
        val riskLevel = RiskLevel.fromScore(score)

        val result = ScanResult(
            timestamp = System.currentTimeMillis(),
            securityScore = score,
            riskLevel = riskLevel,
            deviceInfo = deviceInfo,
            findings = findings,
            installedApps = installedApps
        )

        val id = repository.saveScanResult(result)
        return result.copy(id = id)
    }
}
