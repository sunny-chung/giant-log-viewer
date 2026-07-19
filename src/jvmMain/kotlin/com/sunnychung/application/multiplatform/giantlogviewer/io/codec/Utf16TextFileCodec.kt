package com.sunnychung.application.multiplatform.giantlogviewer.io.codec

import com.sunnychung.application.multiplatform.giantlogviewer.extension.toClampedInt
import com.sunnychung.application.multiplatform.giantlogviewer.io.DecodedTextWindow
import com.sunnychung.application.multiplatform.giantlogviewer.io.ResolvedTextEncoding
import com.sunnychung.application.multiplatform.giantlogviewer.io.TextFileCodec
import com.sunnychung.application.multiplatform.giantlogviewer.io.UTF16_CODE_UNIT_BYTES

internal abstract class Utf16TextFileCodec(
    override val encoding: ResolvedTextEncoding,
) : TextFileCodec {
    override val lineFeedByteLength: Long = UTF16_CODE_UNIT_BYTES.toLong()
    override val minBytesPerCharacter: Long = UTF16_CODE_UNIT_BYTES.toLong()

    override fun readText(
        startBytePosition: Long,
        minBytes: Int,
        fileLength: Long,
        readBytes: (Long, Int) -> Pair<ByteArray, LongRange>,
    ): DecodedTextWindow {
        if (minBytes <= 0 || fileLength <= encoding.contentStartBytePosition) {
            return Utf16DecodedTextWindow("", startBytePosition..<startBytePosition)
        }

        val requestedStart = startBytePosition
            .coerceAtLeast(encoding.contentStartBytePosition)
            .coerceAtMost(fileLength)
        var decodeStart = alignToCodeUnitStart(requestedStart)
        if (decodeStart >= encoding.contentStartBytePosition + UTF16_CODE_UNIT_BYTES && readCodeUnit(decodeStart, fileLength, readBytes)?.toChar()?.isLowSurrogate() == true) {
            decodeStart -= UTF16_CODE_UNIT_BYTES
        }

        val requestedEnd = (requestedStart + minBytes.toLong()).coerceAtMost(fileLength)
        var decodeEnd = alignToCodeUnitEnd(requestedEnd).coerceAtMost(fileLength)
        if (decodeEnd - UTF16_CODE_UNIT_BYTES >= decodeStart && readCodeUnit(decodeEnd - UTF16_CODE_UNIT_BYTES, fileLength, readBytes)?.toChar()?.isHighSurrogate() == true) {
            decodeEnd = (decodeEnd + UTF16_CODE_UNIT_BYTES).coerceAtMost(fileLength)
        }
        if ((decodeEnd - decodeStart) % UTF16_CODE_UNIT_BYTES != 0L) {
            --decodeEnd
        }
        if (decodeEnd <= decodeStart) {
            return Utf16DecodedTextWindow("", decodeStart..<decodeStart)
        }

        val (bytes, byteRange) = readBytes(decodeStart, (decodeEnd - decodeStart).toClampedInt())
        val usableLength = bytes.size - bytes.size % UTF16_CODE_UNIT_BYTES
        val text = String(bytes, 0, usableLength, encoding.charset)
        return Utf16DecodedTextWindow(text, byteRange.start..<(byteRange.start + usableLength.toLong()))
    }

    override fun encodedLength(text: CharSequence): Long = text.length * UTF16_CODE_UNIT_BYTES.toLong()

    override fun rawLineScanReadLength(requestedLength: Int): Int = (requestedLength + 1).coerceAtLeast(1)

    override fun findFirstLineFeedBytePosition(bytes: ByteArray, rangeStart: Long): Long? {
        var index = firstCodeUnitOffset(rangeStart)
        while (index + 1 < bytes.size) {
            if (isLineFeedCodeUnit(bytes, index)) {
                return rangeStart + index.toLong()
            }
            index += UTF16_CODE_UNIT_BYTES
        }
        return null
    }

    override fun findLineFeedBytePositions(bytes: ByteArray, rangeStart: Long): List<Long> {
        val positions = ArrayList<Long>()
        var index = firstCodeUnitOffset(rangeStart)
        while (index + 1 < bytes.size) {
            if (isLineFeedCodeUnit(bytes, index)) {
                positions += rangeStart + index.toLong()
            }
            index += UTF16_CODE_UNIT_BYTES
        }
        return positions
    }

    override fun findLastLineFeedBytePosition(
        bytes: ByteArray,
        rangeStart: Long,
        strictBeforeBytePosition: Long,
    ): Long? {
        var index = lastCodeUnitOffsetBefore(
            rangeStart = rangeStart,
            bytesSize = bytes.size,
            strictBeforeBytePosition = strictBeforeBytePosition,
        )
        while (index >= 0) {
            if (isLineFeedCodeUnit(bytes, index)) {
                return rangeStart + index.toLong()
            }
            index -= UTF16_CODE_UNIT_BYTES
        }
        return null
    }

    private fun alignToCodeUnitStart(bytePosition: Long): Long {
        val relative = bytePosition - encoding.contentStartBytePosition
        return if (relative % UTF16_CODE_UNIT_BYTES == 0L) {
            bytePosition
        } else {
            bytePosition - 1
        }
    }

    private fun alignToCodeUnitEnd(bytePosition: Long): Long {
        val relative = bytePosition - encoding.contentStartBytePosition
        return if (relative % UTF16_CODE_UNIT_BYTES == 0L) {
            bytePosition
        } else {
            bytePosition + 1L
        }
    }

    private fun firstCodeUnitOffset(rangeStart: Long): Int {
        val remainder = positiveMod(rangeStart - encoding.contentStartBytePosition, UTF16_CODE_UNIT_BYTES.toLong())
        return if (remainder == 0L) 0 else (UTF16_CODE_UNIT_BYTES - remainder).toInt()
    }

    private fun lastCodeUnitOffsetBefore(
        rangeStart: Long,
        bytesSize: Int,
        strictBeforeBytePosition: Long,
    ): Int {
        val lastPossibleAbsolutePosition = (strictBeforeBytePosition - UTF16_CODE_UNIT_BYTES)
            .coerceAtMost(rangeStart + bytesSize - UTF16_CODE_UNIT_BYTES)
        if (lastPossibleAbsolutePosition < rangeStart) {
            return -1
        }
        val alignedAbsolutePosition = lastPossibleAbsolutePosition -
            positiveMod(lastPossibleAbsolutePosition - encoding.contentStartBytePosition, UTF16_CODE_UNIT_BYTES.toLong())
        return (alignedAbsolutePosition - rangeStart).toInt()
    }

    protected abstract fun isLineFeedCodeUnit(bytes: ByteArray, index: Int): Boolean

    protected abstract fun readCodeUnit(firstByte: Int, secondByte: Int): Int

    private fun positiveMod(value: Long, modulus: Long): Long {
        return ((value % modulus) + modulus) % modulus
    }

    private fun readCodeUnit(
        bytePosition: Long,
        fileLength: Long,
        readBytes: (Long, Int) -> Pair<ByteArray, LongRange>,
    ): Int? {
        if (bytePosition < encoding.contentStartBytePosition || bytePosition + UTF16_CODE_UNIT_BYTES - 1L >= fileLength) {
            return null
        }
        val (bytes, _) = readBytes(bytePosition, UTF16_CODE_UNIT_BYTES)
        if (bytes.size < UTF16_CODE_UNIT_BYTES) {
            return null
        }
        val first = bytes[0].toInt() and 0xFF
        val second = bytes[1].toInt() and 0xFF
        return readCodeUnit(first, second)
    }
}
