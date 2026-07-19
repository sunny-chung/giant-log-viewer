package com.sunnychung.application.multiplatform.giantlogviewer.ux

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.DragData
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.onExternalDrag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sunnychung.application.giantlogviewer.generated.resources.Res
import com.sunnychung.application.giantlogviewer.generated.resources.fast_forward
import com.sunnychung.application.giantlogviewer.generated.resources.fast_forward_filled
import com.sunnychung.application.giantlogviewer.generated.resources.help
import com.sunnychung.application.giantlogviewer.generated.resources.info
import com.sunnychung.application.giantlogviewer.generated.resources.setting
import com.sunnychung.application.giantlogviewer.generated.resources.wrap_text
import com.sunnychung.application.multiplatform.giantlogviewer.document.ThemeDI
import com.sunnychung.application.multiplatform.giantlogviewer.document.toColorTheme
import com.sunnychung.application.multiplatform.giantlogviewer.extension.subscribeStateToEntity
import com.sunnychung.application.multiplatform.giantlogviewer.io.GiantFileTextPager
import com.sunnychung.application.multiplatform.giantlogviewer.manager.AppContext
import com.sunnychung.application.multiplatform.giantlogviewer.model.SearchMode
import com.sunnychung.application.multiplatform.giantlogviewer.model.SearchOptions
import com.sunnychung.application.multiplatform.giantlogviewer.model.SearchResultType
import com.sunnychung.application.multiplatform.giantlogviewer.ux.local.LocalColor
import com.sunnychung.application.multiplatform.giantlogviewer.ux.local.LocalFont
import com.sunnychung.application.multiplatform.giantlogviewer.viewstate.FileViewState
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.launch
import java.io.File
import java.net.URI
import java.util.regex.Pattern

