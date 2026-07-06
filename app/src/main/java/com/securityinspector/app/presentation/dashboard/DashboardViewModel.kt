package com.securityinspector.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securityinspector.app.domain.model.ScanResult
import com.securityinspector.app.domain.usecase.GetLatestScanUseCase
import com.securityinspector.app.domain.usecase.RunSecurityScanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isScanning: Boolean = false,
    val isLoadingInitial: Boolean = true,
    val scanResult: ScanResult? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val runSecurityScanUseCase: RunSecurityScanUseCase,
    private val getLatestScanUseCase: GetLatestScanUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadLatestOrScan()
    }

    private fun loadLatestOrScan() {
        viewModelScope.launch {
            val latest = runCatching { getLatestScanUseCase() }.getOrNull()
            if (latest != null) {
                _uiState.value = _uiState.value.copy(isLoadingInitial = false, scanResult = latest)
            } else {
                _uiState.value = _uiState.value.copy(isLoadingInitial = false)
                runScan()
            }
        }
    }

    fun runScan() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true, errorMessage = null)
            runCatching { runSecurityScanUseCase() }
                .onSuccess { result ->
                    _uiState.value = _uiState.value.copy(isScanning = false, scanResult = result)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isScanning = false,
                        errorMessage = error.message ?: "Scan failed unexpectedly."
                    )
                }
        }
    }
}
