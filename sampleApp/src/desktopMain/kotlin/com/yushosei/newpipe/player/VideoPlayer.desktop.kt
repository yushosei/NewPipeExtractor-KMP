package com.yushosei.newpipe.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color

@Composable
actual fun VideoPlayer(
    controller: MediaPlayerController,
    modifier: Modifier
) {
    val desktopController = controller as MediaPlayerControllerImpl
    SwingPanel(
        background = Color.Black,
        factory = { desktopController.mediaPlayerComponent },
        modifier = modifier
    )
}
