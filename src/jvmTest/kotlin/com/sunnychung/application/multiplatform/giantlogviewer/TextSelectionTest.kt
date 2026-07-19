package com.sunnychung.application.multiplatform.giantlogviewer

import androidx.compose.ui.geometry.Offset
import com.sunnychung.application.multiplatform.giantlogviewer.io.CoroutineGiantFileTextPager
import com.sunnychung.application.multiplatform.giantlogviewer.io.GiantFileReader
import com.sunnychung.application.multiplatform.giantlogviewer.io.Viewport
import com.sunnychung.application.multiplatform.giantlogviewer.layout.MonospaceBidirectionalTextLayouter
import com.sunnychung.application.multiplatform.giantlogviewer.util.FixedWidthCharMeasurer
import com.sunnychung.application.multiplatform.giantlogviewer.ux.ColumnSelectionPoint
import com.sunnychung.application.multiplatform.giantlogviewer.ux.TextSelection
import com.sunnychung.application.multiplatform.giantlogviewer.ux.buildColumnSelection
import com.sunnychung.application.multiplatform.giantlogviewer.ux.rangeInRow
import com.sunnychung.application.multiplatform.giantlogviewer.ux.readSelectedText
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import kotlin.test.Test
import kotlin.test.assertEquals

class TextSelectionTest {

    @ParameterizedTest
    @EnumSource(TestFileEncoding::class)
    fun contiguousSelectionCopiesPhysicalByteRange(encoding: TestFileEncoding) {
        val content = "abcdef\n123456\n"
        createTestFile(content, encoding) { file ->
            listOf(true, false).forEach { softWrapEnabled ->
                createPager(file.absolutePath, softWrapEnabled = softWrapEnabled).use { (reader, pager) ->
                    val selection = TextSelection.Contiguous(
                        encoding.byteRange(content, 2..<9),
                    )

                    val copied = readSelectedText(reader, pager, selection, maxByteLength = Long.MAX_VALUE)

                    assertEquals("cdef\n12", copied.text, "softWrapEnabled=$softWrapEnabled")
                    assertEquals(
                        "cdef\n12".toByteArray(encoding.charset).size.toLong(),
                        copied.byteLength,
                        "softWrapEnabled=$softWrapEnabled",
                    )
                }
            }
        }
    }

    @ParameterizedTest
    @EnumSource(TestFileEncoding::class)
    fun contiguousSelectionCopyIsTrimmedToByteLimit(encoding: TestFileEncoding) {
        val content = "abcdef\n123456\n"
        createTestFile(content, encoding) { file ->
            listOf(true, false).forEach { softWrapEnabled ->
                createPager(file.absolutePath, softWrapEnabled = softWrapEnabled).use { (reader, pager) ->
                    val selection = TextSelection.Contiguous(
                        encoding.byteRange(content, 0..<content.length),
                    )
                    val maxBytes = "abc".toByteArray(encoding.charset).size.toLong()

                    val copied = readSelectedText(reader, pager, selection, maxByteLength = maxBytes)

                    assertEquals("abc", copied.text, "softWrapEnabled=$softWrapEnabled")
                    assertEquals(maxBytes, copied.byteLength, "softWrapEnabled=$softWrapEnabled")
                }
            }
        }
    }

    @ParameterizedTest
    @EnumSource(TestFileEncoding::class)
    fun columnSelectionCopiesSameColumnsAcrossRows(encoding: TestFileEncoding) {
        val content = "abcdef\n123456\nXYZ\n"
        createTestFile(content, encoding) { file ->
            createPager(file.absolutePath).use { (reader, pager) ->
                val selection = buildColumnSelection(
                    filePager = pager,
                    anchor = Offset(x = 10.1f, y = 1f),
                    focus = Offset(x = 40.1f, y = 13f),
                )

                assertEquals(
                    encoding.byteRange(content, 1..<4),
                    selection.rangeInRow(pager.viewportRows[0], pager),
                )
                assertEquals(
                    encoding.byteRange(content, 8..<11),
                    selection.rangeInRow(pager.viewportRows[1], pager),
                )
                assertEquals(
                    "bcd\n234".toByteArray(encoding.charset).size.toLong(),
                    readSelectedText(reader, pager, selection, maxByteLength = Long.MAX_VALUE).byteLength,
                )
                assertEquals(
                    "bcd\n234",
                    readSelectedText(reader, pager, selection, maxByteLength = Long.MAX_VALUE).text,
                )
            }
        }
    }

