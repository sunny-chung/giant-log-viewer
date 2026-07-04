package com.sunnychung.application.multiplatform.giantlogviewer

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.sunnychung.application.giantlogviewer.generated.resources.Res
import com.sunnychung.application.giantlogviewer.generated.resources.appicon
import com.sunnychung.application.multiplatform.giantlogviewer.extension.setMinimumSize
import com.sunnychung.application.multiplatform.giantlogviewer.manager.AppContext
import com.sunnychung.application.multiplatform.giantlogviewer.ux.App
import com.sunnychung.lib.multiplatform.kdatetime.KZonedInstant
import kotlinx.coroutines.runBlocking
import net.harawata.appdirs.AppDirsFactory
import org.jetbrains.compose.resources.painterResource
import java.io.File

fun main(args: Array<String>) {
    val startAppInstant = KZonedInstant.nowAtLocalZoneOffset()
    log("[$startAppInstant] start app")
    System.setProperty("apple.awt.application.appearance", "system")

    val appDir = AppDirsFactory.getInstance().getUserDataDir("Giant Log Viewer", null, null)
    AppContext.instance.dataDir = File(appDir)
    runBlocking {
        AppContext.instance.ResourceManager.loadAllResources()
    }

    KZonedInstant.nowAtLocalZoneOffset().let { now ->
        log("[$now] (${now - startAppInstant}) pre loaded app")
    }

    application {

        KZonedInstant.nowAtLocalZoneOffset().let { now ->
            log("[$now] (${now - startAppInstant}) app scope")
        }

        Window(
            onCloseRequest = ::exitApplication,
            title = "Giant Log Viewer",
            icon = painterResource(Res.drawable.appicon),
        ) {
            setMinimumSize(250.dp, 150.dp)
            App()

            KZonedInstant.nowAtLocalZoneOffset().let { now ->
                log("[$now] (${now - startAppInstant}) after app")
            }
        }
    }
}

fun log(msg: String) {
    File("C:/Users/Sunny/tmp/timeLog.log").appendText("$msg\n")
}
