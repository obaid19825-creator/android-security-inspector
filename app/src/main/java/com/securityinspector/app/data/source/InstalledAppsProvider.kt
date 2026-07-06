package com.securityinspector.app.data.source

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.securityinspector.app.domain.model.InstalledAppInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lists installed applications using [PackageManager.getInstalledPackages], a fully
 * documented public API. On API 30+ this requires the QUERY_ALL_PACKAGES manifest
 * permission (declared with justification in AndroidManifest.xml) to see apps outside
 * this app's own visibility filter; without it, only a filtered subset would be visible.
 * No package is started, modified, granted permissions, or otherwise interfered with —
 * this only reads already-public metadata.
 */
@Singleton
class InstalledAppsProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dangerousPermissionPrefixes = setOf(
        "android.permission.READ_SMS",
        "android.permission.SEND_SMS",
        "android.permission.READ_CONTACTS",
        "android.permission.WRITE_CONTACTS",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.ACCESS_BACKGROUND_LOCATION",
        "android.permission.READ_CALL_LOG",
        "android.permission.WRITE_CALL_LOG",
        "android.permission.CALL_PHONE",
        "android.permission.RECORD_AUDIO",
        "android.permission.CAMERA",
        "android.permission.READ_EXTERNAL_STORAGE",
        "android.permission.WRITE_EXTERNAL_STORAGE",
        "android.permission.BODY_SENSORS",
        "android.permission.READ_PHONE_STATE",
        "android.permission.SYSTEM_ALERT_WINDOW",
        "android.permission.BIND_ACCESSIBILITY_SERVICE",
        "android.permission.BIND_DEVICE_ADMIN",
        "android.permission.REQUEST_INSTALL_PACKAGES"
    )

    fun getInstalledApps(): List<InstalledAppInfo> {
        val pm = context.packageManager
        val packages = runCatching {
            pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        }.getOrDefault(emptyList())

        return packages.map { pkgInfo ->
            val appInfo: ApplicationInfo? = pkgInfo.applicationInfo
            val isSystem = (appInfo?.flags?.and(ApplicationInfo.FLAG_SYSTEM)) != 0

            val requestedPermissions = pkgInfo.requestedPermissions?.toList().orEmpty()
            val dangerousCount = requestedPermissions.count { it in dangerousPermissionPrefixes }

            val installerName = runCatching {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    pm.getInstallSourceInfo(pkgInfo.packageName).installingPackageName
                } else {
                    @Suppress("DEPRECATION")
                    pm.getInstallerPackageName(pkgInfo.packageName)
                }
            }.getOrNull()

            InstalledAppInfo(
                packageName = pkgInfo.packageName,
                appName = runCatching {
                    appInfo?.let { pm.getApplicationLabel(it).toString() }
                }.getOrNull() ?: pkgInfo.packageName,
                versionName = pkgInfo.versionName,
                installerPackageName = installerName,
                isSystemApp = isSystem,
                requestsDangerousPermissions = dangerousCount > 0,
                dangerousPermissionCount = dangerousCount,
                installTimestamp = pkgInfo.firstInstallTime,
                lastUpdateTimestamp = pkgInfo.lastUpdateTime
            )
        }.sortedByDescending { it.lastUpdateTimestamp }
    }
}
