package com.sunnychung.application.multiplatform.giantlogviewer.util

import java.text.BreakIterator
import java.util.Locale

object GraphemeClusters {
    private val iterator = ThreadLocal.withInitial {
        BreakIterator.getCharacterInstance(Locale.ROOT)
    }

    fun forEach(text: CharSequence, action: (start: Int, endExclusive: Int) -> Unit) {
        forEachUntil(text) { start, end ->
            action(start, end)
            true
        }
    }

    fun forEachUntil(text: CharSequence, action: (start: Int, endExclusive: Int) -> Boolean) {
        if (text.isEmpty()) {
            return
        }
        if (!mayContainGraphemeSequence(text)) {
            var start = 0
            while (start < text.length) {
                val end = nextCodePointBoundary(text, start)
                if (!action(start, end)) {
                    return
                }
                start = end
            }
            return
        }

        val string = text.toString()
        val boundaryIterator = iterator.get()
        boundaryIterator.setText(string)
        var start = boundaryIterator.first()
        var end = boundaryIterator.next()
        while (end != BreakIterator.DONE) {
            if (!action(start, end)) {
                return
            }
            start = end
            end = boundaryIterator.next()
        }
    }

    fun forEachReversedUntil(
        text: CharSequence,
        endIndex: Int = text.length,
        action: (start: Int, endExclusive: Int) -> Boolean,
    ) {
        if (text.isEmpty()) {
            return
        }
        if (!mayContainGraphemeSequence(text)) {
            var end = previousOrSameCodePointBoundary(text, endIndex.coerceIn(0, text.length))
            while (end > 0) {
                val start = previousCodePointBoundary(text, end)
                if (!action(start, end)) {
                    return
                }
                end = start
            }
            return
        }

        val boundaryIterator = iteratorFor(text)
        var end = boundaryAtOrBefore(boundaryIterator, endIndex.coerceIn(0, text.length))
        while (end > 0) {
            val start = boundaryIterator.preceding(end).takeIf { it != BreakIterator.DONE } ?: 0
            if (!action(start, end)) {
                return
            }
            end = start
        }
    }

    fun boundaryAtOrBefore(text: CharSequence, index: Int): Int {
        val clampedIndex = index.coerceIn(0, text.length)
        if (clampedIndex == 0 || clampedIndex == text.length) {
            return clampedIndex
        }
        if (!mayContainGraphemeSequence(text)) {
            return previousOrSameCodePointBoundary(text, clampedIndex)
        }

        val boundaryIterator = iteratorFor(text)
        return boundaryAtOrBefore(boundaryIterator, clampedIndex)
    }

    fun boundaryAtOrAfter(text: CharSequence, index: Int): Int {
        val clampedIndex = index.coerceIn(0, text.length)
        if (clampedIndex == 0 || clampedIndex == text.length) {
            return clampedIndex
        }
        if (!mayContainGraphemeSequence(text)) {
            return nextOrSameCodePointBoundary(text, clampedIndex)
        }

        val boundaryIterator = iteratorFor(text)
        return if (boundaryIterator.isBoundary(clampedIndex)) {
            clampedIndex
        } else {
            boundaryIterator.following(clampedIndex).takeIf { it != BreakIterator.DONE } ?: text.length
        }
    }

    fun nextBoundary(text: CharSequence, index: Int): Int {
        if (!mayContainGraphemeSequence(text)) {
            return nextCodePointBoundary(text, previousOrSameCodePointBoundary(text, index.coerceIn(0, text.length)))
        }
        val boundary = boundaryAtOrBefore(text, index)
        if (boundary >= text.length) {
            return text.length
        }
        val boundaryIterator = iteratorFor(text)
        return boundaryIterator.following(boundary).takeIf { it != BreakIterator.DONE } ?: text.length
    }

    fun previousBoundary(text: CharSequence, index: Int): Int {
        if (!mayContainGraphemeSequence(text)) {
            return previousCodePointBoundary(text, nextOrSameCodePointBoundary(text, index.coerceIn(0, text.length)))
        }
        val boundary = boundaryAtOrAfter(text, index)
        if (boundary <= 0) {
            return 0
        }
        val boundaryIterator = iteratorFor(text)
        return boundaryIterator.preceding(boundary).takeIf { it != BreakIterator.DONE } ?: 0
    }

