package com.sunnychung.application.multiplatform.giantlogviewer.io

internal interface TextFileCodec {
    val encoding: ResolvedTextEncoding
    val lineFeedByteLength: Long
    val minBytesPerCharacter: Long

    /**
     * Decodes a byte window beginning at `startBytePosition`. `minBytes` is the requested minimum
     * before character-boundary adjustment, but file bounds can make the returned range shorter.
     */
    fun readText(
        startBytePosition: Long,
        minBytes: Int,
        fileLength: Long,
        readBytes: (Long, Int) -> Pair<ByteArray, LongRange>,
    ): DecodedTextWindow

    fun encodedLength(text: CharSequence): Long

    fun rawLineScanReadLength(requestedLength: Int): Int

    fun findFirstLineFeedBytePosition(bytes: ByteArray, rangeStart: Long): Long?

    fun findLineFeedBytePositions(bytes: ByteArray, rangeStart: Long): List<Long>

    fun findLastLineFeedBytePosition(
        bytes: ByteArray,
        rangeStart: Long,
        strictBeforeBytePosition: Long,
    ): Long?
}
