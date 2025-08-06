package com.yushosei.newpipe.extractor.youtube.extractors

import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.charsets.forName
import io.ktor.utils.io.core.toByteArray
import kotlinx.io.IOException
import com.yushosei.newpipe.extractor.InfoItem
import com.yushosei.newpipe.extractor.MetaInfo
import com.yushosei.newpipe.extractor.MultiInfoItemsCollector
import com.yushosei.newpipe.extractor.Page
import com.yushosei.newpipe.extractor.StreamingService
import com.yushosei.newpipe.extractor.downloader.Downloader
import com.yushosei.newpipe.extractor.exceptions.ExtractionException
import com.yushosei.newpipe.extractor.linkhandler.SearchQueryHandler
import com.yushosei.newpipe.extractor.search.SearchExtractor
import com.yushosei.newpipe.extractor.youtube.YoutubeMetaInfoHelper
import com.yushosei.newpipe.extractor.youtube.YoutubeParsingHelper
import com.yushosei.newpipe.extractor.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import com.yushosei.newpipe.extractor.utils.JsonUtils
import com.yushosei.newpipe.extractor.utils.Utils
import com.yushosei.newpipe.nanojson.JsonArray
import com.yushosei.newpipe.nanojson.JsonObject
import com.yushosei.newpipe.nanojson.JsonWriter

