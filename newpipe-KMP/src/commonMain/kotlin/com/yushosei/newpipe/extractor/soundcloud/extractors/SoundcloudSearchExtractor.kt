package com.yushosei.newpipe.extractor.soundcloud.extractors

import com.yushosei.newpipe.extractor.InfoItem
import com.yushosei.newpipe.extractor.MetaInfo
import com.yushosei.newpipe.extractor.MultiInfoItemsCollector
import com.yushosei.newpipe.extractor.Page
import com.yushosei.newpipe.extractor.StreamingService
import com.yushosei.newpipe.extractor.downloader.Downloader
import com.yushosei.newpipe.extractor.exceptions.ExtractionException
import com.yushosei.newpipe.extractor.linkhandler.SearchQueryHandler
import com.yushosei.newpipe.extractor.search.SearchExtractor
import com.yushosei.newpipe.extractor.soundcloud.SoundcloudParsingHelper
import com.yushosei.newpipe.nanojson.JsonArray
import com.yushosei.newpipe.nanojson.JsonObject
import com.yushosei.newpipe.nanojson.JsonParser
import com.yushosei.newpipe.nanojson.JsonParserException

class SoundcloudSearchExtractor(
    service: StreamingService,
    linkHandler: SearchQueryHandler
) : SearchExtractor(service, linkHandler) {
    private var initialSearchObject: JsonObject? = null
    private var resolvedClientId: String? = null

    override val searchSuggestion: String?
        get() = null

    override val isCorrectedSearch: Boolean
        get() = false

    override val metaInfo: List<MetaInfo>
        get() = emptyList()

    override val initialPage: InfoItemsPage<InfoItem>
        get() {
            val responseObject = initialSearchObject
                ?: throw IllegalStateException("Search page is not fetched")
            val collector = collectTrackItems(responseObject.getArray(COLLECTION))
            if (collector.items.isEmpty()) {
                throw NothingFoundException("Nothing found")
            }

            val nextPageUrl = SoundcloudParsingHelper.getNextPageUrl(
                responseObject,
                resolvedClientId
            )

            return InfoItemsPage(
                collector,
                nextPageUrl?.let { Page(it) }
            )
        }

    override suspend fun getPage(page: Page?): InfoItemsPage<InfoItem> {
        require(!(page == null || page.url.isNullOrEmpty())) { "Page doesn't contain an URL" }

        val requestUrl = ensureRequestUrlHasClientId(page.url!!)
        val pageResponse = parseSearchResponse(downloader.get(requestUrl, extractorLocalization))
        val collector = collectTrackItems(pageResponse.getArray(COLLECTION))
        val nextPageUrl = SoundcloudParsingHelper.getNextPageUrl(pageResponse, resolvedClientId)

        return InfoItemsPage(collector, nextPageUrl?.let { Page(it) })
    }

    override suspend fun onFetchPage(downloader: Downloader) {
        resolvedClientId = SoundcloudParsingHelper.clientId()
        val initialUrl = ensureRequestUrlHasClientId(url ?: "")

        initialSearchObject = parseSearchResponse(
            downloader.get(initialUrl, extractorLocalization)
        )

        if (initialSearchObject!!.getArray(COLLECTION).isEmpty()) {
            throw NothingFoundException("Nothing found")
        }
    }

    private suspend fun ensureRequestUrlHasClientId(targetUrl: String): String {
        if (targetUrl.contains("client_id=")) {
            return targetUrl
        }
        val currentClientId = resolvedClientId ?: SoundcloudParsingHelper.clientId().also {
            resolvedClientId = it
        }
        return SoundcloudParsingHelper.withClientId(targetUrl, currentClientId)
    }

    private fun parseSearchResponse(response: com.yushosei.newpipe.extractor.downloader.Response): JsonObject {
        return try {
            JsonParser.`object`().from(response.responseBody())
        } catch (e: JsonParserException) {
            throw ExtractionException("Could not parse json response", e)
        }
    }

    private fun collectTrackItems(searchCollection: JsonArray): MultiInfoItemsCollector {
        val collector = MultiInfoItemsCollector(serviceId)

        for (result in searchCollection) {
            val searchResult = result as? JsonObject ?: continue
            val kind = searchResult.getString("kind", "")
            val isTrackResult = kind == "track" || (kind.isEmpty() && searchResult.has("media"))
            if (isTrackResult) {
                collector.commit(SoundcloudStreamInfoItemExtractor(searchResult))
            }
        }

        return collector
    }

    companion object {
        private const val COLLECTION = "collection"
    }
}
