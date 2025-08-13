package com.yushosei.newpipe.player

import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.component.CallbackMediaPlayerComponent
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent
import java.util.Locale

class MediaPlayerControllerImpl : MediaPlayerController {
    private var mediaPlayer: MediaPlayer? = null
    private var listener: MediaPlayerListener? = null

    private fun initMediaPlayer() {
        NativeDiscovery().discover()

        mediaPlayer =
                // see https://github.com/caprica/vlcj/issues/887#issuecomment-503288294 for why we're using CallbackMediaPlayerComponent for macOS.
            if (isMacOS()) {
                CallbackMediaPlayerComponent()
            } else {
                EmbeddedMediaPlayerComponent()
            }.mediaPlayer()

        mediaPlayer?.events()?.addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
            override fun mediaPlayerReady(mediaPlayer: MediaPlayer?) {
                super.mediaPlayerReady(mediaPlayer)
                listener?.onReady()
            }

            override fun finished(mediaPlayer: MediaPlayer?) {
                super.finished(mediaPlayer)
                listener?.onAudioCompleted()
            }

            override fun error(mediaPlayer: MediaPlayer?) {
                super.error(mediaPlayer)
                listener?.onError()
            }
        })

    }

    override fun prepare(
        item: MediaItem, listener: MediaPlayerListener
    ) {

        if (mediaPlayer == null) {
            initMediaPlayer()
            this.listener = listener
        }

        if (mediaPlayer?.status()?.isPlaying == true) {
            mediaPlayer?.controls()?.stop()
        }


        mediaPlayer?.media()?.play(item.url)
    }

    override fun start() {
        mediaPlayer?.controls()?.start()
    }

    override fun pause() {
        mediaPlayer?.controls()?.pause()
    }

    override fun seekTo(seconds: Long) {
        mediaPlayer?.controls()?.setTime(seconds)
    }

    override fun getCurrentPosition(): Long? {
        return mediaPlayer?.status()?.time()
    }

    override fun getDuration(): Long? {
        return mediaPlayer?.media()?.info()?.duration()
    }

    override fun stop() {
        mediaPlayer?.controls()?.stop()
    }

    override fun isPlaying(): Boolean {
        return mediaPlayer?.status()?.isPlaying ?: false
    }

    override fun release() {
        mediaPlayer?.release()
    }

    private fun Any.mediaPlayer(): MediaPlayer {
        return when (this) {
            is CallbackMediaPlayerComponent -> mediaPlayer()
            is EmbeddedMediaPlayerComponent -> mediaPlayer()
            else -> throw IllegalArgumentException("You can only call mediaPlayer() on vlcj player component")
        }
    }

    private fun isMacOS(): Boolean {
        val os = System.getProperty("os.name", "generic").lowercase(Locale.ENGLISH)
        return os.indexOf("mac") >= 0 || os.indexOf("darwin") >= 0
    }
}