package com.sunnychung.application.multiplatform.giantlogviewer.ux

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.onDrag
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.isAltGraphPressed
import androidx.compose.ui.input.pointer.isAltPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.dialogs.openFileSaver
import com.sunnychung.application.multiplatform.giantlogviewer.extension.floorMod
import com.sunnychung.application.multiplatform.giantlogviewer.extension.forwardLength
import com.sunnychung.application.multiplatform.giantlogviewer.io.BYTES_PER_MIB
import com.sunnychung.application.multiplatform.giantlogviewer.io.ComposeGiantFileTextPager
import com.sunnychung.application.multiplatform.giantlogviewer.io.copyFileByteRange
import com.sunnychung.application.multiplatform.giantlogviewer.io.GiantFileReader
import com.sunnychung.application.multiplatform.giantlogviewer.io.FileUnavailableException
import com.sunnychung.application.multiplatform.giantlogviewer.io.GiantFileTextPager
import com.sunnychung.application.multiplatform.giantlogviewer.io.ResolvedTextEncoding
import com.sunnychung.application.multiplatform.giantlogviewer.io.TextEncoding
import com.sunnychung.application.multiplatform.giantlogviewer.io.Viewport
import com.sunnychung.application.multiplatform.giantlogviewer.io.displayName
import com.sunnychung.application.multiplatform.giantlogviewer.io.selectableTextEncodings
import com.sunnychung.application.multiplatform.giantlogviewer.layout.MonospaceBidirectionalTextLayouter
import com.sunnychung.application.multiplatform.giantlogviewer.model.SearchMode
import com.sunnychung.application.multiplatform.giantlogviewer.util.GraphemeClusters
import com.sunnychung.application.multiplatform.giantlogviewer.util.formatByteSize
import com.sunnychung.application.multiplatform.giantlogviewer.ux.local.AppFont
import com.sunnychung.application.multiplatform.giantlogviewer.ux.local.LocalColor
import com.sunnychung.application.multiplatform.giantlogviewer.ux.local.LocalFont
import com.sunnychung.application.multiplatform.giantlogviewer.viewstate.FileViewState
import com.sunnychung.lib.multiplatform.bigtext.annotation.TemporaryBigTextApi
import com.sunnychung.lib.multiplatform.bigtext.compose.ComposeUnicodeCharMeasurer
import com.sunnychung.lib.multiplatform.bigtext.extension.isCtrlOrCmdPressed
import com.sunnychung.lib.multiplatform.bigtext.ux.ContextMenuItemEntry
import com.sunnychung.lib.multiplatform.bigtext.util.debouncedStateOf
import com.sunnychung.lib.multiplatform.bigtext.util.string
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import com.sunnychung.lib.multiplatform.kdatetime.extension.milliseconds
import com.sunnychung.lib.multiplatform.kdatetime.extension.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor

private const val SELECTION_AUTOSCROLL_INTERVAL_MILLIS = 50L
private const val SELECTION_AUTOSCROLL_MAX_ROWS_PER_TICK = 8L
private const val TEXT_COPY_LIMIT_BYTES = 5 * BYTES_PER_MIB
private const val TOAST_DURATION_MILLIS = 3_000L
private const val TOAST_FADE_OUT_MILLIS = 580L

private fun PointerEvent.isColumnSelectionModifierPressed(): Boolean {
    return keyboardModifiers.isAltPressed || keyboardModifiers.isAltGraphPressed
}

