package com.yushosei.newpipe.player

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity

@Composable
actual fun VideoPlayer(
    controller: MediaPlayerController,
    modifier: Modifier
) {
    val webController = controller as MediaPlayerControllerImpl
    val density = LocalDensity.current

    DisposableEffect(webController) {
        webController.attachVideoElement()
        onDispose { webController.detachVideoElement() }
    }

    Box(
        modifier = modifier.onGloballyPositioned { coordinates ->
            val position = coordinates.positionInWindow()
            with(density) {
                webController.updateVideoBounds(
                    left = position.x.toDp().value,
                    top = position.y.toDp().value,
                    width = coordinates.size.width.toDp().value,
                    height = coordinates.size.height.toDp().value
                )
            }
        }
    )
}
