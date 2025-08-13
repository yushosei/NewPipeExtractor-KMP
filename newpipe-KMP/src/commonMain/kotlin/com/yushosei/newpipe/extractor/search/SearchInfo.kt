package com.yushosei.newpipe.extractor.search

import com.yushosei.newpipe.extractor.InfoItem
import com.yushosei.newpipe.extractor.ListExtractor.InfoItemsPage
import com.yushosei.newpipe.extractor.ListInfo
import com.yushosei.newpipe.extractor.MetaInfo
import com.yushosei.newpipe.extractor.Page
import com.yushosei.newpipe.extractor.StreamingService
import com.yushosei.newpipe.extractor.linkhandler.SearchQueryHandler
import com.yushosei.newpipe.extractor.utils.ExtractorHelper

class SearchInfo(
    serviceId: Int,
    qIHandler: SearchQueryHandler,
    // Getter
    val searchString: String
) : ListInfo<InfoItem>(serviceId, qIHandler, "Search") {
    var searchSuggestion: String? = null
    var isCorrectedSearch: Boolean = false
    var metaInfo: List<MetaInfo> = listOf()


    companion object {

        suspend fun getInfo(
            service: StreamingService,
            searchQuery: SearchQueryHandler
        ): SearchInfo {
            val extractor = service.getSearchExtractor(searchQuery)
            extractor.fetchPage()
            return getInfo(extractor)
        }


        fun getInfo(extractor: SearchExtractor): SearchInfo {
            val info = SearchInfo(
                extractor.serviceId,
                extractor.getLinkHandler(),
                extractor.searchString
            )

            try {
                info.originalUrl = extractor.originalUrl
            } catch (e: Exception) {
                info.addError(e)
            }
            try {
                info.searchSuggestion = extractor.searchSuggestion
            } catch (e: Exception) {
                info.addError(e)
            }
            try {
                info.isCorrectedSearch = extractor.isCorrectedSearch
            } catch (e: Exception) {
                info.addError(e)
            }
            try {
                info.metaInfo = extractor.metaInfo
            } catch (e: Exception) {
                info.addError(e)
            }

            val page = ExtractorHelper.getItemsPageOrLogError(info, extractor)
            info.relatedItems = page.items
            info.nextPage = page.nextPage

            return info
        }


        suspend fun getMoreItems(
            service: StreamingService,
            query: SearchQueryHandler,
            page: Page?
        ): InfoItemsPage<InfoItem> {
            return service.getSearchExtractor(query).getPage(page)
        }
    }
}
