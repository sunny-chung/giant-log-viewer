package com.sunnychung.application.multiplatform.giantlogviewer

import com.sunnychung.application.multiplatform.giantlogviewer.io.CoroutineGiantFileTextPager
import com.sunnychung.application.multiplatform.giantlogviewer.io.GiantFileReader
import com.sunnychung.application.multiplatform.giantlogviewer.io.GiantFileTextPager
import com.sunnychung.application.multiplatform.giantlogviewer.io.Viewport
import com.sunnychung.application.multiplatform.giantlogviewer.layout.MonospaceBidirectionalTextLayouter
import com.sunnychung.application.multiplatform.giantlogviewer.util.DivisibleWidthCharMeasurer
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.io.File
import kotlin.random.Random
import kotlin.test.assertEquals

class GiantFileTextPagerSearchBackwardTest {

    @ParameterizedTest
    @EnumSource(TestFileEncoding::class)
    fun simpleSearch(encoding: TestFileEncoding) {
        val fileContent = "abcdfg\nefg\nhijfgfkfgl"
        val searchPattern = "fg"
        verifySearchForEncoding(encoding, fileContent, searchPattern)
    }

    @ParameterizedTest
    @EnumSource(TestFileEncoding::class)
    fun overlappedSearches(encoding: TestFileEncoding) {
        val fileContent = "fffabcdffff\nefg\nfffffffff"
        val searchPattern = "ff"
        verifySearchForEncoding(encoding, fileContent, searchPattern)
    }

    @ParameterizedTest
    @EnumSource(TestFileEncoding::class)
    fun singleChar(encoding: TestFileEncoding) {
        val fileContent = "fffabcdffff\nefg\nfffffffff"
        val searchPattern = "f"
        verifySearchForEncoding(encoding, fileContent, searchPattern)
    }

    @ParameterizedTest
    @EnumSource(TestFileEncoding::class)
    fun notFound(encoding: TestFileEncoding) {
        val fileContent = "fffabcdffff\nefg\nfffffffff"
        val searchPattern = "z"
        verifySearchForEncoding(encoding, fileContent, searchPattern)
    }

    @ParameterizedTest
    @EnumSource(TestFileEncoding::class)
    fun unicode(encoding: TestFileEncoding) {
        val fileContent = "喂你好你好你好你好你好呀."
        val searchPattern = "你好你"
        verifySearchForEncoding(encoding, fileContent, searchPattern)
    }

    @ParameterizedTest
    @EnumSource(TestFileEncoding::class)
    fun asciiAcrossMultipleBlocks(encoding: TestFileEncoding) {
        val random = Random(2345)
        val searchPattern = "@@AB"
        val blockSize = 200
        val fileContent: String = (0 ..< 10000).joinToString { i ->
            if (i % blockSize >= blockSize - searchPattern.length && random.nextInt(9) == 0) {
                return@joinToString searchPattern
            }
            when (val r = random.nextInt(38)) {
                in 0 ..< 10 -> ('0'.code + r).toChar().toString()
                in 10 ..< 36 -> ('a'.code + (r - 10)).toChar().toString()
                36 -> "\n"
                else -> searchPattern
            }
        }
        verifySearchForEncoding(encoding, fileContent, searchPattern, blockSize)
    }

    @ParameterizedTest
    @EnumSource(TestFileEncoding::class)
    fun searchLongTextInAsciiAcrossMultipleBlocks(encoding: TestFileEncoding) {
        val random = Random(2346)
        val searchPattern = "@@ABadiofjbafasoijgfafkmsgoasoksfoiasjfggnJngjgnsgnnsgaIJROEtmkfamskfgsdfgmksldmfgmksglsfGLMSFKGllmksgl"
        val blockSize = 200
        val fileContent: String = (0 ..< 4000).joinToString { i ->
            if (i % blockSize >= blockSize - searchPattern.length && random.nextInt(9) == 0) {
                return@joinToString searchPattern
            }
            when (val r = random.nextInt(38)) {
                in 0 ..< 10 -> ('0'.code + r).toChar().toString()
                in 10 ..< 36 -> ('a'.code + (r - 10)).toChar().toString()
                36 -> "\n"
                else -> searchPattern
            }
        }
        verifySearchForEncoding(encoding, fileContent, searchPattern, blockSize)
//        verifySearchForEncoding(encoding, fileContent, searchPattern, blockSize) { file ->
//            representativeBackwardSearchPositions(
//                fileContent = fileContent,
//                encoding = encoding,
//                searchRegex = searchPattern.toRegex(),
//                fileSize = file.length(),
//                blockSize = blockSize,
//            )
//        }
    }

