package com.yushosei.newpipe.extractor.youtube

import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.core.toByteArray
import com.yushosei.newpipe.extractor.NewPipe
import com.yushosei.newpipe.extractor.localization.ContentCountry
import com.yushosei.newpipe.extractor.localization.Localization
import com.yushosei.newpipe.extractor.youtube.InnertubeClientRequestInfo.Companion.ofAndroidClient
import com.yushosei.newpipe.extractor.youtube.InnertubeClientRequestInfo.Companion.ofIosClient
import com.yushosei.newpipe.extractor.youtube.InnertubeClientRequestInfo.Companion.ofTvHtml5Client
import com.yushosei.newpipe.extractor.youtube.InnertubeClientRequestInfo.Companion.ofWebClient
import com.yushosei.newpipe.extractor.youtube.InnertubeClientRequestInfo.Companion.ofWebEmbeddedPlayerClient
import com.yushosei.newpipe.extractor.youtube.YoutubeParsingHelper.generateTParameter
import com.yushosei.newpipe.extractor.youtube.YoutubeParsingHelper.getAndroidUserAgent
import com.yushosei.newpipe.extractor.youtube.YoutubeParsingHelper.getClientHeaders
import com.yushosei.newpipe.extractor.youtube.YoutubeParsingHelper.getClientVersion
import com.yushosei.newpipe.extractor.youtube.YoutubeParsingHelper.getIosUserAgent
import com.yushosei.newpipe.extractor.youtube.YoutubeParsingHelper.getOriginReferrerHeaders
import com.yushosei.newpipe.extractor.youtube.YoutubeParsingHelper.getValidJsonResponseBody
import com.yushosei.newpipe.extractor.youtube.YoutubeParsingHelper.getVisitorDataFromInnertube
import com.yushosei.newpipe.extractor.youtube.YoutubeParsingHelper.prepareJsonBuilder
import com.yushosei.newpipe.extractor.youtube.YoutubeParsingHelper.youTubeHeaders
import com.yushosei.newpipe.extractor.utils.JsonUtils
import com.yushosei.newpipe.extractor.youtube.ClientsConstants
import com.yushosei.newpipe.nanojson.JsonBuilder
import com.yushosei.newpipe.nanojson.JsonObject
import com.yushosei.newpipe.nanojson.JsonWriter

