package com.yushosei.newpipe.player

import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.MediaSession
import com.yushosei.newpipe.player.notification.MusicNotificationManager
import org.koin.core.module.Module
import org.koin.dsl.module

@OptIn(UnstableApi::class)
internal actual fun platformModule(): Module {
    return module {
        single<AudioAttributes> {
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build()
        }

        single<Player> {
            ExoPlayer.Builder(get())
                .setAudioAttributes(get(), true)
                .setHandleAudioBecomingNoisy(true)
                .setTrackSelector(DefaultTrackSelector(get()))
                .build()
        }

        factory<MediaSession> {
            MediaSession.Builder(get(), get()).build()
        }

        single<MusicNotificationManager> {
            MusicNotificationManager(
                context = get(),
                exoPlayer = get()
            )
        }

        single<MediaPlayerController> {
            MediaPlayerControllerImpl(get(), get())
        }
    }
}