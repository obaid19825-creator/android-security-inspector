package com.securityinspector.app.presentation.filescan

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.securityinspector.app.domain.model.FdAnalysisResult
import com.securityinspector.app.domain.usecase.AnalyzeFileDescriptorsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject

data class FileAnalysisUiState(
    val pastedText: String = "",
    val isAnalyzing: Boolean = false,
    val result: FdAnalysisResult? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class FileAnalysisViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val analyzeFileDescriptorsUseCase: AnalyzeFileDescriptorsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FileAnalysisUiState())
    val uiState: StateFlow<FileAnalysisUiState> = _uiState.asStateFlow()

    fun onPastedTextChanged(text: String) {
        _uiState.value = _uiState.value.copy(pastedText = text)
    }

    fun analyzePastedText() {
        val text = _uiState.value.pastedText
        if (text.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Paste some diagnostic output first.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAnalyzing = true, errorMessage = null)
            val result = analyzeFileDescriptorsUseCase("Pasted text", text)
            _uiState.value = _uiState.value.copy(isAnalyzing = false, result = result)
        }
    }

    fun analyzeUploadedFile(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAnalyzing = true, errorMessage = null)
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BufferedReader(InputStreamReader(stream)).readText()
                    }
                }.getOrNull()
            }

            if (text.isNullOrBlank()) {
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    errorMessage = "Couldn't read that file, or it was empty."
                )
                return@launch
            }

            val fileName = runCatching {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex >= 0) cursor.getString(nameIndex) else null
                }
            }.getOrNull() ?: "Uploaded file"

            val result = analyzeFileDescriptorsUseCase(fileName, text)
            _uiState.value = _uiState.value.copy(isAnalyzing = false, result = result, pastedText = "")
        }
    }

    fun clearResult() {
        _uiState.value = _uiState.value.copy(result = null, pastedText = "", errorMessage = null)
    }
}