@Composable
@OptIn(ExperimentalComposeUiApi::class)
fun App(onExitApplication: () -> Unit = {}) {
    var selectedFileName by remember { mutableStateOf("") }
    var isShowHelpWindow by remember { mutableStateOf(false) }
    var isShowAboutWindow by remember { mutableStateOf(false) }
    var isShowSettingWindow by remember { mutableStateOf(false) }
    var isSoftWrapEnabled by remember { mutableStateOf(true) }

    val themePreference = AppContext.instance.ThemePreferenceRepository
        .subscribeStateToEntity(ThemeDI)
        .themes

    var selectedFilePath by remember { mutableStateOf("") }
    var fileViewState: FileViewState by remember(selectedFilePath) { mutableStateOf(FileViewState(File(selectedFilePath))) }
    var dismissSelectionMenuKey by remember { mutableIntStateOf(0) }
    val isReadableFileSelected = selectedFilePath
        .takeIf { it.isNotEmpty() }
        ?.let { File(it).let { file -> file.isFile && file.canRead() } }
        ?: false

    print("App recompose - $themePreference")

    CompositionLocalProvider(LocalColor provides themePreference.toColorTheme()) {
        val colors = LocalColor.current

        val viewerFocusRequester = remember { FocusRequester() }

        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().height(30.dp)
                    .background(colors.menuBarBackground)
                    .onPointerEvent(eventType = PointerEventType.Press) {
                        dismissSelectionMenuKey++
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppImage(
                    resource = Res.drawable.info,
                    size = 20.dp,
                    color = colors.menuBarIconColor,
                    modifier = Modifier.padding(5.dp)
                        .clickable {
                            isShowAboutWindow = true
                        }
                )
                BasicText(
                    text = selectedFileName,
                    style = TextStyle(
                        color = colors.menuBarTextColor,
                        fontFamily = LocalFont.current.normalFontFamily,
                        textAlign = TextAlign.Center,
                    ),
                    softWrap = false,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
                )
                if (fileViewState.isFileExist) {
                    AppImage(
                        resource = if (!fileViewState.isFollowing) Res.drawable.fast_forward else Res.drawable.fast_forward_filled,
                        size = 20.dp,
                        color = if (!fileViewState.isFollowing) colors.menuBarIconColor else colors.menuBarIconActivated,
                        modifier = Modifier.padding(5.dp)
                            .clickable {
                                fileViewState.isFollowing = !fileViewState.isFollowing
                                viewerFocusRequester.requestFocus()
                            }
                    )
                }
                AppImage(
                    resource = Res.drawable.wrap_text,
                    size = 20.dp,
                    color = if (isSoftWrapEnabled) colors.menuBarIconActivated else colors.menuBarIconColor,
                    enabled = isReadableFileSelected,
                    modifier = Modifier.padding(5.dp)
                        .clickable(enabled = isReadableFileSelected) {
                            isSoftWrapEnabled = !isSoftWrapEnabled
                            viewerFocusRequester.requestFocus()
                        }
                )
                AppImage(
                    resource = Res.drawable.help,
                    size = 20.dp,
                    color = colors.menuBarIconColor,
                    modifier = Modifier.padding(5.dp)
                        .clickable {
                            isShowHelpWindow = true
                        }
                )
                AppImage(
                    resource = Res.drawable.setting,
                    size = 20.dp,
                    color = colors.menuBarIconColor,
                    modifier = Modifier.padding(5.dp)
                        .clickable {
                            isShowSettingWindow = true
                        }
                )
            }

            AppMainContent(
                selectedFilePath = selectedFilePath,
                fileViewState = fileViewState,
                isSoftWrapEnabled = isSoftWrapEnabled,
                dismissSelectionMenuKey = dismissSelectionMenuKey,
                onExitApplication = onExitApplication,
                onSelectFile = { file ->
                    selectedFileName = file?.name ?: ""
                    selectedFilePath = file?.path ?: ""
                },
                modifier = Modifier.focusRequester(viewerFocusRequester)
            )

            HelpWindow(isVisible = isShowHelpWindow, onClose = { isShowHelpWindow = false })
            AboutWindow(isVisible = isShowAboutWindow, onClose = { isShowAboutWindow = false })
            SettingWindow(isVisible = isShowSettingWindow, onClose = { isShowSettingWindow = false })
        }
    }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun AppMainContent(
    modifier: Modifier = Modifier,
    selectedFilePath: String,
    fileViewState: FileViewState,
    isSoftWrapEnabled: Boolean,
    dismissSelectionMenuKey: Int,
    onExitApplication: () -> Unit,
    onSelectFile: (File?) -> Unit,
) {
    val colors = LocalColor.current

    val isNavigationLocked = fileViewState.isFollowing

    val viewerFocusRequester = remember { FocusRequester() }
    val emptyFileFocusRequester = remember { FocusRequester() }
    val openFileCoroutineScope = rememberCoroutineScope()
    var shouldFocusViewerAfterSelect by remember { mutableStateOf(false) }
    var filePager: GiantFileTextPager? by remember { mutableStateOf(null) }

    var isSearchBarVisible by remember { mutableStateOf(false) }
    var searchEntry by remember { mutableStateOf("") }
    var searchOptions by remember { mutableStateOf(
        SearchOptions(
            isRegex = false,
            isCaseSensitive = true,
            isWholeWord = false
        )
    ) }
    var isSearchBackwardDefault by remember { mutableStateOf(true) }

    var searchCursor by remember(filePager) { mutableStateOf(0L) }
    var highlightByteRange by remember(filePager) { mutableStateOf(0L .. -1L) }
    var searchOptionsOfResult by remember { mutableStateOf<SearchOptions?>(null) }
    var searchEntryOfResult by remember { mutableStateOf("") }
    var searchBarReloadKey by remember { mutableIntStateOf(0) }
    var isSearchFieldFocused by remember { mutableStateOf(false) }
    var isSearchError by remember { mutableStateOf(false) }

    fun resetSearchResultState(recreateSearchField: Boolean = false) {
        searchCursor = filePager?.viewportStartBytePosition ?: 0L
        highlightByteRange = 0L .. -1L
        searchOptionsOfResult = null
        searchEntryOfResult = ""
        isSearchError = false
        if (recreateSearchField) {
            searchBarReloadKey++
        }
    }

    fun setSearchError() {
        highlightByteRange = 0L .. -1L
        searchOptionsOfResult = searchOptions
        searchEntryOfResult = searchEntry
        isSearchError = true
    }

    fun isSearchResultStateCurrent(): Boolean {
        return searchOptions == searchOptionsOfResult && searchEntry == searchEntryOfResult
    }

    fun currentSearchRegex(): Regex? {
        if (searchEntry.isEmpty()) {
            return null
        }
        val regexOption = if (searchOptions.isCaseSensitive) setOf() else setOf(RegexOption.IGNORE_CASE)
        try {
            val pattern = if (searchOptions.isRegex) {
                searchEntry.toRegex(regexOption)
            } else if (searchOptions.isWholeWord) {
                "\\b${Pattern.quote(searchEntry)}\\b".toRegex(regexOption)
            } else {
                Pattern.quote(searchEntry).toRegex(regexOption)
            }
            return pattern
        } catch (e: Throwable) {
            e.printStackTrace()
            setSearchError()
        }
        return null
    }

    LaunchedEffect(selectedFilePath) {
        resetSearchResultState()
    }

    LaunchedEffect(isSearchBarVisible) {
        if (!isSearchBarVisible) {
            isSearchFieldFocused = false
        }
    }

    Column(modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .onExternalDrag(
                    onDragStart = { drag ->
                        println("drag: $drag | ${drag.dragData}")
                    },
                    onDrop = { drop ->
                        println("drop: $drop | ${drop.dragData}")
                        if (drop.dragData is DragData.FilesList) {
                            println("drop files: ${(drop.dragData as DragData.FilesList).readFiles()}")
                            val uri = URI((drop.dragData as DragData.FilesList).readFiles().first())

                            println("f: ${uri.scheme} ${File(uri).absolutePath}")
                            onSelectFile(File(uri))
                            shouldFocusViewerAfterSelect = true
                        }
                    }
                )
                .background(colors.fileBodyTheme.background)
        ) {
            if (selectedFilePath.isEmpty()) {
                LaunchedEffect(Unit) {
                    emptyFileFocusRequester.requestFocus()
                }

                EmptyFileView(
                    modifier = Modifier
                        .onPreviewKeyEvent { e ->
                            if (
                                e.type == KeyEventType.KeyDown &&
                                e.key == Key.Q
                            ) {
                                onExitApplication()
                                true
                            } else {
                                false
                            }
                        }
                        .focusRequester(emptyFileFocusRequester)
                        .focusable(),
                    onOpenFileClick = {
                        openFileCoroutineScope.launch {
                            val file = FileKit.openFilePicker(title = "Open text file") ?: return@launch
                            onSelectFile(File(file.path))
                            shouldFocusViewerAfterSelect = true
                        }
                    }
                )
                onSelectFile(null)
                return@Box
            }

            val file = File(selectedFilePath)
            if (!file.exists()) {
                ErrorView(message = "The selected object no longer exists")
                onSelectFile(null)
                return@Box
            }
            if (!file.isFile) {
                ErrorView(message = "The selected object is not a file")
                onSelectFile(null)
                return@Box
            }
            if (!file.canRead()) {
                ErrorView(message = "The selected file is not readable")
                onSelectFile(null)
                return@Box
            }

            onSelectFile(file)

            LaunchedEffect(selectedFilePath, shouldFocusViewerAfterSelect) {
                if (shouldFocusViewerAfterSelect) {
                    viewerFocusRequester.requestFocus()
                    shouldFocusViewerAfterSelect = false
                }
            }

            GiantTextViewer(
                fileViewState = fileViewState,
                isSoftWrapEnabled = isSoftWrapEnabled,
                filePath = selectedFilePath,
                highlightByteRange = highlightByteRange,
                onPagerReady = { filePager = it },
                onNavigate = { searchCursor = it },
                onDocumentContentChanged = {
                    resetSearchResultState(recreateSearchField = true)
                },
                onCloseFile = {
                    isSearchBarVisible = false
                    filePager = null
                    onSelectFile(null)
                },
                onSearchRequest = {
                    if (it == SearchMode.None) {
                        isSearchBarVisible = false
                    } else {
                        isSearchBarVisible = true
                        isSearchBackwardDefault = (it == SearchMode.Backward)
                    }
                },
                dismissSelectionMenuKey = dismissSelectionMenuKey,
                bottomContent = {
                    if (isSearchBarVisible) {
                        TextSearchBar(
                            key = "${selectedFilePath.replace("/", "\\/")}/$searchBarReloadKey",
                            text = searchEntry,
                            onTextChange = {
                                searchEntry = it
                                resetSearchResultState()
                            },
                            searchOptions = searchOptions,
                            isSearchBackwardDefault = isSearchBackwardDefault,
                            searchResultType = if (!isSearchResultStateCurrent()) {
                                SearchResultType.NotYetSearch
                            } else if (isSearchError) {
                                SearchResultType.Error
                            } else if (highlightByteRange.isEmpty()) {
                                SearchResultType.NoResult
                            } else {
                                SearchResultType.SomeResult
                            },
                            onToggleRegex = {
                                if (isNavigationLocked) return@TextSearchBar
                                searchOptions = searchOptions.copy(isRegex = it)
                                resetSearchResultState()
                            },
                            onToggleCaseSensitive = {
                                if (isNavigationLocked) return@TextSearchBar
                                searchOptions = searchOptions.copy(isCaseSensitive = it)
                                resetSearchResultState()
                            },
                            onToggleWholeWord = {
                                if (isNavigationLocked) return@TextSearchBar
                                searchOptions = searchOptions.copy(isWholeWord = it)
                                resetSearchResultState()
                            },
                            onClickPrev = {
                                if (isNavigationLocked) return@TextSearchBar

                                val regex = currentSearchRegex() ?: return@TextSearchBar
                                val pager = filePager ?: return@TextSearchBar
                                val result = try {
                                    pager.searchBackward(searchCursor, regex)
                                } catch (e: Throwable) {
                                    e.printStackTrace()
                                    setSearchError()
                                    return@TextSearchBar
                                }
                                isSearchError = false
                                if (!result.isEmpty()) {
                                    searchCursor = result.start
                                    println("search found at $result")
                                    pager.moveToRowOfBytePosition(result.start)
                                } else {
                                    searchCursor = pager.fileReader.contentStartBytePosition
                                }
                                highlightByteRange = result

                                searchOptionsOfResult = searchOptions
                                searchEntryOfResult = searchEntry
                            },
                            onClickNext = {
                                if (isNavigationLocked) return@TextSearchBar

                                val regex = currentSearchRegex() ?: return@TextSearchBar
                                val pager = filePager ?: return@TextSearchBar
                                if (pager.viewportStartBytePosition < fileViewState.fileLength) {
                                    val searchStartBytePosition = if (isSearchResultStateCurrent() && !highlightByteRange.isEmpty()) {
                                        searchCursor + 1
                                    } else {
                                        searchCursor
                                    }
                                    val result = try {
                                        pager.searchAtAndForward(searchStartBytePosition, regex)
                                    } catch (e: Throwable) {
                                        e.printStackTrace()
                                        setSearchError()
                                        return@TextSearchBar
                                    }
                                    isSearchError = false
                                    if (!result.isEmpty()) {
                                        searchCursor = result.start
                                        println("search found at $result")
                                        pager.moveToRowOfBytePosition(result.start)
                                    } else {
                                        searchCursor = fileViewState.fileLength
                                    }
                                    highlightByteRange = result

                                    searchOptionsOfResult = searchOptions
                                    searchEntryOfResult = searchEntry
                                }
                            },
                            onSearchFieldFocusChanged = {
                                isSearchFieldFocused = it
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colors.background)
                                .padding(2.dp)
                                .onKeyEvent { e ->
//                        println("search onKeyEvent ${e.key}")
                                    if (e.type == KeyEventType.KeyDown && e.key == Key.Escape) {
                                        isSearchBarVisible = false
                                        viewerFocusRequester.requestFocus()
                                        true
                                    } else {
                                        false
                                    }
                                }
                        )
                    }
                },
                shouldRequestFocus = !isSearchBarVisible,
                isKeyboardShortcutEnabled = !isSearchFieldFocused,
                modifier = Modifier.matchParentSize()
                    .focusRequester(viewerFocusRequester)
            )
        }
    }
}
