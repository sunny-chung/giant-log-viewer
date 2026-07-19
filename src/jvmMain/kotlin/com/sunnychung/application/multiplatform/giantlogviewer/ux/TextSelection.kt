package com.sunnychung.application.multiplatform.giantlogviewer.ux

import androidx.compose.ui.geometry.Offset
import com.sunnychung.application.multiplatform.giantlogviewer.extension.forwardLength
import com.sunnychung.application.multiplatform.giantlogviewer.io.GiantFileReader
import com.sunnychung.application.multiplatform.giantlogviewer.io.GiantFileTextPager
import com.sunnychung.application.multiplatform.giantlogviewer.io.ViewportRow
import com.sunnychung.application.multiplatform.giantlogviewer.io.trimTextToByteLength
import com.sunnychung.application.multiplatform.giantlogviewer.layout.BidirectionalTextLayouter
import com.sunnychung.application.multiplatform.giantlogviewer.util.GraphemeClusters
import java.lang.Float.isFinite
import kotlin.math.floor

internal sealed interface TextSelection {
    data object Empty : TextSelection

    /**
     * A normal selection represented as a physical byte range in the file.
     */
    data class Contiguous(val range: LongRange) : TextSelection

    data class Column(
        val anchor: ColumnSelectionPoint,
        val focus: ColumnSelectionPoint,
    ) : TextSelection
}

internal data class SelectionText(
    val text: String,
    val byteLength: Long,
)

internal data class ColumnSelectionPoint(
    val bytePosition: Long,
    val rowStartBytePosition: Long,
    val x: Float,
)

internal fun TextSelection.isEmpty(): Boolean = when (this) {
    TextSelection.Empty -> true
    is TextSelection.Contiguous -> range.isEmpty()
    is TextSelection.Column -> anchor.x == focus.x
}

internal operator fun TextSelection.contains(bytePosition: Long): Boolean {
    return when (this) {
        TextSelection.Empty -> false
        is TextSelection.Contiguous -> !range.isEmpty() && bytePosition in range
        is TextSelection.Column -> bytePosition in minOf(anchor.rowStartBytePosition, focus.rowStartBytePosition)..maxOf(anchor.rowStartBytePosition, focus.rowStartBytePosition)
    }
}

internal fun buildColumnSelection(
    filePager: GiantFileTextPager,
    anchor: Offset,
    focus: Offset,
): TextSelection.Column {
    val anchorRow = rowAtPointInViewport(filePager, anchor)
    val focusRow = rowAtPointInViewport(filePager, focus)
    val anchorBytePosition = bytePositionAtXInRow(
        row = anchorRow,
        x = anchor.x,
        textLayouter = filePager.textLayouter,
        encodedLength = filePager::encodedLengthOfText,
    )
    val focusBytePosition = bytePositionAtXInRow(
        row = focusRow,
        x = focus.x,
        textLayouter = filePager.textLayouter,
        encodedLength = filePager::encodedLengthOfText,
    )
    return TextSelection.Column(
        anchor = ColumnSelectionPoint(
            bytePosition = anchorBytePosition,
            rowStartBytePosition = anchorRow.rowStartBytePosition,
            x = anchor.x,
        ),
        focus = ColumnSelectionPoint(
            bytePosition = focusBytePosition,
            rowStartBytePosition = focusRow.rowStartBytePosition,
            x = focus.x,
        ),
    )
}

internal fun buildColumnSelection(
    filePager: GiantFileTextPager,
    anchor: ColumnSelectionPoint,
    focus: ColumnSelectionPoint,
): TextSelection.Column {
    return TextSelection.Column(anchor = anchor, focus = focus)
}

