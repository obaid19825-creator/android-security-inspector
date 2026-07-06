package com.securityinspector.app.domain.usecase

import com.securityinspector.app.domain.model.ScanResult
import com.securityinspector.app.domain.repository.SecurityScanRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveScanHistoryUseCase @Inject constructor(
    private val repository: SecurityScanRepository
) {
    operator fun invoke(): Flow<List<ScanResult>> = repository.observeScanHistory()
}

class GetScanByIdUseCase @Inject constructor(
    private val repository: SecurityScanRepository
) {
    suspend operator fun invoke(id: Long): ScanResult? = repository.getScanById(id)
}

class GetLatestScanUseCase @Inject constructor(
    private val repository: SecurityScanRepository
) {
    suspend operator fun invoke(): ScanResult? = repository.getLatestScan()
}

class DeleteScanUseCase @Inject constructor(
    private val repository: SecurityScanRepository
) {
    suspend operator fun invoke(id: Long) = repository.deleteScan(id)
}
