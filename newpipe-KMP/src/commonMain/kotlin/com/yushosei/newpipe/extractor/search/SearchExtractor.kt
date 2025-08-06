package com.yushosei.newpipe.extractor.search

import com.yushosei.newpipe.extractor.InfoItem
import com.yushosei.newpipe.extractor.ListExtractor
import com.yushosei.newpipe.extractor.MetaInfo
import com.yushosei.newpipe.extractor.StreamingService
import com.yushosei.newpipe.extractor.exceptions.ExtractionException
import com.yushosei.newpipe.extractor.exceptions.ParsingException
import com.yushosei.newpipe.extractor.linkhandler.SearchQueryHandler

abstract class SearchExtractor(service: StreamingService, linkHandler: SearchQueryHandler) :
    ListExtractor<InfoItem>(service, linkHandler) {
    class NothingFoundException(message: String?) : ExtractionException(message)

    val searchString: String
        get() = getLinkHandler().searchString

    
    abstract val searchSuggestion: String?

    override fun getLinkHandler(): SearchQueryHandler {
        return super.getLinkHandler() as SearchQueryHandler
    }

    override val name: String?
        get() = getLinkHandler().searchString

    
    abstract val isCorrectedSearch: Boolean

    
    abstract val metaInfo: List<MetaInfo>
}
