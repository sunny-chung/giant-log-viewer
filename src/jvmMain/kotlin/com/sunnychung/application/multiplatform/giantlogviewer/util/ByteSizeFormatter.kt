package com.sunnychung.application.multiplatform.giantlogviewer.util

import java.util.Locale

fun formatByteSize(byteCount: Long): String {
    val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB")
    var unitSize = 1L
    var unitIndex = 0
    val normalizedByteCount = byteCount.coerceAtLeast(1L)
    while (normalizedByteCount.toDouble() / unitSize.toDouble() >= 1024.0 && unitIndex < units.lastIndex) {
        unitSize *= 1024L
        unitIndex++
    }

    val number = if (unitIndex == 0) {
        normalizedByteCount.toString()
    } else if (normalizedByteCount % unitSize == 0L) {
        (normalizedByteCount / unitSize).toString()
    } else {
        "%.2f".format(Locale.US, normalizedByteCount.toDouble() / unitSize.toDouble())
    }
    return "$number ${units[unitIndex]}"
}
