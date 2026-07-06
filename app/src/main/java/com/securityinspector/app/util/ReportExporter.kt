package com.securityinspector.app.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.securityinspector.app.data.local.JsonMapper
import com.securityinspector.app.domain.model.ScanResult
import kotlinx.serialization.encodeToString
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates exportable report files (PDF / JSON / CSV) from a [ScanResult] using only
 * documented Android APIs: [PdfDocument] for PDF rendering and plain file I/O for the
 * text-based formats. Files are written to the app's external files "reports" directory
 * and shared out via a [FileProvider] content URI — this app never writes to shared/public
 * storage paths directly.
 */
object ReportExporter {

    private val fileTimestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    private val readableDateFormat = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())

    private fun reportsDir(context: Context): File =
        File(context.getExternalFilesDir(null), "reports").apply { mkdirs() }

    fun exportToJson(context: Context, scan: ScanResult): File {
        val json = JsonMapper.json.encodeToString(scan)
        val file = File(reportsDir(context), "security_report_${fileTimestampFormat.format(Date())}.json")
        file.writeText(json)
        return file
    }

    fun exportToCsv(context: Context, scan: ScanResult): File {
        val file = File(reportsDir(context), "security_report_${fileTimestampFormat.format(Date())}.csv")
        FileOutputStream(file).bufferedWriter().use { writer ->
            writer.appendLine("Section,Field,Value")
            writer.appendLine("Summary,Security Score,${scan.securityScore}")
            writer.appendLine("Summary,Risk Level,${scan.riskLevel.label}")
            writer.appendLine("Summary,Scan Time,${csvEscape(readableDateFormat.format(Date(scan.timestamp)))}")
            writer.appendLine("Device,Manufacturer/Model,${csvEscape("${scan.deviceInfo.manufacturer} ${scan.deviceInfo.model}")}")
            writer.appendLine("Device,Android Version,${csvEscape("${scan.deviceInfo.androidVersion} (API ${scan.deviceInfo.sdkInt})")}")
            writer.appendLine("Device,Security Patch,${csvEscape(scan.deviceInfo.securityPatchLevel)}")
            writer.appendLine("Device,CPU Architecture,${csvEscape(scan.deviceInfo.cpuArchitecture)}")

            writer.appendLine()
            writer.appendLine("Category,Title,Evidence Type,Severity,Description,Recommendation")
            scan.findings.forEach { finding ->
                writer.appendLine(
                    listOf(
                        finding.category.displayName,
                        finding.title,
                        finding.evidenceType.name,
                        finding.severity.name,
                        finding.description,
                        finding.recommendation.orEmpty()
                    ).joinToString(",") { csvEscape(it) }
                )
            }

            writer.appendLine()
            writer.appendLine("Package Name,App Name,Version,Installer,System App,Dangerous Permissions")
            scan.installedApps.forEach { app ->
                writer.appendLine(
                    listOf(
                        app.packageName,
                        app.appName,
                        app.versionName.orEmpty(),
                        app.installerPackageName.orEmpty(),
                        app.isSystemApp.toString(),
                        app.dangerousPermissionCount.toString()
                    ).joinToString(",") { csvEscape(it) }
                )
            }
        }
        return file
    }

    fun exportToPdf(context: Context, scan: ScanResult): File {
        val document = PdfDocument()
        val pageWidth = 595 // A4 @ 72dpi
        val pageHeight = 842
        val margin = 36f

        val titlePaint = Paint().apply { textSize = 20f; isFakeBoldText = true }
        val headerPaint = Paint().apply { textSize = 14f; isFakeBoldText = true }
        val bodyPaint = Paint().apply { textSize = 10.5f }
        val mutedPaint = Paint().apply { textSize = 9f; color = 0xFF777777.toInt() }

        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, document.pages.size + 1).create())
        var canvas: Canvas = page.canvas
        var y = margin + 20f

        fun newPageIfNeeded(linesNeeded: Int = 1) {
            if (y + (linesNeeded * 14f) > pageHeight - margin) {
                document.finishPage(page)
                page = document.startPage(
                    PdfDocument.PageInfo.Builder(pageWidth, pageHeight, document.pages.size + 1).create()
                )
                canvas = page.canvas
                y = margin
            }
        }

        canvas.drawText("Android Security Inspector — Report", margin, y, titlePaint)
        y += 26f
        canvas.drawText("Generated: ${readableDateFormat.format(Date())}", margin, y, mutedPaint)
        y += 24f

        canvas.drawText("Summary", margin, y, headerPaint)
        y += 18f
        canvas.drawText("Security Score: ${scan.securityScore} / 100", margin, y, bodyPaint)
        y += 16f
        canvas.drawText("Risk Level: ${scan.riskLevel.label}", margin, y, bodyPaint)
        y += 16f
        canvas.drawText(
            "Scan Time: ${readableDateFormat.format(Date(scan.timestamp))}",
            margin, y, bodyPaint
        )
        y += 24f

        canvas.drawText("Device Summary", margin, y, headerPaint)
        y += 18f
        listOf(
            "Device: ${scan.deviceInfo.manufacturer} ${scan.deviceInfo.model}",
            "Android Version: ${scan.deviceInfo.androidVersion} (API ${scan.deviceInfo.sdkInt})",
            "Security Patch: ${scan.deviceInfo.securityPatchLevel}",
            "CPU Architecture: ${scan.deviceInfo.cpuArchitecture}"
        ).forEach { line ->
            newPageIfNeeded()
            canvas.drawText(line, margin, y, bodyPaint)
            y += 16f
        }
        y += 10f

        newPageIfNeeded(2)
        canvas.drawText("Security Findings (${scan.findings.size})", margin, y, headerPaint)
        y += 18f

        scan.findings.forEach { finding ->
            newPageIfNeeded(4)
            canvas.drawText(
                "• [${finding.evidenceType.name}] ${finding.title}",
                margin, y, bodyPaint.apply { isFakeBoldText = true }
            )
            bodyPaint.isFakeBoldText = false
            y += 14f
            wrapText(finding.description, bodyPaint, pageWidth - (margin * 2).toInt()).forEach { wrapped ->
                newPageIfNeeded()
                canvas.drawText(wrapped, margin + 12f, y, mutedPaint)
                y += 13f
            }
            finding.recommendation?.let { rec ->
                newPageIfNeeded()
                canvas.drawText("Recommendation: $rec", margin + 12f, y, mutedPaint)
                y += 13f
            }
            y += 8f
        }

        newPageIfNeeded(2)
        y += 6f
        canvas.drawText("Installed App Summary (${scan.installedApps.size} apps)", margin, y, headerPaint)
        y += 18f
        val flagged = scan.installedApps.filter { it.requestsDangerousPermissions }
        canvas.drawText(
            "${flagged.size} app(s) request one or more sensitive runtime permissions.",
            margin, y, bodyPaint
        )
        y += 20f

        document.finishPage(page)

        val file = File(reportsDir(context), "security_report_${fileTimestampFormat.format(Date())}.pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }

    fun shareUriFor(context: Context, file: File) =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    private fun csvEscape(value: String): String {
        val needsQuoting = value.contains(",") || value.contains("\"") || value.contains("\n")
        val escaped = value.replace("\"", "\"\"")
        return if (needsQuoting) "\"$escaped\"" else escaped
    }

    private fun wrapText(text: String, paint: Paint, maxWidthPx: Int): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "${current} $word"
            if (paint.measureText(candidate) > maxWidthPx && current.isNotEmpty()) {
                lines += current.toString()
                current = StringBuilder(word)
            } else {
                current = StringBuilder(candidate)
            }
        }
        if (current.isNotEmpty()) lines += current.toString()
        return lines
    }
}