    @ParameterizedTest
    @EnumSource(TestFileEncoding::class)
    fun searchPatternLongerThanBlockSize(encoding: TestFileEncoding) {
        val searchPattern = "TARGET-" + "0123456789abcdef".repeat(5)
        val blockSize = 32
        val fileContent = "prefix\n" + searchPattern + "\nsuffix"
        verifySearchForEncoding(encoding, fileContent, searchPattern, blockSize) { file -> file.length() downTo 0L }
    }

    @ParameterizedTest
    @EnumSource(TestFileEncoding::class)
    fun boundedRegexSearchWithinLocalWindow(encoding: TestFileEncoding) {
        val matchedText = "A" + "x".repeat(100) + "Z"
        val fileContent = "prefix\n" + matchedText + "\nsuffix"
        createTestFile(fileContent, encoding) { file ->
            val fileReader = GiantFileReader(file.absolutePath, 64)
            val pager = CoroutineGiantFileTextPager(
                fileReader,
                MonospaceBidirectionalTextLayouter(DivisibleWidthCharMeasurer(16f)),
            )
            pager.viewport = Viewport(width = 16 * 7, height = 12 * 5, density = 1f)

            val expectedStart = encoding.bytePosition(fileContent, "prefix\n".length)
            val expectedEnd = encoding.bytePosition(fileContent, "prefix\n".length + matchedText.length)
            assertEquals(expectedStart..<expectedEnd, pager.searchBackward(file.length(), Regex("A[\\s\\S]{100}Z")))
        }
    }

    @ParameterizedTest
    @EnumSource(TestFileEncoding::class)
    fun largeBoundedRegexSearchIsLimitedToLocalWindow(encoding: TestFileEncoding) {
        val matchedText = "A" + "x".repeat(1000) + "Z"
        val fileContent = "prefix\n" + matchedText + "\nsuffix"
        createTestFile(fileContent, encoding) { file ->
            val fileReader = GiantFileReader(file.absolutePath, 64)
            val pager = CoroutineGiantFileTextPager(
                fileReader,
                MonospaceBidirectionalTextLayouter(DivisibleWidthCharMeasurer(16f)),
            )
            pager.viewport = Viewport(width = 16 * 7, height = 12 * 5, density = 1f)

            assertEquals(GiantFileTextPager.NOT_FOUND, pager.searchBackward(file.length(), Regex("A[\\s\\S]{1000}Z")))
        }
    }

    @ParameterizedTest
    @EnumSource(TestFileEncoding::class)
    fun unboundedRegexSearchIsLimitedToLocalWindow(encoding: TestFileEncoding) {
        val fileContent = "prefix\nA" + "x".repeat(1000) + "Z\nsuffix"
        createTestFile(fileContent, encoding) { file ->
            val fileReader = GiantFileReader(file.absolutePath, 64)
            val pager = CoroutineGiantFileTextPager(
                fileReader,
                MonospaceBidirectionalTextLayouter(DivisibleWidthCharMeasurer(16f)),
            )
            pager.viewport = Viewport(width = 16 * 7, height = 12 * 5, density = 1f)

            assertEquals(GiantFileTextPager.NOT_FOUND, pager.searchBackward(file.length(), Regex("A.*Z")))
        }
    }

