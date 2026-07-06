package com.securityinspector.app.data.source

import com.securityinspector.app.domain.model.FdAnalysisResult
import com.securityinspector.app.domain.model.FdCategory
import com.securityinspector.app.domain.model.FdEntry
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parses user-supplied text (pasted or uploaded) that resembles `/proc/<pid>/fd`
 * listings or similar Android diagnostic output, and classifies each line.
 *
 * This is a pure text classifier — it never reads `/proc` itself, never inspects
 * other processes, and operates only on text the user has already collected and
 * provided (e.g. from their own `adb shell` session).
 */
@Singleton
class FdListingParser @Inject constructor() {

    fun parse(sourceLabel: String, rawText: String): FdAnalysisResult {
        val lines = rawText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val entries = lines.map { line -> classifyLine(line) }
        return FdAnalysisResult(sourceLabel = sourceLabel, entries = entries)
    }

    private fun classifyLine(line: String): FdEntry {
        val descriptorNumber = Regex("""^\D*(\d+)\D*->""").find(line)?.groupValues?.get(1)
            ?: Regex("""^(\d+)[:\s]""").find(line)?.groupValues?.get(1)

        val target = line.substringAfter("->", line).trim()
        val lower = line.lowercase()

        val category = when {
            descriptorNumber in setOf("0", "1", "2") &&
                Regex("""^\s*(0|1|2)\b""").containsMatchIn(line) -> FdCategory.STANDARD_STREAMS

            lower.contains(".apk") -> FdCategory.APK_FILES

            lower.contains("/system/framework") || lower.contains(".jar") ||
                Regex("""/lib(64)?/.*\.so""").containsMatchIn(lower) -> FdCategory.FRAMEWORK_LIBRARIES

            lower.contains("/dev/hwbinder") -> FdCategory.HWBINDER

            lower.contains("/dev/binder") || lower.contains("binder:") -> FdCategory.BINDER

            lower.contains("pipe:") -> FdCategory.PIPES

            lower.contains("/dev/ashmem") || lower.contains("ashmem") || lower.contains("memfd:") ->
                FdCategory.SHARED_MEMORY

            lower.contains("anon_inode:[eventfd]") || lower.contains("eventfd") -> FdCategory.EVENTFD

            lower.contains("anon_inode:[eventpoll]") || lower.contains("eventpoll") -> FdCategory.EVENTPOLL

            lower.contains("anon_inode:inotify") || lower.contains("inotify") -> FdCategory.INOTIFY

            lower.contains("socket:") -> FdCategory.SOCKETS

            lower.contains("overlay") || lower.contains(".rro") -> FdCategory.OVERLAY_PACKAGES

            lower.startsWith("/dev/") || lower.startsWith("/sys/") || lower.startsWith("/proc/") ->
                FdCategory.KERNEL_INTERFACES

            else -> FdCategory.UNKNOWN
        }

        return FdEntry(
            rawLine = line,
            descriptorNumber = descriptorNumber,
            target = target,
            category = category
        )
    }
}