internal fun buildColumnSelection(
    rows: List<ViewportRow>,
    rowHeight: Float,
    textLayouter: BidirectionalTextLayouter,
    encodedLength: (CharSequence) -> Long,
    anchor: Offset,
    focus: Offset,
): TextSelection.Column {
    if (rows.isEmpty() || !isFinite(rowHeight) || rowHeight <= 0f) {
        return TextSelection.Column(
            anchor = ColumnSelectionPoint(0L, 0L, anchor.x),
            focus = ColumnSelectionPoint(0L, 0L, focus.x),
        )
    }

    val firstRowIndex = rowIndexAtY(anchor.y, rowHeight, rows.lastIndex)
    val lastRowIndex = rowIndexAtY(focus.y, rowHeight, rows.lastIndex)
    val anchorRow = rows[firstRowIndex]
    val focusRow = rows[lastRowIndex]
    return TextSelection.Column(
        anchor = ColumnSelectionPoint(
            bytePosition = bytePositionAtXInRow(anchorRow, anchor.x, textLayouter, encodedLength),
            rowStartBytePosition = anchorRow.rowStartBytePosition,
            x = anchor.x,
        ),
        focus = ColumnSelectionPoint(
            bytePosition = bytePositionAtXInRow(focusRow, focus.x, textLayouter, encodedLength),
            rowStartBytePosition = focusRow.rowStartBytePosition,
            x = focus.x,
        ),
    )
}

internal fun columnRangeInRow(
    row: ViewportRow,
    startX: Float,
    endX: Float,
    textLayouter: BidirectionalTextLayouter,
    encodedLength: (CharSequence) -> Long,
): LongRange {
    val startBytePosition = bytePositionAtXInRow(
        row = row,
        x = startX,
        textLayouter = textLayouter,
        encodedLength = encodedLength,
    )
    val endBytePosition = bytePositionAtXInRow(
        row = row,
        x = endX,
        textLayouter = textLayouter,
        encodedLength = encodedLength,
    )
    return minOf(startBytePosition, endBytePosition)..<maxOf(startBytePosition, endBytePosition)
}

internal fun TextSelection.rangeInRow(
    row: ViewportRow,
    filePager: GiantFileTextPager,
): LongRange {
    return when (this) {
        TextSelection.Empty -> 0L..<0L
        is TextSelection.Contiguous -> range
        is TextSelection.Column -> {
            val firstRowStart = minOf(anchor.rowStartBytePosition, focus.rowStartBytePosition)
            val lastRowStart = maxOf(anchor.rowStartBytePosition, focus.rowStartBytePosition)
            if (row.rowStartBytePosition !in firstRowStart..lastRowStart) {
                0L..<0L
            } else {
                columnRangeInRow(
                    row = row,
                    startX = minOf(anchor.x, focus.x),
                    endX = maxOf(anchor.x, focus.x),
                    textLayouter = filePager.textLayouter,
                    encodedLength = filePager::encodedLengthOfText,
                )
            }
        }
    }
}

internal fun readSelectedText(
    fileReader: GiantFileReader,
    filePager: GiantFileTextPager,
    selection: TextSelection,
    maxByteLength: Long,
    shouldContinue: () -> Boolean = { true },
): SelectionText {
    if (selection.isEmpty() || maxByteLength <= 0L) {
        return SelectionText("", 0L)
    }

    if (selection is TextSelection.Contiguous) {
        return readSelectedTextRanges(
            fileReader = fileReader,
            ranges = listOf(selection.range).filterNot { it.isEmpty() },
            maxByteLength = maxByteLength,
        )
    }

    if (selection !is TextSelection.Column) {
        return SelectionText("", 0L)
    }
    if (!filePager.isSoftWrapEnabled &&
        (!filePager.isPhysicalLineStartBytePosition(selection.anchor.rowStartBytePosition) ||
            !filePager.isPhysicalLineStartBytePosition(selection.focus.rowStartBytePosition))
    ) {
        return SelectionText("", 0L)
    }

    val ranges = ArrayList<LongRange>(1)
    val result = StringBuilder()
    var copiedBytes = 0L
    var isFirstRow = true
    filePager.forEachViewportRowBetween(
        firstRowStartBytePosition = selection.anchor.rowStartBytePosition,
        secondRowStartBytePosition = selection.focus.rowStartBytePosition,
    ) { row ->
        if (!shouldContinue()) {
            return@forEachViewportRowBetween false
        }
        ranges.clear()
        val rangeInRow = selection.rangeInRow(row, filePager)
        if (rangeInRow.isEmpty() && row.text.isEmpty() && row.visibleStartBytePosition != row.physicalLineStartBytePosition) {
            return@forEachViewportRowBetween true
        }
        ranges += rangeInRow
        if (!isFirstRow) {
            if (copiedBytes + fileReader.lineFeedByteLength > maxByteLength) {
                return@forEachViewportRowBetween false
            }
            result.append('\n')
            copiedBytes += fileReader.lineFeedByteLength
        }
        isFirstRow = false

        val rowText = readSelectedTextRanges(
            fileReader = fileReader,
            ranges = ranges.filterNot { it.isEmpty() },
            maxByteLength = maxByteLength - copiedBytes,
        )
        result.append(rowText.text)
        copiedBytes += rowText.byteLength
        copiedBytes < maxByteLength
    }

    return SelectionText(result.toString(), copiedBytes)
}

