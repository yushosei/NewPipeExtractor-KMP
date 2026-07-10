package com.yushosei.newpipe.player

import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent

class MediaPlayerControllerImpl : MediaPlayerController {
    private var listener: MediaPlayerListener? = null

    internal val mediaPlayerComponent: EmbeddedMediaPlayerComponent by lazy {
        NativeDiscovery().discover()
        EmbeddedMediaPlayerComponent().also { component ->
            component.mediaPlayer().events()
                .addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
                    override fun mediaPlayerReady(mediaPlayer: MediaPlayer?) {
                        listener?.onReady()
                    }

                    override fun finished(mediaPlayer: MediaPlayer?) {
                        listener?.onAudioCompleted()
                    }

                    override fun error(mediaPlayer: MediaPlayer?) {
                        listener?.onError()
                    }
                })
        }
    }

    private val mediaPlayer: MediaPlayer
        get() = mediaPlayerComponent.mediaPlayer()

    override fun prepare(item: MediaItem, listener: MediaPlayerListener) {
        this.listener = listener
        if (mediaPlayer.status().isPlaying) {
            mediaPlayer.controls().stop()
        }
        mediaPlayer.media().play(item.url)
    }

    override fun start() {
        mediaPlayer.controls().play()
    }

    override fun pause() {
        mediaPlayer.controls().pause()
    }

    override fun seekTo(seconds: Long) {
        mediaPlayer.controls().setTime(seconds * 1_000)
    }

    override fun getCurrentPosition(): Long = mediaPlayer.status().time()

    override fun getDuration(): Long = mediaPlayer.media().info().duration()

    override fun stop() {
        mediaPlayer.controls().stop()
    }

    override fun isPlaying(): Boolean = mediaPlayer.status().isPlaying

    override fun release() {
        mediaPlayerComponent.release()
    }
}
