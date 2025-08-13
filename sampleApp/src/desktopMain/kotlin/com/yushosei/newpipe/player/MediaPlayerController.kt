package com.yushosei.newpipe.player

import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSlider
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener

class MediaPlayerControllerImpl : MediaPlayerController {
    private var mediaPlayer: MediaPlayer? = null
    private var listener: MediaPlayerListener? = null

    // UI 컴포넌트
    private var playPauseButton: JButton? = null
    private var positionSlider: JSlider? = null
    private var volumeSlider: JSlider? = null
    private var frame: JFrame? = null

    // 순환 참조 방지를 위한 플래그
    private var isUpdatingSliderFromEvent = false

    private fun initMediaPlayer() {
        val arch = System.getProperty("os.arch")
        val vlcPath = when {
            arch.contains("64") -> "C:\\Program Files\\VideoLAN\\VLC"
            arch.contains("86") -> "C:\\Program Files (x86)\\VideoLAN\\VLC"
            else -> throw IllegalStateException("Unsupported JVM architecture: $arch")
        }

        System.setProperty("jna.library.path", vlcPath)
        val currentPath = System.getenv("PATH") ?: ""
        val newPath = "$vlcPath;$currentPath"
        System.setProperty("PATH", newPath)

        NativeDiscovery().discover()

        // EmbeddedMediaPlayerComponent 생성
        val mediaPlayerComponent = EmbeddedMediaPlayerComponent()
        mediaPlayer = mediaPlayerComponent.mediaPlayer()

        frame = JFrame("Audio Player").apply {
            layout = BorderLayout()
            contentPane.add(mediaPlayerComponent, BorderLayout.CENTER)
            val controls = JPanel().apply {
                layout = FlowLayout()
            }

            playPauseButton = JButton("Play").apply {
                addActionListener {
                    if (mediaPlayer?.status()?.isPlaying == true) {
                        mediaPlayer?.controls()?.pause()
                        text = "Play"
                    } else {
                        mediaPlayer?.controls()?.play()
                        text = "Pause"
                    }
                }
            }
            controls.add(playPauseButton)

            positionSlider = JSlider(0, 1000, 0).apply {
                preferredSize = Dimension(200, 20)
                addChangeListener(object : ChangeListener {
                    override fun stateChanged(e: ChangeEvent?) {
                        if (!valueIsAdjusting || isUpdatingSliderFromEvent) return

                        val pos = value / 1000.0f
                        mediaPlayer?.controls()?.setPosition(pos)
                    }
                })
            }
            controls.add(positionSlider)

            volumeSlider = JSlider(0, 100, 100).apply {
                preferredSize = Dimension(100, 20)
                addChangeListener {
                    mediaPlayer?.audio()?.setVolume(value)
                }
            }
            controls.add(JLabel("Vol"))
            controls.add(volumeSlider)

            add(controls, BorderLayout.SOUTH)
            setSize(400, 120)
            setLocationRelativeTo(null)
            isVisible = true
            defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
        }

        mediaPlayer?.events()?.addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
            override fun mediaPlayerReady(mediaPlayer: MediaPlayer?) {
                super.mediaPlayerReady(mediaPlayer)
                listener?.onReady()
            }

            override fun finished(mediaPlayer: MediaPlayer?) {
                super.finished(mediaPlayer)
                listener?.onAudioCompleted()
                playPauseButton?.text = "Play"
            }

            override fun error(mediaPlayer: MediaPlayer?) {
                super.error(mediaPlayer)
                listener?.onError()
            }

            override fun positionChanged(mediaPlayer: MediaPlayer?, newPosition: Float) {
                super.positionChanged(mediaPlayer, newPosition)

                positionSlider?.let { slider ->
                    if (!slider.valueIsAdjusting) {
                        isUpdatingSliderFromEvent = true
                        slider.value = (newPosition * 1000).toInt()
                        isUpdatingSliderFromEvent = false
                    }
                }
            }
        })
    }

    override fun prepare(item: MediaItem, listener: MediaPlayerListener) {
        if (mediaPlayer == null) {
            this.listener = listener
            initMediaPlayer()
        }

        if (mediaPlayer?.status()?.isPlaying == true) {
            mediaPlayer?.controls()?.stop()
        }

        mediaPlayer?.media()?.play(item.url)
    }

    override fun start() {
        mediaPlayer?.controls()?.start()
        playPauseButton?.text = "Pause"
    }

    override fun pause() {
        mediaPlayer?.controls()?.pause()
        playPauseButton?.text = "Play"
    }

    override fun seekTo(seconds: Long) {
        mediaPlayer?.controls()?.setTime(seconds)
    }

    override fun getCurrentPosition(): Long? = mediaPlayer?.status()?.time()
    override fun getDuration(): Long? = mediaPlayer?.media()?.info()?.duration()

    override fun stop() {
        mediaPlayer?.controls()?.stop()
        playPauseButton?.text = "Play"
    }

    override fun isPlaying(): Boolean = mediaPlayer?.status()?.isPlaying ?: false

    override fun release() {
        mediaPlayer?.release()
        frame?.dispose()
    }
}