internal object YoutubeStreamHelper {
    private const val PLAYER = "player"
    private const val SERVICE_INTEGRITY_DIMENSIONS = "serviceIntegrityDimensions"
    private const val PO_TOKEN = "poToken"
    private const val BASE_YT_DESKTOP_WATCH_URL = "https://www.youtube.com/watch?v="

    
    fun getWebMetadataPlayerResponse(
        localization: Localization,
        contentCountry: ContentCountry,
        videoId: String
    ): JsonObject {
        val innertubeClientRequestInfo =
            ofWebClient()
        innertubeClientRequestInfo.clientInfo.clientVersion = getClientVersion()!!

        val headers = youTubeHeaders

        // We must always pass a valid visitorData to get valid player responses, which needs to be
        // got from YouTube
        innertubeClientRequestInfo.clientInfo.visitorData =
            getVisitorDataFromInnertube(
                innertubeClientRequestInfo,
                localization,
                contentCountry,
                headers,
                YoutubeParsingHelper.YOUTUBEI_V1_URL,
                null,
                false
            )

        val builder = prepareJsonBuilder(
            localization, contentCountry,
            innertubeClientRequestInfo, null
        )

        addVideoIdCpnAndOkChecks(builder, videoId, null)

        val body = JsonWriter.string(builder.done())
            .toByteArray(Charsets.UTF_8)

        val url: String =
            (YoutubeParsingHelper.YOUTUBEI_V1_URL + PLAYER + "?" + YoutubeParsingHelper.DISABLE_PRETTY_PRINT_PARAMETER
                    + "&\$fields=microformat,playabilityStatus,storyboards,videoDetails")

        return JsonUtils.toJsonObject(
            getValidJsonResponseBody(
                NewPipe.downloader.postWithContentTypeJson(
                    url, headers, body, localization
                )
            )
        )
    }


    
    fun getTvHtml5PlayerResponse(
        localization: Localization,
        contentCountry: ContentCountry,
        videoId: String,
        cpn: String?,
        signatureTimestamp: Int
    ): JsonObject {
        val innertubeClientRequestInfo =
            ofTvHtml5Client()

        val headers: MutableMap<String, List<String>> = HashMap<String, List<String>>(
            getClientHeaders(
                ClientsConstants.TVHTML5_CLIENT_ID,
                ClientsConstants.TVHTML5_CLIENT_VERSION
            )
        )
        headers.putAll(getOriginReferrerHeaders("https://www.youtube.com"))
        headers["User-Agent"] = listOf<String>(ClientsConstants.TVHTML5_USER_AGENT)

        // We must always pass a valid visitorData to get valid player responses, which needs to be
        // got from YouTube
        // For some reason, the TVHTML5 client doesn't support the visitor_id endpoint, use the
        // guide one instead, which is quite lightweight
        innertubeClientRequestInfo.clientInfo.visitorData =
            getVisitorDataFromInnertube(
                innertubeClientRequestInfo,
                localization,
                contentCountry,
                headers,
                YoutubeParsingHelper.YOUTUBEI_V1_URL,
                null,
                true
            )

        val builder = prepareJsonBuilder(
            localization, contentCountry,
            innertubeClientRequestInfo, null
        )

        addVideoIdCpnAndOkChecks(builder, videoId, cpn)

        addPlaybackContext(builder, BASE_YT_DESKTOP_WATCH_URL + videoId, signatureTimestamp)

        val body = JsonWriter.string(builder.done())
            .toByteArray(Charsets.UTF_8)

        val url: String =
            YoutubeParsingHelper.YOUTUBEI_V1_URL + PLAYER + "?" + YoutubeParsingHelper.DISABLE_PRETTY_PRINT_PARAMETER

        return JsonUtils.toJsonObject(
            getValidJsonResponseBody(
                NewPipe.downloader.postWithContentTypeJson(url, headers, body, localization)
            )
        )
    }


    
    fun getWebFullPlayerResponse(
        localization: Localization,
        contentCountry: ContentCountry,
        videoId: String,
        cpn: String?,
        webPoTokenResult: PoTokenResult,
        signatureTimestamp: Int
    ): JsonObject {
        val innertubeClientRequestInfo =
            ofWebClient()
        innertubeClientRequestInfo.clientInfo.clientVersion = getClientVersion()!!
        innertubeClientRequestInfo.clientInfo.visitorData = webPoTokenResult.visitorData

        val builder = prepareJsonBuilder(
            localization, contentCountry,
            innertubeClientRequestInfo, null
        )

        addVideoIdCpnAndOkChecks(builder, videoId, cpn)

        addPlaybackContext(builder, BASE_YT_DESKTOP_WATCH_URL + videoId, signatureTimestamp)

        addPoToken(builder, webPoTokenResult.playerRequestPoToken)

        val body = JsonWriter.string(builder.done())
            .toByteArray(Charsets.UTF_8)

        val url: String =
            YoutubeParsingHelper.YOUTUBEI_V1_URL + PLAYER + "?" + YoutubeParsingHelper.DISABLE_PRETTY_PRINT_PARAMETER

        return JsonUtils.toJsonObject(
            getValidJsonResponseBody(
                NewPipe.downloader.postWithContentTypeJson(
                    url, youTubeHeaders, body, localization
                )
            )
        )
    }


    
    fun getWebEmbeddedPlayerResponse(
        localization: Localization,
        contentCountry: ContentCountry,
        videoId: String,
        cpn: String?,
        webEmbeddedPoTokenResult: PoTokenResult?,
        signatureTimestamp: Int
    ): JsonObject {
        val innertubeClientRequestInfo =
            ofWebEmbeddedPlayerClient()

        val headers: MutableMap<String, List<String>> = HashMap<String, List<String>>(
            getClientHeaders(
                ClientsConstants.WEB_EMBEDDED_CLIENT_ID,
                ClientsConstants.WEB_EMBEDDED_CLIENT_VERSION
            )
        )
        headers.putAll(getOriginReferrerHeaders("https://www.youtube.com"))

        val embedUrl = BASE_YT_DESKTOP_WATCH_URL + videoId

        // We must always pass a valid visitorData to get valid player responses, which needs to be
        // got from YouTube
        innertubeClientRequestInfo.clientInfo.visitorData = webEmbeddedPoTokenResult?.visitorData
            ?: getVisitorDataFromInnertube(
                innertubeClientRequestInfo,
                localization,
                contentCountry,
                headers,
                YoutubeParsingHelper.YOUTUBEI_V1_URL,
                embedUrl,
                false
            )

        val builder = prepareJsonBuilder(
            localization, contentCountry,
            innertubeClientRequestInfo, embedUrl
        )

        addVideoIdCpnAndOkChecks(builder, videoId, cpn)

        addPlaybackContext(builder, embedUrl, signatureTimestamp)

        if (webEmbeddedPoTokenResult != null) {
            addPoToken(builder, webEmbeddedPoTokenResult.playerRequestPoToken)
        }

        val body = JsonWriter.string(builder.done())
            .toByteArray(Charsets.UTF_8)
        val url: String =
            YoutubeParsingHelper.YOUTUBEI_V1_URL + PLAYER + "?" + YoutubeParsingHelper.DISABLE_PRETTY_PRINT_PARAMETER

        return JsonUtils.toJsonObject(
            getValidJsonResponseBody(
                NewPipe.downloader.postWithContentTypeJson(url, headers, body, localization)
            )
        )
    }

    
    fun getAndroidPlayerResponse(
        contentCountry: ContentCountry,
        localization: Localization,
        videoId: String,
        cpn: String?,
        androidPoTokenResult: PoTokenResult
    ): JsonObject {
        val innertubeClientRequestInfo =
            ofAndroidClient()
        innertubeClientRequestInfo.clientInfo.visitorData = androidPoTokenResult.visitorData

        val headers =
            getMobileClientHeaders(getAndroidUserAgent(localization))

        val builder = prepareJsonBuilder(
            localization, contentCountry,
            innertubeClientRequestInfo, null
        )

        addVideoIdCpnAndOkChecks(builder, videoId, cpn)

        addPoToken(builder, androidPoTokenResult.playerRequestPoToken)

        val body = JsonWriter.string(builder.done())
            .toByteArray(Charsets.UTF_8)

        val url: String =
            (YoutubeParsingHelper.YOUTUBEI_V1_GAPIS_URL + PLAYER + "?" + YoutubeParsingHelper.DISABLE_PRETTY_PRINT_PARAMETER
                    + "&t=" + generateTParameter() + "&id=" + videoId)

        return JsonUtils.toJsonObject(
            getValidJsonResponseBody(
                NewPipe.downloader.postWithContentTypeJson(url, headers, body, localization)
            )
        )
    }

    
    fun getAndroidReelPlayerResponse(
        contentCountry: ContentCountry,
        localization: Localization,
        videoId: String,
        cpn: String?
    ): JsonObject {
        val innertubeClientRequestInfo =
            ofAndroidClient()

        val headers =
            getMobileClientHeaders(getAndroidUserAgent(localization))

        // We must always pass a valid visitorData to get valid player responses, which needs to be
        // got from YouTube
        innertubeClientRequestInfo.clientInfo.visitorData =
            getVisitorDataFromInnertube(
                innertubeClientRequestInfo,
                localization,
                contentCountry,
                headers,
                YoutubeParsingHelper.YOUTUBEI_V1_GAPIS_URL,
                null,
                false
            )

        val builder = prepareJsonBuilder(
            localization, contentCountry,
            innertubeClientRequestInfo, null
        )

        addVideoIdCpnAndOkChecks(builder, videoId, cpn)

        builder.`object`("playerRequest")
            .value(YoutubeParsingHelper.VIDEO_ID, videoId)
            .end()
            .value("disablePlayerResponse", false)

        val body = JsonWriter.string(builder.done())
            .toByteArray(Charsets.UTF_8)

        val url: String = (YoutubeParsingHelper.YOUTUBEI_V1_GAPIS_URL + "reel/reel_item_watch" + "?"
                + YoutubeParsingHelper.DISABLE_PRETTY_PRINT_PARAMETER + "&t=" + generateTParameter() + "&id=" + videoId
                + "&\$fields=playerResponse")

        return JsonUtils.toJsonObject(
            getValidJsonResponseBody(
                NewPipe.downloader.postWithContentTypeJson(url, headers, body, localization)
            )
        )
            .getObject("playerResponse")
    }

    
    fun getIosPlayerResponse(
        contentCountry: ContentCountry,
        localization: Localization,
        videoId: String,
        cpn: String?,
        iosPoTokenResult: PoTokenResult?
    ): JsonObject {
        val innertubeClientRequestInfo =
            ofIosClient()

        val headers =
            getMobileClientHeaders(getIosUserAgent(localization))

        // We must always pass a valid visitorData to get valid player responses, which needs to be
        // got from YouTube
        innertubeClientRequestInfo.clientInfo.visitorData = iosPoTokenResult?.visitorData
            ?: getVisitorDataFromInnertube(
                innertubeClientRequestInfo,
                localization,
                contentCountry,
                headers,
                YoutubeParsingHelper.YOUTUBEI_V1_URL,
                null,
                false
            )

        val builder = prepareJsonBuilder(
            localization, contentCountry,
            innertubeClientRequestInfo, null
        )

        addVideoIdCpnAndOkChecks(builder, videoId, cpn)

        if (iosPoTokenResult != null) {
            addPoToken(builder, iosPoTokenResult.playerRequestPoToken)
        }

        val body = JsonWriter.string(builder.done())
            .toByteArray(Charsets.UTF_8)

        val url: String =
            (YoutubeParsingHelper.YOUTUBEI_V1_GAPIS_URL + PLAYER + "?" + YoutubeParsingHelper.DISABLE_PRETTY_PRINT_PARAMETER
                    + "&t=" + generateTParameter() + "&id=" + videoId)

        return JsonUtils.toJsonObject(
            getValidJsonResponseBody(
                NewPipe.downloader.postWithContentTypeJson(url, headers, body, localization)
            )
        )
    }

