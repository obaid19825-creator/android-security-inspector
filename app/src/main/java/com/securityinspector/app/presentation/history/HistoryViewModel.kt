package com.securityinspector.app.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securityinspector.app.domain.model.ScanResult
import com.securityinspector.app.domain.usecase.DeleteScanUseCase
import com.securityinspector.app.domain.usecase.ObserveScanHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    observeScanHistoryUseCase: ObserveScanHistoryUseCase,
    private val deleteScanUseCase: DeleteScanUseCase
) : ViewModel() {

    val scanHistory: StateFlow<List<ScanResult>> = observeScanHistoryUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteScan(id: Long) {
        viewModelScope.launch { deleteScanUseCase(id) }
    }
}
