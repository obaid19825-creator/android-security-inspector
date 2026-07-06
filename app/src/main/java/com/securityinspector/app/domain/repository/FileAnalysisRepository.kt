package com.securityinspector.app.domain.repository

import com.securityinspector.app.domain.model.FdAnalysisResult

interface FileAnalysisRepository {
    fun analyzeFdListing(sourceLabel: String, rawText: String): FdAnalysisResult
}
