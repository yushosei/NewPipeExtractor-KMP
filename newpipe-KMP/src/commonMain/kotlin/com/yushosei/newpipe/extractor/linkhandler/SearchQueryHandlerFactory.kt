package com.yushosei.newpipe.extractor.linkhandler

import com.yushosei.newpipe.extractor.exceptions.ParsingException

abstract class SearchQueryHandlerFactory : ListLinkHandlerFactory() {

    @Throws(ParsingException::class, UnsupportedOperationException::class)
    abstract override fun getUrl(
        query: String,
        contentFilter: List<String>,
        sortFilter: String
    ): String

    @Suppress("unused")
    fun getSearchString(url: String?): String {
        return ""
    }

    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getId(url: String): String {
        return getSearchString(url)
    }

    
    override fun fromQuery(
        query: String,
        contentFilter: List<String>,
        sortFilter: String
    ): SearchQueryHandler {
        return SearchQueryHandler(super.fromQuery(query, contentFilter, sortFilter))
    }

    
    fun fromQuery(query: String): SearchQueryHandler {
        return fromQuery(query, emptyList(), "")
    }

    /**
     * It's not mandatory for NewPipe to handle the Url
     */
    override fun onAcceptUrl(url: String): Boolean {
        return false
    }
}