    @Test
    fun columnSelectionKeepsBlankRowsBetweenSelectedRows() {
        val content = "abcdef\n12\nUVWXYZ\n"
        createTestFile(content) { file ->
            createPager(file.absolutePath).use { (reader, pager) ->
                val selection = buildColumnSelection(
                    filePager = pager,
                    anchor = Offset(x = 20.1f, y = 1f),
                    focus = Offset(x = 50.1f, y = 25f),
                )

                assertEquals("cde\n\nWXY", readSelectedText(reader, pager, selection, Long.MAX_VALUE).text)
            }
        }
    }

    @Test
    fun columnSelectionSupportsReversedDrag() {
        val content = "abcdef\n123456\n"
        createTestFile(content) { file ->
            createPager(file.absolutePath).use { (reader, pager) ->
                val selection = buildColumnSelection(
                    filePager = pager,
                    anchor = Offset(x = 40.1f, y = 13f),
                    focus = Offset(x = 10.1f, y = 1f),
                )

                assertEquals("bcd\n234", readSelectedText(reader, pager, selection, Long.MAX_VALUE).text)
            }
        }
    }

    @Test
    fun columnSelectionCopyFromWrappedContinuationRowDoesNotStartWithLineBreak() {
        val content = "abcdef\n123456\n"
        createTestFile(content) { file ->
            createPager(file.absolutePath, viewportWidth = 30).use { (reader, pager) ->
                val selection = buildColumnSelection(
                    filePager = pager,
                    anchor = Offset(x = 0.1f, y = 13f),
                    focus = Offset(x = 20.1f, y = 37f),
                )

                assertEquals("de\n12\n45", readSelectedText(reader, pager, selection, Long.MAX_VALUE).text)
            }
        }
    }

    @Test
    fun columnSelectionFromWrappedContinuationRowCopiesNothingAfterSoftWrapDisabled() {
        val content = "abcdefghijklmnopqrstuvwxyz\n"
        createTestFile(content) { file ->
            createPager(file.absolutePath, viewportWidth = 30).use { (reader, pager) ->
                val selection = buildColumnSelection(
                    filePager = pager,
                    anchor = Offset(x = 0.1f, y = 25f),
                    focus = Offset(x = 30.1f, y = 25f),
                )
                assertEquals("ghi", readSelectedText(reader, pager, selection, Long.MAX_VALUE).text)

                pager.updateSoftWrapEnabled(false)

                assertEquals("", readSelectedText(reader, pager, selection, Long.MAX_VALUE).text)
                assertEquals(0L, readSelectedText(reader, pager, selection, Long.MAX_VALUE).byteLength)
            }
        }
    }

    @ParameterizedTest
    @EnumSource(TestFileEncoding::class)
    fun columnSelectionExtendsBetweenStableBytePositionEndpoints(encoding: TestFileEncoding) {
        val content = "abcdef\n123456\nUVWXYZ\nmnopqr\n"
        createTestFile(content, encoding) { file ->
            createPager(file.absolutePath).use { (reader, pager) ->
                pager.moveToRowOfBytePosition(encoding.bytePosition(content, content.length))

                val selection = buildColumnSelection(
                    filePager = pager,
                    anchor = ColumnSelectionPoint(
                        bytePosition = encoding.bytePosition(content, 1),
                        rowStartBytePosition = encoding.bytePosition(content, 0),
                        x = 10.1f,
                    ),
                    focus = ColumnSelectionPoint(
                        bytePosition = encoding.bytePosition(content, 24),
                        rowStartBytePosition = encoding.bytePosition(content, 21),
                        x = 40.1f,
                    ),
                )

                assertEquals("bcd\n234\nVWX\nnop", readSelectedText(reader, pager, selection, Long.MAX_VALUE).text)
            }
        }
    }

    @ParameterizedTest
    @EnumSource(TestFileEncoding::class)
    fun columnSelectionUsesEncodedByteRangesForMultibyteText(encoding: TestFileEncoding) {
        val content = "A😄BCD\n1😄345\n"
        createTestFile(content, encoding) { file ->
            createPager(file.absolutePath).use { (reader, pager) ->
                val selection = buildColumnSelection(
                    filePager = pager,
                    anchor = Offset(x = 10.1f, y = 1f),
                    focus = Offset(x = 30.1f, y = 13f),
                )

                assertEquals(
                    encoding.byteRange(content, 1..<4),
                    selection.rangeInRow(pager.viewportRows[0], pager),
                )
                assertEquals(
                    encoding.byteRange(content, 8..<11),
                    selection.rangeInRow(pager.viewportRows[1], pager),
                )
                assertEquals("😄B\n😄3", readSelectedText(reader, pager, selection, Long.MAX_VALUE).text)
            }
        }
    }

