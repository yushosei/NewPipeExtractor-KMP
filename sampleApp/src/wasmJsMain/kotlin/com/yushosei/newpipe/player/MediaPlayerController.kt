package com.yushosei.newpipe.player

import kotlinx.browser.document
import org.w3c.dom.HTMLVideoElement

class MediaPlayerControllerImpl : MediaPlayerController {
    internal val videoElement = document.createElement("video") as HTMLVideoElement
    private var listener: MediaPlayerListener? = null

    init {
        videoElement.controls = true
        videoElement.style.backgroundColor = "black"
        videoElement.style.objectFit = "contain"
        videoElement.addEventListener("canplay", { listener?.onReady() })
        videoElement.addEventListener("ended", { listener?.onAudioCompleted() })
        videoElement.addEventListener("error", { listener?.onError() })
    }

    override fun prepare(item: MediaItem, listener: MediaPlayerListener) {
        this.listener = listener
        videoElement.src = item.url
        videoElement.load()
    }

    override fun start() {
        videoElement.play()
    }

    override fun pause() {
        videoElement.pause()
    }

    override fun seekTo(seconds: Long) {
        videoElement.currentTime = seconds.toDouble()
    }

    override fun getCurrentPosition(): Long = (videoElement.currentTime * 1_000).toLong()

    override fun getDuration(): Long = (videoElement.duration * 1_000).toLong()

    override fun stop() {
        videoElement.pause()
        videoElement.currentTime = 0.0
    }

    override fun isPlaying(): Boolean = !videoElement.paused

    override fun release() {
        stop()
        videoElement.remove()
    }

    internal fun attachVideoElement() {
        if (videoElement.parentElement == null) {
            document.body?.appendChild(videoElement)
        }
        videoElement.style.display = "block"
        videoElement.style.position = "absolute"
        videoElement.style.zIndex = "2"
    }

    internal fun detachVideoElement() {
        videoElement.style.display = "none"
        videoElement.remove()
    }

    internal fun updateVideoBounds(left: Float, top: Float, width: Float, height: Float) {
        videoElement.style.left = "${left}px"
        videoElement.style.top = "${top}px"
        videoElement.style.width = "${width}px"
        videoElement.style.height = "${height}px"
    }
}
