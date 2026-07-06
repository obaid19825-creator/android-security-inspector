package com.securityinspector.app.presentation.report

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securityinspector.app.domain.model.ScanResult
import com.securityinspector.app.domain.usecase.GetScanByIdUseCase
import com.securityinspector.app.util.ReportExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class ExportFormat { PDF, JSON, CSV }

data class ReportUiState(
    val isLoading: Boolean = true,
    val scanResult: ScanResult? = null,
    val isExporting: Boolean = false,
    val exportError: String? = null
)

@HiltViewModel
class ReportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getScanByIdUseCase: GetScanByIdUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    fun loadScan(id: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val scan = getScanByIdUseCase(id)
            _uiState.value = _uiState.value.copy(isLoading = false, scanResult = scan)
        }
    }

    fun export(format: ExportFormat, onReady: (Intent) -> Unit) {
        val scan = _uiState.value.scanResult ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, exportError = null)
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    when (format) {
                        ExportFormat.PDF -> ReportExporter.exportToPdf(context, scan)
                        ExportFormat.JSON -> ReportExporter.exportToJson(context, scan)
                        ExportFormat.CSV -> ReportExporter.exportToCsv(context, scan)
                    }
                }
            }
            result.onSuccess { file ->
                val uri = ReportExporter.shareUriFor(context, file)
                val mime = when (format) {
                    ExportFormat.PDF -> "application/pdf"
                    ExportFormat.JSON -> "application/json"
                    ExportFormat.CSV -> "text/csv"
                }
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = mime
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                _uiState.value = _uiState.value.copy(isExporting = false)
                onReady(Intent.createChooser(intent, "Share security report"))
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportError = error.message ?: "Export failed."
                )
            }
        }
    }
}
