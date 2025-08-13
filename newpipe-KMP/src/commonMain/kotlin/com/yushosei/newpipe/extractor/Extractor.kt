package com.yushosei.newpipe.extractor

import kotlinx.io.IOException
import com.yushosei.newpipe.extractor.downloader.Downloader
import com.yushosei.newpipe.extractor.exceptions.ExtractionException
import com.yushosei.newpipe.extractor.exceptions.ParsingException
import com.yushosei.newpipe.extractor.linkhandler.LinkHandler
import com.yushosei.newpipe.extractor.localization.ContentCountry
import com.yushosei.newpipe.extractor.localization.Localization


abstract class Extractor protected constructor(
    val service: StreamingService,
    private val linkHandler: LinkHandler
) {
    /**
     * [StreamingService] currently related to this extractor.<br></br>
     * Useful for getting other things from a service (like the url handlers for
     * cleaning/accepting/get id from urls).
     */
    private var forcedLocalization: Localization? = null

    private var forcedContentCountry: ContentCountry? = null

    protected var isPageFetched: Boolean = false
        private set

    // called like this to prevent checkstyle errors about "hiding a field"

    val downloader: Downloader = NewPipe.downloader


    /**
     * @return The [LinkHandler] of the current extractor object (e.g. a ChannelExtractor
     * should return a channel url handler).
     */
    open fun getLinkHandler(): LinkHandler? {
        return linkHandler
    }

    /**
     * Fetch the current page.
     *
     * @throws IOException         if the page can not be loaded
     * @throws ExtractionException if the pages content is not understood
     */

    suspend fun fetchPage() {
        if (isPageFetched) {
            return
        }
        onFetchPage(downloader)
        isPageFetched = true
    }

    protected fun assertPageFetched() {
        check(isPageFetched) { "Page is not fetched. Make sure you call fetchPage()" }
    }

    /**
     * Fetch the current page.
     *
     * @param downloader the downloader to use
     * @throws IOException         if the page can not be loaded
     * @throws ExtractionException if the pages content is not understood
     */

    abstract suspend fun onFetchPage(downloader: Downloader)


    
    open val id: String?
        get() = linkHandler.id

    
    abstract val name: String?


    
    val originalUrl: String
        get() = linkHandler.originalUrl


    
    open val url: String?
        get() = linkHandler.url


    
    val baseUrl: String
        get() = linkHandler.baseUrl


    val serviceId: Int
        get() = service.serviceId

    /*//////////////////////////////////////////////////////////////////////////
    // Localization
    ////////////////////////////////////////////////////////////////////////// */
    fun forceLocalization(localization: Localization?) {
        this.forcedLocalization = localization
    }

    fun forceContentCountry(contentCountry: ContentCountry?) {
        this.forcedContentCountry = contentCountry
    }


    val extractorLocalization: Localization
        get() = if (forcedLocalization == null) service.localization else forcedLocalization!!


    val extractorContentCountry: ContentCountry
        get() = if (forcedContentCountry == null)
            service.contentCountry
        else
            forcedContentCountry!!

}
