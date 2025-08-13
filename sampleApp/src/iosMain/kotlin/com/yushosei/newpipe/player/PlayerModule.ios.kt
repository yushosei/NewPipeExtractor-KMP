package com.yushosei.newpipe.player

import org.koin.core.module.Module

internal actual fun platformModule(): Module {
    return org.koin.dsl.module {
        single<MediaPlayerController> {
            MediaPlayerControllerImpl()
        }
    }
}