    private fun iteratorFor(text: CharSequence): BreakIterator {
        val boundaryIterator = iterator.get()
        boundaryIterator.setText(text.toString())
        return boundaryIterator
    }

    private fun boundaryAtOrBefore(boundaryIterator: BreakIterator, index: Int): Int {
        return if (boundaryIterator.isBoundary(index)) {
            index
        } else {
            boundaryIterator.preceding(index).takeIf { it != BreakIterator.DONE } ?: 0
        }
    }

    private fun mayContainGraphemeSequence(text: CharSequence): Boolean {
        var i = 0
        while (i < text.length) {
            val codePoint = Character.codePointAt(text, i)
            if (isGraphemeSequenceCodePoint(codePoint)) {
                return true
            }
            i += Character.charCount(codePoint)
        }
        return false
    }

    /**
     *  This function is a fast short-circuit. The codePoint list requires manual maintenance
     *  if new emoji rules are added.
     */
    private fun isGraphemeSequenceCodePoint(codePoint: Int): Boolean {
        return when {
            codePoint == ZERO_WIDTH_JOINER -> true
            codePoint in VARIATION_SELECTORS -> true
            codePoint in SUPPLEMENTARY_VARIATION_SELECTORS -> true
            codePoint in EMOJI_MODIFIERS -> true
            codePoint in REGIONAL_INDICATORS -> true
            codePoint in TAG_CHARACTERS -> true
            Character.getType(codePoint) in COMBINING_MARK_TYPES -> true
            else -> false
        }
    }

    private fun nextCodePointBoundary(text: CharSequence, index: Int): Int {
        val safeIndex = previousOrSameCodePointBoundary(text, index)
        return if (
            safeIndex < text.length &&
            text[safeIndex].isHighSurrogate() &&
            safeIndex + 1 < text.length &&
            text[safeIndex + 1].isLowSurrogate()
        ) {
            safeIndex + 2
        } else {
            (safeIndex + 1).coerceAtMost(text.length)
        }
    }

    private fun previousCodePointBoundary(text: CharSequence, index: Int): Int {
        val safeIndex = nextOrSameCodePointBoundary(text, index)
        return if (
            safeIndex >= 2 &&
            text[safeIndex - 2].isHighSurrogate() &&
            text[safeIndex - 1].isLowSurrogate()
        ) {
            safeIndex - 2
        } else {
            (safeIndex - 1).coerceAtLeast(0)
        }
    }

    private fun previousOrSameCodePointBoundary(text: CharSequence, index: Int): Int {
        val clampedIndex = index.coerceIn(0, text.length)
        return if (clampedIndex in 1..<text.length && text[clampedIndex].isLowSurrogate() && text[clampedIndex - 1].isHighSurrogate()) {
            clampedIndex - 1
        } else {
            clampedIndex
        }
    }

    private fun nextOrSameCodePointBoundary(text: CharSequence, index: Int): Int {
        val clampedIndex = index.coerceIn(0, text.length)
        return if (clampedIndex in 1..<text.length && text[clampedIndex].isLowSurrogate() && text[clampedIndex - 1].isHighSurrogate()) {
            clampedIndex + 1
        } else {
            clampedIndex
        }
    }

    private val COMBINING_MARK_TYPES = setOf(
        Character.NON_SPACING_MARK.toInt(),
        Character.COMBINING_SPACING_MARK.toInt(),
        Character.ENCLOSING_MARK.toInt(),
    )
    private const val ZERO_WIDTH_JOINER = 0x200D
    private val VARIATION_SELECTORS = 0xFE00..0xFE0F
    private val SUPPLEMENTARY_VARIATION_SELECTORS = 0xE0100..0xE01EF
    private val EMOJI_MODIFIERS = 0x1F3FB..0x1F3FF
    private val REGIONAL_INDICATORS = 0x1F1E6..0x1F1FF
    private val TAG_CHARACTERS = 0xE0020..0xE007F
}