// TODO onPagerReady is an anti-pattern -- reverse of data flow. refactor it.
@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class, TemporaryBigTextApi::class)
@Composable
fun GiantTextViewer(
    modifier: Modifier,
    fileViewState: FileViewState,
    isSoftWrapEnabled: Boolean,
    filePath: String,
    refreshKey: Int = 0,
    highlightByteRange: LongRange,
    onPagerReady: (GiantFileTextPager?) -> Unit,
    onNavigate: (bytePosition: Long) -> Unit,
    onDocumentContentChanged: () -> Unit,
    onFileUnavailable: (String) -> Unit = {},
    onSearchRequest: (SearchMode) -> Unit,
    dismissSelectionMenuKey: Int = 0,
    bottomContent: @Composable () -> Unit = {},
    shouldRequestFocus: Boolean = true,
    isKeyboardShortcutEnabled: Boolean = true,
) {
    GiantTextViewerContent(
        modifier = modifier,
        fileViewState = fileViewState,
        isSoftWrapEnabled = isSoftWrapEnabled,
        filePath = filePath,
        refreshKey = refreshKey,
        highlightByteRange = highlightByteRange,
        onPagerReady = onPagerReady,
        onNavigate = onNavigate,
        onDocumentContentChanged = onDocumentContentChanged,
        onFileUnavailable = onFileUnavailable,
        onSearchRequest = onSearchRequest,
        dismissSelectionMenuKey = dismissSelectionMenuKey,
        bottomContent = bottomContent,
        shouldRequestFocus = shouldRequestFocus,
        isKeyboardShortcutEnabled = isKeyboardShortcutEnabled,
    )
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class, TemporaryBigTextApi::class)
@Composable
private fun GiantTextViewerContent(
    modifier: Modifier,
    fileViewState: FileViewState,
    isSoftWrapEnabled: Boolean,
    filePath: String,
    refreshKey: Int = 0,
    highlightByteRange: LongRange,
    onPagerReady: (GiantFileTextPager?) -> Unit,
    onNavigate: (bytePosition: Long) -> Unit,
    onDocumentContentChanged: () -> Unit,
    onFileUnavailable: (String) -> Unit = {},
    onSearchRequest: (SearchMode) -> Unit,
    dismissSelectionMenuKey: Int = 0,
    bottomContent: @Composable () -> Unit = {},
    shouldRequestFocus: Boolean = true,
    isKeyboardShortcutEnabled: Boolean = true,
) {
    val file = File(filePath)
    val unavailableMessage = when {
        !file.isFile -> "The selected file was moved or deleted."
        !file.canRead() -> "The selected file is no longer readable."
        else -> null
    }
    if (unavailableMessage != null) {
        LaunchedEffect(filePath, unavailableMessage) {
            onPagerReady(null)
            onFileUnavailable(unavailableMessage)
        }
        return
    }

    println("recompose $filePath $refreshKey")

    var contentComponentWidth by remember { mutableIntStateOf(0) }
    var contentComponentHeight by remember { mutableIntStateOf(0) }

    val density = LocalDensity.current
    val font = LocalFont.current
    val colors = LocalColor.current

    val isNavigationLocked = fileViewState.isFollowing

    val textMeasurer = rememberTextMeasurer(0)
    val textStyle = remember(font, colors) {
        TextStyle(
            fontFamily = font.monospaceFontFamily,
            color = colors.fileBodyTheme.plainText,
        )
    }
    val charMeasurer = remember(density, textStyle) { ComposeUnicodeCharMeasurer(textMeasurer, textStyle) }
    val textLayouter = remember(charMeasurer) { MonospaceBidirectionalTextLayouter(charMeasurer) }

    var selectedTextEncoding by remember(filePath) { mutableStateOf(TextEncoding.Auto) }
    var encodingReloadKey by remember(filePath, refreshKey) { mutableIntStateOf(0) }
    var lastModifiedMillis by remember(filePath, refreshKey, encodingReloadKey) {
        mutableLongStateOf(file.lastModified())
    }

    fun reloadFileForEncoding(encoding: TextEncoding) {
        selectedTextEncoding = encoding
        fileViewState.isFollowing = false
        fileViewState.fileLength = file.length()
        lastModifiedMillis = file.lastModified()
        encodingReloadKey++
        onDocumentContentChanged()
        onNavigate(0L)
    }

    val fileReader = remember(filePath, refreshKey, selectedTextEncoding, encodingReloadKey) {
        GiantFileReader(filePath, initialFileLength = fileViewState.fileLength, textEncoding = selectedTextEncoding)
    }
    val filePager: GiantFileTextPager = remember(fileReader, textLayouter) {
        ComposeGiantFileTextPager(fileReader, textLayouter, fileViewState.fileLength)
    }
    val fileLength = fileViewState.fileLength
    fileReader.fileLength = fileLength
    filePager.fileLength = fileLength

    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    var draggedPoint by remember { mutableStateOf<Offset>(Offset.Zero) }
    var isSelectionDragInProgress by remember { mutableStateOf(false) }
    var isRangeExtensionGesture by remember { mutableStateOf(false) }
    var isColumnSelectionGesture by remember { mutableStateOf(false) }
    var pendingColumnSelectionGesture by remember(filePath, refreshKey, encodingReloadKey) { mutableStateOf(false) }
    var dragStartBytePosition by remember(filePath, refreshKey, encodingReloadKey) { mutableLongStateOf(0L) }
    var dragEndBytePosition by remember(filePath, refreshKey, encodingReloadKey) { mutableLongStateOf(0L) }
    var columnSelectionAnchor by remember(filePath, refreshKey, encodingReloadKey) {
        mutableStateOf<ColumnSelectionPoint?>(null)
    }
    var columnSelection by remember(filePath, refreshKey, encodingReloadKey) { mutableStateOf<TextSelection>(TextSelection.Empty) }
    var isColumnSelectionActive by remember(filePath, refreshKey, encodingReloadKey) { mutableStateOf(false) }
    var isSelectionMenuVisible by remember(filePath, refreshKey, encodingReloadKey) { mutableStateOf(false) }
    var selectionMenuPosition by remember(filePath, refreshKey, encodingReloadKey) { mutableStateOf(Offset.Zero) }
    var pendingSelectionMenuPosition by remember(filePath, refreshKey, encodingReloadKey) { mutableStateOf<Offset?>(null) }
    var selectedSelectionMenuItemIndex by remember(filePath, refreshKey, encodingReloadKey) { mutableIntStateOf(0) }
    var toastMessage by remember(filePath, refreshKey, encodingReloadKey) { mutableStateOf<String?>(null) }
    var displayedToastMessage by remember(filePath, refreshKey, encodingReloadKey) { mutableStateOf<String?>(null) }
    var isToastVisible by remember(filePath, refreshKey, encodingReloadKey) { mutableStateOf(false) }
    var copySelectionJob by remember(filePath, refreshKey, encodingReloadKey) { mutableStateOf<Job?>(null) }
    var keyboardShortcutFocusRequest by remember(filePath, refreshKey, encodingReloadKey) { mutableIntStateOf(0) }
    val toastAlpha by animateFloatAsState(
        targetValue = if (isToastVisible) 1f else 0f,
        animationSpec = tween(durationMillis = (if (isToastVisible) 200L else TOAST_FADE_OUT_MILLIS).toInt()),
    )

    val (contentWidth, isContentWidthLatest) = debouncedStateOf(200.milliseconds(), tolerateCount = 1, filePager) {
        contentComponentWidth
    }

    if (isContentWidthLatest) {
        remember(filePager, contentWidth, contentComponentHeight, density) {
            filePager.viewport = Viewport(contentWidth, contentComponentHeight, density.density)
        }
    }

    LaunchedEffect(filePager, isSoftWrapEnabled) {
        if (isColumnSelectionActive) {
            copySelectionJob?.cancel()
            copySelectionJob = null
            columnSelectionAnchor = null
            columnSelection = TextSelection.Empty
            isColumnSelectionActive = false
            isColumnSelectionGesture = false
            pendingColumnSelectionGesture = false
            isSelectionMenuVisible = false
            pendingSelectionMenuPosition = null
            dragStartBytePosition = 0L
            dragEndBytePosition = 0L
        }
        filePager.updateSoftWrapEnabled(isSoftWrapEnabled)
        onNavigate(filePager.viewportStartBytePosition)
    }

    val contiguousSelection = TextSelection.Contiguous(
        minOf(dragStartBytePosition, dragEndBytePosition)..<
            maxOf(dragStartBytePosition, dragEndBytePosition)
    )
    val selection: TextSelection = if (isColumnSelectionActive) {
        columnSelection
    } else {
        contiguousSelection
    }

    fun currentSelection(): TextSelection {
        return if (isColumnSelectionActive) {
            columnSelection
        } else {
            TextSelection.Contiguous(
                minOf(dragStartBytePosition, dragEndBytePosition)..<
                    maxOf(dragStartBytePosition, dragEndBytePosition)
            )
        }
    }

    var scrollY by remember(filePager.viewportStartBytePosition) {
        mutableStateOf(0f)
    }
    val scrollState = rememberScrollableState { delta ->
        if (isNavigationLocked) {
            return@rememberScrollableState 0f
        }

        val reversedDelta = -delta
        if (reversedDelta < 0 && filePager.viewportStartBytePosition <= 0) {
            return@rememberScrollableState 0f
        }
        if (reversedDelta > 0 && filePager.viewportStartBytePosition >= fileLength) {
            return@rememberScrollableState 0f
        }
        scrollY += reversedDelta
        val rowHeight = charMeasurer.getRowHeight()
        if (scrollY >= rowHeight) {
            val numOfRowsToScroll = (scrollY / rowHeight).toLong()
            scrollY -= rowHeight * numOfRowsToScroll.toFloat()
            filePager.moveToNextRow(numOfRowsToScroll)
            onNavigate(filePager.viewportStartBytePosition)
        } else if (scrollY <= -rowHeight) {
            val numOfRowsToScroll = (abs(scrollY) / rowHeight).toLong()
            scrollY += rowHeight * numOfRowsToScroll.toFloat()
            filePager.moveToPrevRow(numOfRowsToScroll)
            onNavigate(filePager.viewportStartBytePosition)
        }
        delta
    }

    LaunchedEffect(filePager) {
        onPagerReady(filePager)
    }

    DisposableEffect(filePath, refreshKey, encodingReloadKey) {
        onDispose {
            copySelectionJob?.cancel()
        }
    }

    fun cancelCopySelection() {
        val job = copySelectionJob
        if (job?.isActive == true) {
            job.cancel()
            copySelectionJob = null
            toastMessage = "Copy cancelled"
        }
    }

    fun copySelection() {
        val currentSelection = currentSelection()
        if (currentSelection.isEmpty()) {
            return
        }

        copySelectionJob?.cancel()
        val knownSelectedLength = (currentSelection as? TextSelection.Contiguous)?.range?.let {
            it.forwardLength()
        }
        toastMessage = "Copying selection..."
        val copyJob = coroutineScope.launch(start = CoroutineStart.LAZY) {
            try {
                val copiedSelection = withContext(Dispatchers.IO) {
                    val copyContext = currentCoroutineContext()
                    readSelectedText(
                        fileReader = fileReader,
                        filePager = filePager,
                        selection = currentSelection,
                        maxByteLength = TEXT_COPY_LIMIT_BYTES.toLong(),
                        shouldContinue = {
                            try {
                                copyContext.ensureActive()
                                true
                            } catch (_: CancellationException) {
                                false
                            }
                        },
                    ).also {
                        copyContext.ensureActive()
                    }
                }
                val copiedLength = copiedSelection.byteLength
                clipboardManager.setText(AnnotatedString(text = copiedSelection.text))
                val isTrimmed = when {
                    knownSelectedLength != null -> knownSelectedLength > copiedLength
                    else -> copiedLength >= TEXT_COPY_LIMIT_BYTES.toLong()
                }
                if (isTrimmed) {
                    toastMessage = if (currentSelection is TextSelection.Column) {
                        "Copied text was trimmed to ${NumberFormat.getIntegerInstance(Locale.US).format(copiedLength)} bytes."
                    } else {
                        "Copied text was trimmed to ${NumberFormat.getIntegerInstance(Locale.US).format(copiedLength)} bytes." +
                            "\nConsider copying to a file instead."
                    }
                } else {
                    toastMessage = "Copied ${NumberFormat.getIntegerInstance(Locale.US).format(copiedLength)} bytes."
                }
            } catch (_: CancellationException) {
                if (copySelectionJob == this.coroutineContext[Job]) {
                    toastMessage = "Copy cancelled"
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                if (copySelectionJob == this.coroutineContext[Job]) {
                    toastMessage = "Failed to copy selection"
                }
            } finally {
                if (copySelectionJob == this.coroutineContext[Job]) {
                    copySelectionJob = null
                }
            }
        }
        copySelectionJob = copyJob
        copyJob.start()
    }

    fun canCopySelectionAsText(): Boolean {
        val currentSelection = currentSelection()
        return when (currentSelection) {
            TextSelection.Empty -> false
            is TextSelection.Contiguous -> !currentSelection.range.isEmpty() &&
                currentSelection.range.forwardLength() <= TEXT_COPY_LIMIT_BYTES.toLong()
            is TextSelection.Column -> false
        }
    }

    fun selectionSizeText(): String {
        return when (val currentSelection = selection) {
            TextSelection.Empty -> formatByteSize(0L)
            is TextSelection.Contiguous -> formatByteSize(currentSelection.range.forwardLength())
            is TextSelection.Column -> "Column selection"
        }
    }

    fun copySelectionToFile(destination: File) {
        when (val currentSelection = currentSelection()) {
            TextSelection.Empty -> Unit
            is TextSelection.Contiguous -> copyFileByteRange(
                source = file,
                destination = destination,
                byteRange = currentSelection.range,
            )
            is TextSelection.Column -> {
                val selectedText = readSelectedText(
                    fileReader = fileReader,
                    filePager = filePager,
                    selection = currentSelection,
                    maxByteLength = if (currentSelection is TextSelection.Column) {
                        TEXT_COPY_LIMIT_BYTES.toLong()
                    } else {
                        Long.MAX_VALUE
                    },
                )
                destination.writeText(selectedText.text, fileReader.resolvedTextEncoding.charset)
            }
        }
    }

    suspend fun chooseSelectionDestination(): File? {
        val selectedFile: PlatformFile = FileKit.openFileSaver(
            suggestedName = "${file.name}.selection",
            extension = "txt",
        ) ?: return null
        return File(selectedFile.absolutePath())
    }

    fun requestKeyboardShortcutFocusRestore() {
        keyboardShortcutFocusRequest++
    }

    fun copySelectionToFileWithPrompt(onComplete: () -> Unit = {}) {
        coroutineScope.launch {
            try {
                val destination = chooseSelectionDestination() ?: return@launch
                withContext(Dispatchers.IO) {
                    copySelectionToFile(destination)
                }
                toastMessage = "Selection copied to ${destination.name}"
            } catch (e: Throwable) {
                e.printStackTrace()
                toastMessage = "Failed to copy selection to a file"
            } finally {
                onComplete()
            }
        }
    }

    fun navigate(action: GiantFileTextPager.() -> Unit): Boolean {
        if (isNavigationLocked) return false
        filePager.action()
        onNavigate(filePager.viewportStartBytePosition)
        return true
    }

    /**
     * Moves the viewport by visual rows and reports whether the start byte position actually changed.
     * Positive deltas scroll downward; negative deltas scroll upward.
     */
    fun moveViewportByRows(rowDelta: Long): Boolean {
        if (isNavigationLocked || rowDelta == 0L) return false

        val previousPosition = filePager.viewportStartBytePosition
        if (rowDelta > 0L) {
            if (previousPosition >= fileLength) return false
            filePager.moveToNextRow(rowDelta)
        } else {
            if (previousPosition <= 0L) return false
            filePager.moveToPrevRow(-rowDelta)
        }

        val didMove = filePager.viewportStartBytePosition != previousPosition
        if (didMove) {
            onNavigate(filePager.viewportStartBytePosition)
        }
        return didMove
    }

    fun clearSelection() {
        dragStartBytePosition = 0L
        dragEndBytePosition = 0L
        columnSelectionAnchor = null
        columnSelection = TextSelection.Empty
        isColumnSelectionActive = false
        isColumnSelectionGesture = false
        pendingColumnSelectionGesture = false
        isSelectionMenuVisible = false
        pendingSelectionMenuPosition = null
    }

    fun restartSelectionAt(point: Offset) {
        val bytePosition = findBytePositionByCoordinatePx(filePager, point)
        dragStartBytePosition = bytePosition
        dragEndBytePosition = bytePosition
        columnSelectionAnchor = null
        columnSelection = TextSelection.Empty
        isColumnSelectionActive = false
        selectionMenuPosition = point
    }

    fun extendSelectionTo(point: Offset) {
        dragEndBytePosition = findBytePositionByCoordinatePx(filePager, point)
        selectionMenuPosition = point
    }

    fun columnSelectionPointAt(point: Offset): ColumnSelectionPoint {
        val row = rowAtPointInViewport(filePager, point)
        val bytePosition = bytePositionAtXInRow(
            row = row,
            x = point.x,
            textLayouter = filePager.textLayouter,
            encodedLength = filePager::encodedLengthOfText,
        )
        return ColumnSelectionPoint(
            bytePosition = bytePosition,
            rowStartBytePosition = row.rowStartBytePosition,
            x = point.x,
        )
    }

    fun updateColumnSelectionTo(point: Offset): TextSelection.Column {
        val focus = columnSelectionPointAt(point)
        val anchor = columnSelectionAnchor ?: focus.also {
            columnSelectionAnchor = it
        }
        val newSelection = buildColumnSelection(filePager, anchor, focus)
        columnSelection = newSelection
        isColumnSelectionActive = true
        selectionMenuPosition = point
        return newSelection
    }

    /**
     * Starts a mouse-driven selection drag. When extending an existing selection, the original
     * anchor remains unchanged and only the active endpoint moves.
     */
    fun beginSelectionGesture(point: Offset, isRangeExtension: Boolean, isColumnSelection: Boolean) {
        draggedPoint = point
        isSelectionDragInProgress = true
        isSelectionMenuVisible = false
        pendingSelectionMenuPosition = null
        isColumnSelectionGesture = isColumnSelection || pendingColumnSelectionGesture
        if (isColumnSelectionGesture) {
            val canExtendColumnSelection = isRangeExtension && isColumnSelectionActive && !currentSelection().isEmpty() &&
                columnSelectionAnchor != null
            isRangeExtensionGesture = canExtendColumnSelection
            if (!canExtendColumnSelection) {
                columnSelectionAnchor = columnSelectionPointAt(point)
            }
            updateColumnSelectionTo(point)
        } else {
            val canExtendContiguousSelection = isRangeExtension && !isColumnSelectionActive &&
                !contiguousSelection.isEmpty()
            isRangeExtensionGesture = canExtendContiguousSelection
            if (isRangeExtensionGesture) {
                extendSelectionTo(point)
            } else {
                restartSelectionAt(point)
            }
        }
    }

    fun extendSelectionFromPress(point: Offset) {
        draggedPoint = point
        isRangeExtensionGesture = true
        isColumnSelectionGesture = false
        extendSelectionTo(point)
        pendingSelectionMenuPosition = point
    }

    fun extendColumnSelectionFromPress(point: Offset, isRangeExtension: Boolean) {
        draggedPoint = point
        isColumnSelectionGesture = true
        pendingColumnSelectionGesture = true
        isSelectionMenuVisible = false
        pendingSelectionMenuPosition = null
        val canExtendColumnSelection = isRangeExtension && isColumnSelectionActive && !currentSelection().isEmpty() &&
            columnSelectionAnchor != null
        isRangeExtensionGesture = canExtendColumnSelection
        if (!canExtendColumnSelection) {
            columnSelectionAnchor = columnSelectionPointAt(point)
        }
        val newSelection = updateColumnSelectionTo(point)
        if (!newSelection.isEmpty()) {
            pendingSelectionMenuPosition = point
        }
    }

    fun stopSelectionGesture() {
        draggedPoint = Offset.Zero
        isSelectionDragInProgress = false
        isRangeExtensionGesture = false
        isColumnSelectionGesture = false
        pendingColumnSelectionGesture = false
        isSelectionMenuVisible = !currentSelection().isEmpty()
        selectedSelectionMenuItemIndex = 0
    }

    /**
     * Returns how many visual rows the selection autoscroll should move on the next tick.
     * Negative values scroll upward, positive values scroll downward, and zero means no autoscroll.
     */
    fun selectionAutoScrollRows(point: Offset): Long {
        val rowHeight = charMeasurer.getRowHeight()
        if (contentComponentHeight <= 0 || !java.lang.Float.isFinite(rowHeight) || rowHeight <= 0f) {
            return 0L
        }

        val distanceOutside = when {
            point.y < 0f -> point.y
            point.y > contentComponentHeight.toFloat() -> point.y - contentComponentHeight.toFloat()
            else -> return 0L
        }
        val rows = (abs(distanceOutside) / rowHeight).toLong() + 1L
        return rows
            .coerceAtMost(SELECTION_AUTOSCROLL_MAX_ROWS_PER_TICK)
            .let { if (distanceOutside < 0f) -it else it }
    }

    LaunchedEffect(isSelectionDragInProgress, filePager, contentComponentHeight, isNavigationLocked) {
        while (isSelectionDragInProgress) {
            val rowDelta = selectionAutoScrollRows(draggedPoint)
            if (rowDelta != 0L && moveViewportByRows(rowDelta)) {
                if (isColumnSelectionGesture) {
                    updateColumnSelectionTo(draggedPoint)
                } else {
                    extendSelectionTo(draggedPoint)
                }
            }
            delay(SELECTION_AUTOSCROLL_INTERVAL_MILLIS)
        }
    }

    LaunchedEffect(toastMessage) {
        val message = toastMessage ?: return@LaunchedEffect
        displayedToastMessage = message
        isToastVisible = true
        if (message == "Copying selection...") {
            return@LaunchedEffect
        }
        delay(TOAST_DURATION_MILLIS)
        isToastVisible = false
        delay(TOAST_FADE_OUT_MILLIS.toLong())
        if (displayedToastMessage == message) {
            displayedToastMessage = null
            toastMessage = null
        }
    }

    fun selectionMenuItems(): List<SelectionMenuItem> {
        return buildList {
            add(SelectionMenuItem(buildAnnotatedString {
                append("Copy selection as ")
                if (!canCopySelectionAsText()) {
                    withStyle(SpanStyle(color = colors.warningText, fontStyle = FontStyle.Italic)) {
                        append("trimmed")
                    }
                    append(" ")
                }
                append("text")
            }, ::copySelection))
            add(SelectionMenuItem(AnnotatedString("Copy selection to a file")) {
                copySelectionToFileWithPrompt(::requestKeyboardShortcutFocusRestore)
            })
        }
    }

    fun dismissSelectionMenu() {
        isSelectionMenuVisible = false
        pendingSelectionMenuPosition = null
        selectedSelectionMenuItemIndex = 0
    }

    fun showSelectionMenuAt(point: Offset) {
        selectionMenuPosition = point
        isSelectionMenuVisible = !currentSelection().isEmpty()
        selectedSelectionMenuItemIndex = 0
    }

    fun isPointInSelectionMenu(point: Offset): Boolean {
        if (!isSelectionMenuVisible) {
            return false
        }
        val menuWidthPx = with(density) { 240.dp.toPx() }
        val menuHeightPx = with(density) { selectionMenuHeightDp(selectionMenuItems().size).toPx() }
        val topLeft = selectionMenuTopLeft(
            anchor = selectionMenuPosition,
            menuWidthPx = menuWidthPx,
            menuHeightPx = menuHeightPx,
            lineHeight = charMeasurer.getRowHeight(),
            contentWidth = contentComponentWidth,
            contentHeight = contentComponentHeight,
        )
        return point.x in topLeft.x..topLeft.x + menuWidthPx &&
            point.y in topLeft.y..topLeft.y + menuHeightPx
    }

    fun viewportRowAt(point: Offset): com.sunnychung.application.multiplatform.giantlogviewer.io.ViewportRow? {
        val rows = filePager.viewportRows
        if (rows.isEmpty()) {
            return null
        }
        val rowHeight = filePager.rowHeight()
        if (!java.lang.Float.isFinite(rowHeight) || rowHeight <= 0f) {
            return null
        }
        val rowIndex = when {
            point.y <= 0f -> 0
            else -> floor(point.y / rowHeight).toInt().coerceIn(0, rows.lastIndex)
        }
        return rows[rowIndex]
    }

    fun showPendingSelectionMenu() {
        pendingSelectionMenuPosition?.let {
            showSelectionMenuAt(it)
            pendingSelectionMenuPosition = null
        }
    }

    LaunchedEffect(dismissSelectionMenuKey) {
        dismissSelectionMenu()
    }

    LaunchedEffect(keyboardShortcutFocusRequest) {
        if (keyboardShortcutFocusRequest > 0) {
            delay(1L)
            focusRequester.requestFocus()
            delay(50L)
            focusRequester.requestFocus()
        }
    }

    Column(modifier
//        .onKeyEvent { e ->
        .onPreviewKeyEvent { e ->
            if (!isKeyboardShortcutEnabled) {
                return@onPreviewKeyEvent false
            }
            println("onKeyEvent ${e.key}")
            val startTime = KInstant.now()
            if (e.type == KeyEventType.KeyDown) {
                val isCtrlCWithoutCommand = e.key == Key.C && e.isCtrlPressed && !e.isMetaPressed
                if ((e.key == Key.Escape || isCtrlCWithoutCommand) && copySelectionJob?.isActive == true) {
                    cancelCopySelection()
                    return@onPreviewKeyEvent true
                }

                if (e.key == Key.C && e.isCtrlOrCmdPressed()) {
                    if (isSelectionMenuVisible) {
                        dismissSelectionMenu()
                    }
                    copySelection()
                    return@onPreviewKeyEvent true
                }

                if (isSelectionMenuVisible) {
                    val menuItems = selectionMenuItems()
                    when (e.key) {
                        Key.Escape -> {
                            dismissSelectionMenu()
                            return@onPreviewKeyEvent true
                        }

                        Key.DirectionUp -> {
                            selectedSelectionMenuItemIndex = (selectedSelectionMenuItemIndex - 1)
                                .floorMod(menuItems.size)
                            return@onPreviewKeyEvent true
                        }

                        Key.DirectionDown -> {
                            selectedSelectionMenuItemIndex = (selectedSelectionMenuItemIndex + 1)
                                .floorMod(menuItems.size)
                            return@onPreviewKeyEvent true
                        }

                        Key.Enter -> {
                            menuItems.getOrNull(selectedSelectionMenuItemIndex)?.action?.invoke()
                            dismissSelectionMenu()
                            requestKeyboardShortcutFocusRestore()
                            return@onPreviewKeyEvent true
                        }
                    }
                }
                when {
                    e.key == Key.F && e.isCtrlOrCmdPressed() -> onSearchRequest(SearchMode.Forward)

                    e.key == Key.F && e.isShiftPressed -> {
                        fileViewState.isFollowing = true
                        return@onPreviewKeyEvent true
                    }

                    e.key == Key.F -> return@onPreviewKeyEvent navigate { moveToNextPage() }
                    e.key == Key.B -> return@onPreviewKeyEvent navigate { moveToPrevPage() }
                    e.key == Key.DirectionUp && e.isAltPressed -> return@onPreviewKeyEvent navigate { moveToPrevPage() }
                    e.key == Key.DirectionDown && e.isAltPressed -> return@onPreviewKeyEvent navigate { moveToNextPage() }

                    e.key == Key.G && e.isShiftPressed -> return@onPreviewKeyEvent navigate { moveToTheLastRow() }
                    e.key == Key.DirectionDown && e.isCtrlOrCmdPressed() -> return@onPreviewKeyEvent navigate { moveToTheLastRow() }
                    e.key == Key.G -> return@onPreviewKeyEvent navigate { moveToTheFirstRow() }
                    e.key == Key.DirectionUp && e.isCtrlOrCmdPressed() -> return@onPreviewKeyEvent navigate { moveToTheFirstRow() }

                    e.key == Key.DirectionUp -> return@onPreviewKeyEvent navigate { moveToPrevRow() }
                    e.key == Key.DirectionDown -> return@onPreviewKeyEvent navigate { moveToNextRow() }
                    e.key == Key.DirectionLeft -> {
                        if (isNavigationLocked) return@onPreviewKeyEvent false
                        val scrollPx = if (e.isShiftPressed) {
                            textLayouter.measureCharWidth("I")
                        } else {
                            contentComponentWidth.toFloat().takeIf { it > 0f }
                                ?: textLayouter.measureCharWidth("I")
                        }
                        filePager.scrollHorizontallyByPx(-scrollPx)
                        return@onPreviewKeyEvent true
                    }
                    e.key == Key.DirectionRight -> {
                        if (isNavigationLocked) return@onPreviewKeyEvent false
                        val scrollPx = if (e.isShiftPressed) {
                            textLayouter.measureCharWidth("I")
                        } else {
                            contentComponentWidth.toFloat().takeIf { it > 0f }
                                ?: textLayouter.measureCharWidth("I")
                        }
                        filePager.scrollHorizontallyByPx(scrollPx)
                        return@onPreviewKeyEvent true
                    }

                    e.key == Key.Slash && e.isShiftPressed -> onSearchRequest(SearchMode.Backward)
                    e.key == Key.Slash -> onSearchRequest(SearchMode.Forward)

                    e.key == Key.Escape -> {
                        if (fileViewState.isFollowing) {
                            fileViewState.isFollowing = false
                            return@onPreviewKeyEvent true
                        }
                        onSearchRequest(SearchMode.None)
                    }

                    e.key == Key.C && e.isCtrlPressed -> {
                        if (fileViewState.isFollowing) {
                            fileViewState.isFollowing = false
                            // continue to remaining actions
                        }
                    }

                    else -> {
//                    return@onKeyEvent false
                        return@onPreviewKeyEvent false
                    }
                }
                println("onKeyEvent handled in ${KInstant.now() - startTime}")
                return@onPreviewKeyEvent true
            }
            false
        }
        .focusRequester(focusRequester)
        .focusable()
    ) {
        Row(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .onPointerEvent(eventType = PointerEventType.Press) {
                    // not sure which Compose bug leading to require implementing "click to focus" manually
                    focusRequester.requestFocus()

                    val point = it.changes.firstOrNull()?.position
                    if (point != null && point.x > contentComponentWidth) {
                        dismissSelectionMenu()
                        return@onPointerEvent
                    }

                    if (
                        point != null &&
                        point.x <= contentComponentWidth &&
                        it.isColumnSelectionModifierPressed() &&
                        !it.buttons.isSecondaryPressed
                    ) {
                        extendColumnSelectionFromPress(
                            point = point,
                            isRangeExtension = it.keyboardModifiers.isShiftPressed,
                        )
                        return@onPointerEvent
                    }

                    if (isSelectionMenuVisible) {
                        if (point != null) {
                            val isRightClickOnSelection = it.buttons.isSecondaryPressed &&
                                point.x <= contentComponentWidth &&
                                viewportRowAt(point)?.let { row ->
                                    findBytePositionByCoordinatePx(filePager, point) in selection.rangeInRow(row, filePager)
                                } == true
                            val isShiftClickOnTextCanvas = it.keyboardModifiers.isShiftPressed &&
                                point.x <= contentComponentWidth
                            val isColumnClickOnTextCanvas = it.isColumnSelectionModifierPressed() &&
                                point.x <= contentComponentWidth
                            if (isPointInSelectionMenu(point) || isRightClickOnSelection) {
                                return@onPointerEvent
                            }
                            if (isShiftClickOnTextCanvas || isColumnClickOnTextCanvas) {
                                return@onPointerEvent
                            }
                        }
                        clearSelection()
                        return@onPointerEvent
                    }
                    if (it.buttons.isSecondaryPressed) {
                        return@onPointerEvent
                    }
                    if (!it.keyboardModifiers.isShiftPressed && !it.isColumnSelectionModifierPressed()) {
                        clearSelection()
                    }
                }
        ) {
            val startTime = KInstant.now()

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .onGloballyPositioned {
                            contentComponentWidth = it.size.width
                            contentComponentHeight = it.size.height
                        }
                        .onDrag(
                            onDragStart = {
                                beginSelectionGesture(
                                    point = it,
                                    isRangeExtension = isRangeExtensionGesture,
                                    isColumnSelection = isColumnSelectionGesture,
                                )
                            },
                            onDrag = {
                                draggedPoint += it
                                if (isColumnSelectionGesture) {
                                    updateColumnSelectionTo(draggedPoint)
                                } else {
                                    extendSelectionTo(draggedPoint)
                                }
                            },
                            onDragEnd = {
                                stopSelectionGesture()
                            }
                        )
                        .onPointerEvent(eventType = PointerEventType.Scroll) {
                            if (isNavigationLocked || isSoftWrapEnabled) {
                                return@onPointerEvent
                            }
                            val scrollDelta = it.changes.fold(Offset.Zero) { acc, change ->
                                acc + change.scrollDelta
                            }
                            val horizontalDelta = when {
                                scrollDelta.x != 0f -> scrollDelta.x
                                it.keyboardModifiers.isShiftPressed && scrollDelta.y != 0f -> scrollDelta.y
                                else -> 0f
                            }
                            if (horizontalDelta != 0f) {
                                filePager.scrollHorizontallyByPx(horizontalDelta)
                            }
                        }
                        .scrollable(scrollState, Orientation.Vertical)
                ) {
                    fun handleSelectionPress(point: Offset, isRangeExtension: Boolean) {
                        if (isRangeExtension && !isColumnSelectionActive && !contiguousSelection.isEmpty()) {
                            extendSelectionFromPress(point)
                        }
                    }

                    fun handleColumnSelectionPress(point: Offset, isRangeExtension: Boolean) {
                        extendColumnSelectionFromPress(point, isRangeExtension)
                    }

                    val textToDisplay: List<CharSequence> = filePager.textInViewport
                    val bytePositionsOfDisplay: List<Long> = filePager.startBytePositionsInViewport
//                  println("textToDisplay:\n$textToDisplay")
                    val lineHeight = charMeasurer.getRowHeight()
                    Canvas(
                        modifier = Modifier
                            .matchParentSize()
                            .onPointerEvent(eventType = PointerEventType.Press) {
                                val change = it.changes.firstOrNull() ?: return@onPointerEvent
                                if (it.buttons.isSecondaryPressed) {
                                    val bytePosition = findBytePositionByCoordinatePx(filePager, change.position)
                                    val selectionRange = viewportRowAt(change.position)?.let { row ->
                                        selection.rangeInRow(row, filePager)
                                    } ?: 0L..<0L
                                    if (bytePosition in selectionRange) {
                                        pendingSelectionMenuPosition = change.position
                                    } else if (isSelectionMenuVisible) {
                                        dismissSelectionMenu()
                                    }
                                    return@onPointerEvent
                                }
                                if (it.isColumnSelectionModifierPressed()) {
                                    handleColumnSelectionPress(
                                        point = change.position,
                                        isRangeExtension = it.keyboardModifiers.isShiftPressed,
                                    )
                                    return@onPointerEvent
                                }
                                handleSelectionPress(
                                    point = change.position,
                                    isRangeExtension = it.keyboardModifiers.isShiftPressed,
                                )
                            }
                            .onPointerEvent(eventType = PointerEventType.Release) {
                                showPendingSelectionMenu()
                            }
                    ) {
//                      with(density) {
                        textToDisplay.forEachIndexed { rowRelativeIndex, row ->
                            val viewportRow = filePager.viewportRows.getOrNull(rowRelativeIndex)
                            val selectedByteRangeInRow = viewportRow?.let {
                                selection.rangeInRow(it, filePager)
                            } ?: 0L..<0L
                            val rowYOffset = rowRelativeIndex * lineHeight
                            val globalXOffset = 0f
                            var accumulateXOffset = 0f
                            var bytePosition = bytePositionsOfDisplay[rowRelativeIndex]
                            GraphemeClusters.forEach(row) { start, end ->
                                val charAnnotated = row.subSequence(start, end)
                                val charText = charAnnotated.string()
                                val textLayoutResult = charMeasurer.getTextLayoutResult(charAnnotated, null)
                                val charWidth = textLayouter.measureCharWidth(charAnnotated)
                                val charYOffset = textLayouter.measureCharYOffset(charAnnotated)

                                if (bytePosition in highlightByteRange) {
                                    drawRect(
                                        color = colors.fileBodyTheme.searchMatchBackground,
                                        topLeft = Offset(globalXOffset + accumulateXOffset, rowYOffset + charYOffset),
                                        size = Size(charWidth, lineHeight),
                                    )
                                }

                                if (bytePosition in selectedByteRangeInRow) {
                                    drawRect(
                                        color = colors.fileBodyTheme.selectionBackground,
                                        topLeft = Offset(globalXOffset + accumulateXOffset, rowYOffset + charYOffset),
                                        size = Size(charWidth, lineHeight),
                                    )
                                }

//                        BasicText(
//                            charAnnotated.annotatedString(),
//                            style = textStyle,
//                            maxLines = 1,
//                            softWrap = false,
//                            modifier = Modifier
//                                .offset((globalXOffset + accumulateXOffset).toDp(), (rowYOffset + charYOffset).toDp())
//                        )

                                if (textLayoutResult != null) { // use cache to avoid object allocations and interop calls
                                    drawText(
                                        textLayoutResult = textLayoutResult,
                                        topLeft = Offset(globalXOffset + accumulateXOffset, rowYOffset + charYOffset),
                                    )
                                } else {
                                    drawText(
                                        textMeasurer = textMeasurer,
                                        text = charText,
                                        topLeft = Offset(globalXOffset + accumulateXOffset, rowYOffset + charYOffset),
                                        size = Size(charWidth, lineHeight),
                                        style = textStyle,
                                        overflow = TextOverflow.Visible,
                                        softWrap = false,
                                        maxLines = 1,
                                    )
                                }

                                accumulateXOffset += charWidth
                                bytePosition += filePager.encodedLengthOfText(charText)
                            }
                        }
//                      }
                    }

                    if (isSelectionMenuVisible && !selection.isEmpty()) {
                        SelectionActionMenu(
                            items = selectionMenuItems(),
                            selectionSizeText = selectionSizeText(),
                            selectedIndex = selectedSelectionMenuItemIndex,
                            onSelectIndex = { selectedSelectionMenuItemIndex = it },
                            onDismiss = ::dismissSelectionMenu,
                            onActionComplete = ::requestKeyboardShortcutFocusRestore,
                            modifier = Modifier
                                .offset {
                                    val menuWidthPx = with(density) { 240.dp.roundToPx() }
                                    val menuHeightPx = with(density) {
                                        selectionMenuHeightDp(selectionMenuItems().size).roundToPx()
                                    }
                                    selectionMenuTopLeft(
                                        anchor = selectionMenuPosition,
                                        menuWidthPx = menuWidthPx.toFloat(),
                                        menuHeightPx = menuHeightPx.toFloat(),
                                        lineHeight = lineHeight,
                                        contentWidth = contentComponentWidth,
                                        contentHeight = contentComponentHeight,
                                    ).let {
                                        IntOffset(it.x.toInt(), it.y.toInt())
                                    }
                                }
                                .width(240.dp)
                        )
                    }

                    displayedToastMessage?.let {
                        ToastMessage(
                            message = it,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp)
                                .graphicsLayer { alpha = toastAlpha }
                        )
                    }
                }

                if (!isSoftWrapEnabled) {
                    HorizontalIndicatorView(
                        value = filePager.horizontalScrollRatio(),
                        onSelectRatio = { filePager.scrollHorizontallyToRatio(it) },
                        onScrollByPx = { filePager.scrollHorizontallyByPx(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp)
                    )
                }
            }

            var dragY by remember { mutableStateOf(0f) }

            fun moveToPositionByDragY(dragY: Float) {
                val confinedDragY = dragY.coerceIn(0f .. contentComponentHeight.toFloat())
                val desiredPosition = (fileViewState.fileLength * (confinedDragY.toDouble() / contentComponentHeight.toDouble())).toLong()
                filePager.moveToRowOfBytePosition(desiredPosition)
            }

            VerticalIndicatorView(
                value = if (fileViewState.fileLength > 0) {
                    (filePager.viewportStartBytePosition.toDouble() / fileViewState.fileLength.toDouble()).toFloat()
                } else {
                    1f
                },
                modifier = Modifier.width(20.dp).fillMaxHeight()
                    .onPointerEvent(eventType = PointerEventType.Press) {
                        dragY = it.changes.first().position.y
                        moveToPositionByDragY(dragY)
                    }
                    .onDrag {
                        dragY += it.y
                        moveToPositionByDragY(dragY)
                    }
            )

            println("prepare rendering in ${KInstant.now() - startTime}")
        }

        bottomContent()

        GiantTextViewerStatusBar(
            filePager = filePager,
            fileLength = fileLength,
            lastModifiedMillis = lastModifiedMillis,
            selectedTextEncoding = selectedTextEncoding,
            resolvedTextEncoding = fileReader.resolvedTextEncoding,
            onSelectTextEncoding = {
                if (it != selectedTextEncoding || it == TextEncoding.Auto) {
                    reloadFileForEncoding(it)
                }
            },
            onMenuActionComplete = ::requestKeyboardShortcutFocusRestore,
            modifier = Modifier.onPointerEvent(eventType = PointerEventType.Press) {
                dismissSelectionMenu()
            },
        )
    }

    LaunchedEffect(filePath, refreshKey, encodingReloadKey, shouldRequestFocus) {
        if (shouldRequestFocus) {
            focusRequester.requestFocus()
        }
    }

    LaunchedEffect(filePath, fileViewState.fileLength) {
        while (true) {
            delay(1.seconds().millis)
            val errorMessage = when {
                !fileViewState.file.isFile -> "The selected file was moved or deleted."
                !fileViewState.file.canRead() -> "The selected file is no longer readable."
                fileViewState.file.length() < fileViewState.fileLength -> "The selected file was shortened."
                else -> null
            }
            if (errorMessage != null) {
                onPagerReady(null)
                onFileUnavailable(errorMessage)
                break
            }
        }
    }

    LaunchedEffect(filePath, fileViewState.isFollowing) {
        if (fileViewState.isFollowing) {
            launch {
                try {
                    while (fileViewState.isFollowing) {
                        val currentFileLength = fileViewState.file.length()
                        when {
                            !fileViewState.file.isFile -> throw FileUnavailableException("The selected file was moved or deleted.")
                            !fileViewState.file.canRead() -> throw FileUnavailableException("The selected file is no longer readable.")
                            currentFileLength < fileViewState.fileLength -> throw FileUnavailableException("The selected file was shortened.")
                        }
                        val currentLastModifiedMillis = fileViewState.file.lastModified()
                        val isFileContentChanged = currentFileLength != fileViewState.fileLength ||
                            currentLastModifiedMillis != lastModifiedMillis

                        fileViewState.fileLength = currentFileLength
                        lastModifiedMillis = currentLastModifiedMillis
                        filePager.moveToTheLastRow()
                        filePager.moveToPrevRow(rows = (filePager.numOfRowsInViewport - 3L).coerceAtLeast(0L))
                        onNavigate(filePager.viewportStartBytePosition)
                        if (isFileContentChanged) {
                            onDocumentContentChanged()
                        }
                        delay(1.seconds().millis)
                    }
                } catch (e: FileUnavailableException) {
                    e.printStackTrace()
                    onPagerReady(null)
                    onFileUnavailable(e.message ?: "The selected file is no longer available.")
                }
            }
        }
    }

    DisposableEffect(fileReader) {
        onDispose {
            println("Disposing ${fileReader.filePath}")
            fileReader.close()
            println("Disposed ${fileReader.filePath}")
        }
    }
}

