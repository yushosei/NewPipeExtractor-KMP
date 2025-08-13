package com.yushosei.newpipe.player

import androidx.core.net.toUri
import androidx.media3.common.MediaMetadata

fun MediaItem.toMediaItem(): androidx.media3.common.MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)
        .setArtworkUri(artworkUri?.toUri())
        .build()

    return androidx.media3.common.MediaItem.Builder()
        .setMediaId(url)
        .setUri(url)
        .setMediaMetadata(metadata)
        .build()
}