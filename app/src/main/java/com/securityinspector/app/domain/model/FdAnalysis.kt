package com.securityinspector.app.domain.model

/**
 * Categories used to classify lines from a user-supplied `/proc/<pid>/fd` style
 * listing (pasted in or uploaded as a text/log file). This is purely a text
 * classification utility for diagnostics the user has already collected
 * themselves (e.g. via `adb shell ls -l /proc/<pid>/fd` on their own device) —
 * this app does not read `/proc` on its own or access other processes.
 */
enum class FdCategory(val displayName: String, val explanation: String) {
    STANDARD_STREAMS(
        "Standard Streams",
        "File descriptors 0, 1, and 2 — stdin, stdout, and stderr. Present in virtually " +
            "every process and not noteworthy on their own."
    ),
    APK_FILES(
        "APK Files",
        "Open handles to .apk package files, typically the app's own base or split APK " +
            "mapped for code/resource access."
    ),
    FRAMEWORK_LIBRARIES(
        "Framework Libraries",
        "Shared object (.so) or framework .jar/.dex files loaded from the Android " +
            "framework or system library paths."
    ),
    BINDER(
        "Binder",
        "Handles to /dev/binder, the primary Android IPC transport. Expected in almost " +
            "all app and system processes."
    ),
    HWBINDER(
        "HwBinder",
        "Handles to /dev/hwbinder, used for HAL (Hardware Abstraction Layer) IPC. More " +
            "common in system/vendor processes than regular apps."
    ),
    PIPES(
        "Pipes",
        "Anonymous pipe() file descriptors, often used for inter-thread or inter-process " +
            "signalling within the same app."
    ),
    SHARED_MEMORY(
        "Shared Memory",
        "ashmem or memfd shared-memory regions, commonly used for graphics buffers or " +
            "large data transfer between processes."
    ),
    EVENTFD(
        "EventFD",
        "eventfd() descriptors used as lightweight inter-thread event/notification " +
            "primitives."
    ),
    EVENTPOLL(
        "EventPoll",
        "epoll instances used by the process's I/O event loop (common in Android's " +
            "Looper/native event-handling code)."
    ),
    INOTIFY(
        "Inotify",
        "inotify file-change-notification instances, used to watch directories or files " +
            "for changes."
    ),
    SOCKETS(
        "Sockets",
        "Network or local (Unix domain) socket connections. Worth reviewing if you don't " +
            "recognize the destination, but most are routine app/system communication."
    ),
    OVERLAY_PACKAGES(
        "Overlay Packages",
        "References to Runtime Resource Overlay (RRO) packages, used by Android to " +
            "theme or substitute resources at runtime."
    ),
    KERNEL_INTERFACES(
        "Kernel Interfaces",
        "Misc kernel device or pseudo-filesystem handles (e.g. /dev, /sys, /proc paths) " +
            "not covered by a more specific category above."
    ),
    UNKNOWN(
        "Unknown Entries",
        "Entries that didn't match a recognized pattern. Not inherently suspicious — " +
            "review manually if you have specific concerns about a particular process."
    )
}

data class FdEntry(
    val rawLine: String,
    val descriptorNumber: String?,
    val target: String,
    val category: FdCategory
)

data class FdAnalysisResult(
    val sourceLabel: String,
    val entries: List<FdEntry>
) {
    val countsByCategory: Map<FdCategory, Int>
        get() = entries.groupingBy { it.category }.eachCount()
}
