package com.yushosei.newpipe.player

interface MediaPlayerController {
    fun prepare(item: MediaItem, listener: MediaPlayerListener)

    fun start()

    fun pause()

    fun stop()

    fun getCurrentPosition(): Long?

    fun getDuration(): Long?

    fun seekTo(seconds: Long)

    fun isPlaying(): Boolean

    fun release()
}