    @Test
    fun columnSelectionCopyIsTrimmedToByteLimit() {
        val content = "abcdef\n123456\n"
        createTestFile(content) { file ->
            createPager(file.absolutePath).use { (reader, pager) ->
                val selection = buildColumnSelection(
                    filePager = pager,
                    anchor = Offset(x = 0.1f, y = 1f),
                    focus = Offset(x = 60.1f, y = 13f),
                )

                assertEquals("abcdef\n1", readSelectedText(reader, pager, selection, maxByteLength = 8L).text)
                assertEquals(8L, readSelectedText(reader, pager, selection, maxByteLength = 8L).byteLength)
            }
        }
    }

    @Test
    fun columnSelectionCopyWithSoftWrapDisabledTraversesPhysicalRows() {
        val content = "abcdef\n123456\nUVWXYZ\nmnopqr\nstuvwx\n"
        createTestFile(content) { file ->
            createPager(file.absolutePath).use { (reader, pager) ->
                pager.updateSoftWrapEnabled(false)
                val selection = buildColumnSelection(
                    filePager = pager,
                    anchor = Offset(x = 10.1f, y = 1f),
                    focus = Offset(x = 40.1f, y = 49f),
                )

                assertEquals("bcd\n234\nVWX\nnop\ntuv", readSelectedText(reader, pager, selection, Long.MAX_VALUE).text)
            }
        }
    }

    @Test
    fun columnSelectionCopyWithSoftWrapDisabledStopsAtByteLimitAcrossManyShortRows() {
        val content = (0 until 1_000).joinToString(separator = "\n", postfix = "\n") { "x" }
        createTestFile(content) { file ->
            createPager(file.absolutePath).use { (reader, pager) ->
                pager.updateSoftWrapEnabled(false)
                val selection = buildColumnSelection(
                    filePager = pager,
                    anchor = ColumnSelectionPoint(
                        bytePosition = 1L,
                        rowStartBytePosition = 0L,
                        x = 20.1f,
                    ),
                    focus = ColumnSelectionPoint(
                        bytePosition = content.length - 1L,
                        rowStartBytePosition = content.length - 2L,
                        x = 40.1f,
                    ),
                )

                val copied = readSelectedText(reader, pager, selection, maxByteLength = 256L)

                assertEquals(256L, copied.byteLength)
                assertEquals("\n".repeat(256), copied.text)
            }
        }
    }

    @Test
    fun columnSelectionCopyStopsWhenCancelled() {
        val content = "abcdef\n123456\nUVWXYZ\nmnopqr\n"
        createTestFile(content) { file ->
            createPager(file.absolutePath).use { (reader, pager) ->
                val selection = buildColumnSelection(
                    filePager = pager,
                    anchor = Offset(x = 0.1f, y = 1f),
                    focus = Offset(x = 60.1f, y = 37f),
                )
                var shouldContinueCalls = 0

                val copied = readSelectedText(reader, pager, selection, Long.MAX_VALUE) {
                    shouldContinueCalls += 1
                    shouldContinueCalls == 1
                }

                assertEquals("abcdef", copied.text)
                assertEquals(2, shouldContinueCalls)
            }
        }
    }

    private fun createPager(
        filePath: String,
        viewportWidth: Int = 1000,
        softWrapEnabled: Boolean = true,
    ): ReaderAndPager {
        val reader = GiantFileReader(filePath)
        val pager = CoroutineGiantFileTextPager(
            reader,
            MonospaceBidirectionalTextLayouter(FixedWidthCharMeasurer(charWidth = 10f)),
        )
        pager.viewport = Viewport(width = viewportWidth, height = 1000, density = 1f)
        pager.updateSoftWrapEnabled(softWrapEnabled)
        return ReaderAndPager(reader, pager)
    }

    private data class ReaderAndPager(
        val reader: GiantFileReader,
        val pager: CoroutineGiantFileTextPager,
    ) : AutoCloseable {
        override fun close() {
            reader.close()
        }
    }
}
