package com.yushosei.newpipe.player

interface MediaPlayerListener {
    fun onReady()
    fun onAudioCompleted()
    fun onError()
}
