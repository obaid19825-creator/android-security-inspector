package com.securityinspector.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val versionName: String?,
    val installerPackageName: String?,
    val isSystemApp: Boolean,
    val requestsDangerousPermissions: Boolean,
    val dangerousPermissionCount: Int,
    val installTimestamp: Long,
    val lastUpdateTimestamp: Long
)
