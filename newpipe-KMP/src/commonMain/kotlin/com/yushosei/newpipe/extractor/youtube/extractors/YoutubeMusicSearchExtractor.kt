package com.yushosei.newpipe.extractor.youtube.extractors

import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.core.toByteArray
import kotlinx.io.IOException
import com.yushosei.newpipe.extractor.InfoItem
import com.yushosei.newpipe.extractor.MetaInfo
import com.yushosei.newpipe.extractor.MultiInfoItemsCollector
import com.yushosei.newpipe.extractor.Page
import com.yushosei.newpipe.extractor.StreamingService
import com.yushosei.newpipe.extractor.downloader.Downloader
import com.yushosei.newpipe.extractor.exceptions.ExtractionException
import com.yushosei.newpipe.extractor.exceptions.ParsingException
import com.yushosei.newpipe.extractor.linkhandler.SearchQueryHandler
import com.yushosei.newpipe.extractor.search.SearchExtractor
import com.yushosei.newpipe.extractor.youtube.YoutubeParsingHelper
import com.yushosei.newpipe.extractor.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import com.yushosei.newpipe.extractor.utils.JsonUtils
import com.yushosei.newpipe.extractor.utils.Utils
import com.yushosei.newpipe.nanojson.JsonArray
import com.yushosei.newpipe.nanojson.JsonObject
import com.yushosei.newpipe.nanojson.JsonParser
import com.yushosei.newpipe.nanojson.JsonParserException
import com.yushosei.newpipe.nanojson.JsonWriter

