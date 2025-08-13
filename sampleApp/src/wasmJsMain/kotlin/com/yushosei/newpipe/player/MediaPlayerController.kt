package com.yushosei.newpipe.player

import kotlinx.browser.document
import org.w3c.dom.HTMLAudioElement

class MediaPlayerControllerImpl : MediaPlayerController {
    private val audioElement = document.createElement("audio") as HTMLAudioElement

    override fun prepare(
        item: MediaItem,
        listener: MediaPlayerListener
    ) {
        audioElement.src = item.url
        audioElement.addEventListener("canplaythrough", {
            // Audio is ready to play without interruption
            listener.onReady()
            audioElement.play()
        })

        audioElement.onended = {
            listener.onAudioCompleted()
        }
        audioElement.addEventListener("error", {
            listener.onError()
        })

    }

    override fun start() {
        audioElement.play()
    }

    override fun pause() {
        audioElement.pause()
    }

    override fun seekTo(seconds: Long) {
        audioElement.fastSeek(seconds.toDouble())
    }

    override fun getCurrentPosition(): Long? {
        return (audioElement.currentTime * 1000).toLong()
    }

    override fun getDuration(): Long? {
        return (audioElement.duration * 1000).toLong()
    }

    override fun stop() {
    }

    override fun isPlaying(): Boolean {
        return !audioElement.paused
    }

    override fun release() {
    }
}
