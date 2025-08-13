package com.yushosei.newpipe.player

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.common.Player.STATE_READY
import com.yushosei.newpipe.player.service.MediaService

class MediaPlayerControllerImpl(private val context: Context, private val player: Player) :
    MediaPlayerController {

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying)
                    startMediaServiceIfNeeded()
            }
        })
    }

    private fun startMediaServiceIfNeeded() {
        if (MediaService.isRunning) return
        val intent = Intent(context.applicationContext, MediaService::class.java)
        ContextCompat.startForegroundService(context.applicationContext, intent)
    }

    override fun prepare(
        item: com.yushosei.newpipe.player.MediaItem,
        listener: MediaPlayerListener
    ) {
        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
                listener.onError()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                when (playbackState) {
                    STATE_READY -> listener.onReady()
                    STATE_ENDED -> listener.onAudioCompleted()
                }
            }

            override fun onPlayerErrorChanged(error: PlaybackException?) {
                super.onPlayerErrorChanged(error)
                listener.onError()
            }
        })
        player.setMediaItem(item.toMediaItem())
        player.prepare()
        player.play()
    }

    override fun start() {
        player.play()
    }

    override fun pause() {
        if (player.isPlaying)
            player.pause()
    }

    override fun seekTo(seconds: Long) {
        if (player.isPlaying)
            player.seekTo(seconds)
    }

    override fun getCurrentPosition(): Long? {
        return player.currentPosition
    }

    override fun getDuration(): Long? {
        return player.duration
    }

    override fun stop() {
        player.stop()
    }

    override fun release() {
        player.release()
    }

    override fun isPlaying(): Boolean {
        return player.isPlaying
    }
}

