package com.yushosei.newpipe.extractor.linkhandler

open class ListLinkHandler(
    originalUrl: String,
    url: String,
    id: String,
    val contentFilters: List<String>,
    val sortFilter: String?
) : LinkHandler(originalUrl, url, id) {

    constructor(handler: ListLinkHandler) : this(
        handler.originalUrl,
        handler.url,
        handler.id,
        handler.contentFilters,
        handler.sortFilter
    )

    constructor(handler: LinkHandler) : this(
        handler.originalUrl,
        handler.url,
        handler.id,
        emptyList<String>(),
        ""
    )
}
