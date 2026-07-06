package com.securityinspector.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Flattened, serialized representation of a [com.securityinspector.app.domain.model.ScanResult]
 * for Room storage. Findings, device info, and installed apps are stored as JSON blobs
 * (via [com.securityinspector.app.data.local.Converters]) since they are read-mostly,
 * variable-shape aggregates that don't need to be queried by sub-field.
 */
@Entity(tableName = "scan_results")
data class ScanResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val timestamp: Long,
    val securityScore: Int,
    val riskLevel: String,
    val deviceInfoJson: String,
    val findingsJson: String,
    val installedAppsJson: String
)