    private fun addVideoIdCpnAndOkChecks(
        builder: JsonBuilder<JsonObject>,
        videoId: String,
        cpn: String?
    ) {
        builder.value(YoutubeParsingHelper.VIDEO_ID, videoId)

        if (cpn != null) {
            builder.value(YoutubeParsingHelper.CPN, cpn)
        }

        builder.value(YoutubeParsingHelper.CONTENT_CHECK_OK, true)
            .value(YoutubeParsingHelper.RACY_CHECK_OK, true)
    }

    private fun addPlaybackContext(
        builder: JsonBuilder<JsonObject>,
        referer: String,
        signatureTimestamp: Int
    ) {
        builder.`object`("playbackContext")
            .`object`("contentPlaybackContext")
            .value("signatureTimestamp", signatureTimestamp)
            .value("referer", referer)
            .end()
            .end()
    }

    private fun addPoToken(
        builder: JsonBuilder<JsonObject>,
        poToken: String
    ) {
        builder.`object`(SERVICE_INTEGRITY_DIMENSIONS)
            .value(PO_TOKEN, poToken)
            .end()
    }


    private fun getMobileClientHeaders(
        userAgent: String
    ): Map<String, List<String>> {
        return mapOf(
            "User-Agent" to listOf(userAgent),
            "X-Goog-Api-Format-Version" to listOf("2")
        )
    }
}
