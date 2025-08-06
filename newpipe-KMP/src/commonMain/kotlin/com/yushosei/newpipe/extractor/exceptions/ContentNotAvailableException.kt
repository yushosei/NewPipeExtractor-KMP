package com.yushosei.newpipe.extractor.exceptions

open class ContentNotAvailableException : ParsingException {
    constructor(message: String?) : super(message)

    constructor(message: String?, cause: Throwable?) : super(message, cause)
}
