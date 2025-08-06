package com.yushosei.newpipe.extractor.linkhandler

import com.yushosei.newpipe.extractor.utils.Utils

open class LinkHandler(
    val originalUrl: String,
    val url: String,
    val id: String
) {
    constructor(handler: LinkHandler) : this(handler.originalUrl, handler.url, handler.id)


    val baseUrl: String
        get() = Utils.getBaseUrl(url)
}