    @ParameterizedTest
    @EnumSource(TestFileEncoding::class)
    fun unboundedRegexSearchFindsMatchInsideLocalWindow(encoding: TestFileEncoding) {
        val matchedText = "A" + "x".repeat(100) + "Z"
        val fileContent = "prefix\n" + matchedText + "\nsuffix"
        createTestFile(fileContent, encoding) { file ->
            val fileReader = GiantFileReader(file.absolutePath, 64)
            val pager = CoroutineGiantFileTextPager(
                fileReader,
                MonospaceBidirectionalTextLayouter(DivisibleWidthCharMeasurer(16f)),
            )
            pager.viewport = Viewport(width = 16 * 7, height = 12 * 5, density = 1f)

            val expectedStart = encoding.bytePosition(fileContent, "prefix\n".length)
            val expectedEnd = encoding.bytePosition(fileContent, "prefix\n".length + matchedText.length)
            assertEquals(expectedStart..<expectedEnd, pager.searchBackward(file.length(), Regex("A.*Z")))
        }
    }

    @ParameterizedTest
    @EnumSource(TestFileEncoding::class)
    fun searchDoesNotMatchInsideEmojiSequence(encoding: TestFileEncoding) {
        val fileContent = "A👆🏿B"
        createTestFile(fileContent, encoding) { file ->
            val fileReader = GiantFileReader(file.absolutePath, 16)
            val pager = CoroutineGiantFileTextPager(
                fileReader,
                MonospaceBidirectionalTextLayouter(DivisibleWidthCharMeasurer(16f)),
            )
            pager.viewport = Viewport(width = 16 * 7, height = 12 * 5, density = 1f)

            assertEquals(GiantFileTextPager.NOT_FOUND, pager.searchBackward(file.length(), Regex("👆")))
            assertEquals(
                encoding.byteRange(fileContent, 1..4),
                pager.searchBackward(file.length(), Regex("👆🏿")),
            )
        }
    }

    @ParameterizedTest
    @EnumSource(TestFileEncoding::class)
    fun unicodeAcrossMultipleBlocks(encoding: TestFileEncoding) {
        val random = Random(2347)
        val searchPattern = "喂你好😄😄!"
        val blockSize = 200
        val charset = "零一二三四五六七八九ABCDabc".map { it.toString() } + listOf("😄", "😄", "😇", "🤣", "🤯", "🤬", "🫡", "🫠", "😵")
        val fileContent: String = (0 ..< 5000).joinToString { i ->
            if (i % blockSize >= blockSize - searchPattern.length && random.nextInt(9) == 0) {
                return@joinToString searchPattern
            }
            when (val r = random.nextInt(100)) {
                in 0 ..< 95 -> charset[random.nextInt(0, charset.size)]
                in 96 ..< 97 -> "\n"
                else -> searchPattern
            }
        }
        verifySearchForEncoding(encoding, fileContent, searchPattern, blockSize)
    }

    @ParameterizedTest
    @EnumSource(TestFileEncoding::class)
    fun unicodeAcrossMultipleBlocksNotFound(encoding: TestFileEncoding) {
        val random = Random(2347)
        val searchPattern = "喂你好😄😄!"
        val blockSize = 200
        val charset = "零一二三四五六七八九ABCDabc".map { it.toString() } + listOf("😄", "😄", "😇", "🤣", "🤯", "🤬", "🫡", "🫠", "😵")
        val fileContent: String = (0 ..< 10000).joinToString { i ->
            when (val r = random.nextInt(100)) {
                in 0 ..< 96 -> charset[random.nextInt(0, charset.size)]
                else -> "\n"
            }
        }
        createTestFile(fileContent, encoding) { file ->
            val fileSize = file.length()
            verifySearch(file, encoding, fileContent, searchPattern, blockSize, searchRange = fileSize downTo fileSize - 1005)
            verifySearch(file, encoding, fileContent, searchPattern, blockSize, searchRange = fileSize * 7 / 10 + 300 downTo fileSize * 7 / 10 - 300)
            verifySearch(file, encoding, fileContent, searchPattern, blockSize, searchRange = fileSize / 2 + 600 downTo fileSize / 2 - 600)
            verifySearch(file, encoding, fileContent, searchPattern, blockSize, searchRange = fileSize / 5 + 300 downTo fileSize / 5 - 300)
            verifySearch(file, encoding, fileContent, searchPattern, blockSize, searchRange = 1200L downTo 0L)
        }
    }
}

