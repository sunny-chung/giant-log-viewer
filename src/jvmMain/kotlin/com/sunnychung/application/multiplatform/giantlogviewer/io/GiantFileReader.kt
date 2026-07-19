package com.sunnychung.application.multiplatform.giantlogviewer.io

import com.sunnychung.application.multiplatform.giantlogviewer.io.codec.Utf16BETextFileCodec
import com.sunnychung.application.multiplatform.giantlogviewer.io.codec.Utf16LETextFileCodec
import com.sunnychung.application.multiplatform.giantlogviewer.io.codec.Utf8TextFileCodec
import java.io.ByteArrayOutputStream
import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

private const val BLOCK_CURRENT = 1
private const val BLOCK_PREVIOUS = 0
private const val BLOCK_NEXT = 2
private const val BLOCK_AFTER_NEXT = 3
private const val BLOCK_CACHE_SIZE = BLOCK_AFTER_NEXT + 1
private const val BLOCK_AFTER_NEXT_DISTANCE_FROM_CURRENT = BLOCK_AFTER_NEXT - BLOCK_CURRENT

class GiantFileReader(
    val filePath: String,
    val blockSize: Int = DEFAULT_FILE_BLOCK_SIZE_BYTES,
    initialFileLength: Long = -1,
    val textEncoding: TextEncoding = TextEncoding.Auto,
) : AutoCloseable {
    private val file = RandomAccessFile(filePath, "r")

    init {
        require(blockSize >= MIN_BLOCK_SIZE) { "`blockSize` should be at least $MIN_BLOCK_SIZE bytes" }
    }

    var fileLength: Long = initialFileLength.takeIf { it > 0 } ?:
        file.length()

    private val blockCacheLock = ReentrantReadWriteLock()
    private val blockCache: Array<FileBlock?> = arrayOfNulls(BLOCK_CACHE_SIZE)
    private var bytePositions: LongRange = -1L .. -2L
    private val codec: TextFileCodec by lazy {
        createCodec(resolveTextEncoding())
    }

    val resolvedTextEncoding: ResolvedTextEncoding
        get() = codec.encoding

    val contentStartBytePosition: Long
        get() = resolvedTextEncoding.contentStartBytePosition

    override fun close() {
        file.close()
    }

    fun lengthInBytes(): Long = fileLength

    private fun readBlock(block: FileBlockPosition, fileSize: Long): Pair<ByteArray, LongRange> {
        if (block.position < 0 || block.position > fileSize / blockSize) {
            throw IndexOutOfBoundsException("Attempt to read block ${block.position} from ${block.anchor} but there are only ${fileSize / blockSize} blocks. File size is ${fileSize}.")
        }
        if (block.anchor == FileAnchor.Start) {
            val readStart = block.position * blockSize
            val readLength = minOf(blockSize.toLong(), fileSize - readStart).toInt()
            val bytes = ByteArray(readLength)
            if (readLength > 0) {
                file.seek(readStart)
                file.read(bytes)
            }
            return bytes to (readStart ..< readStart + readLength)
        } else {
            throw NotImplementedError()
        }
    }

    private fun readBlockIfNotRead(block: FileBlockPosition): FileBlock {
        val fileSize = fileLength
        blockCache.forEach {
            if (it != null
                && it.pos == block
                && (!isLastBlock(it.pos, fileSize) || it.bytes.size == (fileSize % blockSize).toInt())
            ) {
                return it
            }
        }
        return readBlock(block, fileSize).let {
            FileBlock(block, it.first, it.second)
        }
    }

    private fun isLastBlock(pos: FileBlockPosition, fileSize: Long): Boolean {
        return if (pos.anchor == FileAnchor.Start) {
            pos.position >= fileSize / blockSize
        } else {
            pos.position == 0L
        }
    }

    private fun loadBlockPosition(block: FileBlockPosition) {
        val fileSize = fileLength
        if (block.position < 0 || block.position > fileSize / blockSize) {
            throw IndexOutOfBoundsException("Attempt to read block ${block.position} from ${block.anchor} but there are only ${fileSize / blockSize} blocks. File size is ${fileSize}.")
        }
        val currentBlock = readBlockIfNotRead(block)
        val prevBlock = block.takeIf { it.position > 0 }?.let {
            val pos = block.copy(position = it.position - 1)
            readBlockIfNotRead(pos)
        }
        val nextBlock1 = block.takeIf { it.position < fileSize / blockSize }?.let {
            val pos = block.copy(position = it.position + 1)
            readBlockIfNotRead(pos)
        }
        val nextBlock2 = block.takeIf { it.position + 1 < fileSize / blockSize }?.let {
            val pos = block.copy(position = it.position + BLOCK_AFTER_NEXT_DISTANCE_FROM_CURRENT)
            readBlockIfNotRead(pos)
        }
        blockCacheLock.write {
            blockCache[BLOCK_PREVIOUS] = prevBlock
            blockCache[BLOCK_CURRENT] = currentBlock
            blockCache[BLOCK_NEXT] = nextBlock1
            blockCache[BLOCK_AFTER_NEXT] = nextBlock2 // this is for tracking multi-byte characters that start at the end of nextBlock1
            bytePositions = (blockCache.filterNotNull().minOfOrNull { it.bytePositions.first } ?: -1) ..
                (blockCache.filterNotNull().maxOfOrNull { it.bytePositions.last } ?: -2)
        }
    }

    private fun loadBytePosition(position: Long) {
//        val bytePositions = blockCacheLock.read { bytePositions }
//        if (position in bytePositions) {
//            return
//        }

        blockCacheLock.read {
            if (blockCache[BLOCK_CURRENT]?.bytePositions?.contains(position) == true) {
                return
            }
        }

        loadBlockPosition(FileBlockPosition(FileAnchor.Start, position / blockSize))
    }

    /**
     * This should be called within a lock scope
     */
    private fun read(positionInBlock: PositionInBlock?): Byte? {
        if (positionInBlock == null) {
            return null
        }
        return blockCache[positionInBlock.blockIndex]?.bytes?.get(positionInBlock.bytePosition)
    }

    internal fun readRawBytes(startBytePosition: Long, length: Int): Pair<ByteArray, LongRange> {
        if (length <= 0) {
            return ByteArray(0) to (startBytePosition ..< startBytePosition)
        }

        val fileSize = fileLength
        val start = startBytePosition.coerceIn(0L, fileSize)
        val endExclusive = (start + length.toLong()).coerceAtMost(fileSize)
        if (endExclusive <= start) {
            return ByteArray(0) to (start ..< start)
        }

        val out = ByteArrayOutputStream((endExclusive - start).toInt())
        var current = start
        while (current < endExclusive) {
            loadBytePosition(current)
            val block = blockCacheLock.read { blockCache[BLOCK_CURRENT] }
                ?: throw IllegalStateException("Cannot load file block for byte position $current")
            val blockReadStart = (current - block.bytePositions.start).toInt()
            val blockReadLength = (endExclusive - current)
                .coerceAtMost(block.bytes.size - blockReadStart.toLong())
                .toInt()
            if (blockReadLength <= 0) {
                break
            }
            out.write(block.bytes, blockReadStart, blockReadLength)
            current += blockReadLength
        }
        return out.toByteArray() to (start ..< current)
    }

    private fun readRawBytesDirect(
        file: RandomAccessFile,
        startBytePosition: Long,
        length: Int,
    ): Pair<ByteArray, LongRange> {
        if (length <= 0) {
            return ByteArray(0) to (startBytePosition ..< startBytePosition)
        }

        val fileSize = fileLength
        val start = startBytePosition.coerceIn(0L, fileSize)
        val endExclusive = (start + length.toLong()).coerceAtMost(fileSize)
        if (endExclusive <= start) {
            return ByteArray(0) to (start ..< start)
        }

        val bytes = ByteArray((endExclusive - start).toInt())
        file.seek(start)
        var offset = 0
        while (offset < bytes.size) {
            val bytesRead = file.read(bytes, offset, bytes.size - offset)
            if (bytesRead < 0) {
                break
            }
            offset += bytesRead
        }
        return if (offset == bytes.size) {
            bytes to (start ..< endExclusive)
        } else {
            bytes.copyOf(offset) to (start ..< start + offset.toLong())
        }
    }

    internal fun readAsByteArrayOutputStream(startBytePosition: Long, length: Int): Pair<ByteArrayOutputStream2, LongRange> {
        val window = readText(startBytePosition, length)
        val bytes = window.text.toByteArray(resolvedTextEncoding.charset)
        val out = ByteArrayOutputStream2(bytes.size)
        out.write(bytes)
        return out to window.byteRange
    }

    /**
     * Reads at least `length` bytes when possible, then expands the decoded range as needed so the
     * returned text does not start or end inside a multi-byte character. File bounds may make the
     * returned range shorter than `length`.
     *
     * @param startBytePosition 0-based, in bytes
     * @param length minimum bytes to include before character-boundary adjustment
     * @return decoded text and the absolute byte range that produced it
     */
    fun readText(startBytePosition: Long, length: Int): DecodedTextWindow {
        return codec.readText(startBytePosition, length, fileLength, ::readRawBytes)
    }

    /**
     * Same decoding contract as `readText`, but reads bytes through a separate file handle instead
     * of the block cache. This is intended for large one-shot reads such as clipboard copy.
     */
    fun readTextUncached(startBytePosition: Long, length: Int): DecodedTextWindow {
        RandomAccessFile(filePath, "r").use { directFile ->
            return codec.readText(startBytePosition, length, fileLength) { start, readLength ->
                readRawBytesDirect(directFile, start, readLength)
            }
        }
    }

    fun readString(startBytePosition: Long, length: Int): Pair<String, LongRange> {
        val window = readText(startBytePosition, length)
        return window.text to window.byteRange
    }

    fun readStringBytes(startBytePosition: Long, length: Int): Pair<ByteArray, LongRange> {
        val window = readText(startBytePosition, length)
        return window.text.toByteArray(resolvedTextEncoding.charset) to window.byteRange
    }

    fun encodedLength(text: CharSequence): Long = codec.encodedLength(text)

    fun rawLineScanReadLength(requestedLength: Int): Int = codec.rawLineScanReadLength(requestedLength)

    fun findFirstLineFeedBytePosition(bytes: ByteArray, rangeStart: Long): Long? {
        return codec.findFirstLineFeedBytePosition(bytes, rangeStart)
    }

    fun findLineFeedBytePositions(bytes: ByteArray, rangeStart: Long): List<Long> {
        return codec.findLineFeedBytePositions(bytes, rangeStart)
    }

    fun findLastLineFeedBytePosition(
        bytes: ByteArray,
        rangeStart: Long,
        strictBeforeBytePosition: Long,
    ): Long? {
        return codec.findLastLineFeedBytePosition(bytes, rangeStart, strictBeforeBytePosition)
    }

    val lineFeedByteLength: Long
        get() = codec.lineFeedByteLength

    val minBytesPerCharacter: Long
        get() = codec.minBytesPerCharacter

    private fun resolveTextEncoding(): ResolvedTextEncoding {
        val header = readRawBytes(0L, TEXT_ENCODING_PROBE_BYTES).first
        return when {
            textEncoding == TextEncoding.Utf8WithoutBom -> utf8Encoding(bomLength = 0)
            textEncoding == TextEncoding.Utf8WithBom -> utf8Encoding(bomLength = UTF8_BOM_BYTE_COUNT)
            textEncoding == TextEncoding.Utf16LEWithoutBom -> utf16LEEncoding(bomLength = 0)
            textEncoding == TextEncoding.Utf16LEWithBom -> utf16LEEncoding(bomLength = UTF16_BOM_BYTE_COUNT)
            textEncoding == TextEncoding.Utf16BEWithoutBom -> utf16BEEncoding(bomLength = 0)
            textEncoding == TextEncoding.Utf16BEWithBom -> utf16BEEncoding(bomLength = UTF16_BOM_BYTE_COUNT)
            textEncoding == TextEncoding.Utf8 -> utf8Encoding(bomLength = if (header.hasUtf8Bom()) UTF8_BOM_BYTE_COUNT else 0)
            textEncoding == TextEncoding.Utf16LE -> utf16LEEncoding(bomLength = if (header.hasUtf16LEBom()) UTF16_BOM_BYTE_COUNT else 0)
            textEncoding == TextEncoding.Utf16BE -> utf16BEEncoding(bomLength = if (header.hasUtf16BEBom()) UTF16_BOM_BYTE_COUNT else 0)
            header.hasUtf8Bom() -> utf8Encoding(bomLength = UTF8_BOM_BYTE_COUNT)
            header.hasUtf16LEBom() -> utf16LEEncoding(bomLength = UTF16_BOM_BYTE_COUNT)
            header.hasUtf16BEBom() -> utf16BEEncoding(bomLength = UTF16_BOM_BYTE_COUNT)
            else -> utf8Encoding(bomLength = 0)
        }
    }

    private fun createCodec(encoding: ResolvedTextEncoding): TextFileCodec {
        return when (encoding.kind) {
            TextEncodingKind.Utf8 -> Utf8TextFileCodec(encoding)
            TextEncodingKind.Utf16LE -> Utf16LETextFileCodec(encoding)
            TextEncodingKind.Utf16BE -> Utf16BETextFileCodec(encoding)
        }
    }

    private fun utf8Encoding(bomLength: Int): ResolvedTextEncoding {
        return ResolvedTextEncoding(
            kind = TextEncodingKind.Utf8,
            charset = StandardCharsets.UTF_8,
            bomLength = bomLength,
            contentStartBytePosition = bomLength.toLong(),
            lookBehindBytes = UTF8_MAX_CONTINUATION_BYTES,
            lookAheadBytes = UTF8_MAX_CONTINUATION_BYTES,
            maxBytesPerCharacter = UTF8_MAX_BYTES_PER_CODE_POINT,
        )
    }

    private fun utf16LEEncoding(bomLength: Int): ResolvedTextEncoding {
        return ResolvedTextEncoding(
            kind = TextEncodingKind.Utf16LE,
            charset = StandardCharsets.UTF_16LE,
            bomLength = bomLength,
            contentStartBytePosition = bomLength.toLong(),
            lookBehindBytes = UTF16_SURROGATE_PAIR_BYTES,
            lookAheadBytes = UTF16_SURROGATE_PAIR_BYTES,
            maxBytesPerCharacter = UTF16_SURROGATE_PAIR_BYTES,
        )
    }

    private fun utf16BEEncoding(bomLength: Int): ResolvedTextEncoding {
        return ResolvedTextEncoding(
            kind = TextEncodingKind.Utf16BE,
            charset = StandardCharsets.UTF_16BE,
            bomLength = bomLength,
            contentStartBytePosition = bomLength.toLong(),
            lookBehindBytes = UTF16_SURROGATE_PAIR_BYTES,
            lookAheadBytes = UTF16_SURROGATE_PAIR_BYTES,
            maxBytesPerCharacter = UTF16_SURROGATE_PAIR_BYTES,
        )
    }

    private fun ByteArray.hasUtf8Bom(): Boolean {
        return size >= 3 &&
            (this[0].toInt() and 0xFF) == 0xEF &&
            (this[1].toInt() and 0xFF) == 0xBB &&
            (this[2].toInt() and 0xFF) == 0xBF
    }

    private fun ByteArray.hasUtf16LEBom(): Boolean {
        return size >= 2 &&
            (this[0].toInt() and 0xFF) == 0xFF &&
            (this[1].toInt() and 0xFF) == 0xFE
    }

    private fun ByteArray.hasUtf16BEBom(): Boolean {
        return size >= 2 &&
            (this[0].toInt() and 0xFF) == 0xFE &&
            (this[1].toInt() and 0xFF) == 0xFF
    }

    companion object {
        const val MIN_BLOCK_SIZE: Int = TEXT_ENCODING_PROBE_BYTES
    }

    private class FileBlock(val pos: FileBlockPosition, val bytes: ByteArray, val bytePositions: LongRange)

    private inner class PositionInBlock(val blockIndex: Int, val bytePosition: Int) {
        operator fun plus(byteOffset: Int): PositionInBlock? {
            blockCacheLock.read {
                if (blockIndex !in blockCache.indices) {
                    throw IndexOutOfBoundsException()
                }
                val block = blockCache[blockIndex] ?: return null
                val newBytePosition = block.bytePositions.start + bytePosition + byteOffset
                (blockIndex - 1..blockIndex + 1)
                    .forEach {
                        val block = blockCache.getOrNull(it) ?: return@forEach
                        if (newBytePosition in block.bytePositions) {
                            return PositionInBlock(it, (newBytePosition - block.bytePositions.start).toInt())
                        }
                    }
            }
            return null
        }

        operator fun minus(byteOffset: Int): PositionInBlock? = plus(- byteOffset)
    }
}

enum class FileAnchor {
    Start, End
}

data class FileRelativePosition(val anchor: FileAnchor, val position: Long)

typealias FileBlockPosition = FileRelativePosition
