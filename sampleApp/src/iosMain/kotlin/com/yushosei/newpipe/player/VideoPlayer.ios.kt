package com.yushosei.newpipe.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVLayerVideoGravityResizeAspect
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerLayer
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIColor
import platform.UIKit.UIView

@OptIn(ExperimentalForeignApi::class)
private class PlayerContainerView(player: AVPlayer) : UIView(
    frame = CGRectMake(0.0, 0.0, 0.0, 0.0)
) {
    private val playerLayer = AVPlayerLayer().apply {
        this.player = player
        videoGravity = AVLayerVideoGravityResizeAspect
    }

    init {
        backgroundColor = UIColor.blackColor
        layer.addSublayer(playerLayer)
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        playerLayer.frame = bounds
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun VideoPlayer(
    controller: MediaPlayerController,
    modifier: Modifier
) {
    val iosController = controller as MediaPlayerControllerImpl
    UIKitView(
        factory = { PlayerContainerView(iosController.player) },
        modifier = modifier,
        interactive = true
    )
}
