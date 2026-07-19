package com.sunnychung.application.multiplatform.giantlogviewer.document

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

data class ColorTheme(
    val name: String,
    val background: Color,
    val bigTextHint: Color,
    val bigTextWarning: Color,
    val textFieldContent: Color,
    val textFieldPlaceholder: Color,
    val textFieldCursor: Color,
    val textFieldBackground: Color,
    val searchTextFieldBackgroundNoResult: Color,
    val searchTextFieldBackgroundSomeResult: Color,
    val searchTextFieldBackgroundError: Color,
    val warningText: Color,
    val contextMenuBackground: Color,
    val contextMenuText: Color,
    val contextMenuDisabledText: Color,
    val contextMenuDividerLine: Color,
    val menuBarTextColor: Color,
    val menuBarIconColor: Color = menuBarTextColor,
    val menuBarIconActivated: Color,
    val menuBarBackground: Color,
    val statusBarBackground: Color,
    val toggleButtonOnBackground: Color,
    val toggleButtonOffBackground: Color,
    val toggleButtonOnText: Color,
    val toggleButtonOffText: Color = toggleButtonOnText,
    val toggleButtonDisabledText: Color,
    val readPositionBarNegativeBackground: Color,
    val readPositionBarPositiveBackground: Color,
    val readPositionBarText: Color,
    val fileBodyTheme: FileBodyTheme,
    val aboutDialogText: Color,
    val aboutDialogButton: Color = aboutDialogText,
    val dialogBackground: Color,
    val dialogPrimary: Color,
)

data class FileBodyTheme(
    val background: Color,
    val plainText: Color,
    val selectionBackground: Color,
    val searchMatchBackground: Color,
)

fun lightColorTheme(): ColorTheme = ColorTheme(
    name = "Light",
    background = Color(.45f, .45f, .45f),
    bigTextHint = Color.Gray,
    bigTextWarning = Color(red = 0.7f, green = 0.4f, blue = 0.4f),
    textFieldPlaceholder = Color.Gray,
    textFieldBackground = Color.White,
    textFieldCursor = Color.Black,
    textFieldContent = Color.Black,
    searchTextFieldBackgroundNoResult = Color(red = 1f, green = 0.7f, blue = 0.7f),
    searchTextFieldBackgroundSomeResult = Color(red = 0.7f, green = 1f, blue = 0.7f),
    searchTextFieldBackgroundError = Color(red = 0.62f, green = 0.18f, blue = 0.18f),
    warningText = Color(red = .8f, green = .3f, blue = 0f),
    contextMenuBackground = Color.Cyan,
    contextMenuText = Color.Black,
    contextMenuDisabledText = Color.LightGray,
    contextMenuDividerLine = Color.Gray,
    menuBarBackground = Color(red = 0.4f, green = 0.4f, blue = 0.4f),
    menuBarIconColor = Color.White,
    menuBarTextColor = Color.White,
    menuBarIconActivated = Color(red = 0.7f, green = 1f, blue = 0f),
    statusBarBackground = Color(red = 0.55f, green = 0.63f, blue = 0.55f),
    toggleButtonOnText = Color.White,
    toggleButtonDisabledText = Color(.6f, .6f, .6f),
    toggleButtonOnBackground = Color(.2f, .2f, .7f),
    toggleButtonOffBackground = Color(0f, 0f, .3f),
    readPositionBarNegativeBackground = Color(red = 0.3f, green = 0.3f, blue = 0.3f),
    readPositionBarPositiveBackground = Color(red = 0.85f, green = 0.6f, blue = 0f),
    readPositionBarText = Color.White,
    fileBodyTheme = FileBodyTheme(
        searchMatchBackground = Color(red = 0.85f, green = 0.6f, blue = 0f),
        selectionBackground = Color(red = 0.3f, green = 0.3f, blue = 0.7f, alpha = .6f),
        plainText = Color.Black,
        background = Color.Cyan,
    ),
    aboutDialogText = Color.Black,
    dialogBackground = Color.Cyan,
    dialogPrimary = Color(red = 0f, green = 0f, blue = 0.19f),
)

fun darkColorTheme(): ColorTheme {
    val primaryBackground = Color(.06f, .06f, .06f)
    val brightBackground = Color(0.235f, 0.196f, 0.196f)
    val primaryText = Color(0.722f, 0.682f, 0.682f)
    val brightText = Color(0.816f, 0.780f, 0.780f)
    val disabledText = Color(0.176f, 0.216f, 0.216f)
    return ColorTheme(
        name = "Dark",
        background = Color(.4f, .4f, .4f),
        bigTextHint = Color.Gray,
        bigTextWarning = Color(red = 0.7f, green = 0.4f, blue = 0.4f),
        textFieldPlaceholder = Color.Gray,
        textFieldBackground = brightBackground,
        textFieldCursor = brightText,
        textFieldContent = brightText,
        searchTextFieldBackgroundNoResult = Color(red = 0.45f, green = 0.18f, blue = 0.18f),
        searchTextFieldBackgroundSomeResult = Color(red = 0.15f, green = 0.4f, blue = 0.15f),
        searchTextFieldBackgroundError = Color(red = 0.30f, green = 0.06f, blue = 0.06f),
        warningText = Color(red = 1f, green = .6f, blue = .15f),
        contextMenuBackground = Color(0.071f, 0.071f, 0.071f),
        contextMenuText = brightText,
        contextMenuDisabledText = disabledText,
        contextMenuDividerLine = Color.Gray,
        menuBarBackground = Color(red = 0.3f, green = 0.3f, blue = 0.3f),
        menuBarIconColor = Color.White,
        menuBarTextColor = Color.White,
        menuBarIconActivated = Color(red = 0.7f, green = 1f, blue = 0f),
        statusBarBackground = Color(red = 0.16f, green = 0.16f, blue = 0.16f),
        toggleButtonOnText = primaryText,
        toggleButtonDisabledText = Color(.4f, .4f, .4f),
        toggleButtonOnBackground = Color(.2f, .2f, .7f),
        toggleButtonOffBackground = Color(0f, 0f, .3f),
        readPositionBarNegativeBackground = Color(red = 0.19f, green = 0.19f, blue = 0.19f),
        readPositionBarPositiveBackground = Color(red = 0.85f, green = 0.6f, blue = 0f),
        readPositionBarText = Color.White,
        fileBodyTheme = FileBodyTheme(
            searchMatchBackground = Color(0.451f, 0.373f, 0.000f),
            selectionBackground = Color(red = 0.2f, green = 0.2f, blue = 0.8f, alpha = .6f),
            plainText = primaryText,
            background = primaryBackground,
        ),
        aboutDialogText = primaryText,
        dialogBackground = primaryBackground,
        dialogPrimary = Color(0.486f, 0.780f, 1.000f),
    )
}

@Composable
fun ThemePreference.toColorTheme(): ColorTheme {
    return when (selectedThemeType) {
        ThemeType.Light -> lightColorTheme()
        ThemeType.Dark -> darkColorTheme()
        null -> if (isSystemInDarkTheme()) darkColorTheme() else lightColorTheme()
    }
}
