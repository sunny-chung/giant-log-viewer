package com.sunnychung.application.multiplatform.giantlogviewer.io

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

private const val FILE_COPY_BUFFER_BYTES = 1024 * 1024

fun copyFileByteRange(
    source: File,
    destination: File,
    byteRange: LongRange,
) {
    val start = byteRange.start.coerceAtLeast(0L)
    val endExclusive = byteRange.endExclusive.coerceAtLeast(start)
    FileInputStream(source).use { input ->
        FileOutputStream(destination).use { output ->
            val buffer = ByteArray(FILE_COPY_BUFFER_BYTES)
            var remainingBytes = endExclusive - start
            input.channel.position(start)
            while (remainingBytes > 0L) {
                val bytesToRead = remainingBytes
                    .coerceAtMost(buffer.size.toLong())
                    .toInt()
                val bytesRead = input.read(buffer, 0, bytesToRead)
                if (bytesRead < 0) {
                    break
                }
                output.write(buffer, 0, bytesRead)
                remainingBytes -= bytesRead.toLong()
            }
        }
    }
}
