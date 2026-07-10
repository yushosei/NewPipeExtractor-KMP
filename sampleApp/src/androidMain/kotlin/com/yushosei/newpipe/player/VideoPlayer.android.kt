package com.yushosei.newpipe.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView

@Composable
actual fun VideoPlayer(
    controller: MediaPlayerController,
    modifier: Modifier
) {
    val androidController = controller as MediaPlayerControllerImpl
    AndroidView(
        modifier = modifier,
        factory = { context ->
            PlayerView(context).apply {
                player = androidController.player
                useController = true
            }
        },
        update = { playerView ->
            playerView.player = androidController.player
        }
    )
}
