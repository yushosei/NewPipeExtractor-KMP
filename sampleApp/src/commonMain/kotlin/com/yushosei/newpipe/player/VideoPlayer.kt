package com.yushosei.newpipe.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun VideoPlayer(
    controller: MediaPlayerController,
    modifier: Modifier = Modifier
)
