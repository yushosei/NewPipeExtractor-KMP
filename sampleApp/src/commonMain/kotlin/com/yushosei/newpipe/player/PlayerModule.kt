package com.yushosei.newpipe.player

import org.koin.core.module.Module
import org.koin.dsl.module

internal expect fun platformModule(): Module

val playerModule: Module = module {
    includes(platformModule())
}