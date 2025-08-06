package com.yushosei.newpipe

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "NewPipeExtractor-KMP",
    ) {
        App()
    }
}