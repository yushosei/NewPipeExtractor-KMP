package com.yushosei.newpipe.extractor.exceptions

class YoutubeMusicPremiumContentException : ContentNotAvailableException {
    constructor() : super("This video is a YouTube Music Premium video")

    constructor(cause: Throwable?) : super("This video is a YouTube Music Premium video", cause)
}