@Composable
private fun GiantTextViewerStatusBar(
    modifier: Modifier = Modifier,
    filePager: GiantFileTextPager,
    fileLength: Long,
    lastModifiedMillis: Long,
    selectedTextEncoding: TextEncoding,
    resolvedTextEncoding: ResolvedTextEncoding,
    onSelectTextEncoding: (TextEncoding) -> Unit,
    onMenuActionComplete: () -> Unit = {},
) {
    val colors = LocalColor.current
    val font = LocalFont.current
    val numberFormat = remember { NumberFormat.getIntegerInstance(Locale.US) }
    val statusTextMeasurer = rememberTextMeasurer(0)
    val density = LocalDensity.current
    val fontSize = 12.sp
    val textStyle = TextStyle(
        color = colors.dialogPrimary,
        fontFamily = font.normalFontFamily,
        fontSize = fontSize,
    )
    val monospaceStyle = textStyle.copy(fontFamily = font.monospaceFontFamily)
    val dropdownTextStyle = textStyle.copy(fontWeight = FontWeight.Bold)

    val (viewportStartBytePosition, viewportEndBytePosition) = currentViewportByteRange(filePager, fileLength) // 0-based
        .let { (it.first + 1).coerceAtMost(fileLength) to (it.second + 1).coerceAtMost(fileLength) } // map to 1-based
    val fullBytePositionText = "${numberFormat.format(viewportStartBytePosition)} ~ ${numberFormat.format(viewportEndBytePosition)} / ${numberFormat.format(fileLength)} B"
    val compactBytePositionText = "${numberFormat.format(viewportStartBytePosition)} / ${numberFormat.format(fileLength)}"
    val selectedEncodingLabel = if (selectedTextEncoding == TextEncoding.Auto) {
        "${selectedTextEncoding.displayName()} (${resolvedTextEncoding.displayName()})"
    } else {
        selectedTextEncoding.displayName()
    }
    val lastModifiedDateTime = formatLastModified(lastModifiedMillis)
    val lastModifiedTime = formatLastModified(lastModifiedMillis, "HH:mm:ss")
    val lastModifiedTimeWithoutSeconds = formatLastModified(lastModifiedMillis, "HH:mm")

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .background(colors.statusBarBackground)
            .padding(horizontal = 8.dp),
    ) {
        val availableWidthPx = with(density) { maxWidth.toPx() }
        val spacingPx = with(density) { 8.dp.toPx() }
        val dropdownWidthPx = statusTextMeasurer.measure(
            text = selectedEncodingLabel,
            style = dropdownTextStyle,
            maxLines = 1,
        ).size.width + with(density) { 38.dp.toPx() }

        val statusVariant = listOf(
            StatusBarTextVariant(
                bytePositionText = fullBytePositionText,
                lastModifiedText = annotatedLastModifiedText(font, "Last modified: ", lastModifiedDateTime),
            ),
            StatusBarTextVariant(
                bytePositionText = compactBytePositionText,
                lastModifiedText = annotatedLastModifiedText(font, "Last modified: ", lastModifiedDateTime),
            ),
            StatusBarTextVariant(
                bytePositionText = compactBytePositionText,
                lastModifiedText = annotatedLastModifiedText(font, "", lastModifiedDateTime),
            ),
            StatusBarTextVariant(
                bytePositionText = compactBytePositionText,
                lastModifiedText = annotatedLastModifiedText(font, "", lastModifiedTime),
            ),
            StatusBarTextVariant(
                bytePositionText = compactBytePositionText,
                lastModifiedText = annotatedLastModifiedText(font, "", lastModifiedTimeWithoutSeconds),
            ),
            StatusBarTextVariant(
                bytePositionText = compactBytePositionText,
                lastModifiedText = null,
            ),
        ).firstOrNull {
            val bytePositionWidthPx = statusTextMeasurer.measure(
                text = it.bytePositionText,
                style = monospaceStyle,
                maxLines = 1,
            ).size.width
            val lastModifiedWidthPx = it.lastModifiedText?.let { text ->
                statusTextMeasurer.measure(
                    text = text,
                    style = textStyle,
                    maxLines = 1,
                ).size.width
            } ?: 0
            bytePositionWidthPx + lastModifiedWidthPx + dropdownWidthPx + spacingPx * 2 <= availableWidthPx
        } ?: StatusBarTextVariant(
            bytePositionText = compactBytePositionText,
            lastModifiedText = null,
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = statusVariant.bytePositionText,
                style = monospaceStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier,
            )

            statusVariant.lastModifiedText?.let {
                BasicText(
                    text = it,
                    style = textStyle.copy(textAlign = TextAlign.Center),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            } ?: Box(Modifier.weight(1f))

            DropDownView(
                selected = selectedEncodingLabel,
                entries = selectableTextEncodings.map {
                    ContextMenuItemEntry(
                        type = ContextMenuItemEntry.Type.Button,
                        displayText = it.displayName(),
                        isEnabled = true,
                        testTag = it.name,
                        action = { onSelectTextEncoding(it) },
                    )
                },
                textStyleModifier = { it.copy(fontSize = fontSize, fontWeight = FontWeight.Bold) },
                onItemActionComplete = onMenuActionComplete,
                modifier = Modifier.wrapContentSize()
            )
        }
    }
}

