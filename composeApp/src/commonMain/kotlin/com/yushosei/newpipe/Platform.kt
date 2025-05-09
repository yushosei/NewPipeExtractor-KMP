package com.yushosei.newpipe

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform