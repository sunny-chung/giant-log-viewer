package com.sunnychung.application.multiplatform.giantlogviewer.io.codec

import com.sunnychung.application.multiplatform.giantlogviewer.io.ResolvedTextEncoding

internal class Utf16BETextFileCodec(
    encoding: ResolvedTextEncoding,
) : Utf16TextFileCodec(encoding) {
    override fun isLineFeedCodeUnit(bytes: ByteArray, index: Int): Boolean {
        return index >= 0 &&
            index + 1 < bytes.size &&
            bytes[index] == NUL_BYTE &&
            bytes[index + 1] == LF_BYTE
    }

    override fun readCodeUnit(firstByte: Int, secondByte: Int): Int {
        return (firstByte shl 8) or secondByte
    }
}
