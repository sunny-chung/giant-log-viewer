package com.sunnychung.application.multiplatform.giantlogviewer

import com.sunnychung.application.multiplatform.giantlogviewer.io.CoroutineGiantFileTextPager
import com.sunnychung.application.multiplatform.giantlogviewer.io.GiantFileReader
import com.sunnychung.application.multiplatform.giantlogviewer.io.Viewport
import com.sunnychung.application.multiplatform.giantlogviewer.layout.MonospaceBidirectionalTextLayouter
import com.sunnychung.application.multiplatform.giantlogviewer.util.DivisibleWidthCharMeasurer
import com.sunnychung.application.multiplatform.giantlogviewer.util.FixedWidthCharMeasurer
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GiantFileTextPagerMixedNavigationTest {

    @ParameterizedTest
    @EnumSource(TestFileEncoding::class)
    fun computedPropertiesAndEncodedLengthReflectCurrentConfiguration(encoding: TestFileEncoding) {
        val fileContent = "ab\ncd\n"
        createTestFile(fileContent, encoding) { file ->
            val fileReader = GiantFileReader(file.absolutePath)
            val pager = CoroutineGiantFileTextPager(
                fileReader,
                MonospaceBidirectionalTextLayouter(FixedWidthCharMeasurer(charWidth = 10f, rowHeight = 5f)),
            )

            pager.viewport = Viewport(width = 20, height = 15, density = 1f)

            assertEquals(5f, pager.rowHeight())
            assertEquals(3L, pager.numOfRowsInViewport)
            assertTrue(pager.isSoftWrapEnabled)
            assertEquals("A\uD83D\uDE04".toByteArray(encoding.charset).size.toLong(), pager.encodedLengthOfText("A\uD83D\uDE04"))
            assertEquals(Viewport(width = 20, height = 15, density = 1f), pager.viewport)
            assertEquals(listOf("ab", "cd"), pager.textInViewport.take(2).map { it.toString() })
            assertEquals(listOf(encoding.bytePosition(fileContent, 0), encoding.bytePosition(fileContent, 3)), pager.startBytePositionsInViewport.take(2))

            pager.viewportStartBytePosition = encoding.bytePosition(fileContent, 3)

            assertEquals(encoding.bytePosition(fileContent, 3), pager.viewportStartBytePosition)
            assertEquals("cd", pager.textInViewport.first().toString())
        }
    }

    @ParameterizedTest
    @EnumSource(TestFileEncoding::class)
    fun softWrapCanBeDisabledAndPhysicalRowsAreUsed(encoding: TestFileEncoding) {
        val fileContent = "abcdef\nsecond\nthird"
        createTestFile(fileContent, encoding) { file ->
            val fileReader = GiantFileReader(file.absolutePath)
            val pager = CoroutineGiantFileTextPager(
                fileReader,
                MonospaceBidirectionalTextLayouter(FixedWidthCharMeasurer(charWidth = 10f, rowHeight = 12f)),
            )
            pager.viewport = Viewport(width = 100, height = 36, density = 1f)

            pager.updateSoftWrapEnabled(false)

            assertEquals(false, pager.isSoftWrapEnabled)
            assertEquals(listOf("abcdef", "second", "third"), pager.textInViewport.take(3).map { it.toString() })

            pager.moveToNextRow()
            assertEquals(encoding.bytePosition(fileContent, "abcdef\n".length), pager.viewportStartBytePosition)
            assertEquals("second", pager.textInViewport.first().toString())

            pager.moveToPrevRow()
            assertEquals(encoding.contentStartBytePosition, pager.viewportStartBytePosition)
            assertEquals("abcdef", pager.textInViewport.first().toString())
        }
    }

    @ParameterizedTest
    @EnumSource(TestFileEncoding::class)
    fun togglingSoftWrapAnchorsToSamePhysicalLine(encoding: TestFileEncoding) {
        val fileContent = "abcdef\nsecond"
        createTestFile(fileContent, encoding) { file ->
            val fileReader = GiantFileReader(file.absolutePath)
            val pager = CoroutineGiantFileTextPager(
                fileReader,
                MonospaceBidirectionalTextLayouter(FixedWidthCharMeasurer(charWidth = 10f, rowHeight = 12f)),
            )
            pager.viewport = Viewport(width = 20, height = 48, density = 1f)

            pager.moveToNextRow()
            assertEquals(encoding.bytePosition(fileContent, 2), pager.viewportStartBytePosition)
            assertEquals("cd", pager.textInViewport.first().toString())

            pager.viewport = Viewport(width = 100, height = 48, density = 1f)
            pager.updateSoftWrapEnabled(false)

            assertEquals(encoding.contentStartBytePosition, pager.viewportStartBytePosition)
            assertEquals("abcdef", pager.textInViewport.first().toString())
        }
    }

    @ParameterizedTest
    @EnumSource(TestFileEncoding::class)
    fun horizontalScrollChangesVisibleSliceWhenSoftWrapIsDisabled(encoding: TestFileEncoding) {
        val fileContent = "abcdef\nsecond"
        createTestFile(fileContent, encoding) { file ->
            val fileReader = GiantFileReader(file.absolutePath)
            val pager = CoroutineGiantFileTextPager(
                fileReader,
                MonospaceBidirectionalTextLayouter(FixedWidthCharMeasurer(charWidth = 10f, rowHeight = 12f)),
            )
            pager.viewport = Viewport(width = 30, height = 36, density = 1f)
            pager.updateSoftWrapEnabled(false)

            assertEquals("abc", pager.textInViewport.first().toString())
            assertEquals(encoding.contentStartBytePosition, pager.startBytePositionsInViewport.first())

            pager.scrollHorizontallyByPx(20f)

            assertEquals("cde", pager.textInViewport.first().toString())
            assertEquals(encoding.bytePosition(fileContent, 2), pager.startBytePositionsInViewport.first())

            pager.scrollHorizontallyByPx(-10_000f)

            assertEquals("abc", pager.textInViewport.first().toString())
            assertEquals(encoding.contentStartBytePosition, pager.startBytePositionsInViewport.first())
        }
    }

    @ParameterizedTest
    @EnumSource(TestFileEncoding::class)
    fun horizontalScrollWorksWhenFirstVisibleRowIsShort(encoding: TestFileEncoding) {
        val fileContent = "x\nabcdef\nsecond"
        createTestFile(fileContent, encoding) { file ->
            val fileReader = GiantFileReader(file.absolutePath)
            val pager = CoroutineGiantFileTextPager(
                fileReader,
                MonospaceBidirectionalTextLayouter(FixedWidthCharMeasurer(charWidth = 10f, rowHeight = 12f)),
            )
            pager.viewport = Viewport(width = 30, height = 36, density = 1f)
            pager.updateSoftWrapEnabled(false)

            assertEquals(listOf("x", "abc", "sec"), pager.textInViewport.take(3).map { it.toString() })

            pager.scrollHorizontallyByPx(20f)

            assertEquals(listOf("", "cde", "con"), pager.textInViewport.take(3).map { it.toString() })
            assertEquals(encoding.bytePosition(fileContent, "x\nab".length), pager.startBytePositionsInViewport[1])
        }
    }

    @ParameterizedTest
    @EnumSource(TestFileEncoding::class)
    fun horizontalScrollWorksWhenMultipleFirstVisibleRowsAreShort(encoding: TestFileEncoding) {
        val fileContent = "x\nx\nabcdef\nsecond"
        createTestFile(fileContent, encoding) { file ->
            val fileReader = GiantFileReader(file.absolutePath)
            val pager = CoroutineGiantFileTextPager(
                fileReader,
                MonospaceBidirectionalTextLayouter(FixedWidthCharMeasurer(charWidth = 10f, rowHeight = 12f)),
            )
            pager.viewport = Viewport(width = 30, height = 48, density = 1f)
            pager.updateSoftWrapEnabled(false)

            assertEquals(listOf("x", "x", "abc", "sec"), pager.textInViewport.take(4).map { it.toString() })

            pager.scrollHorizontallyByPx(20f)

            assertEquals(listOf("", "", "cde", "con"), pager.textInViewport.take(4).map { it.toString() })
            assertEquals(encoding.bytePosition(fileContent, "x\nx\nab".length), pager.startBytePositionsInViewport[2])
        }
    }

    @ParameterizedTest
    @EnumSource(TestFileEncoding::class)
    fun horizontalScrollKeepsEmojiSequencesWholeWhenSoftWrapIsDisabled(encoding: TestFileEncoding) {
        val clusters = listOf("A", "👆🏿", "B", "👨‍👩‍👧‍👦", "C", "🇭🇰", "D", "1️⃣", "E")
        val fileContent = clusters.joinToString("")
        createTestFile(fileContent, encoding) { file ->
            val fileReader = GiantFileReader(file.absolutePath)
            val pager = CoroutineGiantFileTextPager(
                fileReader,
                MonospaceBidirectionalTextLayouter(FixedWidthCharMeasurer(charWidth = 10f, rowHeight = 12f)),
            )
            pager.viewport = Viewport(width = 20, height = 36, density = 1f)
            pager.updateSoftWrapEnabled(false)

            assertEquals(clusters.take(2).joinToString(""), pager.textInViewport.first().toString())

            pager.scrollHorizontallyByPx(10f)
            assertEquals(clusters.drop(1).take(2).joinToString(""), pager.textInViewport.first().toString())

            pager.scrollHorizontallyByPx(10f)
            assertEquals(clusters.drop(2).take(2).joinToString(""), pager.textInViewport.first().toString())
        }
    }

    @ParameterizedTest
    @EnumSource(TestFileEncoding::class)
    fun horizontalScrollRatioUsesVisibleByteOffsetForEmojiLines(encoding: TestFileEncoding) {
        val cluster = "👆🏿"
        val clusterCount = 2_000
        val fileContent = cluster.repeat(clusterCount)
        createTestFile(fileContent, encoding) { file ->
            val fileReader = GiantFileReader(file.absolutePath)
            val pager = CoroutineGiantFileTextPager(
                fileReader,
                MonospaceBidirectionalTextLayouter(FixedWidthCharMeasurer(charWidth = 10f, rowHeight = 12f)),
            )
            pager.viewport = Viewport(width = 20, height = 36, density = 1f)
            pager.updateSoftWrapEnabled(false)

            pager.scrollHorizontallyByPx((clusterCount - 2) * 10f)

            assertTrue(pager.horizontalScrollRatio() > 0.95f)
            assertEquals(cluster.repeat(2), pager.textInViewport.first().toString())
        }
    }

    @ParameterizedTest
    @EnumSource(TestFileEncoding::class)
    fun horizontalScrollRatioUsesVisibleByteOffsetForAsciiLines(encoding: TestFileEncoding) {
        val lineLength = 2_000
        val fileContent = "x".repeat(lineLength)
        createTestFile(fileContent, encoding) { file ->
            val fileReader = GiantFileReader(file.absolutePath)
            val pager = CoroutineGiantFileTextPager(
                fileReader,
                MonospaceBidirectionalTextLayouter(FixedWidthCharMeasurer(charWidth = 10f, rowHeight = 12f)),
            )
            pager.viewport = Viewport(width = 20, height = 36, density = 1f)
            pager.updateSoftWrapEnabled(false)

            pager.scrollHorizontallyByPx((lineLength - 2) * 10f)

            assertTrue(pager.horizontalScrollRatio() > 0.95f)
            assertEquals("xx", pager.textInViewport.first().toString())
        }
    }

    @ParameterizedTest
    @EnumSource(TestFileEncoding::class)
    fun horizontalScrollbarCanReachEndOfWideEmojiSequenceLine(encoding: TestFileEncoding) {
        val cluster = "👨‍👩‍👧‍👦"
        val clusterCount = 2_000
        val fileContent = cluster.repeat(clusterCount)
        createTestFile(fileContent, encoding) { file ->
            val fileReader = GiantFileReader(file.absolutePath)
            val pager = CoroutineGiantFileTextPager(
                fileReader,
                MonospaceBidirectionalTextLayouter(DivisibleWidthCharMeasurer(charWidth = 10f)),
            )
            pager.viewport = Viewport(width = 40, height = 36, density = 1f)
            pager.updateSoftWrapEnabled(false)

            pager.scrollHorizontallyToRatio(1f)

            assertEquals(1f, pager.horizontalScrollRatio())
            assertEquals("", pager.textInViewport.first().toString())
            assertEquals(encoding.bytePosition(fileContent, fileContent.length), pager.startBytePositionsInViewport.first())
        }
    }

    @ParameterizedTest
    @EnumSource(TestFileEncoding::class)
    fun horizontalScrollbarJumpDoesNotDecodeHugeLinePrefix(encoding: TestFileEncoding) {
        val longLine = "x".repeat(2 * 1024 * 1024 + 123)
        val fileContent = "$longLine\nshort"
        createTestFile(fileContent, encoding) { file ->
            val fileReader = GiantFileReader(file.absolutePath)
            val pager = CoroutineGiantFileTextPager(
                fileReader,
                MonospaceBidirectionalTextLayouter(FixedWidthCharMeasurer(charWidth = 10f, rowHeight = 12f)),
            )
            pager.viewport = Viewport(width = 30, height = 36, density = 1f)
            pager.updateSoftWrapEnabled(false)

            pager.scrollHorizontallyToRatio(0.9f)

            assertEquals("xxx", pager.textInViewport.first().toString())
            assert(pager.startBytePositionsInViewport.first() > encoding.contentStartBytePosition)
        }
    }

    @ParameterizedTest
    @EnumSource(TestFileEncoding::class)
    fun unwrappedLayoutFindsRowsAfterHugePhysicalLine(encoding: TestFileEncoding) {
        val prefix = "short-before\n"
        val longLine = "x".repeat(2 * 1024 * 1024 + 123)
        val suffix = "\nafter-1\nafter-2\nafter-3\n"
        val fileContent = prefix + longLine + suffix
        createTestFile(fileContent, encoding) { file ->
            val fileReader = GiantFileReader(file.absolutePath)
            val pager = CoroutineGiantFileTextPager(
                fileReader,
                MonospaceBidirectionalTextLayouter(FixedWidthCharMeasurer(charWidth = 10f, rowHeight = 12f)),
            )
            pager.viewport = Viewport(width = 30, height = 12 * 5, density = 1f)
            pager.updateSoftWrapEnabled(false)

            pager.moveToRowOfBytePosition(encoding.bytePosition(fileContent, prefix.length + longLine.length / 2))

            assertEquals(encoding.bytePosition(fileContent, prefix.length), pager.viewportStartBytePosition)
            assertEquals(
                listOf("xxx", "aft", "aft", "aft"),
                pager.textInViewport.take(4).map { it.toString() },
            )
            assertEquals(
                listOf(
                    encoding.bytePosition(fileContent, prefix.length),
                    encoding.bytePosition(fileContent, prefix.length + longLine.length + 1),
                    encoding.bytePosition(fileContent, prefix.length + longLine.length + 1 + "after-1\n".length),
                    encoding.bytePosition(fileContent, prefix.length + longLine.length + 1 + "after-1\nafter-2\n".length),
                ),
                pager.startBytePositionsInViewport.take(4),
            )
        }
    }

    @ParameterizedTest
    @EnumSource(TestFileEncoding::class)
    fun unwrappedPreviousRowFindsHugePhysicalLineBeforeShortLines(encoding: TestFileEncoding) {
        val prefix = "short-before\n"
        val firstLongLine = "a".repeat(2 * 1024 * 1024 + 17)
        val secondLongLine = "b".repeat(2 * 1024 * 1024 + 19)
        val suffix = "\nafter-1\nafter-2"
        val fileContent = prefix + firstLongLine + "\n" + secondLongLine + suffix
        createTestFile(fileContent, encoding) { file ->
            val fileReader = GiantFileReader(file.absolutePath)
            val pager = CoroutineGiantFileTextPager(
                fileReader,
                MonospaceBidirectionalTextLayouter(FixedWidthCharMeasurer(charWidth = 10f, rowHeight = 12f)),
            )
            pager.viewport = Viewport(width = 30, height = 12 * 5, density = 1f)
            pager.updateSoftWrapEnabled(false)
            val afterFirstLineStart = prefix.length + firstLongLine.length + 1 + secondLongLine.length + 1
            pager.moveToRowOfBytePosition(encoding.bytePosition(fileContent, afterFirstLineStart))

            pager.moveToPrevRow()

            val secondLongLineStart = prefix.length + firstLongLine.length + 1
            assertEquals(encoding.bytePosition(fileContent, secondLongLineStart), pager.viewportStartBytePosition)
            assertEquals("bbb", pager.textInViewport.first().toString())
        }
    }

    @ParameterizedTest
    @EnumSource(TestFileEncoding::class)
    fun moveToRowOfBytePositionAlignsToRenderedRowStart(encoding: TestFileEncoding) {
        val fileContent = "abcde\nsecond\n"
        createTestFile(fileContent, encoding) { file ->
            val fileReader = GiantFileReader(file.absolutePath)
            val pager = CoroutineGiantFileTextPager(
                fileReader,
                MonospaceBidirectionalTextLayouter(FixedWidthCharMeasurer(10f)),
            )
            pager.viewport = Viewport(width = 20, height = 36, density = 1f)

            pager.moveToRowOfBytePosition(encoding.bytePosition(fileContent, 3))

            assertEquals(encoding.bytePosition(fileContent, 2), pager.viewportStartBytePosition)
            assertEquals("cd", pager.textInViewport.first().toString())
        }
    }

    @ParameterizedTest
    @EnumSource(TestFileEncoding::class)
    fun emojiSequencesWrapAndNavigateAsSingleDisplayUnits(encoding: TestFileEncoding) {
        val clusters = listOf("A", "👆🏿", "B", "👨‍👩‍👧‍👦", "C", "🇭🇰", "D", "1️⃣", "E")
        val fileContent = clusters.joinToString("")
        createTestFile(fileContent, encoding) { file ->
            val fileReader = GiantFileReader(file.absolutePath)
            val pager = CoroutineGiantFileTextPager(
                fileReader,
                MonospaceBidirectionalTextLayouter(FixedWidthCharMeasurer(10f)),
            )
            pager.viewport = Viewport(width = 20, height = 12 * 6, density = 1f)

            val expectedRows = clusters.chunked(2).map { it.joinToString("") }
            assertEquals(expectedRows, pager.textInViewport.take(expectedRows.size).map { it.toString() })

            val expectedRowStartCharIndexes = expectedRows.runningFold(0) { index, row -> index + row.length }
                .dropLast(1)
            assertEquals(
                expectedRowStartCharIndexes.map { encoding.bytePosition(fileContent, it) },
                pager.startBytePositionsInViewport.take(expectedRows.size),
            )

            pager.moveToNextRow()
            assertEquals(encoding.bytePosition(fileContent, expectedRowStartCharIndexes[1]), pager.viewportStartBytePosition)
            assertEquals(expectedRows.drop(1), pager.textInViewport.take(expectedRows.size - 1).map { it.toString() })

            pager.moveToPrevRow()
            assertEquals(encoding.contentStartBytePosition, pager.viewportStartBytePosition)
            assertEquals(expectedRows, pager.textInViewport.take(expectedRows.size).map { it.toString() })
        }
    }

    @ParameterizedTest
    @EnumSource(TestFileEncoding::class)
    fun longEmojiSequenceLineKeepsForwardAndBackwardRowStartsConsistent(encoding: TestFileEncoding) {
        val repeatedClusters = List(43) { listOf("🇭🇰", "👨‍👩‍👧‍👦", "1️⃣") }.flatten()
        val fileContent = repeatedClusters.joinToString("")
        val clustersPerRow = 43
        createTestFile(fileContent, encoding) { file ->
            val fileReader = GiantFileReader(file.absolutePath)
            val pager = CoroutineGiantFileTextPager(
                fileReader,
                MonospaceBidirectionalTextLayouter(FixedWidthCharMeasurer(10f)),
            )
            pager.viewport = Viewport(width = 10 * clustersPerRow, height = 12 * 4, density = 1f)

            val expectedRows = repeatedClusters.chunked(clustersPerRow).map { it.joinToString("") }
            val expectedRowStartCharIndexes = expectedRows.runningFold(0) { index, row -> index + row.length }
                .dropLast(1)
            val expectedRowStartBytePositions = expectedRowStartCharIndexes.map {
                encoding.bytePosition(fileContent, it)
            }

            assertEquals(expectedRows, pager.textInViewport.take(expectedRows.size).map { it.toString() })
            assertEquals(expectedRowStartBytePositions, pager.startBytePositionsInViewport.take(expectedRows.size))

            pager.moveToNextRow(2L)
            assertEquals(expectedRowStartBytePositions[2], pager.viewportStartBytePosition)
            assertEquals(expectedRows[2], pager.textInViewport.first().toString())

            pager.moveToPrevRow()
            assertEquals(expectedRowStartBytePositions[1], pager.viewportStartBytePosition)
            assertEquals(expectedRows[1], pager.textInViewport.first().toString())

            pager.moveToPrevRow()
            assertEquals(expectedRowStartBytePositions[0], pager.viewportStartBytePosition)
            assertEquals(expectedRows[0], pager.textInViewport.first().toString())
        }
    }

    @ParameterizedTest
    @EnumSource(TestFileEncoding::class)
    fun moveMultipleRowsForwardAndBackward(encoding: TestFileEncoding) {
        val fileContent = "r0\nr1\nr2\nr3\n"
        createTestFile(fileContent, encoding) { file ->
            val fileReader = GiantFileReader(file.absolutePath)
            val pager = CoroutineGiantFileTextPager(
                fileReader,
                MonospaceBidirectionalTextLayouter(FixedWidthCharMeasurer(10f)),
            )
            pager.viewport = Viewport(width = 80, height = 36, density = 1f)

            pager.moveToNextRow(2L)

            assertEquals(encoding.bytePosition(fileContent, 6), pager.viewportStartBytePosition)
            assertEquals("r2", pager.textInViewport.first().toString())

            pager.moveToPrevRow(1L)

            assertEquals(encoding.bytePosition(fileContent, 3), pager.viewportStartBytePosition)
            assertEquals("r1", pager.textInViewport.first().toString())

            pager.moveToPrevRow(5L)

            assertEquals(encoding.contentStartBytePosition, pager.viewportStartBytePosition)
            assertEquals("r0", pager.textInViewport.first().toString())
        }
    }

    @ParameterizedTest
    @EnumSource(TestFileEncoding::class)
    fun pageMovementUsesOneRowWhenViewportHasNoFullRows(encoding: TestFileEncoding) {
        val fileContent = "first\nsecond\nthird"
        createTestFile(fileContent, encoding) { file ->
            val fileReader = GiantFileReader(file.absolutePath)
            val pager = CoroutineGiantFileTextPager(
                fileReader,
                MonospaceBidirectionalTextLayouter(DivisibleWidthCharMeasurer(16f)),
            )
            pager.viewport = Viewport(width = 16 * 23, height = 1, density = 1f)
            val initialPosition = pager.viewportStartBytePosition

            pager.moveToNextRow(0L)
            assertEquals(initialPosition, pager.viewportStartBytePosition)

            pager.moveToNextPage()
            val secondLinePosition = encoding.bytePosition(fileContent, "first\n".length)
            assertEquals(secondLinePosition, pager.viewportStartBytePosition)

            pager.moveToPrevRow(0L)
            assertEquals(secondLinePosition, pager.viewportStartBytePosition)

            pager.moveToPrevPage()
            assertEquals(initialPosition, pager.viewportStartBytePosition)
        }
    }
}
