package com.securityinspector.app.data.repository

import com.securityinspector.app.data.source.FdListingParser
import com.securityinspector.app.domain.model.FdAnalysisResult
import com.securityinspector.app.domain.repository.FileAnalysisRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileAnalysisRepositoryImpl @Inject constructor(
    private val parser: FdListingParser
) : FileAnalysisRepository {
    override fun analyzeFdListing(sourceLabel: String, rawText: String): FdAnalysisResult =
        parser.parse(sourceLabel, rawText)
}
