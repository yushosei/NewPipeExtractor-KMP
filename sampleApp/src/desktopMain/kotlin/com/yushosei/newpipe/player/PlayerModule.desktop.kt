package com.yushosei.newpipe.player

import org.koin.core.module.Module

internal actual fun platformModule(): Module {
    return org.koin.dsl.module {
        // Define platform-specific dependencies here
        // For example, you might want to provide a PlayerController implementation for desktop
        single<MediaPlayerController> { MediaPlayerControllerImpl() }
    }
}