/*
* Created by Christian Schabesberger on 22.07.2018
*
* Copyright (C) 2018 Christian Schabesberger <chris.schabesberger@mailbox.org>
* YoutubeSearchExtractor.java is part of NewPipe Extractor.
*
* NewPipe Extractor is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* NewPipe Extractor is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with NewPipe Extractor.  If not, see <http://www.gnu.org/licenses/>.
*/
class YoutubeSearchExtractor(
    service: StreamingService,
    linkHandler: SearchQueryHandler
) :
    SearchExtractor(service, linkHandler) {
    private val searchType: String?
    private val extractVideoResults: Boolean
    private val extractChannelResults: Boolean
    private val extractPlaylistResults: Boolean

    private var initialData: JsonObject? = null

    init {
        val contentFilters = linkHandler.contentFilters
        searchType = if (Utils.isNullOrEmpty(contentFilters)) null else contentFilters[0]
        // Save whether we should extract video, channel and playlist results depending on the
        // requested search type, as YouTube returns sometimes videos inside channel search results
        // If no search type is provided or ALL filter is requested, extract everything
        extractVideoResults =
            searchType == null || YoutubeSearchQueryHandlerFactory.ALL == searchType
                    || YoutubeSearchQueryHandlerFactory.VIDEOS == searchType
        extractChannelResults =
            searchType == null || YoutubeSearchQueryHandlerFactory.ALL == searchType
                    || YoutubeSearchQueryHandlerFactory.CHANNELS == searchType
        extractPlaylistResults =
            searchType == null || YoutubeSearchQueryHandlerFactory.ALL == searchType
                    || YoutubeSearchQueryHandlerFactory.PLAYLISTS == searchType
    }

    
    override fun onFetchPage(downloader: Downloader) {
        val query = super.searchString
        val localization = extractorLocalization
        val params = YoutubeSearchQueryHandlerFactory.getSearchParameter(searchType)

        val jsonBody = YoutubeParsingHelper.prepareDesktopJsonBuilder(
            localization,
            extractorContentCountry
        )
            .value("query", query)
        if (!Utils.isNullOrEmpty(params)) {
            jsonBody.value("params", params)
        }

        val body = JsonWriter.string(jsonBody.done()).toByteArray(Charsets.forName("UTF-8"))

        initialData = YoutubeParsingHelper.getJsonPostResponse("search", body, localization)
    }

    override val url: String?
        get() = super.url + "&gl=" + extractorContentCountry.countryCode


    override val searchSuggestion: String
        get() {
            val itemSectionRenderer =
                initialData!!.getObject("contents")
                    .getObject("twoColumnSearchResultsRenderer")
                    .getObject("primaryContents")
                    .getObject("sectionListRenderer")
                    .getArray("contents")
                    .getObject(0)
                    .getObject("itemSectionRenderer")
            val didYouMeanRenderer =
                itemSectionRenderer.getArray("contents")
                    .getObject(0)
                    .getObject("didYouMeanRenderer")

            if (!didYouMeanRenderer.isEmpty()) {
                return JsonUtils.getString(
                    didYouMeanRenderer,
                    "correctedQueryEndpoint.searchEndpoint.query"
                )
            }

            return YoutubeParsingHelper.getTextFromObject(
                itemSectionRenderer.getArray("contents")
                    .getObject(0)
                    .getObject("showingResultsForRenderer")
                    .getObject("correctedQuery")
            ) ?: ""
        }

    override val isCorrectedSearch: Boolean
        get() {
            val showingResultsForRenderer =
                initialData!!.getObject("contents")
                    .getObject("twoColumnSearchResultsRenderer").getObject("primaryContents")
                    .getObject("sectionListRenderer").getArray("contents").getObject(0)
                    .getObject("itemSectionRenderer").getArray("contents").getObject(0)
                    .getObject("showingResultsForRenderer")
            return !showingResultsForRenderer.isEmpty()
        }


    override val metaInfo: List<MetaInfo>
        get() = YoutubeMetaInfoHelper.getMetaInfo(
            initialData!!.getObject("contents")
                .getObject("twoColumnSearchResultsRenderer")
                .getObject("primaryContents")
                .getObject("sectionListRenderer")
                .getArray("contents")
        )


    
    override val initialPage: InfoItemsPage<InfoItem>
        get() {
            val collector = MultiInfoItemsCollector(serviceId)

            val sections = initialData!!.getObject("contents")
                .getObject("twoColumnSearchResultsRenderer")
                .getObject("primaryContents")
                .getObject("sectionListRenderer")
                .getArray("contents")

            var nextPage: Page? = null

            for (section in sections) {
                val sectionJsonObject = section as JsonObject
                if (sectionJsonObject.has("itemSectionRenderer")) {
                    val itemSectionRenderer =
                        sectionJsonObject.getObject("itemSectionRenderer")

                    collectStreamsFrom(collector, itemSectionRenderer.getArray("contents"))
                } else if (sectionJsonObject.has("continuationItemRenderer")) {
                    nextPage = getNextPageFrom(
                        sectionJsonObject.getObject("continuationItemRenderer")
                    )
                }
            }

            return InfoItemsPage(collector, nextPage)
        }

    
    override fun getPage(page: Page?): InfoItemsPage<InfoItem> {
        require(!(page == null || Utils.isNullOrEmpty(page.url))) { "Page doesn't contain an URL" }

        val localization = extractorLocalization
        val collector = MultiInfoItemsCollector(serviceId)

        // @formatter:off
        val json = JsonWriter.string(YoutubeParsingHelper.prepareDesktopJsonBuilder(localization, 
        extractorContentCountry)
        .value("continuation", page.id)
        .done())
        .toByteArray(Charsets.UTF_8)
        
                // @formatter:on
        val ajaxJson = YoutubeParsingHelper.getJsonPostResponse("search", json, localization)

        val continuationItems = ajaxJson.getArray("onResponseReceivedCommands")
            .getObject(0)
            .getObject("appendContinuationItemsAction")
            .getArray("continuationItems")

        val contents = continuationItems.getObject(0)
            .getObject("itemSectionRenderer")
            .getArray("contents")
        collectStreamsFrom(collector, contents)

        return InfoItemsPage(
            collector, getNextPageFrom(
                continuationItems.getObject(1)
                    .getObject("continuationItemRenderer")
            )
        )
    }

    @Throws(NothingFoundException::class)
    private fun collectStreamsFrom(
        collector: MultiInfoItemsCollector,
        contents: JsonArray
    ) {
        for (content in contents) {
            val item = content as JsonObject
            if (item.has("backgroundPromoRenderer")) {
                throw NothingFoundException(
                    YoutubeParsingHelper.getTextFromObject(
                        item.getObject("backgroundPromoRenderer")
                            .getObject("bodyText")
                    )
                )
            } else if (extractVideoResults && item.has("videoRenderer")) {
                collector.commit(
                    YoutubeStreamInfoItemExtractor(
                        item.getObject("videoRenderer")
                    )
                )
            }
        }
    }


    private fun getNextPageFrom(continuationItemRenderer: JsonObject): Page? {
        if (Utils.isNullOrEmpty(continuationItemRenderer)) {
            return null
        }

        val token = continuationItemRenderer.getObject("continuationEndpoint")
            .getObject("continuationCommand")
            .getString("token")

        val url =
            YoutubeParsingHelper.YOUTUBEI_V1_URL + "search?" + YoutubeParsingHelper.DISABLE_PRETTY_PRINT_PARAMETER

        return Page(url, token)
    }
}
