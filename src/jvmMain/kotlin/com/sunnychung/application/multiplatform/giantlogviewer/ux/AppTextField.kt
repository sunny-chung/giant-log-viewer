package com.sunnychung.application.multiplatform.giantlogviewer.ux

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.TextFieldColors
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.giantlogviewer.ux.local.LocalColor
import com.sunnychung.lib.multiplatform.bigtext.annotation.ExperimentalBigTextUiApi
import com.sunnychung.lib.multiplatform.bigtext.core.BigText
import com.sunnychung.lib.multiplatform.bigtext.core.BigTextDecorator
import com.sunnychung.lib.multiplatform.bigtext.core.transform.IncrementalTextTransformation
import com.sunnychung.lib.multiplatform.bigtext.platform.AsyncOperation
import com.sunnychung.lib.multiplatform.bigtext.ux.BigTextFieldState
import com.sunnychung.lib.multiplatform.bigtext.ux.CoreBigTextField
import com.sunnychung.lib.multiplatform.bigtext.ux.rememberConcurrentLargeAnnotatedBigTextFieldState

@OptIn(ExperimentalBigTextUiApi::class)
@Composable
fun AppTextField(
    textState: BigTextFieldState,
    onValueChange: (BigText) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    transformation: IncrementalTextTransformation<*>? = null,
    decorator: BigTextDecorator? = null,
    singleLine: Boolean = false,
    maxInputLength: Long = Long.MAX_VALUE,
//    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: TextFieldColors = TextFieldDefaults.textFieldColors(
        textColor = LocalColor.current.textFieldContent,
        placeholderColor = LocalColor.current.textFieldPlaceholder,
        cursorColor = LocalColor.current.textFieldCursor,
        backgroundColor = LocalColor.current.textFieldBackground,
    ),
    contentPadding: PaddingValues = PaddingValues(6.dp),
//    hasIndicatorLine: Boolean = false,
    onPointerEvent: ((event: PointerEvent, charIndex: Int, tag: String?) -> Unit)? = null,
    isAsynchronous: Boolean = false,

    /**
     * This parameter exists to work around the weird limitation of `Modifier.semantics(mergeDescendants = true)` that
     * nested nodes with `semantics(mergeDescendants = true)` would not be merged.
     */
    isDisableMerging: Boolean = false,
    onFinishInit: () -> Unit = {},
) {
    var isTextEmpty by remember { mutableStateOf(textState.text.isEmpty) } // TODO this should be fixed in the BigText library

    var modifier = modifier
    var textFieldModifier: Modifier = Modifier
    BigTextFieldLayout(
        modifier = modifier
//            .debugConstraints("BigTextFieldLayout")
            .background(colors.backgroundColor(enabled).value)
            .padding(contentPadding),
        textField = {
            CoreBigTextField(
                text = textState.text,
                viewState = textState.viewState,
                onTextChange = {
                    onValueChange(it.bigText)
                    isTextEmpty = textState.text.isEmpty
                },
                isEditable = enabled && !readOnly,
                isSelectable = enabled,
                isSoftWrapEnabled = !singleLine,
                fontSize = textStyle.fontSize,
                fontFamily = textStyle.fontFamily ?: FontFamily.SansSerif,
                color = colors.textColor(enabled).value,
                cursorColor = colors.cursorColor(false).value,
                textTransformation = transformation,
                textDecorator = decorator,
                isSingleLineInput = singleLine,
                maxInputLength = maxInputLength,
                contextMenu = AppBigTextFieldContextMenu,
                onPointerEvent = onPointerEvent,
//                interactionSource = interactionSource,
                onFinishInit = onFinishInit,
                onHeavyComputation = if (isAsynchronous) AsyncOperation.Asynchronous else AsyncOperation.Synchronous,
                padding = PaddingValues(0.dp),
//                modifier = Modifier.debugConstraints("core tf"),
            )
        },
        leadingIcon = {
            leadingIcon?.invoke()
        },
        placeholder = {
            if (placeholder != null && isTextEmpty) {
                placeholder()
            }
        },
        isDisableMerging = isDisableMerging,
    )
}

@Composable
fun AppTextField(
    key: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    backgroundColor: Color = LocalColor.current.textFieldBackground,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    transformation: IncrementalTextTransformation<*>? = null,
    decorator: BigTextDecorator? = null,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    maxInputLength: Long = Long.MAX_VALUE,
    colors: TextFieldColors = TextFieldDefaults.textFieldColors(
        textColor = LocalColor.current.textFieldContent,
        placeholderColor = LocalColor.current.textFieldPlaceholder,
        cursorColor = LocalColor.current.textFieldCursor,
        backgroundColor = backgroundColor,
    ),
    contentPadding: PaddingValues = PaddingValues(6.dp),
//    hasIndicatorLine: Boolean = false,
    onPointerEvent: ((event: PointerEvent, charIndex: Int, tag: String?) -> Unit)? = null,
    onFinishInit: () -> Unit = {},
) {
    val textState by rememberConcurrentLargeAnnotatedBigTextFieldState(value, key)

    AppTextField(
        textState = textState,
        onValueChange = {
            val newStringValue = it.buildString()
            onValueChange(newStringValue)
        },
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        transformation = transformation,
        decorator = decorator,
        singleLine = singleLine,
        maxInputLength = maxInputLength,
        colors = colors,
        contentPadding = contentPadding,
        onPointerEvent = onPointerEvent,
        onFinishInit = onFinishInit
    )
}
