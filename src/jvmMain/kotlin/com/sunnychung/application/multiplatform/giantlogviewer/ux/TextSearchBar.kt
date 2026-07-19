package com.sunnychung.application.multiplatform.giantlogviewer.ux

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.giantlogviewer.model.SearchOptions
import com.sunnychung.application.multiplatform.giantlogviewer.model.SearchResultType
import com.sunnychung.application.multiplatform.giantlogviewer.ux.local.LocalColor
import com.sunnychung.application.multiplatform.giantlogviewer.ux.local.LocalFont

private val SEARCH_OPTION_BUTTON_WIDTH = 24.dp

@Composable
fun TextSearchBar(
    modifier: Modifier = Modifier,
    key: String,
    text: String,
    onTextChange: (String) -> Unit,
//    statusText: String,
    searchOptions: SearchOptions,
    isSearchBackwardDefault: Boolean,
    searchResultType: SearchResultType,
    onToggleRegex: (Boolean) -> Unit,
    onToggleCaseSensitive: (Boolean) -> Unit,
    onToggleWholeWord: (Boolean) -> Unit,
    onClickPrev: () -> Unit,
    onClickNext: () -> Unit,
    onSearchFieldFocusChanged: (Boolean) -> Unit = {},
) {
    val colors = LocalColor.current

    val focusRequester = remember(key) { FocusRequester() }

    Row(
        modifier = modifier.onPreviewKeyEvent { e ->
            if (e.type == KeyEventType.KeyDown && e.key == Key.Enter) {
                var isBackward = isSearchBackwardDefault
                if (e.isShiftPressed) {
                    isBackward = !isBackward
                }
                if (isBackward) {
                    onClickPrev()
                } else {
                    onClickNext()
                }
                true
            } else {
                false
            }
        }, verticalAlignment = Alignment.CenterVertically
    ) {
        AppTextField(
            key = "$key/SearchText",
            value = text,
            onValueChange = onTextChange,
            placeholder = {
                BasicText(
                    text = "Text/Pattern to Search for",
                    style = TextStyle(
                        color = colors.textFieldPlaceholder,
                        fontFamily = LocalFont.current.normalFontFamily,
                    )
                )
            },
            textStyle = TextStyle(
                fontFamily = LocalFont.current.monospaceFontFamily,
            ),
            maxInputLength = 512 * 1024, // 512 KB
            maxLines = 1,
            singleLine = true, // not allow '\n'
            contentPadding = PaddingValues(4.dp),
            onFinishInit = {
                focusRequester.requestFocus()
            },
            backgroundColor = when (searchResultType) {
                SearchResultType.NotYetSearch -> colors.textFieldBackground
                SearchResultType.NoResult -> colors.searchTextFieldBackgroundNoResult
                SearchResultType.SomeResult -> colors.searchTextFieldBackgroundSomeResult
                SearchResultType.Error -> colors.searchTextFieldBackgroundError
            },
            modifier = Modifier.weight(1f)
                .onFocusChanged { onSearchFieldFocusChanged(it.hasFocus) }
                .focusRequester(focusRequester)
        )
        TextToggleButton(
            text = ".*",
            fontFamily = LocalFont.current.monospaceFontFamily,
            isSelected = searchOptions.isRegex,
            onToggle = onToggleRegex,
            modifier = Modifier.width(SEARCH_OPTION_BUTTON_WIDTH).focusProperties { canFocus = false },
        )
        TextToggleButton(
            text = "Aa",
            isSelected = searchOptions.isCaseSensitive,
            onToggle = onToggleCaseSensitive,
            modifier = Modifier.width(SEARCH_OPTION_BUTTON_WIDTH).focusProperties { canFocus = false },
        )
        TextToggleButton(
            text = "W",
            isSelected = searchOptions.isWholeWord,
            isEnabled = !searchOptions.isRegex,
            onToggle = onToggleWholeWord,
            modifier = Modifier.width(SEARCH_OPTION_BUTTON_WIDTH).focusProperties { canFocus = false },
        )
        TextToggleButton(
            text = "↑",
            isSelected = isSearchBackwardDefault,
            onToggle = { onClickPrev() },
            modifier = Modifier.width(SEARCH_OPTION_BUTTON_WIDTH).focusProperties { canFocus = false },
        )
        TextToggleButton(
            text = "↓",
            isSelected = !isSearchBackwardDefault,
            onToggle = { onClickNext() },
            modifier = Modifier.width(SEARCH_OPTION_BUTTON_WIDTH).focusProperties { canFocus = false },
        )
    }
}
