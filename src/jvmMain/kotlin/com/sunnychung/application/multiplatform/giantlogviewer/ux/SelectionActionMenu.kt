package com.sunnychung.application.multiplatform.giantlogviewer.ux

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunnychung.application.giantlogviewer.generated.resources.Res
import com.sunnychung.application.giantlogviewer.generated.resources.close
import com.sunnychung.application.multiplatform.giantlogviewer.ux.local.LocalColor
import com.sunnychung.application.multiplatform.giantlogviewer.ux.local.LocalFont

data class SelectionMenuItem(
    val label: AnnotatedString,
    val action: () -> Unit,
)

fun selectionMenuHeightDp(itemCount: Int) = (36 + itemCount * 31).dp

fun selectionMenuTopLeft(
    anchor: Offset,
    menuWidthPx: Float,
    menuHeightPx: Float,
    lineHeight: Float,
    contentWidth: Int,
    contentHeight: Int,
): Offset {
    val rawX = if (anchor.x + menuWidthPx > contentWidth) {
        anchor.x - menuWidthPx
    } else {
        anchor.x
    }
    val rawY = if (anchor.y + lineHeight + menuHeightPx > contentHeight) {
        anchor.y - menuHeightPx - lineHeight
    } else {
        anchor.y + lineHeight
    }
    return Offset(
        x = rawX.coerceIn(0f, (contentWidth.toFloat() - menuWidthPx).coerceAtLeast(0f)),
        y = rawY.coerceIn(0f, (contentHeight.toFloat() - menuHeightPx).coerceAtLeast(0f)),
    )
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
fun SelectionActionMenu(
    items: List<SelectionMenuItem>,
    selectionSizeText: String,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    onDismiss: () -> Unit,
    onActionComplete: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = LocalColor.current
    val font = LocalFont.current
    Column(
        modifier = modifier
            .background(colors.contextMenuBackground)
            .border(1.dp, colors.contextMenuText)
            .padding(bottom = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = selectionSizeText,
                style = TextStyle(
                    color = colors.contextMenuText,
                    fontFamily = font.monospaceFontFamily,
                    fontSize = 13.sp,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            )

            AppImage(
                resource = Res.drawable.close,
                size = 24.dp,
                color = colors.contextMenuText,
                modifier = Modifier
                    .focusProperties { canFocus = false }
                    .clickable {
                        onDismiss()
                        onActionComplete()
                    }
                    .padding(horizontal = 2.dp, vertical = 2.dp)
            )
        }

        items.forEachIndexed { index, item ->
            val isSelected = index == selectedIndex
            BasicText(
                text = item.label,
                style = TextStyle(
                    color = colors.contextMenuText,
                    fontFamily = font.normalFontFamily,
                    fontSize = 13.sp,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .focusProperties { canFocus = false }
                    .fillMaxWidth()
                    .background(if (isSelected) colors.fileBodyTheme.selectionBackground else colors.contextMenuBackground)
                    .onPointerEvent(eventType = PointerEventType.Enter) {
                        onSelectIndex(index)
                    }
                    .clickable {
                        onSelectIndex(index)
                        onDismiss()
                        try {
                            item.action()
                        } finally {
                            onActionComplete()
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun ToastMessage(
    message: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalColor.current
    val font = LocalFont.current
    BasicText(
        text = message,
        style = TextStyle(
            color = colors.contextMenuText,
            fontFamily = font.normalFontFamily,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        ),
        modifier = modifier
            .background(colors.contextMenuBackground)
            .border(1.dp, colors.contextMenuText)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}
