package com.yushosei.newpipe.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import com.yushosei.newpipe.extractor.Info
import com.yushosei.newpipe.extractor.InfoItem
import com.yushosei.newpipe.extractor.ListExtractor
import com.yushosei.newpipe.extractor.NewPipe
import com.yushosei.newpipe.extractor.Page
import com.yushosei.newpipe.extractor.search.SearchInfo
import com.yushosei.newpipe.extractor.stream.StreamInfo
import com.yushosei.newpipe.extractor.suggestion.SuggestionExtractor

object ExtractorHelper {
    private val CACHE by lazy { InfoCache.instance }
    private val loadingUrls = mutableSetOf<String>()
    private val loadingLock = Mutex()

    private fun checkServiceId(serviceId: Int) {
        require(serviceId != NO_SERVICE_ID) { "serviceId is NO_SERVICE_ID" }
    }

    suspend fun searchFor(
        serviceId: Int, searchString: String,
        contentFilter: List<String>,
        sortFilter: String
    ): SearchInfo = withContext(Dispatchers.IO) {
        checkServiceId(serviceId)
        val service = NewPipe.getService(serviceId)
        return@withContext SearchInfo.getInfo(
            service,
            service.searchQHFactory.fromQuery(
                searchString,
                contentFilter,
                sortFilter
            )
        )
    }

    fun getMoreSearchItems(
        serviceId: Int,
        searchString: String,
        contentFilter: List<String>,
        sortFilter: String,
        page: Page?
    ): ListExtractor.InfoItemsPage<InfoItem> {
        checkServiceId(serviceId)
        return SearchInfo.getMoreItems(
            NewPipe.getService(serviceId),
            NewPipe.getService(serviceId)
                .searchQHFactory
                .fromQuery(searchString, contentFilter, sortFilter), page
        )
    }

    suspend fun suggestionsFor(serviceId: Int, query: String): List<String> =
        withContext(Dispatchers.IO) {
            checkServiceId(serviceId)
            val extractor: SuggestionExtractor = NewPipe.getService(serviceId).suggestionExtractor
            return@withContext extractor.suggestionList(query)
        }

    suspend fun isLoading(url: String): Boolean = loadingLock.withLock {
        loadingUrls.contains(url)
    }

    suspend fun getStreamInfo(
        serviceId: Int,
        url: String,
        forceLoad: Boolean
    ): StreamInfo = withContext(Dispatchers.IO) {
        checkServiceId(serviceId)
        loadingLock.withLock { loadingUrls.add(url) }
        
        try {
            val info = checkCache(
                forceLoad,
                serviceId,
                url,
                InfoItem.InfoType.STREAM
            )

            return@withContext info
        } finally {
            loadingLock.withLock { loadingUrls.remove(url) }
        }
    }

    private suspend fun checkCache(
        forceLoad: Boolean,
        serviceId: Int,
        url: String,
        infoType: InfoItem.InfoType,
    ): StreamInfo {
        if (forceLoad) {
            CACHE.removeInfo(serviceId, url, infoType)
            val info = StreamInfo.getInfo(NewPipe.getService(serviceId), url)
            CACHE.putInfo(serviceId, url, info, infoType)
            return info
        }

        return loadFromCache<StreamInfo>(serviceId, url, infoType)
            ?: StreamInfo.getInfo(NewPipe.getService(serviceId), url).also {
                CACHE.putInfo(serviceId, url, it, infoType)
            }
    }

    suspend fun clearCache() {
        CACHE.clearCache()
    }

    private suspend fun <I : Info?> loadFromCache(
        serviceId: Int,
        url: String,
        infoType: InfoItem.InfoType
    ): I? {
        checkServiceId(serviceId)
        @Suppress("UNCHECKED_CAST")
        return CACHE.getFromKey(serviceId, url, infoType) as I?
    }
}
