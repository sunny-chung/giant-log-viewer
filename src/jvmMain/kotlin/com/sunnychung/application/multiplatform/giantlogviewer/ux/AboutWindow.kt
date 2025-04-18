package com.sunnychung.application.multiplatform.giantlogviewer.ux

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import com.sunnychung.application.giantlogviewer.generated.resources.Res
import com.sunnychung.application.giantlogviewer.generated.resources.appicon
import com.sunnychung.application.giantlogviewer.generated.resources.copy
import com.sunnychung.application.giantlogviewer.generated.resources.world_wide_web
import com.sunnychung.application.multiplatform.giantlogviewer.extension.setMinimumSize
import com.sunnychung.application.multiplatform.giantlogviewer.manager.AppContext
import com.sunnychung.application.multiplatform.giantlogviewer.ux.local.LocalColor
import com.sunnychung.application.multiplatform.giantlogviewer.ux.local.LocalFont
import org.jetbrains.compose.resources.painterResource

@Composable
fun AboutWindow(isVisible: Boolean, onClose: () -> Unit) {
    val font = LocalFont.current.normalFontFamily
    val colors = LocalColor.current
    val clipboardManager = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    val buildInfo = AppContext.instance.MetadataManager

    Window(
        visible = isVisible,
        onCloseRequest = onClose,
        title = "About",
        icon = painterResource(Res.drawable.appicon),
        state = WindowState(
            position = WindowPosition.Aligned(Alignment.Center),
            width = 420.dp,
            height = 200.dp,
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
        setMinimumSize(width = 400.dp, 180.dp)

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize().background(colors.dialogBackground).padding(12.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            ) {
                Image(
                    painter = painterResource(resource = Res.drawable.appicon),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                )
                BasicText(
                    "Giant Log Viewer",
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontFamily = font,
                        fontWeight = FontWeight.Medium,
                        color = colors.aboutDialogText,
                    )
                )
                BasicText(
                    "${buildInfo.version} (${buildInfo.gitCommitHash})",
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontFamily = font,
                        color = colors.aboutDialogText,
                    )
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val repoUrl = "https://github.com/sunny-chung/giant-log-viewer"
                    BasicText(repoUrl, style = TextStyle(fontFamily = font, color = colors.aboutDialogText))
                    AppImage(
                        resource = Res.drawable.copy,
                        size = 16.dp,
                        color = colors.aboutDialogButton,
                        modifier = Modifier.clickable {
                            clipboardManager.setText(AnnotatedString(repoUrl))
                        }
                    )
                    AppImage(
                        resource = Res.drawable.world_wide_web,
                        size = 16.dp,
                        color = colors.aboutDialogButton,
                        modifier = Modifier.clickable {
                            uriHandler.openUri(repoUrl)
                        }
                    )
                }
//                Spacer(Modifier.height(24.dp))
//                BasicText("", style = TextStyle(fontFamily = font))
            }
        }
    }
}
