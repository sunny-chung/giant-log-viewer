package com.sunnychung.application.multiplatform.giantlogviewer.ux

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.sunnychung.application.giantlogviewer.generated.resources.Res
import com.sunnychung.application.giantlogviewer.generated.resources.move_to
import com.sunnychung.application.multiplatform.giantlogviewer.ux.local.LocalColor
import com.sunnychung.application.multiplatform.giantlogviewer.ux.local.LocalFont

@Composable
fun EmptyFileView(modifier: Modifier = Modifier, errorMessage: String? = null, onOpenFileClick: () -> Unit = {}) {
    val colors = LocalColor.current
    Box(modifier.fillMaxSize().padding(32.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.Center)) {
            AppImage(
                resource = Res.drawable.move_to,
                size = 96.dp,
                color = colors.bigTextHint,
            )
            Spacer(Modifier.height(32.dp))
            BasicText(
                text = "Drop a UTF-8, UTF-16 or ASCII text file here\nto get started",
                style = TextStyle(
                    color = colors.bigTextHint,
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 1.5.em,
                    fontFamily = LocalFont.current.normalFontFamily,
                    fontWeight = FontWeight.Medium,
                ),
            )
            errorMessage?.let {
                Spacer(Modifier.height(18.dp))
                BasicText(
                    text = it,
                    style = TextStyle(
                        color = colors.bigTextWarning,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 1.3.em,
                        fontFamily = LocalFont.current.normalFontFamily,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
            Spacer(Modifier.height(29.dp))
            OpenFileButton(onClick = onOpenFileClick)
        }
    }
}

@Composable
private fun OpenFileButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = LocalColor.current
    val shape = RoundedCornerShape(4.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .border(1.dp, colors.bigTextHint.copy(alpha = 0.65f), shape)
            .background(colors.bigTextHint.copy(alpha = 0.20f), shape)
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        AppImage(
            resource = Res.drawable.move_to,
            size = 18.dp,
            color = colors.bigTextHint,
        )
        Spacer(Modifier.width(10.dp))
        BasicText(
            text = "Open File",
            style = TextStyle(
                color = colors.bigTextHint,
                fontSize = 15.sp,
                fontFamily = LocalFont.current.normalFontFamily,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}
