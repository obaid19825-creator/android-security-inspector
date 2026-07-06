package com.securityinspector.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val sdkInt: Int,
    val securityPatchLevel: String,
    val cpuArchitecture: String,
    val totalStorageBytes: Long,
    val availableStorageBytes: Long,
    val totalRamBytes: Long,
    val availableRamBytes: Long,
    val batteryPercent: Int,
    val isCharging: Boolean,
    val isLowRamDevice: Boolean
)