private fun readSelectedTextRanges(
    fileReader: GiantFileReader,
    ranges: List<LongRange>,
    maxByteLength: Long,
): SelectionText {
    val result = StringBuilder()
    var copiedBytes = 0L

    ranges.forEachIndexed { index, range ->
        if (index > 0) {
            if (copiedBytes + fileReader.lineFeedByteLength > maxByteLength) {
                return SelectionText(result.toString(), copiedBytes)
            }
            result.append('\n')
            copiedBytes += fileReader.lineFeedByteLength
        }

        val availableBytes = maxByteLength - copiedBytes
        val requestedLength = range.forwardLength()
            .coerceAtMost(availableBytes)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
        if (requestedLength <= 0) {
            return@forEachIndexed
        }

        val window = fileReader.readTextUncached(range.start, requestedLength)
        val copiedLength = window.byteRange.forwardLength()
            .coerceAtMost(range.forwardLength())
        val (text, byteLength) = trimTextToByteLength(
            text = window.text,
            byteLength = copiedLength,
            maxByteLength = availableBytes.coerceAtMost(range.forwardLength()),
            encodedLength = fileReader::encodedLength,
        )
        result.append(text)
        copiedBytes += byteLength
    }

    return SelectionText(result.toString(), copiedBytes)
}

private fun rowIndexAtY(y: Float, rowHeight: Float, lastRowIndex: Int): Int {
    if (y <= 0f) {
        return 0
    }
    return floor(y / rowHeight).toInt().coerceIn(0, lastRowIndex)
}

internal fun rowAtPointInViewport(filePager: GiantFileTextPager, point: Offset): ViewportRow {
    val rows = filePager.viewportRows
    if (rows.isEmpty()) {
        return ViewportRow(
            text = "",
            visibleStartBytePosition = filePager.viewportStartBytePosition,
            rowStartBytePosition = filePager.viewportStartBytePosition,
            physicalLineStartBytePosition = filePager.viewportStartBytePosition,
        )
    }

    val rowHeight = filePager.rowHeight()
    if (!isFinite(rowHeight) || rowHeight <= 0f) {
        return rows.first()
    }

    val rowIndex = rowIndexAtY(point.y, rowHeight, rows.lastIndex)
    return rows[rowIndex]
}

internal fun bytePositionAtXInRow(
    row: ViewportRow,
    x: Float,
    textLayouter: BidirectionalTextLayouter,
    encodedLength: (CharSequence) -> Long,
): Long {
    if (x <= 0f) {
        return row.visibleStartBytePosition
    }

    var accumulatedPx = 0f
    var accumulatedBytes = 0L
    var matchedBytePosition: Long? = null
    GraphemeClusters.forEach(row.text) { start, end ->
        if (matchedBytePosition != null) {
            return@forEach
        }
        val fullChar = row.text.subSequence(start, end)
        val charWidth = textLayouter.measureCharWidth(fullChar)
        if (x in accumulatedPx..<accumulatedPx + charWidth) {
            matchedBytePosition = row.visibleStartBytePosition + accumulatedBytes
            return@forEach
        }
        accumulatedBytes += encodedLength(fullChar)
        accumulatedPx += charWidth
    }
    return matchedBytePosition ?: (row.visibleStartBytePosition + accumulatedBytes)
}
