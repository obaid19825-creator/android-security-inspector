package com.securityinspector.app.data.source

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.securityinspector.app.domain.model.EvidenceType
import com.securityinspector.app.domain.model.FindingCategory
import com.securityinspector.app.domain.model.FindingSeverity
import com.securityinspector.app.domain.model.SecurityFinding
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implements every security check from the product spec using only documented,
 * publicly-accessible Android APIs.
 *
 * IMPORTANT DESIGN PRINCIPLE: this class never attempts privilege escalation, never
 * shells out to `su`, never reads other apps' private data, and never uses
 * reflection to reach hidden/non-SDK APIs. Every check either reads a public
 * Settings key, a public system service, or checks for the *existence* (not
 * execution) of a small set of well-known, world-readable file paths that are
 * commonly associated with root management tools. A positive existence check is
 * always reported as HEURISTIC evidence, never as proof of compromise.
 */
@Singleton
class SecurityCheckProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun runAllChecks(): List<SecurityFinding> = buildList {
        addAll(checkRootIndicators())
        add(checkBootloaderStatus())
        add(checkDeveloperOptions())
        add(checkUsbDebugging())
        add(checkInstallSources())
        addAll(checkAccessibilityServices())
        addAll(checkDeviceAdministrators())
        add(checkVpnStatus())
        add(checkActiveNetwork())
        add(checkPackageIntegritySummary())
    }

    // ---------------------------------------------------------------------
    // Root indicators (heuristic, read-only file existence + package checks)
    // ---------------------------------------------------------------------
    private val rootBinaryPaths = listOf(
        "/system/app/Superuser.apk",
        "/sbin/su",
        "/system/bin/su",
        "/system/xbin/su",
        "/system/xbin/busybox",
        "/system/sd/xbin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/data/local/su",
        "/su/bin/su"
    )

    private val rootManagementPackages = listOf(
        "com.topjohnwu.magisk",
        "eu.chainfire.supersu",
        "com.koushikdutta.superuser",
        "com.noshufou.android.su",
        "com.thirdparty.superuser"
    )

    private fun checkRootIndicators(): List<SecurityFinding> {
        val foundPaths = rootBinaryPaths.filter { runCatching { File(it).exists() }.getOrDefault(false) }

        val foundPackages = rootManagementPackages.filter { pkg ->
            runCatching {
                context.packageManager.getPackageInfo(pkg, 0)
                true
            }.getOrDefault(false)
        }

        val testKeys = Build.TAGS?.contains("test-keys") == true

        val results = mutableListOf<SecurityFinding>()

        if (foundPaths.isEmpty() && foundPackages.isEmpty() && !testKeys) {
            results += SecurityFinding(
                id = "root_clean",
                category = FindingCategory.ROOT_INDICATORS,
                title = "No common root indicators found",
                description = "No well-known root-management binaries or apps were detected, " +
                    "and the build is not signed with test-keys. This does not guarantee the " +
                    "device is unrooted — only that the common, easily-checked indicators are absent.",
                evidenceType = EvidenceType.HEURISTIC,
                severity = FindingSeverity.NONE,
                isPositiveFinding = true
            )
            return results
        }

        if (foundPaths.isNotEmpty()) {
            results += SecurityFinding(
                id = "root_binary_paths",
                category = FindingCategory.ROOT_INDICATORS,
                title = "Root-associated file paths detected",
                description = "Found ${foundPaths.size} path(s) commonly associated with root " +
                    "access tools (e.g. su binaries). This is a heuristic signal, not definitive " +
                    "proof of root — some of these paths can exist for other reasons on certain ROMs.",
                evidenceType = EvidenceType.HEURISTIC,
                severity = FindingSeverity.HIGH,
                recommendation = "If you did not intentionally root this device, investigate how " +
                    "these files were placed and consider a factory reset from a trusted source."
            )
        }

        if (foundPackages.isNotEmpty()) {
            results += SecurityFinding(
                id = "root_management_apps",
                category = FindingCategory.ROOT_INDICATORS,
                title = "Root management app(s) installed",
                description = "Detected installed package(s) commonly used to manage root access: " +
                    foundPackages.joinToString(", ") + ".",
                evidenceType = EvidenceType.HEURISTIC,
                severity = FindingSeverity.HIGH,
                recommendation = "Confirm this matches your own intent. Uninstall if you did not " +
                    "install it yourself."
            )
        }

        if (testKeys) {
            results += SecurityFinding(
                id = "root_test_keys",
                category = FindingCategory.ROOT_INDICATORS,
                title = "Build signed with test-keys",
                description = "Build.TAGS reports 'test-keys', meaning the OS build was not signed " +
                    "with official release keys. This is common on custom ROMs and some development " +
                    "builds, and is not by itself proof of compromise.",
                evidenceType = EvidenceType.HEURISTIC,
                severity = FindingSeverity.MEDIUM
            )
        }

        return results
    }

    // ---------------------------------------------------------------------
    // Bootloader status (limited by platform — documented limitation noted)
    // ---------------------------------------------------------------------
    private fun checkBootloaderStatus(): SecurityFinding {
        // Android does not expose a documented, non-privileged public API to read
        // bootloader lock state. We deliberately do not use reflection on hidden
        // SystemProperties APIs to work around this. We report the limitation
        // transparently instead of guessing.
        return SecurityFinding(
            id = "bootloader_status",
            category = FindingCategory.BOOTLOADER,
            title = "Bootloader lock state not available",
            description = "Android does not provide a public, documented API for third-party " +
                "apps to read bootloader lock state. To check this yourself, reboot into the " +
                "bootloader and look at the on-screen lock status, or run 'adb shell getprop " +
                "ro.boot.verifiedbootstate' (requires a USB connection and developer/ADB access).",
            evidenceType = EvidenceType.INFORMATIONAL,
            severity = FindingSeverity.NONE
        )
    }

    // ---------------------------------------------------------------------
    // Developer Options / USB Debugging (documented Settings.Global keys)
    // ---------------------------------------------------------------------
    private fun checkDeveloperOptions(): SecurityFinding {
        val enabled = Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
            0
        ) == 1

        return SecurityFinding(
            id = "developer_options",
            category = FindingCategory.DEVELOPER_OPTIONS,
            title = if (enabled) "Developer Options is enabled" else "Developer Options is disabled",
            description = if (enabled) {
                "Developer Options exposes settings intended for app development and debugging. " +
                    "Leaving it enabled is common among developers but increases the device's " +
                    "exposed settings surface for everyday use."
            } else {
                "Developer Options is disabled, which is the recommended state for typical " +
                    "day-to-day device use."
            },
            evidenceType = EvidenceType.INFORMATIONAL,
            severity = if (enabled) FindingSeverity.LOW else FindingSeverity.NONE,
            recommendation = if (enabled) {
                "Disable Developer Options in Settings if you don't actively need it."
            } else null,
            isPositiveFinding = !enabled
        )
    }

    private fun checkUsbDebugging(): SecurityFinding {
        val enabled = Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.ADB_ENABLED,
            0
        ) == 1

        return SecurityFinding(
            id = "usb_debugging",
            category = FindingCategory.USB_DEBUGGING,
            title = if (enabled) "USB debugging is enabled" else "USB debugging is disabled",
            description = if (enabled) {
                "USB debugging (ADB) allows a connected computer to issue commands to this " +
                    "device. It should only be enabled while actively developing or debugging."
            } else {
                "USB debugging is disabled, which is the recommended state outside of active " +
                    "development."
            },
            evidenceType = EvidenceType.INFORMATIONAL,
            severity = if (enabled) FindingSeverity.MEDIUM else FindingSeverity.NONE,
            recommendation = if (enabled) {
                "Disable USB debugging when not actively developing or debugging an app."
            } else null,
            isPositiveFinding = !enabled
        )
    }

    // ---------------------------------------------------------------------
    // Unknown sources / install permission (API-version dependent)
    // ---------------------------------------------------------------------
    private fun checkInstallSources(): SecurityFinding {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canInstall = context.packageManager.canRequestPackageInstalls()
            SecurityFinding(
                id = "install_sources",
                category = FindingCategory.INSTALL_SOURCES,
                title = "Install-from-unknown-sources (this app)",
                description = "On Android 8+, 'unknown sources' is granted per-app rather than " +
                    "globally, so this app can only check its own permission to install packages " +
                    "(currently: ${if (canInstall) "granted" else "not granted"}). It cannot read " +
                    "this setting for other apps without their cooperation.",
                evidenceType = EvidenceType.INFORMATIONAL,
                severity = FindingSeverity.NONE,
                recommendation = "Review Settings > Apps > Special access > Install unknown apps " +
                    "periodically to see which apps you've granted this to."
            )
        } else {
            val enabled = Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.INSTALL_NON_MARKET_APPS,
                0
            ) == 1
            SecurityFinding(
                id = "install_sources",
                category = FindingCategory.INSTALL_SOURCES,
                title = if (enabled) "Installing from unknown sources is allowed" else "Unknown sources is disabled",
                description = "Device-wide setting controlling whether apps may be installed from " +
                    "outside the default app store.",
                evidenceType = EvidenceType.INFORMATIONAL,
                severity = if (enabled) FindingSeverity.LOW else FindingSeverity.NONE,
                isPositiveFinding = !enabled
            )
        }
    }

    // ---------------------------------------------------------------------
    // Accessibility services currently enabled
    // ---------------------------------------------------------------------
    private fun checkAccessibilityServices(): List<SecurityFinding> {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        val enabledServices = am?.getEnabledAccessibilityServiceList(
            android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        ).orEmpty()

        if (enabledServices.isEmpty()) {
            return listOf(
                SecurityFinding(
                    id = "accessibility_none",
                    category = FindingCategory.ACCESSIBILITY,
                    title = "No accessibility services currently enabled",
                    description = "No apps currently have an active accessibility service. " +
                        "Accessibility services are powerful (they can read screen content and " +
                        "perform actions on your behalf), so an empty list is generally a good sign.",
                    evidenceType = EvidenceType.INFORMATIONAL,
                    severity = FindingSeverity.NONE,
                    isPositiveFinding = true
                )
            )
        }

        return listOf(
            SecurityFinding(
                id = "accessibility_enabled",
                category = FindingCategory.ACCESSIBILITY,
                title = "${enabledServices.size} accessibility service(s) enabled",
                description = "Enabled: " + enabledServices.joinToString(", ") {
                    it.resolveInfo?.serviceInfo?.packageName ?: "Unknown package"
                } + ". Accessibility services can read on-screen content and simulate user input. " +
                    "Make sure you recognize and trust every app in this list.",
                evidenceType = EvidenceType.INFORMATIONAL,
                severity = FindingSeverity.LOW,
                recommendation = "Review Settings > Accessibility and disable any service you " +
                    "don't recognize or no longer use."
            )
        )
    }

    // ---------------------------------------------------------------------
    // Device Administrator apps
    // ---------------------------------------------------------------------
    private fun checkDeviceAdministrators(): List<SecurityFinding> {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        val admins = dpm?.activeAdmins.orEmpty()

        if (admins.isEmpty()) {
            return listOf(
                SecurityFinding(
                    id = "device_admin_none",
                    category = FindingCategory.DEVICE_ADMIN,
                    title = "No device administrator apps active",
                    description = "No apps currently hold device administrator privileges.",
                    evidenceType = EvidenceType.INFORMATIONAL,
                    severity = FindingSeverity.NONE,
                    isPositiveFinding = true
                )
            )
        }

        return listOf(
            SecurityFinding(
                id = "device_admin_active",
                category = FindingCategory.DEVICE_ADMIN,
                title = "${admins.size} device administrator app(s) active",
                description = "Active: " + admins.joinToString(", ") { it.packageName } +
                    ". Device admin apps can enforce policies like screen lock rules or remote " +
                    "wipe. Common for enterprise/MDM and 'find my phone' style apps.",
                evidenceType = EvidenceType.INFORMATIONAL,
                severity = FindingSeverity.LOW,
                recommendation = "Review Settings > Security > Device admin apps and remove any " +
                    "you don't recognize."
            )
        )
    }

    // ---------------------------------------------------------------------
    // VPN status / Active network
    // ---------------------------------------------------------------------
    private fun checkVpnStatus(): SecurityFinding {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork
        val capabilities = network?.let { cm.getNetworkCapabilities(it) }
        val isVpn = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true

        return SecurityFinding(
            id = "vpn_status",
            category = FindingCategory.VPN,
            title = if (isVpn) "VPN is active" else "No VPN detected",
            description = if (isVpn) {
                "Active network traffic is currently routed through a VPN transport."
            } else {
                "The active network connection is not using a VPN transport."
            },
            evidenceType = EvidenceType.INFORMATIONAL,
            severity = FindingSeverity.NONE
        )
    }

    private fun checkActiveNetwork(): SecurityFinding {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork
        val capabilities = network?.let { cm.getNetworkCapabilities(it) }

        val type = when {
            capabilities == null -> "No active network"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
            else -> "Other"
        }
        val isValidated = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
        val isMetered = !(capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) ?: false)

        return SecurityFinding(
            id = "active_network",
            category = FindingCategory.NETWORK,
            title = "Active network: $type",
            description = "Connection validated by the system: ${if (isValidated) "yes" else "no"}. " +
                "Metered: ${if (isMetered) "yes" else "no"}.",
            evidenceType = EvidenceType.INFORMATIONAL,
            severity = FindingSeverity.NONE
        )
    }

    // ---------------------------------------------------------------------
    // Basic package integrity summary (signature presence + installer origin)
    // ---------------------------------------------------------------------
    private fun checkPackageIntegritySummary(): SecurityFinding {
        val pm = context.packageManager
        val packages = runCatching {
            pm.getInstalledPackages(PackageManager.GET_SIGNATURES)
        }.getOrDefault(emptyList())

        val unsignedOrUnverifiable = packages.count { pkgInfo ->
            runCatching { pkgInfo.signatures.isNullOrEmpty() }.getOrDefault(false)
        }

        return SecurityFinding(
            id = "package_integrity_summary",
            category = FindingCategory.PACKAGE_INTEGRITY,
            title = "Package signature summary",
            description = "Checked ${packages.size} installed package(s) for the presence of a " +
                "reportable signing signature. $unsignedOrUnverifiable package(s) returned no " +
                "signature information via the public API. This is a basic presence check, not a " +
                "cryptographic verification against a known-good baseline.",
            evidenceType = EvidenceType.HEURISTIC,
            severity = if (unsignedOrUnverifiable > 0) FindingSeverity.LOW else FindingSeverity.NONE,
            isPositiveFinding = unsignedOrUnverifiable == 0
        )
    }
}
