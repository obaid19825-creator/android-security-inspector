package com.securityinspector.app.data.local

import com.securityinspector.app.domain.model.DeviceInfo
import com.securityinspector.app.domain.model.InstalledAppInfo
import com.securityinspector.app.domain.model.SecurityFinding
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Centralized Json instance + helpers for (de)serializing the JSON blob columns
 * stored in [com.securityinspector.app.data.local.entity.ScanResultEntity].
 */
object JsonMapper {

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encodeDeviceInfo(value: DeviceInfo): String = json.encodeToString(value)
    fun decodeDeviceInfo(value: String): DeviceInfo = json.decodeFromString(value)

    fun encodeFindings(value: List<SecurityFinding>): String = json.encodeToString(value)
    fun decodeFindings(value: String): List<SecurityFinding> = json.decodeFromString(value)

    fun encodeApps(value: List<InstalledAppInfo>): String = json.encodeToString(value)
    fun decodeApps(value: String): List<InstalledAppInfo> = json.decodeFromString(value)
}
