package com.yushosei.newpipe.extractor.exceptions

class AgeRestrictedContentException : ContentNotAvailableException {
    constructor(message: String?) : super(message)

    constructor(message: String?, cause: Throwable?) : super(message, cause)
}
