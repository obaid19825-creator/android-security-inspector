package com.securityinspector.app.domain.repository

import com.securityinspector.app.domain.model.DeviceInfo
import com.securityinspector.app.domain.model.InstalledAppInfo
import com.securityinspector.app.domain.model.ScanResult
import com.securityinspector.app.domain.model.SecurityFinding
import kotlinx.coroutines.flow.Flow

/**
 * Abstraction over device-state reading. Implementations use only documented,
 * publicly-available Android APIs and never require root or elevated privilege.
 */
interface SecurityScanRepository {
    suspend fun getDeviceInfo(): DeviceInfo
    suspend fun runSecurityChecks(): List<SecurityFinding>
    suspend fun getInstalledApps(): List<InstalledAppInfo>

    suspend fun saveScanResult(scanResult: ScanResult): Long
    fun observeScanHistory(): Flow<List<ScanResult>>
    suspend fun getScanById(id: Long): ScanResult?
    suspend fun getLatestScan(): ScanResult?
    suspend fun deleteScan(id: Long)
}
