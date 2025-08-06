/*
 * Created by Christian Schabesberger on 06.08.15.
 *
 * Copyright (C) 2019 Christian Schabesberger <chris.schabesberger@mailbox.org>
 * YoutubeStreamExtractor.java is part of NewPipe Extractor.
 *
 * NewPipe Extractor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NewPipe Extractor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NewPipe Extractor. If not, see <https://www.gnu.org/licenses/>.
 */
package com.yushosei.newpipe.extractor.youtube.extractors

import com.yushosei.newpipe.extractor.Image
import com.yushosei.newpipe.extractor.MetaInfo
import com.yushosei.newpipe.extractor.StreamingService
import com.yushosei.newpipe.extractor.downloader.Downloader
import com.yushosei.newpipe.extractor.exceptions.AgeRestrictedContentException
import com.yushosei.newpipe.extractor.exceptions.ContentNotAvailableException
import com.yushosei.newpipe.extractor.exceptions.ExtractionException
import com.yushosei.newpipe.extractor.exceptions.GeographicRestrictionException
import com.yushosei.newpipe.extractor.exceptions.PaidContentException
import com.yushosei.newpipe.extractor.exceptions.ParsingException
import com.yushosei.newpipe.extractor.exceptions.PrivateContentException
import com.yushosei.newpipe.extractor.exceptions.YoutubeMusicPremiumContentException
import com.yushosei.newpipe.extractor.linkhandler.LinkHandler
import com.yushosei.newpipe.extractor.locale.Locale
import com.yushosei.newpipe.extractor.localization.ContentCountry
import com.yushosei.newpipe.extractor.localization.Localization
import com.yushosei.newpipe.extractor.stream.AudioStream
import com.yushosei.newpipe.extractor.stream.DeliveryMethod
import com.yushosei.newpipe.extractor.stream.Stream
import com.yushosei.newpipe.extractor.stream.StreamExtractor
import com.yushosei.newpipe.extractor.stream.StreamType
import com.yushosei.newpipe.extractor.stream.VideoStream
import com.yushosei.newpipe.extractor.utils.JsonUtils
import com.yushosei.newpipe.extractor.utils.LocaleCompat
import com.yushosei.newpipe.extractor.utils.Parser
import com.yushosei.newpipe.extractor.utils.Utils
import com.yushosei.newpipe.extractor.youtube.ItagItem
import com.yushosei.newpipe.extractor.youtube.ItagItem.ItagType
import com.yushosei.newpipe.extractor.youtube.PoTokenProvider
import com.yushosei.newpipe.extractor.youtube.PoTokenResult
import com.yushosei.newpipe.extractor.youtube.YoutubeJavaScriptPlayerManager.deobfuscateSignature
import com.yushosei.newpipe.extractor.youtube.YoutubeJavaScriptPlayerManager.getSignatureTimestamp
import com.yushosei.newpipe.extractor.youtube.YoutubeJavaScriptPlayerManager.getUrlWithThrottlingParameterDeobfuscated
import com.yushosei.newpipe.extractor.youtube.YoutubeMetaInfoHelper
import com.yushosei.newpipe.extractor.youtube.YoutubeParsingHelper
import com.yushosei.newpipe.extractor.youtube.YoutubeParsingHelper.extractAudioTrackType
import com.yushosei.newpipe.extractor.youtube.YoutubeParsingHelper.generateContentPlaybackNonce
import com.yushosei.newpipe.extractor.youtube.YoutubeParsingHelper.getImagesFromThumbnailsArray
import com.yushosei.newpipe.extractor.youtube.YoutubeParsingHelper.getJsonPostResponse
import com.yushosei.newpipe.extractor.youtube.YoutubeParsingHelper.getTextFromObject
import com.yushosei.newpipe.extractor.youtube.YoutubeParsingHelper.prepareDesktopJsonBuilder
import com.yushosei.newpipe.extractor.youtube.YoutubeStreamHelper
import com.yushosei.newpipe.nanojson.JsonObject
import com.yushosei.newpipe.nanojson.JsonWriter
import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.core.toByteArray
import kotlin.math.roundToInt

