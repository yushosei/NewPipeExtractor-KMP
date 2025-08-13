package com.yushosei.newpipe.player

import org.koin.core.module.Module
import org.koin.dsl.module

internal actual fun platformModule(): Module {
    return module {
        single<MediaPlayerController> { MediaPlayerControllerImpl() }
    }
}

