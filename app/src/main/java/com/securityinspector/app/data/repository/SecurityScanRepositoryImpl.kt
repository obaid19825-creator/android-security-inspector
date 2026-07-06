package com.securityinspector.app.data.repository

import com.securityinspector.app.data.local.JsonMapper
import com.securityinspector.app.data.local.dao.ScanResultDao
import com.securityinspector.app.data.local.entity.ScanResultEntity
import com.securityinspector.app.data.source.DeviceInfoProvider
import com.securityinspector.app.data.source.InstalledAppsProvider
import com.securityinspector.app.data.source.SecurityCheckProvider
import com.securityinspector.app.domain.model.DeviceInfo
import com.securityinspector.app.domain.model.InstalledAppInfo
import com.securityinspector.app.domain.model.RiskLevel
import com.securityinspector.app.domain.model.ScanResult
import com.securityinspector.app.domain.model.SecurityFinding
import com.securityinspector.app.domain.repository.SecurityScanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityScanRepositoryImpl @Inject constructor(
    private val deviceInfoProvider: DeviceInfoProvider,
    private val securityCheckProvider: SecurityCheckProvider,
    private val installedAppsProvider: InstalledAppsProvider,
    private val dao: ScanResultDao
) : SecurityScanRepository {

    override suspend fun getDeviceInfo(): DeviceInfo = withContext(Dispatchers.Default) {
        deviceInfoProvider.getDeviceInfo()
    }

    override suspend fun runSecurityChecks(): List<SecurityFinding> = withContext(Dispatchers.Default) {
        securityCheckProvider.runAllChecks()
    }

    override suspend fun getInstalledApps(): List<InstalledAppInfo> = withContext(Dispatchers.Default) {
        installedAppsProvider.getInstalledApps()
    }

    override suspend fun saveScanResult(scanResult: ScanResult): Long = withContext(Dispatchers.IO) {
        dao.insert(scanResult.toEntity())
    }

    override fun observeScanHistory(): Flow<List<ScanResult>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getScanById(id: Long): ScanResult? = withContext(Dispatchers.IO) {
        dao.getById(id)?.toDomain()
    }

    override suspend fun getLatestScan(): ScanResult? = withContext(Dispatchers.IO) {
        dao.getLatest()?.toDomain()
    }

    override suspend fun deleteScan(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }

    private fun ScanResult.toEntity() = ScanResultEntity(
        id = id,
        timestamp = timestamp,
        securityScore = securityScore,
        riskLevel = riskLevel.name,
        deviceInfoJson = JsonMapper.encodeDeviceInfo(deviceInfo),
        findingsJson = JsonMapper.encodeFindings(findings),
        installedAppsJson = JsonMapper.encodeApps(installedApps)
    )

    private fun ScanResultEntity.toDomain() = ScanResult(
        id = id,
        timestamp = timestamp,
        securityScore = securityScore,
        riskLevel = RiskLevel.valueOf(riskLevel),
        deviceInfo = JsonMapper.decodeDeviceInfo(deviceInfoJson),
        findings = JsonMapper.decodeFindings(findingsJson),
        installedApps = JsonMapper.decodeApps(installedAppsJson)
    )
}
