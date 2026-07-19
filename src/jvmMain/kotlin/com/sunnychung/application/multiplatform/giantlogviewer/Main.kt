package com.sunnychung.application.multiplatform.giantlogviewer

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.sunnychung.application.giantlogviewer.generated.resources.Res
import com.sunnychung.application.giantlogviewer.generated.resources.appicon
import com.sunnychung.application.multiplatform.giantlogviewer.extension.setMinimumSize
import com.sunnychung.application.multiplatform.giantlogviewer.manager.AppContext
import com.sunnychung.application.multiplatform.giantlogviewer.ux.App
import kotlinx.coroutines.runBlocking
import net.harawata.appdirs.AppDirsFactory
import org.jetbrains.compose.resources.painterResource
import java.io.File

fun main(args: Array<String>) {
    System.setProperty("apple.awt.application.appearance", "system")

    val appDir = AppDirsFactory.getInstance().getUserDataDir("Giant Log Viewer", null, null)
    AppContext.instance.dataDir = File(appDir)
    runBlocking {
        AppContext.instance.ResourceManager.loadAllResources()
    }

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Giant Log Viewer",
            icon = painterResource(Res.drawable.appicon),
        ) {
            setMinimumSize(250.dp, 150.dp)
            App(onExitApplication = ::exitApplication)
        }
    }
}
