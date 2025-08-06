package com.yushosei.newpipe.extractor

import com.yushosei.newpipe.extractor.linkhandler.ListLinkHandler

abstract class ListInfo<T : InfoItem> : Info {
    var relatedItems: List<T> = emptyList()
    
    var nextPage: Page? = null
    val contentFilters: List<String>
    val sortFilter: String?

    constructor(
        serviceId: Int,
        id: String,
        url: String,
        originalUrl: String,
        name: String,
        contentFilter: List<String>,
        sortFilter: String?
    ) : super(serviceId, id, url, originalUrl, name) {
        this.contentFilters = contentFilter
        this.sortFilter = sortFilter
    }

    constructor(
        serviceId: Int,
        listUrlIdHandler: ListLinkHandler,
        name: String
    ) : super(serviceId, listUrlIdHandler, name) {
        this.contentFilters = listUrlIdHandler.contentFilters
        this.sortFilter = listUrlIdHandler.sortFilter
    }

    fun hasNextPage(): Boolean {
        return Page.isValid(nextPage)
    }
}
