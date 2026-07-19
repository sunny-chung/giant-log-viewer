package com.sunnychung.application.multiplatform.giantlogviewer.io.codec

import com.sunnychung.application.multiplatform.giantlogviewer.extension.toClampedInt
import com.sunnychung.application.multiplatform.giantlogviewer.io.DecodedTextWindow
import com.sunnychung.application.multiplatform.giantlogviewer.io.KOTLIN_CHARS_PER_SURROGATE_PAIR
import com.sunnychung.application.multiplatform.giantlogviewer.io.ResolvedTextEncoding
import com.sunnychung.application.multiplatform.giantlogviewer.io.TextFileCodec
import com.sunnychung.application.multiplatform.giantlogviewer.io.UTF8_MAX_BYTES_PER_CODE_POINT
import com.sunnychung.application.multiplatform.giantlogviewer.io.UTF8_MAX_CONTINUATION_BYTES
import java.nio.charset.StandardCharsets

internal class Utf8TextFileCodec(
    override val encoding: ResolvedTextEncoding,
) : TextFileCodec {
    override val lineFeedByteLength: Long = 1L
    override val minBytesPerCharacter: Long = 1L

    override fun readText(
        startBytePosition: Long,
        minBytes: Int,
        fileLength: Long,
        readBytes: (Long, Int) -> Pair<ByteArray, LongRange>,
    ): DecodedTextWindow {
        if (minBytes <= 0 || fileLength <= encoding.contentStartBytePosition) {
            return Utf8DecodedTextWindow("", startBytePosition..<startBytePosition)
        }

        val requestedStart = startBytePosition
            .coerceAtLeast(encoding.contentStartBytePosition)
            .coerceAtMost(fileLength)
        val rawStart = (requestedStart - encoding.lookBehindBytes)
            .coerceAtLeast(encoding.contentStartBytePosition)
        val rawEndExclusive = (requestedStart + minBytes.toLong() + encoding.lookAheadBytes.toLong())
            .coerceAtMost(fileLength)
        val (bytes, byteRange) = readBytes(rawStart, (rawEndExclusive - rawStart).toClampedInt())
        if (bytes.isEmpty()) {
            return Utf8DecodedTextWindow("", requestedStart..<requestedStart)
        }

        val requestedStartIndex = (requestedStart - byteRange.start).toInt().coerceIn(0, bytes.size)
        val requestedEndIndex = (requestedStart + minBytes.toLong() - byteRange.start).toClampedInt().coerceIn(0, bytes.size)
        val decodeStartIndex = findSequenceStart(bytes, requestedStartIndex)
        val decodeEndIndex = findSequenceEnd(bytes, decodeStartIndex, requestedEndIndex)
            .coerceAtLeast(decodeStartIndex)
            .coerceAtMost(bytes.size)

        val text = String(bytes, decodeStartIndex, decodeEndIndex - decodeStartIndex, StandardCharsets.UTF_8)
        val decodedStart = byteRange.start + decodeStartIndex.toLong()
        val decodedEnd = byteRange.start + decodeEndIndex.toLong()
        return Utf8DecodedTextWindow(text, decodedStart..<decodedEnd)
    }

    override fun encodedLength(text: CharSequence): Long {
        var bytes = 0L
        var i = 0
        while (i < text.length) {
            val char = text[i]
            if (char.isHighSurrogate() && i + 1 < text.length && text[i + 1].isLowSurrogate()) {
                bytes += UTF8_MAX_BYTES_PER_CODE_POINT
                i += KOTLIN_CHARS_PER_SURROGATE_PAIR
            } else {
                bytes += when {
                    char.code <= 0x7F -> 1
                    char.code <= 0x7FF -> 2
                    else -> 3
                }
                ++i
            }
        }
        return bytes
    }

    override fun rawLineScanReadLength(requestedLength: Int): Int = requestedLength.coerceAtLeast(1)

    override fun findFirstLineFeedBytePosition(bytes: ByteArray, rangeStart: Long): Long? {
        return bytes.indexOfFirst { it == LF_BYTE }
            .takeIf { it >= 0 }
            ?.let { rangeStart + it.toLong() }
    }

    override fun findLineFeedBytePositions(bytes: ByteArray, rangeStart: Long): List<Long> {
        val positions = ArrayList<Long>()
        bytes.forEachIndexed { index, byte ->
            if (byte == LF_BYTE) {
                positions += rangeStart + index.toLong()
            }
        }
        return positions
    }

    override fun findLastLineFeedBytePosition(
        bytes: ByteArray,
        rangeStart: Long,
        strictBeforeBytePosition: Long,
    ): Long? {
        var index = (strictBeforeBytePosition - rangeStart - 1L)
            .coerceAtMost((bytes.size - 1).toLong())
            .toInt()
        while (index >= 0) {
            if (bytes[index] == LF_BYTE) {
                return rangeStart + index.toLong()
            }
            --index
        }
        return null
    }

    private fun findSequenceStart(bytes: ByteArray, index: Int): Int {
        var i = index.coerceIn(0, bytes.size)
        var lookBehind = 0
        while (i in 1..<bytes.size && lookBehind < UTF8_MAX_CONTINUATION_BYTES && bytes[i].isContinuationByte()) {
            --i
            ++lookBehind
        }
        return i
    }

    private fun findSequenceEnd(bytes: ByteArray, startIndex: Int, endIndex: Int): Int {
        if (endIndex >= bytes.size) {
            return bytes.size
        }
        var i = startIndex
        while (i < endIndex && i < bytes.size) {
            val sequenceLength = bytes[i].sequenceLengthRepresentedByHeaderByte()
            if (i + sequenceLength > endIndex) {
                return i + sequenceLength
            }
            i += sequenceLength
        }
        return endIndex
    }

    private fun Byte.isContinuationByte(): Boolean = toUByte() in 0x80u..0xBFu

    private fun Byte.sequenceLengthRepresentedByHeaderByte(): Int {
        return when (toUByte()) {
            in 0xC2u..0xDFu -> 2
            in 0xE0u..0xEFu -> 3
            in 0xF0u..0xF4u -> UTF8_MAX_BYTES_PER_CODE_POINT
            else -> 1
        }
    }
}
