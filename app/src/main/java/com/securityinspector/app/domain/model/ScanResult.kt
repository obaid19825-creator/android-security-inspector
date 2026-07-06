package com.securityinspector.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ScanResult(
    val id: Long = 0L,
    val timestamp: Long,
    val securityScore: Int,
    val riskLevel: RiskLevel,
    val deviceInfo: DeviceInfo,
    val findings: List<SecurityFinding>,
    val installedApps: List<InstalledAppInfo>
) {
    val heuristicFindingCount: Int
        get() = findings.count { it.evidenceType == EvidenceType.HEURISTIC && !it.isPositiveFinding }

    val informationalFindingCount: Int
        get() = findings.count { it.evidenceType == EvidenceType.INFORMATIONAL }
}
