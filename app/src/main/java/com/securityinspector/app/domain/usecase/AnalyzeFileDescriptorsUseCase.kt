package com.securityinspector.app.domain.usecase

import com.securityinspector.app.domain.model.FdAnalysisResult
import com.securityinspector.app.domain.repository.FileAnalysisRepository
import javax.inject.Inject

class AnalyzeFileDescriptorsUseCase @Inject constructor(
    private val repository: FileAnalysisRepository
) {
    operator fun invoke(sourceLabel: String, rawText: String): FdAnalysisResult =
        repository.analyzeFdListing(sourceLabel, rawText)
}
