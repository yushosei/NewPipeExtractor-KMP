package com.yushosei.newpipe.player

data class MediaItem(
    val title: String,
    val artist: String,
    val artworkUri: String?,
    val url: String,
    val type: MediaType = MediaType.AUDIO
)

enum class MediaType {
    AUDIO,
    VIDEO
}