private data class StatusBarTextVariant(
    val bytePositionText: String,
    val lastModifiedText: AnnotatedString?,
)

private fun annotatedLastModifiedText(
    font: AppFont,
    label: String,
    dateTimeText: String,
): AnnotatedString = buildAnnotatedString {
    append(label)
    withStyle(SpanStyle(fontFamily = font.monospaceFontFamily)) {
        append(dateTimeText)
    }
}
private fun currentViewportByteRange(filePager: GiantFileTextPager, fileLength: Long): Pair<Long, Long> {
    val viewportStart = filePager.viewportStartBytePosition.coerceIn(0L, fileLength)
    val rowHeight = filePager.rowHeight()
    val visibleRowCount = if (filePager.viewport.height <= 0 || !java.lang.Float.isFinite(rowHeight) || rowHeight <= 0f) {
        0
    } else {
        floor(filePager.viewport.height.toDouble() / rowHeight.toDouble())
            .toInt()
            .coerceAtLeast(0)
    }
    val visibleRows = filePager.textInViewport.take(visibleRowCount)
    val rowStartBytePositions = filePager.startBytePositionsInViewport

    val viewportEnd = when {
        visibleRows.isEmpty() -> viewportStart
        rowStartBytePositions.size > visibleRows.size -> rowStartBytePositions[visibleRows.size]
        rowStartBytePositions.isNotEmpty() -> rowStartBytePositions.last() + filePager.encodedLengthOfText(visibleRows.last().string())
        else -> viewportStart
    }.coerceIn(viewportStart, fileLength)

    return viewportStart to viewportEnd
}

