package com.yushosei.newpipe.extractor.exceptions

class GeographicRestrictionException : ContentNotAvailableException {
    constructor(message: String?) : super(message)

    constructor(message: String?, cause: Throwable?) : super(message, cause)
}
