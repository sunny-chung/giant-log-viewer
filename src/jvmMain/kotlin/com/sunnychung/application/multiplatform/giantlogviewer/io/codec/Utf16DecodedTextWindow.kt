package com.sunnychung.application.multiplatform.giantlogviewer.io.codec

import com.sunnychung.application.multiplatform.giantlogviewer.io.DecodedTextWindow
import com.sunnychung.application.multiplatform.giantlogviewer.io.UTF16_CODE_UNIT_BYTES

internal class Utf16DecodedTextWindow(
    override val text: String,
    override val byteRange: LongRange,
) : DecodedTextWindow {
    override fun bytePositionAtCharIndex(charIndex: Int): Long {
        val target = normalizeCharIndex(charIndex.coerceIn(0, text.length))
        return byteRange.start + target * UTF16_CODE_UNIT_BYTES.toLong()
    }

    private fun normalizeCharIndex(index: Int): Int {
        return if (index in 1..<text.length && text[index].isLowSurrogate() && text[index - 1].isHighSurrogate()) {
            index - 1
        } else {
            index
        }
    }
}
