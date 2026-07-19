package com.sunnychung.application.multiplatform.giantlogviewer.ux

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import com.sunnychung.application.giantlogviewer.generated.resources.Res
import com.sunnychung.application.giantlogviewer.generated.resources.appicon
import com.sunnychung.application.multiplatform.giantlogviewer.extension.setMinimumSize
import com.sunnychung.application.multiplatform.giantlogviewer.ux.local.LocalColor
import com.sunnychung.application.multiplatform.giantlogviewer.ux.local.LocalFont
import com.sunnychung.lib.android.composabletable.ux.Table
import org.jetbrains.compose.resources.painterResource

@Composable
fun HelpWindow(isVisible: Boolean, onClose: () -> Unit) {
    Window(
        visible = isVisible,
        onCloseRequest = onClose,
        title = "Help",
        icon = painterResource(Res.drawable.appicon),
        state = WindowState(
            position = WindowPosition.Aligned(Alignment.Center),
            width = 720.dp,
            height = 560.dp,
        ),
        onKeyEvent = { e ->
            if (e.type == KeyEventType.KeyDown && e.key == Key.Escape) {
                onClose()
                true
            } else {
                false
            }
        }
    ) {
        setMinimumSize(width = 600.dp, 250.dp)

        val colors = LocalColor.current

        Row(Modifier.background(colors.dialogBackground)) {
            KeyBindingTable(
                title = buildAnnotatedString {
                    append("The ")
                    withStyle(SpanStyle(fontFamily = LocalFont.current.monospaceFontFamily)) {
                        append("less")
                    }
                    append(" style")
                },
                keyBindings = listOf(
                    KeyBinding("↓", "Next row"),
                    KeyBinding("↑", "Previous row"),
                    KeyBinding("→", "Scroll right one window"),
                    KeyBinding("←", "Scroll left one window"),
                    KeyBinding("⇧→", "Scroll right one character"),
                    KeyBinding("⇧←", "Scroll left one character"),
                    KeyBinding("f", "One window forward"),
                    KeyBinding("b", "One window backward"),
                    KeyBinding("⇧G", "End of file"),
                    KeyBinding("g", "Start of file"),
                    KeyBinding("⇧/", "Search backward"),
                    KeyBinding("/", "Search forward"),
//                    KeyBinding("Alt/Option-click/drag", "Column selection"),
//                    KeyBinding("Alt/Option-Shift-click/drag", "Extend column selection"),
                    KeyBinding("⇧F", "Follow new appends"),
                    KeyBinding("Ctrl-C", "Cancel follow/copy"),
                    KeyBinding("Esc", "Exit search"),
                    KeyBinding("q", "Close file / Exit app"),
                ),
                modifier = Modifier.weight(.43f).fillMaxHeight()
            )
            KeyBindingTable(
                title = AnnotatedString("The memory-less style"),
                keyBindings = listOf(
                    KeyBinding("↓", "Next row"),
                    KeyBinding("↑", "Previous row"),
                    KeyBinding("→", "Scroll right one window"),
                    KeyBinding("←", "Scroll left one window"),
//                    KeyBinding("Shift-→", "Scroll right one character"),
//                    KeyBinding("Shift-←", "Scroll left one character"),
                    KeyBinding("Alt/Option-↓", "One window forward"),
                    KeyBinding("Alt/Option-↑", "One window backward"),
                    KeyBinding("Ctrl/Command-↓", "End of file"),
                    KeyBinding("Ctrl/Command-↑", "Start of file"),
                    KeyBinding("Ctrl/Command-F", "Search forward"),
                    KeyBinding("Shift-F", "Follow new appends"),
                    KeyBinding("Esc", "Cancel follow/search/copy"),
                    KeyBinding("Enter", "Search next"),
                    KeyBinding("Shift-Enter", "Search previous"),
                    KeyBinding("Ctrl/Command-C", "Copy selection"),
                    KeyBinding("Alt/Option-🤚", "Column selection"),
                    KeyBinding("Alt/Option-Shift-👆/🤚", "Extend column selection"),
                ),
                modifier = Modifier.weight(.57f).fillMaxHeight()
            )
        }
    }
}

@Composable
private fun KeyBindingTable(modifier: Modifier = Modifier, title: AnnotatedString, keyBindings: List<KeyBinding>) {
    val colors = LocalColor.current
    val primaryColor = colors.dialogPrimary
    val density = LocalDensity.current

    var componentWidth by remember { mutableIntStateOf(0) }
    var componentHeight by remember { mutableIntStateOf(0) }

    with (density) {
        val textMeasurer = rememberTextMeasurer(0) // TextMeasurer has a bug that its cache did not respect text color
        val titleTextStyle = TextStyle(
            fontSize = 16.sp,
            color = primaryColor,
            fontFamily = LocalFont.current.normalFontFamily
        )
        val titleSize = textMeasurer.measure(title, titleTextStyle).size
        val titleHorizontalPadding = 12.dp.toPx()

        Column(modifier
            .background(colors.dialogBackground)
            .onGloballyPositioned {
                componentWidth = it.size.width
                componentHeight = it.size.height
            }
            .drawBehind {
//                println("color p = $primaryColor, pp = ${colors.dialogPrimary} b = ${colors.dialogBackground}, s = $titleTextStyle")
                val primaryColor = colors.dialogPrimary
                drawRect(
                    color = primaryColor,
                    style = Stroke(),
                    topLeft = Offset(15.dp.toPx(), 15.dp.toPx()),
                    size = Size(componentWidth - 30.dp.toPx(), componentHeight - 30.dp.toPx()),
                )
                drawRect(
                    color = colors.dialogBackground,
                    topLeft = Offset(
                        x = (componentWidth - titleSize.width) / 2f - titleHorizontalPadding,
                        y = 0f,
                    ),
                    size = Size(
                        width = titleSize.width + titleHorizontalPadding * 2,
                        height = 31.dp.toPx(),
                    )
                )
                drawText(
                    text = title,
                    textMeasurer = textMeasurer,
                    style = titleTextStyle,
                    topLeft = Offset(
                        x = (componentWidth - titleSize.width) / 2f,
                        y = (31.dp.toPx() - titleSize.height) / 2,
                    ),
                )
            }
            .padding(31.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Table(
                rowCount = keyBindings.size,
                columnCount = 2,
            ) { rowIndex, columnIndex ->
                BasicText(
                    text = if (columnIndex == 0) keyBindings[rowIndex].keys else keyBindings[rowIndex].description,
                    style = TextStyle(
                        color = primaryColor,
                        fontFamily = LocalFont.current.normalFontFamily
                    ),
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
    }
}

private data class KeyBinding(val keys: String, val description: String)
