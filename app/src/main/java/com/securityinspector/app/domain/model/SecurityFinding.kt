package com.securityinspector.app.domain.model

import kotlinx.serialization.Serializable

/**
 * Indicates how much weight a finding should be given.
 *
 * INFORMATIONAL: a simple fact about device state (e.g. "Developer Options: Enabled").
 *   No claim of risk is implied on its own.
 * HEURISTIC: an indicator that *may* correlate with elevated risk (e.g. presence of a
 *   common root-management binary), but is not proof by itself and can have legitimate
 *   explanations. Heuristic findings should always be considered in combination, never
 *   in isolation, when assessing device trust.
 */
@Serializable
enum class EvidenceType {
    INFORMATIONAL,
    HEURISTIC
}

@Serializable
enum class FindingSeverity(val weight: Int) {
    NONE(0),
    LOW(5),
    MEDIUM(12),
    HIGH(22),
    CRITICAL(35)
}

@Serializable
enum class FindingCategory(val displayName: String) {
    ROOT_INDICATORS("Root Indicators"),
    BOOTLOADER("Bootloader Status"),
    DEVELOPER_OPTIONS("Developer Options"),
    USB_DEBUGGING("USB Debugging"),
    INSTALL_SOURCES("Unknown Sources / Install Settings"),
    INSTALLED_APPS("Installed Applications"),
    ACCESSIBILITY("Accessibility Services"),
    DEVICE_ADMIN("Device Administrator Apps"),
    VPN("VPN Status"),
    NETWORK("Active Network"),
    PACKAGE_INTEGRITY("Package Integrity")
}

/**
 * A single security observation produced by a [com.securityinspector.app.domain.usecase.SecurityCheck].
 *
 * Every finding is explicitly labeled with an [EvidenceType] so the UI and reports never
 * present a heuristic signal as a definitive compromise determination.
 */
@Serializable
data class SecurityFinding(
    val id: String,
    val category: FindingCategory,
    val title: String,
    val description: String,
    val evidenceType: EvidenceType,
    val severity: FindingSeverity,
    val recommendation: String? = null,
    val isPositiveFinding: Boolean = false
)