private fun verifySearchForEncoding(
    encoding: TestFileEncoding,
    fileContent: String,
    searchPattern: String,
    blockSize: Int = 1 * 1024 * 1024,
    searchRange: (File) -> Iterable<Long>? = { null },
) {
    createTestFile(fileContent, encoding) { file ->
        verifySearch(file, encoding, fileContent, searchPattern, blockSize, searchRange(file))
    }
}

private fun verifySearch(
    file: File,
    encoding: TestFileEncoding,
    fileContent: String,
    searchPattern: String,
    blockSize: Int = 1 * 1024 * 1024,
    searchRange: Iterable<Long>? = null,
) {
    val fileReader = GiantFileReader(file.absolutePath, blockSize)
    val pager = CoroutineGiantFileTextPager(
        fileReader, MonospaceBidirectionalTextLayouter(
            DivisibleWidthCharMeasurer(16f)
        )
    )
    pager.viewport = Viewport(width = 16 * 7, height = 12 * 5, density = 1f)
    val fileSize = file.length()
    val searchRegex = searchPattern.toRegex()
    val matchRanges = matchByteRanges(fileContent, encoding, searchRegex)
    (searchRange ?: (fileSize downTo 0)).forEach { i ->
        assertEquals(
            lastBytePositionOf(matchRanges, i),
            pager.searchBackward(i, searchRegex).also {
//                println("search starts at $i found $it")
            },
            "search starts at $i"
        )
    }
}

private fun matchByteRanges(content: String, encoding: TestFileEncoding, regex: Regex): List<LongRange> {
    return regex.findAll(content)
        .map { encoding.byteRange(content, it.range) }
        .toList()
}

private fun lastBytePositionOf(matchRanges: List<LongRange>, start: Long): LongRange {
    var low = 0
    var high = matchRanges.lastIndex
    var result = -1
    while (low <= high) {
        val mid = (low + high) ushr 1
        if (matchRanges[mid].start < start) {
            result = mid
            low = mid + 1
        } else {
            high = mid - 1
        }
    }
    return matchRanges.getOrNull(result) ?: GiantFileTextPager.NOT_FOUND
}

private fun representativeBackwardSearchPositions(
    fileContent: String,
    encoding: TestFileEncoding,
    searchRegex: Regex,
    fileSize: Long,
    blockSize: Int,
): List<Long> {
    val positions = sortedSetOf<Long>()

    fun add(position: Long) {
        if (position in 0L..fileSize) {
            positions += position
        }
    }

    listOf(
        0L,
        encoding.contentStartBytePosition,
        fileSize,
        fileSize / 4,
        fileSize / 2,
        fileSize * 3 / 4,
    ).forEach(::add)

    (0L..fileSize step blockSize.toLong()).toList().sampleEvenly(32).forEach { blockStart ->
        (-4L..4L).forEach { delta -> add(blockStart + delta) }
    }

    matchByteRanges(fileContent, encoding, searchRegex).sampleEvenly(64).forEach { range ->
        listOf(
            range.start - 2,
            range.start - 1,
            range.start,
            range.start + 1,
            range.start + 2,
            range.endInclusive,
            range.endInclusive + 1,
            range.endInclusive + 2,
        ).forEach(::add)
    }

    return positions.toList().asReversed()
}

private fun <T> List<T>.sampleEvenly(maxSamples: Int): List<T> {
    require(maxSamples > 0)
    if (size <= maxSamples) {
        return this
    }
    return (0..<maxSamples).map { index ->
        this[(index.toLong() * lastIndex / (maxSamples - 1L)).toInt()]
    }.distinct()
}
