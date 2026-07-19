package com.sunnychung.application.multiplatform.giantlogviewer.io

import com.sunnychung.application.multiplatform.giantlogviewer.util.GraphemeClusters

fun trimTextToByteLength(
    text: String,
    byteLength: Long,
    maxByteLength: Long,
    encodedLength: (CharSequence) -> Long,
): Pair<String, Long> {
    var actualText = text
    var actualByteLength = byteLength
    while (actualByteLength > maxByteLength && actualText.isNotEmpty()) {
        val removeStart = GraphemeClusters.previousBoundary(actualText, actualText.length)
        val removedText = actualText.subSequence(removeStart, actualText.length)
        actualByteLength -= encodedLength(removedText)
        actualText = actualText.substring(0, removeStart)
    }
    return actualText to actualByteLength.coerceAtLeast(0L)
}
