package com.securityinspector.app.data.source

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.securityinspector.app.domain.model.DeviceInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads general device information using only public, documented Android APIs
 * ([Build], [StatFs], [ActivityManager], [BatteryManager]). No root or private
 * API access is used.
 */
@Singleton
class DeviceInfoProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getDeviceInfo(): DeviceInfo {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo().also { activityManager.getMemoryInfo(it) }

        val internalStat = StatFs(Environment.getDataDirectory().path)
        val totalStorage = internalStat.totalBytes
        val availableStorage = internalStat.availableBytes

        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val batteryLevel = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val batteryScale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPercent = if (batteryLevel >= 0 && batteryScale > 0) {
            (batteryLevel * 100 / batteryScale)
        } else {
            -1
        }
        val chargePlug = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val isCharging = chargePlug == BatteryManager.BATTERY_PLUGGED_AC ||
            chargePlug == BatteryManager.BATTERY_PLUGGED_USB ||
            chargePlug == BatteryManager.BATTERY_PLUGGED_WIRELESS

        return DeviceInfo(
            manufacturer = Build.MANUFACTURER ?: "Unknown",
            model = Build.MODEL ?: "Unknown",
            androidVersion = Build.VERSION.RELEASE ?: "Unknown",
            sdkInt = Build.VERSION.SDK_INT,
            securityPatchLevel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Build.VERSION.SECURITY_PATCH ?: "Unavailable"
            } else {
                "Unavailable on this Android version"
            },
            cpuArchitecture = Build.SUPPORTED_ABIS?.firstOrNull() ?: "Unknown",
            totalStorageBytes = totalStorage,
            availableStorageBytes = availableStorage,
            totalRamBytes = memoryInfo.totalMem,
            availableRamBytes = memoryInfo.availMem,
            batteryPercent = batteryPercent,
            isCharging = isCharging,
            isLowRamDevice = activityManager.isLowRamDevice
        )
    }
}
