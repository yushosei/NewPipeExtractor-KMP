package com.yushosei.newpipe.extractor.linkhandler

import com.yushosei.newpipe.extractor.exceptions.ParsingException
import com.yushosei.newpipe.extractor.utils.Utils

abstract class ListLinkHandlerFactory : LinkHandlerFactory() {

    @Throws(ParsingException::class, UnsupportedOperationException::class)
    abstract fun getUrl(id: String, contentFilter: List<String>, sortFilter: String): String

    @Throws(ParsingException::class, UnsupportedOperationException::class)
    fun getUrl(
        id: String,
        contentFilter: List<String>,
        sortFilter: String,
        baseUrl: String?
    ): String {
        return getUrl(id, contentFilter, sortFilter)
    }

    
    override fun fromUrl(url: String): ListLinkHandler {
        val polishedUrl = Utils.followGoogleRedirectIfNeeded(url)
        val baseUrl = Utils.getBaseUrl(polishedUrl)
        return fromUrl(polishedUrl, baseUrl)
    }

    
    override fun fromUrl(url: String, baseUrl: String): ListLinkHandler {
        return ListLinkHandler(super.fromUrl(url, baseUrl))
    }

    
    override fun fromId(id: String): ListLinkHandler {
        return ListLinkHandler(super.fromId(id))
    }

    
    override fun fromId(id: String, baseUrl: String): ListLinkHandler {
        return ListLinkHandler(super.fromId(id, baseUrl))
    }

    
    open fun fromQuery(
        id: String,
        contentFilters: List<String>,
        sortFilter: String
    ): ListLinkHandler {
        val url = getUrl(id, contentFilters, sortFilter)
        return ListLinkHandler(url, url, id, contentFilters, sortFilter)
    }

    
    fun fromQuery(
        id: String,
        contentFilters: List<String>,
        sortFilter: String,
        baseUrl: String
    ): ListLinkHandler {
        val url = getUrl(id, contentFilters, sortFilter, baseUrl)
        return ListLinkHandler(url, url, id, contentFilters, sortFilter)
    }


    /**
     * For making ListLinkHandlerFactory compatible with LinkHandlerFactory we need to override
     * this, however it should not be overridden by the actual implementation.
     *
     * @return the url corresponding to id without any filters applied
     */
    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getUrl(id: String): String {
        return getUrl(id, ArrayList(0), "")
    }

    override fun getUrl(id: String, baseUrl: String): String {
        return getUrl(id, ArrayList(0), "", baseUrl)
    }

    open val availableContentFilter: List<String>
        /**
         * Will returns content filter the corresponding extractor can handle like "channels", "videos",
         * "music", etc.
         *
         * @return filter that can be applied when building a query for getting a list
         */
        get() = emptyList()

    val availableSortFilter: List<String>
        /**
         * Will returns sort filter the corresponding extractor can handle like "A-Z", "oldest first",
         * "size", etc.
         *
         * @return filter that can be applied when building a query for getting a list
         */
        get() = emptyList()
}
