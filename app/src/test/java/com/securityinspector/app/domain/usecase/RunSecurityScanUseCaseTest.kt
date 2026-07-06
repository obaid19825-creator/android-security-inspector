package com.securityinspector.app.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.securityinspector.app.domain.model.DeviceInfo
import com.securityinspector.app.domain.model.EvidenceType
import com.securityinspector.app.domain.model.FindingCategory
import com.securityinspector.app.domain.model.FindingSeverity
import com.securityinspector.app.domain.model.RiskLevel
import com.securityinspector.app.domain.model.SecurityFinding
import com.securityinspector.app.domain.repository.SecurityScanRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RunSecurityScanUseCaseTest {

    private lateinit var repository: SecurityScanRepository
    private lateinit var useCase: RunSecurityScanUseCase

    private val fakeDeviceInfo = DeviceInfo(
        manufacturer = "Google",
        model = "Pixel Test",
        androidVersion = "14",
        sdkInt = 34,
        securityPatchLevel = "2026-06-01",
        cpuArchitecture = "arm64-v8a",
        totalStorageBytes = 128_000_000_000L,
        availableStorageBytes = 64_000_000_000L,
        totalRamBytes = 8_000_000_000L,
        availableRamBytes = 3_000_000_000L,
        batteryPercent = 80,
        isCharging = false,
        isLowRamDevice = false
    )

    @Before
    fun setUp() {
        repository = mockk()
        useCase = RunSecurityScanUseCase(repository)
        coEvery { repository.getDeviceInfo() } returns fakeDeviceInfo
        coEvery { repository.getInstalledApps() } returns emptyList()
        coEvery { repository.saveScanResult(any()) } returns 1L
    }

    @Test
    fun `no negative findings yields a perfect score and SAFE risk`() = runTest {
        coEvery { repository.runSecurityChecks() } returns listOf(
            finding(severity = FindingSeverity.NONE, isPositive = true)
        )

        val result = useCase()

        assertThat(result.securityScore).isEqualTo(100)
        assertThat(result.riskLevel).isEqualTo(RiskLevel.SAFE)
    }

    @Test
    fun `positive findings are never deducted from score`() = runTest {
        coEvery { repository.runSecurityChecks() } returns listOf(
            finding(severity = FindingSeverity.CRITICAL, isPositive = true)
        )

        val result = useCase()

        assertThat(result.securityScore).isEqualTo(100)
    }

    @Test
    fun `a single critical finding drops score by its weight`() = runTest {
        coEvery { repository.runSecurityChecks() } returns listOf(
            finding(severity = FindingSeverity.CRITICAL, isPositive = false)
        )

        val result = useCase()

        assertThat(result.securityScore).isEqualTo(100 - FindingSeverity.CRITICAL.weight)
    }

    @Test
    fun `score never drops below zero regardless of deductions`() = runTest {
        coEvery { repository.runSecurityChecks() } returns List(10) {
            finding(severity = FindingSeverity.CRITICAL, isPositive = false)
        }

        val result = useCase()

        assertThat(result.securityScore).isEqualTo(0)
        assertThat(result.riskLevel).isEqualTo(RiskLevel.CRITICAL)
    }

    private fun finding(severity: FindingSeverity, isPositive: Boolean) = SecurityFinding(
        id = "test_${severity.name}_${isPositive}_${System.nanoTime()}",
        category = FindingCategory.ROOT_INDICATORS,
        title = "Test finding",
        description = "Test description",
        evidenceType = EvidenceType.HEURISTIC,
        severity = severity,
        isPositiveFinding = isPositive
    )
}