private fun formatLastModified(lastModifiedMillis: Long, pattern: String = "yyyy-MM-dd HH:mm:ss"): String {
    return KInstant(lastModifiedMillis).atLocalZoneOffset()
        .format(pattern)
}

private fun findBytePositionByCoordinatePx(filePager: GiantFileTextPager, point: Offset): Long {
    val startBytePositions = filePager.startBytePositionsInViewport
    val rowTexts = filePager.textInViewport
//    val charMeasurer = filePager.textLayouter.charMeasurer

    if (rowTexts.isEmpty()) {
        return filePager.viewportStartBytePosition
    }

    if (point.y <= 0) {
        return filePager.viewportStartBytePosition
    }

    // y-axis
    val rowFromTopLeft = floor(point.y / filePager.rowHeight()).toInt()
    if (rowFromTopLeft > rowTexts.lastIndex) {
        return startBytePositions.last() + filePager.encodedLengthOfText(rowTexts.last().string())
    }
    val startBytePosition = startBytePositions[rowFromTopLeft]

    // x-axis
    if (point.x <= 0f) {
        return startBytePosition
    }
    val rowText = rowTexts[rowFromTopLeft]
    var accumulatedPx = 0f
    var accumulatedBytes = 0L
    var matchedBytePosition: Long? = null
    GraphemeClusters.forEach(rowText) { start, end ->
        if (matchedBytePosition != null) {
            return@forEach
        }
        val fullChar = rowText.subSequence(start, end)
        val charWidth = filePager.textLayouter.measureCharWidth(fullChar)
        if (point.x in accumulatedPx ..< accumulatedPx + charWidth) {
            matchedBytePosition = startBytePosition + accumulatedBytes
            return@forEach
        }

        accumulatedBytes += filePager.encodedLengthOfText(fullChar.string())
        accumulatedPx += charWidth
    }
    matchedBytePosition?.let {
        return it
    }
    // reached end of row
    return startBytePosition + accumulatedBytes
}