class YoutubeMusicSearchExtractor(
    service: StreamingService,
    linkHandler: SearchQueryHandler
) :
    SearchExtractor(service, linkHandler) {
    private var initialData: JsonObject? = null

    
    override suspend fun onFetchPage(downloader: Downloader) {
        val url = ("https://music.youtube.com/youtubei/v1/search?"
                + YoutubeParsingHelper.DISABLE_PRETTY_PRINT_PARAMETER)

        val params = when (getLinkHandler().contentFilters[0]) {
            YoutubeSearchQueryHandlerFactory.MUSIC_SONGS -> "Eg-KAQwIARAAGAAgACgAMABqChAEEAUQAxAKEAk%3D"
            YoutubeSearchQueryHandlerFactory.MUSIC_VIDEOS -> "Eg-KAQwIABABGAAgACgAMABqChAEEAUQAxAKEAk%3D"
            YoutubeSearchQueryHandlerFactory.MUSIC_ALBUMS -> "Eg-KAQwIABAAGAEgACgAMABqChAEEAUQAxAKEAk%3D"
            YoutubeSearchQueryHandlerFactory.MUSIC_PLAYLISTS -> "Eg-KAQwIABAAGAAgACgBMABqChAEEAUQAxAKEAk%3D"
            YoutubeSearchQueryHandlerFactory.MUSIC_ARTISTS -> "Eg-KAQwIABAAGAAgASgAMABqChAEEAUQAxAKEAk%3D"
            else -> null
        }

        // @formatter:off
        val json = JsonWriter.string()
        .`object`()
        .`object`("context")
        .`object`("client")
        .value("clientName", "WEB_REMIX")
        .value("clientVersion", YoutubeParsingHelper.getYoutubeMusicClientVersion())
        .value("hl", "en-GB")
        .value("gl", extractorContentCountry.countryCode)
        .value("platform", "DESKTOP")
        .value("utcOffsetMinutes", 0)
        .end()
        .`object`("request")
        .array("internalExperimentFlags")
        .end()
        .value("useSsl", true)
        .end()
        .`object`("user") // TODO: provide a way to enable restricted mode with:
 //  .value("enableSafetyMode", boolean)
        .value("lockedSafetyMode", false)
        .end()
        .end()
        .value("query", searchString)
        .value("params", params)
        .end().done().toByteArray(Charsets.UTF_8)
        
                // @formatter:on
        val responseBody = YoutubeParsingHelper.getValidJsonResponseBody(
            downloader.postWithContentTypeJson(
                url,
                YoutubeParsingHelper.youtubeMusicHeaders,
                json
            )
        )

        try {
            initialData = JsonParser.`object`().from(responseBody)
        } catch (e: JsonParserException) {
            throw ParsingException("Could not parse JSON", e)
        }
    }

    private val itemSectionRendererContents: List<JsonObject>
        get() = initialData?.let {
            it.getObject("contents")
                .getObject("tabbedSearchResultsRenderer")
                .getArray("tabs")
                .getObject(0)
                .getObject("tabRenderer")
                .getObject("content")
                .getObject("sectionListRenderer")
                .getArray("contents")
                .filterIsInstance<JsonObject>()
                .mapNotNull { it.getObject("itemSectionRenderer").takeIf { isr -> !isr.isEmpty() } }
                .map { it.getArray("contents").getObject(0) }
        } ?: emptyList()


    override val searchSuggestion: String
        get() {
            for (obj in itemSectionRendererContents) {
                val didYouMeanRenderer = obj
                    .getObject("didYouMeanRenderer")
                val showingResultsForRenderer = obj
                    .getObject("showingResultsForRenderer")

                if (!didYouMeanRenderer.isEmpty()) {
                    return YoutubeParsingHelper.getTextFromObject(didYouMeanRenderer.getObject("correctedQuery"))
                        ?: ""
                } else if (!showingResultsForRenderer.isEmpty()) {
                    return JsonUtils.getString(
                        showingResultsForRenderer,
                        "correctedQueryEndpoint.searchEndpoint.query"
                    )
                }
            }

            return ""
        }


    override val isCorrectedSearch: Boolean
        get() = itemSectionRendererContents.any { obj: JsonObject -> obj.has("showingResultsForRenderer") }


    override val metaInfo: List<MetaInfo>
        get() = emptyList()
    
    override val initialPage: InfoItemsPage<InfoItem>
        get() {
            val collector = MultiInfoItemsCollector(serviceId)

            val contents = JsonUtils.getArray(
                JsonUtils.getArray(
                    initialData,
                    "contents.tabbedSearchResultsRenderer.tabs"
                ).getObject(0),
                "tabRenderer.content.sectionListRenderer.contents"
            )

            var nextPage: Page? = null

            for (content in contents) {
                if ((content as JsonObject).has("musicShelfRenderer")) {
                    val musicShelfRenderer =
                        content
                            .getObject("musicShelfRenderer")

                    collectMusicStreamsFrom(collector, musicShelfRenderer.getArray("contents"))

                    nextPage = getNextPageFrom(musicShelfRenderer.getArray("continuations"))
                }
            }

            return InfoItemsPage(collector, nextPage)
        }

    
    override suspend fun getPage(page: Page?): InfoItemsPage<InfoItem> {
        require(!(page == null || Utils.isNullOrEmpty(page.url))) { "Page doesn't contain an URL" }

        val collector = MultiInfoItemsCollector(serviceId)

        // @formatter:off
        val json = JsonWriter.string()
        .`object`()
        .`object`("context")
        .`object`("client")
        .value("clientName", "WEB_REMIX")
        .value("clientVersion", YoutubeParsingHelper.getYoutubeMusicClientVersion())
        .value("hl", "en-GB")
        .value("gl", extractorContentCountry.countryCode)
        .value("platform", "DESKTOP")
        .value("utcOffsetMinutes", 0)
        .end()
        .`object`("request")
        .array("internalExperimentFlags")
        .end()
        .value("useSsl", true)
        .end()
        .`object`("user") // TODO: provide a way to enable restricted mode with:
 //  .value("enableSafetyMode", boolean)
        .value("lockedSafetyMode", false)
        .end()
        .end()
        .end().done().toByteArray(Charsets.UTF_8)
        
                // @formatter:on
        val responseBody = YoutubeParsingHelper.getValidJsonResponseBody(
            downloader.postWithContentTypeJson(
                page.url, YoutubeParsingHelper.youtubeMusicHeaders, json
            )
        )

        val ajaxJson: JsonObject
        try {
            ajaxJson = JsonParser.`object`().from(responseBody)
        } catch (e: JsonParserException) {
            throw ParsingException("Could not parse JSON", e)
        }

        val musicShelfContinuation = ajaxJson.getObject("continuationContents")
            .getObject("musicShelfContinuation")

        collectMusicStreamsFrom(collector, musicShelfContinuation.getArray("contents"))
        val continuations = musicShelfContinuation.getArray("continuations")

        return InfoItemsPage(collector, getNextPageFrom(continuations))
    }

    private fun collectMusicStreamsFrom(
        collector: MultiInfoItemsCollector,
        videos: JsonArray
    ) {
        val searchType = getLinkHandler().contentFilters[0]

        videos
            .filterIsInstance<JsonObject>() // Only JsonObjects
            .mapNotNull {
                it.getObject(
                    "musicResponsiveListItemRenderer",
                    null
                )
            } // nullable → skip nulls
            .forEach { infoItem ->
                val displayPolicy = infoItem.getString("musicItemRendererDisplayPolicy", "")
                if (displayPolicy == "MUSIC_ITEM_RENDERER_DISPLAY_POLICY_GREY_OUT") {
                    // No info about URL available
                    return@forEach
                }

                val descriptionElements = infoItem
                    .getArray("flexColumns")
                    .getObject(1)
                    .getObject("musicResponsiveListItemFlexColumnRenderer")
                    .getObject("text")
                    .getArray("runs")

                when (searchType) {
                    YoutubeSearchQueryHandlerFactory.MUSIC_SONGS,
                    YoutubeSearchQueryHandlerFactory.MUSIC_VIDEOS -> {
                        collector.commit(
                            YoutubeMusicSongOrVideoInfoItemExtractor(
                                infoItem,
                                descriptionElements,
                                searchType
                            )
                        )
                    }
                }
            }
    }


    private fun getNextPageFrom(continuations: JsonArray): Page? {
        if (continuations.isEmpty()) {
            return null
        }

        val nextContinuationData = continuations.getObject(0)
            .getObject("nextContinuationData")
        val continuation = nextContinuationData.getString("continuation")

        return Page(
            ("https://music.youtube.com/youtubei/v1/search?ctoken=" + continuation
                    + "&continuation=" + continuation + "&" + YoutubeParsingHelper.DISABLE_PRETTY_PRINT_PARAMETER)
        )
    }
}