internal class YoutubeStreamExtractor(service: StreamingService, linkHandler: LinkHandler) :
    StreamExtractor(service, linkHandler) {
    private var playerResponse: JsonObject? = null
    private var nextResponse: JsonObject? = null


    private var iosStreamingData: JsonObject? = null

    private var androidStreamingData: JsonObject? = null

    private var html5StreamingData: JsonObject? = null

    private var videoPrimaryInfoRenderer: JsonObject? = null
        /*//////////////////////////////////////////////////////////////////////////
            // newpipe.timeago.raw.Utils
            ////////////////////////////////////////////////////////////////////////// */
        get() {
            if (field != null) {
                return field
            }

            field = getVideoInfoRenderer("videoPrimaryInfoRenderer")
            return field
        }
    private var videoSecondaryInfoRenderer: JsonObject? = null
    private var playerMicroFormatRenderer: JsonObject? = null
    private var playerCaptionsTracklistRenderer: JsonObject? = null
    override var streamType: StreamType = StreamType.NONE

    // We need to store the contentPlaybackNonces because we need to append them to videoplayback
    // URLs (with the cpn parameter).
    // Also because a nonce should be unique, it should be different between clients used, so
    // three different strings are used.
    private var iosCpn: String? = null
    private var androidCpn: String? = null
    private var html5Cpn: String? = null


    private var html5StreamingUrlsPoToken: String? = null

    private var androidStreamingUrlsPoToken: String? = null

    private var iosStreamingUrlsPoToken: String? = null


    override val name: String?
        get() {
            assertPageFetched()
            var title: String?

            // Try to get the video's original title, which is untranslated
            title = playerResponse!!.getObject("videoDetails").getString("title")

            if (Utils.isNullOrEmpty(title)) {
                title = getTextFromObject(videoPrimaryInfoRenderer!!.getObject("title"))

                if (Utils.isNullOrEmpty(title)) {
                    throw ParsingException("Could not get name")
                }
            }

            return title
        }

    override val thumbnails: List<Image>
        get() {
            assertPageFetched()
            try {
                return getImagesFromThumbnailsArray(
                    playerResponse!!.getObject("videoDetails")
                        .getObject("thumbnail")
                        .getArray("thumbnails")
                )
            } catch (e: Exception) {
                throw ParsingException("Could not get thumbnails")
            }
        }

    override val ageLimit: Int
        get() {
            val ageRestricted = getVideoSecondaryInfoRenderer()
                .getObject("metadataRowContainer")
                .getObject("metadataRowContainerRenderer")
                .getArray("rows")
                .filterIsInstance<JsonObject>() // Only JsonObjects
                .flatMap { metadataRow ->
                    metadataRow
                        .getObject("metadataRowRenderer")
                        .getArray("contents")
                        .filterIsInstance<JsonObject>()
                }
                .flatMap { content ->
                    content
                        .getArray("runs")
                        .filterIsInstance<JsonObject>()
                }
                .map { run -> run.getString("text", "") }
                .any { rowText -> rowText!!.contains("Age-restricted") }

            return if (ageRestricted) 18 else 0
        }

    override val length: Long
        get() {
            assertPageFetched()

            try {
                val duration = playerResponse!!
                    .getObject("videoDetails")
                    .getString("lengthSeconds")
                return duration!!.toLong()
            } catch (e: Exception) {
                return getDurationFromFirstAdaptiveFormat(
                    listOf(
                        html5StreamingData!!, androidStreamingData!!, iosStreamingData!!
                    )
                ).toLong()
            }
        }


    private fun getDurationFromFirstAdaptiveFormat(streamingDatas: List<JsonObject>): Int {
        for (streamingData in streamingDatas) {
            val adaptiveFormats = streamingData.getArray(ADAPTIVE_FORMATS)
            if (adaptiveFormats.isEmpty()) continue

            val durationMs = adaptiveFormats.getObject(0)
                .getString("approxDurationMs")

            try {
                val seconds = durationMs!!.toLong() / 1000f
                return seconds.roundToInt()
            } catch (ignored: NumberFormatException) {
                // ignore and continue loop
            }
        }

        throw ParsingException("Could not get duration")
    }


    /**
     * Attempts to parse (and return) the offset to start playing the video from.
     *
     * @return the offset (in seconds), or 0 if no timestamp is found.
     */

    override val timeStamp: Long
        get() {
            val timestamp =
                getTimestampSeconds("((#|&|\\?)t=\\d*h?\\d*m?\\d+s?)")

            if (timestamp == -2L) {
                // Regex for timestamp was not found
                return 0
            }
            return timestamp
        }

    override val uploaderName: String
        get() {
            assertPageFetched()

            // Don't use the name in the videoSecondaryRenderer object to get real name of the uploader
            // The difference between the real name of the channel and the displayed name is especially
            // visible for music channels and autogenerated channels.
            val uploaderName = playerResponse!!.getObject("videoDetails").getString("author")
            if (Utils.isNullOrEmpty(uploaderName)) {
                throw ParsingException("Could not get uploader name")
            }

            return uploaderName!!
        }


    override val dashMpdUrl: String
        get() {
            assertPageFetched()

            // There is no DASH manifest available with the iOS client
            return getManifestUrl(
                "dash",
                listOf(
                    Pair(androidStreamingData, androidStreamingUrlsPoToken),
                    Pair(html5StreamingData, html5StreamingUrlsPoToken)
                ),  // Return version 7 of the DASH manifest, which is the latest one, reducing
                // manifest size and allowing playback with some DASH players
                "mpd_version=7"
            )
        }

    override val hlsUrl: String
        get() {
            assertPageFetched()

            // Return HLS manifest of the iOS client first because on livestreams, the HLS manifest
            // returned has separated audio and video streams and poTokens requirement do not seem to
            // impact HLS formats (if a poToken is provided, it is added)
            // Also, on videos, non-iOS clients don't have an HLS manifest URL in their player response
            // unless a Safari macOS user agent is used
            return getManifestUrl(
                "hls",
                listOf(
                    Pair(iosStreamingData, iosStreamingUrlsPoToken),
                    Pair(androidStreamingData, androidStreamingUrlsPoToken),
                    Pair(html5StreamingData, html5StreamingUrlsPoToken)
                ),
                ""
            )
        }

    override val audioStreams: List<AudioStream>
        get() {
            assertPageFetched()
            return getItags(
                ADAPTIVE_FORMATS, ItagType.AUDIO,
                audioStreamBuilderHelper, "audio"
            )
        }

    private fun setStreamType() {
        streamType =
            if (playerResponse!!.getObject(PLAYABILITY_STATUS)
                    .has("liveStreamability")
            ) {
                StreamType.LIVE_STREAM
            } else if (playerResponse!!.getObject("videoDetails")
                    .getBoolean("isPostLiveDvr", false)
            ) {
                StreamType.POST_LIVE_STREAM
            } else {
                StreamType.VIDEO_STREAM
            }
    }

    override val errorMessage: String?
        get() {
            return try {
                getTextFromObject(
                    playerResponse!!.getObject(PLAYABILITY_STATUS)
                        .getObject("errorScreen").getObject("playerErrorMessageRenderer")
                        .getObject("reason")
                )
            } catch (e: NullPointerException) {
                null // No error message
            }
        }


    override fun onFetchPage(downloader: Downloader) {
        val videoId = id

        val localization = extractorLocalization
        val contentCountry = extractorContentCountry

        val poTokenproviderInstance = poTokenProvider
        val noPoTokenProviderSet = poTokenproviderInstance == null

        fetchHtml5Client(
            localization, contentCountry, videoId!!, poTokenproviderInstance,
            noPoTokenProviderSet
        )

        setStreamType()

        val androidPoTokenResult = if (noPoTokenProviderSet)
            null
        else
            poTokenproviderInstance?.getAndroidClientPoToken(videoId)

        fetchAndroidClient(localization, contentCountry, videoId, androidPoTokenResult)

        if (fetchIosClient) {
            val iosPoTokenResult = if (noPoTokenProviderSet)
                null
            else
                poTokenproviderInstance?.getIosClientPoToken(videoId)
            fetchIosClient(localization, contentCountry, videoId, iosPoTokenResult)
        }

        val nextBody = JsonWriter.string(
            prepareDesktopJsonBuilder(localization, contentCountry)
                .value(YoutubeParsingHelper.VIDEO_ID, videoId)
                .value(YoutubeParsingHelper.CONTENT_CHECK_OK, true)
                .value(YoutubeParsingHelper.RACY_CHECK_OK, true)
                .done()
        )
            .toByteArray(Charsets.UTF_8)
        nextResponse = getJsonPostResponse(NEXT, nextBody, localization)
    }


    private fun fetchHtml5Client(
        localization: Localization,
        contentCountry: ContentCountry,
        videoId: String,
        poTokenProviderInstance: PoTokenProvider?,
        noPoTokenProviderSet2: Boolean
    ) {
        html5Cpn = generateContentPlaybackNonce()

        // Suppress NPE warning as nullability is already checked before and passed with
        // noPoTokenProviderSet
        val noPoTokenProviderSet = noPoTokenProviderSet2 && poTokenProviderInstance == null

        val webPoTokenResult = if (noPoTokenProviderSet)
            null
        else
            poTokenProviderInstance!!.getWebClientPoToken(videoId)
        val webPlayerResponse: JsonObject
        if (noPoTokenProviderSet || webPoTokenResult == null) {
            webPlayerResponse = YoutubeStreamHelper.getWebMetadataPlayerResponse(
                localization, contentCountry, videoId
            )

            throwExceptionIfPlayerResponseNotValid(webPlayerResponse, videoId)

            // Save the webPlayerResponse into playerResponse in the case the video cannot be
            // played, so some metadata can be retrieved
            playerResponse = webPlayerResponse

            // The microformat JSON object of the content is only returned on the WEB client,
            // so we need to store it instead of getting it directly from the playerResponse
            playerMicroFormatRenderer = playerResponse!!.getObject("microformat")
                .getObject("playerMicroformatRenderer")

            val playabilityStatus = webPlayerResponse.getObject(PLAYABILITY_STATUS)

            if (isVideoAgeRestricted(playabilityStatus)) {
                fetchHtml5EmbedClient(
                    localization, contentCountry, videoId,
                    if (noPoTokenProviderSet)
                        null
                    else
                        poTokenProviderInstance!!.getWebEmbedClientPoToken(videoId)
                )
            } else {
                checkPlayabilityStatus(playabilityStatus)

                val tvHtml5PlayerResponse =
                    YoutubeStreamHelper.getTvHtml5PlayerResponse(
                        localization, contentCountry, videoId, html5Cpn,
                        getSignatureTimestamp(videoId)!!
                    )

                if (isPlayerResponseNotValid(tvHtml5PlayerResponse, videoId)) {
                    throw ExtractionException("TVHTML5 player response is not valid")
                }

                html5StreamingData = tvHtml5PlayerResponse.getObject(STREAMING_DATA)
                playerCaptionsTracklistRenderer = tvHtml5PlayerResponse.getObject(CAPTIONS)
                    .getObject(PLAYER_CAPTIONS_TRACKLIST_RENDERER)
            }
        } else {
            webPlayerResponse = YoutubeStreamHelper.getWebFullPlayerResponse(
                localization, contentCountry, videoId, html5Cpn, webPoTokenResult,
                getSignatureTimestamp(videoId)!!
            )

            throwExceptionIfPlayerResponseNotValid(webPlayerResponse, videoId)

            // Save the webPlayerResponse into playerResponse in the case the video cannot be
            // played, so some metadata can be retrieved
            playerResponse = webPlayerResponse

            // The microformat JSON object of the content is only returned on the WEB client,
            // so we need to store it instead of getting it directly from the playerResponse
            playerMicroFormatRenderer = playerResponse!!.getObject("microformat")
                .getObject("playerMicroformatRenderer")

            val playabilityStatus = webPlayerResponse.getObject(PLAYABILITY_STATUS)

            if (isVideoAgeRestricted(playabilityStatus)) {
                fetchHtml5EmbedClient(
                    localization, contentCountry, videoId,
                    poTokenProviderInstance!!.getWebEmbedClientPoToken(videoId)
                )
            } else {
                checkPlayabilityStatus(playabilityStatus)
                html5StreamingData = webPlayerResponse.getObject(STREAMING_DATA)
                playerCaptionsTracklistRenderer = webPlayerResponse.getObject(CAPTIONS)
                    .getObject(PLAYER_CAPTIONS_TRACKLIST_RENDERER)
                html5StreamingUrlsPoToken = webPoTokenResult.streamingDataPoToken
            }
        }
    }


    private fun fetchHtml5EmbedClient(
        localization: Localization,
        contentCountry: ContentCountry,
        videoId: String,
        webEmbedPoTokenResult: PoTokenResult?
    ) {
        html5Cpn = generateContentPlaybackNonce()

        val webEmbeddedPlayerResponse =
            YoutubeStreamHelper.getWebEmbeddedPlayerResponse(
                localization, contentCountry,
                videoId, html5Cpn, webEmbedPoTokenResult,
                getSignatureTimestamp(videoId)!!
            )

        // Save the webEmbeddedPlayerResponse into playerResponse in the case the video cannot be
        // played, so some metadata can be retrieved
        playerResponse = webEmbeddedPlayerResponse

        // Check if the playability status in the player response, if the age-restriction could not
        // be bypassed, an exception will be thrown
        checkPlayabilityStatus(webEmbeddedPlayerResponse.getObject(PLAYABILITY_STATUS))

        if (isPlayerResponseNotValid(webEmbeddedPlayerResponse, videoId)) {
            throw ExtractionException("WEB_EMBEDDED_PLAYER player response is not valid")
        }

        html5StreamingData = webEmbeddedPlayerResponse.getObject(STREAMING_DATA)
        playerCaptionsTracklistRenderer = webEmbeddedPlayerResponse.getObject(CAPTIONS)
            .getObject(PLAYER_CAPTIONS_TRACKLIST_RENDERER)
        if (webEmbedPoTokenResult != null) {
            html5StreamingUrlsPoToken = webEmbedPoTokenResult.streamingDataPoToken
        }
    }

    private fun fetchAndroidClient(
        localization: Localization,
        contentCountry: ContentCountry,
        videoId: String,
        androidPoTokenResult: PoTokenResult?
    ) {
        try {
            androidCpn = generateContentPlaybackNonce()
            val androidPlayerResponse = if (androidPoTokenResult == null) {
                YoutubeStreamHelper.getAndroidReelPlayerResponse(
                    contentCountry, localization, videoId, androidCpn
                )
            } else {
                YoutubeStreamHelper.getAndroidPlayerResponse(
                    contentCountry, localization, videoId, androidCpn,
                    androidPoTokenResult
                )
            }

            if (!isPlayerResponseNotValid(androidPlayerResponse, videoId)) {
                androidStreamingData = androidPlayerResponse.getObject(STREAMING_DATA)

                if (Utils.isNullOrEmpty(playerCaptionsTracklistRenderer)) {
                    playerCaptionsTracklistRenderer =
                        androidPlayerResponse.getObject(CAPTIONS)
                            .getObject(PLAYER_CAPTIONS_TRACKLIST_RENDERER)
                }

                if (androidPoTokenResult != null) {
                    androidStreamingUrlsPoToken = androidPoTokenResult.streamingDataPoToken
                }
            }
        } catch (ignored: Exception) {
            // Ignore exceptions related to ANDROID client fetch or parsing, as it is not
            // compulsory to play contents
        }
    }

    private fun fetchIosClient(
        localization: Localization,
        contentCountry: ContentCountry,
        videoId: String,
        iosPoTokenResult: PoTokenResult?
    ) {
        try {
            iosCpn = generateContentPlaybackNonce()

            val iosPlayerResponse = YoutubeStreamHelper.getIosPlayerResponse(
                contentCountry, localization, videoId, iosCpn, iosPoTokenResult
            )

            if (!isPlayerResponseNotValid(iosPlayerResponse, videoId)) {
                iosStreamingData = iosPlayerResponse.getObject(STREAMING_DATA)

                if (Utils.isNullOrEmpty(playerCaptionsTracklistRenderer)) {
                    playerCaptionsTracklistRenderer = iosPlayerResponse.getObject(CAPTIONS)
                        .getObject(PLAYER_CAPTIONS_TRACKLIST_RENDERER)
                }

                if (iosPoTokenResult != null) {
                    iosStreamingUrlsPoToken = iosPoTokenResult.streamingDataPoToken
                }
            }
        } catch (ignored: Exception) {
            // Ignore exceptions related to IOS client fetch or parsing, as it is not
            // compulsory to play contents
        }
    }


    private fun getVideoSecondaryInfoRenderer(): JsonObject {
        if (videoSecondaryInfoRenderer != null) {
            return videoSecondaryInfoRenderer!!
        }

        videoSecondaryInfoRenderer = getVideoInfoRenderer("videoSecondaryInfoRenderer")
        return videoSecondaryInfoRenderer!!
    }


    private fun getVideoInfoRenderer(videoRendererName: String): JsonObject {
        return nextResponse!!.getObject("contents")
            .getObject("twoColumnWatchNextResults")
            .getObject("results")
            .getObject("results")
            .getArray("contents")
            .filterIsInstance<JsonObject>()
            .firstOrNull { it.has(videoRendererName) }
            ?.getObject(videoRendererName)
            ?: JsonObject()
    }


    private fun <T : Stream> getItags(
        streamingDataKey: String,
        itagTypeWanted: ItagType,
        streamBuilderHelper: (ItagInfo?) -> T,
        streamTypeExceptionMessage: String
    ): List<T> {
        try {
            val videoId = id ?: throw ParsingException("Video ID is null")
            val streamList = mutableListOf<T>()

            listOf(
                Pair(html5StreamingData, Pair(html5Cpn, html5StreamingUrlsPoToken)),
                Pair(androidStreamingData, Pair(androidCpn, androidStreamingUrlsPoToken)),
                Pair(iosStreamingData, Pair(iosCpn, iosStreamingUrlsPoToken))
            ).flatMap { pair ->
                val streamingData = pair.first
                val context = pair.second
                val cpn = context.first
                val poToken = context.second

                getStreamsFromStreamingDataKey(
                    videoId,
                    streamingData,
                    streamingDataKey,
                    itagTypeWanted,
                    cpn,
                    poToken
                )
            }.map(streamBuilderHelper)
                .forEach { stream ->
                    if (!Stream.containSimilarStream(stream, streamList)) {
                        streamList.add(stream)
                    }
                }

            return streamList
        } catch (e: Exception) {
            throw ParsingException("Could not get $streamTypeExceptionMessage streams", e)
        }
    }

    private val audioStreamBuilderHelper: (ItagInfo?) -> AudioStream = { itagInfo ->
        val itagItem = itagInfo!!.itagItem

        val builder = AudioStream.Builder()
            .setId(itagItem.id.toString())
            .setContent(itagInfo.content, itagInfo.isUrl)
            .setMediaFormat(itagItem.mediaFormat)
            .setAverageBitrate(itagItem.averageBitrate)
            .setAudioTrackId(itagItem.audioTrackId)
            .setAudioTrackName(itagItem.audioTrackName)
            .setAudioLocale(itagItem.audioLocale)
            .setAudioTrackType(itagItem.audioTrackType)
            .setItagItem(itagItem)

        if (
            streamType == StreamType.LIVE_STREAM ||
            streamType == StreamType.POST_LIVE_STREAM ||
            !itagInfo.isUrl
        ) {
            builder.setDeliveryMethod(DeliveryMethod.DASH)
        }

        builder.build()
    }


    /**
     * Get the stream builder helper which will be used to build [VideoStream]s in
     * [.getItags]
     *
     *
     *
     * The `StreamBuilderHelper` will set the following attributes in the
     * [VideoStream]s built:
     *
     *  * the [ItagItem]'s id of the stream as its id;
     *  * [ItagInfo.getContent] and [ItagInfo.getIsUrl] as its content and
     * as the value of `isUrl`;
     *  * the media format returned by the [ItagItem] as its media format;
     *  * whether it is video-only with the `areStreamsVideoOnly` parameter
     *  * the [ItagItem];
     *  * the resolution, by trying to use, in this order:
     *
     *  1. the height returned by the [ItagItem] + `p` + the frame rate if
     * it is more than 30;
     *  1. the default resolution string from the [ItagItem];
     *  1. an empty string.
     *
     *
     *  * the [DASH delivery method][DeliveryMethod.DASH], for OTF streams, live streams
     * and ended streams.
     *
     *
     *
     *
     * Note that the [ItagItem] comes from an [ItagInfo] instance.
     *
     *
     * @param areStreamsVideoOnly whether the stream builder helper will set the video
     * streams as video-only streams
     * @return a stream builder helper to build [VideoStream]s
     */
    private fun getStreamsFromStreamingDataKey(
        videoId: String,
        streamingData: JsonObject?,
        streamingDataKey: String,
        itagTypeWanted: ItagType,
        contentPlaybackNonce: String?,
        poToken: String?
    ): List<ItagInfo> {
        if (streamingData == null || !streamingData.has(streamingDataKey)) {
            return emptyList()
        }

        return streamingData.getArray(streamingDataKey)
            .filterIsInstance<JsonObject>()
            .mapNotNull { formatData ->
                try {
                    val itagItem = ItagItem.getItag(formatData.getInt("itag"))
                    if (itagItem.itagType == itagTypeWanted) {
                        buildAndAddItagInfoToList(
                            videoId, formatData, itagItem,
                            itagItem.itagType, contentPlaybackNonce, poToken
                        )
                    } else {
                        null
                    }
                } catch (_: ExtractionException) {
                    null
                }
            }
    }


    private fun buildAndAddItagInfoToList(
        videoId: String,
        formatData: JsonObject,
        itagItem: ItagItem,
        itagType: ItagType,
        contentPlaybackNonce: String?,
        poToken: String?
    ): ItagInfo {
        var streamUrl: String?
        if (formatData.has("url")) {
            streamUrl = formatData.getString("url")
        } else {
            // This url has an obfuscated signature
            val cipherString = formatData.getString(
                CIPHER,
                formatData.getString(SIGNATURE_CIPHER)
            )
            val cipher = Parser.compatParseMap(cipherString.toString())
            val signature = deobfuscateSignature(
                videoId,
                cipher["s"] ?: ""
            )
            streamUrl = cipher["url"] + "&" + cipher["sp"] + "=" + signature
        }

        // Decode the n parameter if it is present
        // If it cannot be decoded, the stream cannot be used as streaming URLs return HTTP 403
        // responses if it has not the right value
        // Exceptions thrown by
        // YoutubeJavaScriptPlayerManager.getUrlWithThrottlingParameterDeobfuscated are so
        // propagated to the parent which ignores streams in this case
        streamUrl = getUrlWithThrottlingParameterDeobfuscated(
            videoId, streamUrl!!
        )

        // Add the content playback nonce to the stream URL
        streamUrl += "&${YoutubeParsingHelper.CPN}=$contentPlaybackNonce"

        // Add the poToken, if there is one
        if (poToken != null) {
            streamUrl += "&pot=$poToken"
        }

        val initRange = formatData.getObject("initRange")
        val indexRange = formatData.getObject("indexRange")
        val mimeType = formatData.getString("mimeType", "")
        val codec = if (mimeType.contains("codecs"))
            mimeType.split("\"".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
        else
            ""

        itagItem.bitrate = formatData.getInt("bitrate")
        itagItem.width = formatData.getInt("width")
        itagItem.height = formatData.getInt("height")
        itagItem.initStart = initRange.getString("start", "-1").toInt()
        itagItem.initEnd = initRange.getString("end", "-1").toInt()
        itagItem.indexStart = indexRange.getString("start", "-1").toInt()
        itagItem.indexEnd = indexRange.getString("end", "-1").toInt()
        itagItem.quality = formatData.getString("quality")
        itagItem.codec = codec

        if (streamType == StreamType.LIVE_STREAM || streamType == StreamType.POST_LIVE_STREAM) {
            itagItem.targetDurationSec = formatData.getInt("targetDurationSec")
        }

        if (itagType == ItagType.VIDEO || itagType == ItagType.VIDEO_ONLY) {
            itagItem.fps = formatData.getInt("fps")
        } else if (itagType == ItagType.AUDIO) {
            // YouTube return the audio sample rate as a string
            itagItem.sampleRate = formatData.getString("audioSampleRate")!!.toInt()
            itagItem.audioChannels = formatData.getInt(
                "audioChannels",  // Most audio streams have two audio channels, so use this value if the real
                // count cannot be extracted
                // Doing this prevents an exception when generating the
                // AudioChannelConfiguration element of DASH manifests of audio streams in
                // YoutubeDashManifestCreatorUtils
                2
            )

            val audioTrackId = formatData.getObject("audioTrack")
                .getString("id")
            if (!Utils.isNullOrEmpty(audioTrackId)) {
                itagItem.audioTrackId = audioTrackId
                val audioTrackIdLastLocaleCharacter = audioTrackId!!.indexOf(".")
                if (audioTrackIdLastLocaleCharacter != -1) {
                    // Audio tracks IDs are in the form LANGUAGE_CODE.TRACK_NUMBER
                    LocaleCompat.forLanguageTag(
                        audioTrackId.substring(0, audioTrackIdLastLocaleCharacter)
                    )?.let {
                        itagItem.audioLocale = it
                    }
                }
                itagItem.audioTrackType = extractAudioTrackType(streamUrl)
            }

            itagItem.audioTrackName = formatData.getObject("audioTrack")
                .getString("displayName")
        }

        // YouTube return the content length and the approximate duration as strings
        itagItem.contentLength = formatData.getString(
            "contentLength",
            ItagItem.CONTENT_LENGTH_UNKNOWN.toString()
        ).toLong()
        itagItem.approxDurationMs = formatData.getString(
            "approxDurationMs",
            ItagItem.APPROX_DURATION_MS_UNKNOWN.toString()
        ).toLong()

        val itagInfo = ItagInfo(streamUrl, itagItem)

        if (streamType == StreamType.VIDEO_STREAM) {
            itagInfo.isUrl = !formatData.getString("type", "")
                .equals("FORMAT_STREAM_TYPE_OTF", ignoreCase = true)
        } else {
            // We are currently not able to generate DASH manifests for running
            // livestreams, so because of the requirements of StreamInfo
            // objects, return these streams as DASH URL streams (even if they
            // are not playable).
            // Ended livestreams are returned as non URL streams
            itagInfo.isUrl = streamType != StreamType.POST_LIVE_STREAM
        }

        return itagInfo
    }


    /**
     * {@inheritDoc}
     * Should return a list of Frameset object that contains preview of stream frames
     *
     *
     * **Warning:** When using this method be aware
     * that the YouTube API very rarely returns framesets,
     * that are slightly too small e.g. framesPerPageX = 5, frameWidth = 160, but the url contains
     * a storyboard that is only 795 pixels wide (5*160 &gt; 795). You will need to handle this
     * "manually" to avoid errors.
     *
     * @see [
     * TeamNewPipe/NewPipe.11596](https://github.com/TeamNewPipe/NewPipe/pull/11596)
     */

    override val category: String
        get() {
            return playerMicroFormatRenderer!!.getString("category", "")
        }

    override val languageInfo: Locale?
        get() = null

    override val tags: List<String>
        get() = JsonUtils.getStringListFromJsonArray(
            playerResponse!!.getObject("videoDetails")
                .getArray("keywords")
        )

    override val metaInfo: List<MetaInfo>
        get() = YoutubeMetaInfoHelper.getMetaInfo(
            nextResponse!!
                .getObject("contents")
                .getObject("twoColumnWatchNextResults")
                .getObject("results")
                .getObject("results")
                .getArray("contents")
        )

    companion object {
        private var poTokenProvider: PoTokenProvider? = null
        private var fetchIosClient = false

        private fun getManifestUrl(
            manifestType: String,
            streamingDataObjects: List<Pair<JsonObject?, String?>>,
            partToAppendToManifestUrlEnd: String
        ): String {
            val manifestKey = manifestType + "ManifestUrl"

            for (streamingDataObj in streamingDataObjects) {
                if (streamingDataObj.first != null) {
                    val manifestUrl = streamingDataObj.first!!.getString(manifestKey)
                    if (Utils.isNullOrEmpty(manifestUrl)) {
                        continue
                    }

                    // If poToken is not null, add it to manifest URL
                    return if (streamingDataObj.second == null) {
                        "$manifestUrl?$partToAppendToManifestUrlEnd"
                    } else {
                        (manifestUrl + "?pot=" + streamingDataObj.second + "&"
                                + partToAppendToManifestUrlEnd)
                    }
                }
            }

            return ""
        }

        /*//////////////////////////////////////////////////////////////////////////
    // Fetch page
    ////////////////////////////////////////////////////////////////////////// */
        private const val FORMATS = "formats"
        private const val ADAPTIVE_FORMATS = "adaptiveFormats"
        private const val STREAMING_DATA = "streamingData"
        private const val NEXT = "next"
        private const val SIGNATURE_CIPHER = "signatureCipher"
        private const val CIPHER = "cipher"
        private const val PLAYER_CAPTIONS_TRACKLIST_RENDERER = "playerCaptionsTracklistRenderer"
        private const val CAPTIONS = "captions"
        private const val PLAYABILITY_STATUS = "playabilityStatus"


        private fun checkPlayabilityStatus(playabilityStatus: JsonObject) {
            val status = playabilityStatus.getString("status")
            if (status == null || status.equals("ok", ignoreCase = true)) {
                return
            }

            val reason = playabilityStatus.getString("reason")

            if (status.equals("login_required", ignoreCase = true)) {
                if (reason == null) {
                    val message = playabilityStatus.getArray("messages").getString(0)
                    if (message != null && message.contains("private")) {
                        throw PrivateContentException("This video is private")
                    }
                } else if (reason.contains("age")) {
                    throw AgeRestrictedContentException(
                        "This age-restricted video cannot be watched anonymously"
                    )
                }
            }

            if ((status.equals("unplayable", ignoreCase = true) || status.equals(
                    "error",
                    ignoreCase = true
                ))
                && reason != null
            ) {
                if (reason.contains("Music Premium")) {
                    throw YoutubeMusicPremiumContentException()
                }

                if (reason.contains("payment")) {
                    throw PaidContentException("This video is a paid video")
                }

                if (reason.contains("members-only")) {
                    throw PaidContentException(
                        "This video is only available"
                                + " for members of the channel of this video"
                    )
                }

                if (reason.contains("unavailable")) {
                    val detailedErrorMessage = getTextFromObject(
                        playabilityStatus
                            .getObject("errorScreen")
                            .getObject("playerErrorMessageRenderer")
                            .getObject("subreason")
                    )
                    if (detailedErrorMessage != null && detailedErrorMessage.contains("country")) {
                        throw GeographicRestrictionException(
                            "This video is not available in client's country."
                        )
                    } else {
                        throw ContentNotAvailableException(detailedErrorMessage ?: reason)
                    }
                }

                if (reason.contains("age-restricted")) {
                    throw AgeRestrictedContentException(
                        "This age-restricted video cannot be watched anonymously"
                    )
                }
            }

            throw ContentNotAvailableException("Got error: \"$reason\"")
        }


        private fun throwExceptionIfPlayerResponseNotValid(
            webPlayerResponse: JsonObject,
            videoId: String
        ) {
            if (isPlayerResponseNotValid(webPlayerResponse, videoId)) {
                // Check the playability status, as private and deleted videos and invalid video
                // IDs do not return the ID provided in the player response
                // When the requested video is playable and a different video ID is returned, it
                // has the OK playability status, meaning the ExtractionException after this check
                // will be thrown
                checkPlayabilityStatus(webPlayerResponse.getObject(PLAYABILITY_STATUS))
                throw ExtractionException("WEB player response is not valid")
            }
        }

        /**
         * Checks whether a player response is invalid.
         *
         *
         *
         * If YouTube detects that requests come from a third party client, they may replace the real
         * player response by another one of a video saying that this content is not available on this
         * app and to watch it on the latest version of YouTube. This behavior has been observed on the
         * `ANDROID` client, see
         * [
 * https://github.com/TeamNewPipe/NewPipe/issues/8713](https://github.com/TeamNewPipe/NewPipe/issues/8713).
         *
         *
         *
         *
         * YouTube may also sometimes for currently unknown reasons rate-limit an IP, and replace the
         * real one by a player response with a video that says that the requested video is
         * unavailable. This behaviour has been observed in Piped on the InnerTube clients used by the
         * extractor (`ANDROID` and `WEB` clients) which should apply for all clients, see
         * [
 * https://github.com/TeamPiped/Piped/issues/2487](https://github.com/TeamPiped/Piped/issues/2487).
         *
         *
         *
         *
         * We can detect this by checking whether the video ID of the player response returned is the
         * same as the one requested by the extractor.
         *
         *
         * @param playerResponse a player response from any client
         * @param videoId        the video ID of the content requested
         * @return whether the video ID of the player response is not equal to the one requested
         */
        private fun isPlayerResponseNotValid(
            playerResponse: JsonObject,
            videoId: String
        ): Boolean {
            return videoId != playerResponse.getObject("videoDetails")
                .getString("videoId")
        }

        private fun isVideoAgeRestricted(playabilityStatus: JsonObject): Boolean {
            // This is language dependent
            return "login_required".equals(playabilityStatus.getString("status"), ignoreCase = true)
                    && playabilityStatus.getString("reason", "")
                .contains("age")
        }


        /**
         * Set the [PoTokenProvider] instance to be used for fetching `poToken`s.
         *
         *
         *
         * This method allows setting an implementation of [PoTokenProvider] which will be used
         * to obtain poTokens required for YouTube player requests and streaming URLs. These tokens
         * are used by YouTube to verify the integrity of the user's device or browser and are required
         * for playback with several clients.
         *
         *
         *
         *
         * Without a [PoTokenProvider], the extractor makes its best effort to fetch as many
         * streams as possible, but without `poToken`s, some formats may be not available or
         * fetching may be slower due to additional requests done to get streams.
         *
         *
         *
         *
         * Note that any provider change will be only applied on the next [.fetchPage] request.
         *
         *
         * @param poTokenProvider the [PoTokenProvider] instance to set, which can be null to
         * remove a provider already passed
         * @see PoTokenProvider
         */
        @Suppress("unused")
        fun setPoTokenProvider(poTokenProvider: PoTokenProvider?) {
            Companion.poTokenProvider = poTokenProvider
        }

        /**
         * Set whether to fetch the iOS player responses.
         *
         *
         *
         * This method allows fetching the iOS player response, which can be useful in scenarios where
         * streams from the iOS player response are needed, especially HLS manifests.
         *
         *
         *
         *
         * Note that at the time of writing, YouTube is rolling out a `poToken` requirement on
         * this client, formats from HLS manifests do not seem to be affected.
         *
         *
         * @param fetchIosClient whether to fetch the iOS client
         */
        @Suppress("unused")
        fun setFetchIosClient(fetchIosClient: Boolean) {
            Companion.fetchIosClient = fetchIosClient
        }
    }
}
