/*
 * Created by Christian Schabesberger on 02.03.16.
 *
 * Copyright (C) 2016 Christian Schabesberger <chris.schabesberger@mailbox.org>
 * YoutubeParsingHelper.java is part of NewPipe Extractor.
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
package com.yushosei.newpipe.extractor.youtube

import com.yushosei.newpipe.extractor.Image
import com.yushosei.newpipe.extractor.Image.ResolutionLevel.Companion.fromHeight
import com.yushosei.newpipe.extractor.NewPipe
import com.yushosei.newpipe.extractor.downloader.Response
import com.yushosei.newpipe.extractor.exceptions.ContentNotAvailableException
import com.yushosei.newpipe.extractor.exceptions.ExtractionException
import com.yushosei.newpipe.extractor.exceptions.ParsingException
import com.yushosei.newpipe.extractor.exceptions.ReCaptchaException
import com.yushosei.newpipe.extractor.localization.ContentCountry
import com.yushosei.newpipe.extractor.localization.Localization
import com.yushosei.newpipe.extractor.stream.AudioTrackType
import com.yushosei.newpipe.extractor.utils.JsonUtils
import com.yushosei.newpipe.extractor.utils.Parser
import com.yushosei.newpipe.extractor.utils.Parser.RegexException
import com.yushosei.newpipe.extractor.utils.ProtoBuilder
import com.yushosei.newpipe.extractor.utils.RandomStringFromAlphabetGenerator
import com.yushosei.newpipe.extractor.utils.Utils
import com.yushosei.newpipe.nanojson.JsonArray
import com.yushosei.newpipe.nanojson.JsonBuilder
import com.yushosei.newpipe.nanojson.JsonObject
import com.yushosei.newpipe.nanojson.JsonParser
import com.yushosei.newpipe.nanojson.JsonParserException
import com.yushosei.newpipe.nanojson.JsonWriter
import io.ktor.http.Url
import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.core.toByteArray
import kotlinx.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

internal object YoutubeParsingHelper {
    /**
     * The base URL of requests of the `WEB` clients to the InnerTube internal API.
     */
    const val YOUTUBEI_V1_URL: String = "https://www.youtube.com/youtubei/v1/"

    /**
     * The base URL of requests of non-web clients to the InnerTube internal API.
     */
    const val YOUTUBEI_V1_GAPIS_URL: String = "https://youtubei.googleapis.com/youtubei/v1/"

    /**
     * The base URL of YouTube Music.
     */
    private const val YOUTUBE_MUSIC_URL = "https://music.youtube.com"

    /**
     * A parameter to disable pretty-printed response of InnerTube requests, to reduce response
     * sizes.
     *
     *
     *
     * Sent in query parameters of the requests.
     *
     */
    const val DISABLE_PRETTY_PRINT_PARAMETER: String = "prettyPrint=false"

    /**
     * A parameter sent by official clients named `contentPlaybackNonce`.
     *
     *
     *
     * It is sent by official clients on videoplayback requests and InnerTube player requests in
     * most cases.
     *
     *
     *
     *
     * It is composed of 16 characters which are generated from
     * [this alphabet][.CONTENT_PLAYBACK_NONCE_ALPHABET], with the use of strong random
     * values.
     *
     *
     * @see .generateContentPlaybackNonce
     */
    const val CPN: String = "cpn"
    const val VIDEO_ID: String = "videoId"

    /**
     * A parameter sent by official clients named `contentCheckOk`.
     *
     *
     *
     * Setting it to `true` allows us to get streaming data on videos with a warning about
     * what the sensible content they contain.
     *
     */
    const val CONTENT_CHECK_OK: String = "contentCheckOk"

    /**
     * A parameter which may be sent by official clients named `racyCheckOk`.
     *
     *
     *
     * What this parameter does is not really known, but it seems to be linked to sensitive
     * contents such as age-restricted content.
     *
     */
    const val RACY_CHECK_OK: String = "racyCheckOk"

    private var clientVersion: String? = null

    private var youtubeMusicClientVersion: String? = null

    private var clientVersionExtracted = false

    private var _isHardcodedClientVersionValid: Boolean? = null

    suspend fun isHardcodedClientVersionValid(): Boolean {
        if (_isHardcodedClientVersionValid != null) {
            return _isHardcodedClientVersionValid!!
        }

        // JSON payload 구성
        val body = JsonWriter.string()
            .`object`()
            .`object`("context")
            .`object`("client")
            .value("hl", "en-GB")
            .value("gl", "GB")
            .value("clientName", ClientsConstants.WEB_CLIENT_NAME)
            .value("clientVersion", ClientsConstants.WEB_HARDCODED_CLIENT_VERSION)
            .value("platform", ClientsConstants.DESKTOP_CLIENT_PLATFORM)
            .value("utcOffsetMinutes", 0)
            .end()
            .`object`("request")
            .array("internalExperimentFlags")
            .end()
            .value("useSsl", true)
            .end()
            .`object`("user")
            .value("lockedSafetyMode", false)
            .end()
            .end()
            .value("fetchLiveState", true)
            .end().done().toByteArray(Charsets.UTF_8)

        val headers = getClientHeaders(
            ClientsConstants.WEB_CLIENT_ID,
            ClientsConstants.WEB_HARDCODED_CLIENT_VERSION
        )

        val response = NewPipe.downloader.postWithContentTypeJson(
            YOUTUBEI_V1_URL + "guide?$DISABLE_PRETTY_PRINT_PARAMETER",
            headers, body
        )
        val responseBody = response.responseBody()
        val responseCode = response.responseCode()

        _isHardcodedClientVersionValid = (responseBody.length > 5000 && responseCode == 200)
        return _isHardcodedClientVersionValid!!
    }

    private val INNERTUBE_CONTEXT_CLIENT_VERSION_REGEXES = listOf(
        "INNERTUBE_CONTEXT_CLIENT_VERSION\":\"([0-9\\.]+?)\"",
        "innertube_context_client_version\":\"([0-9\\.]+?)\"",
        "client.version=([0-9\\.]+)"
    )
    private val INITIAL_DATA_REGEXES = listOf(
        "window\\[\"ytInitialData\"\\]\\s*=\\s*(\\{.*?\\});",
        "var\\s*ytInitialData\\s*=\\s*(\\{.*?\\});"
    )

    private const val CONTENT_PLAYBACK_NONCE_ALPHABET =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"

    @OptIn(ExperimentalTime::class)
    private var numberGenerator = Random(Clock.System.now().epochSeconds)

    private const val FEED_BASE_CHANNEL_ID = "https://www.youtube.com/feeds/videos.xml?channel_id="
    private const val FEED_BASE_USER = "https://www.youtube.com/feeds/videos.xml?user="
    private val C_WEB_PATTERN: Regex = Regex("&c=WEB")
    private val C_WEB_EMBEDDED_PLAYER_PATTERN: Regex = Regex("&c=WEB_EMBEDDED_PLAYER")
    private val C_TVHTML5_PLAYER_PATTERN: Regex = Regex("&c=TVHTML5")
    private val C_ANDROID_PATTERN: Regex = Regex("&c=ANDROID")
    private val C_IOS_PATTERN: Regex = Regex("&c=IOS")

    private val GOOGLE_URLS = setOf("google.", "m.google.", "www.google.")
    private val INVIDIOUS_URLS = setOf(
        "invidio.us", "dev.invidio.us",
        "www.invidio.us", "redirect.invidious.io", "invidious.snopyta.org", "yewtu.be",
        "tube.connect.cafe", "tubus.eduvid.org", "invidious.kavin.rocks", "invidious.site",
        "invidious-us.kavin.rocks", "piped.kavin.rocks", "vid.mint.lgbt", "invidiou.site",
        "invidious.fdn.fr", "invidious.048596.xyz", "invidious.zee.li", "vid.puffyan.us",
        "ytprivate.com", "invidious.namazso.eu", "invidious.silkky.cloud", "ytb.trom.tf",
        "invidious.exonip.de", "inv.riverside.rocks", "invidious.blamefran.net", "y.com.cm",
        "invidious.moomoo.me", "yt.cyberhost.uk"
    )
    private val YOUTUBE_URLS = setOf(
        "youtube.com", "www.youtube.com",
        "m.youtube.com", "music.youtube.com"
    )

    /**
     * Get the value of the consent's acceptance.
     *
     * @see .setConsentAccepted
     * @return the consent's acceptance value
     */
    /**
     * Determines how the consent cookie that is required for YouTube, `SOCS`, will be
     * generated.
     *
     *
     *  * `false` (the default value) will use `CAE=`;
     *  * `true` will use `CAISAiAD`.
     *
     *
     *
     *
     * Setting this value to `true` is needed to extract mixes and some YouTube Music
     * playlists in some countries such as the EU ones.
     *
     */
    var isConsentAccepted: Boolean = false


    fun isGoogleURL(url: String?): Boolean {
        val cachedUrl = extractCachedUrlIfNeeded(url)
        try {
            val u = Url(cachedUrl!!)
            return GOOGLE_URLS.any { item: String? ->
                u.host.startsWith(
                    item!!
                )
            }
        } catch (e: IllegalArgumentException) {
            return false
        }
    }


    fun isYoutubeURL(url: Url): Boolean {
        return YOUTUBE_URLS.contains(url.host.lowercase())
    }


    fun isYoutubeServiceURL(url: Url): Boolean {
        val host = url.host
        return host.equals("www.youtube-nocookie.com", ignoreCase = true)
                || host.equals("youtu.be", ignoreCase = true)
    }


    fun isHooktubeURL(url: Url): Boolean {
        val host = url.host
        return host.equals("hooktube.com", ignoreCase = true)
    }


    fun isInvidiousURL(url: Url): Boolean {
        return INVIDIOUS_URLS.contains(url.host.lowercase())
    }


    fun isY2ubeURL(url: Url): Boolean {
        return url.host.equals("y2u.be", ignoreCase = true)
    }

    @OptIn(ExperimentalTime::class)
    fun randomVisitorData(country: ContentCountry): String {
        val pbE2 = ProtoBuilder()
        pbE2.string(2, "")
        pbE2.varint(4, (numberGenerator.nextInt(255) + 1).toLong())

        val pbE = ProtoBuilder()
        pbE.string(1, country.countryCode)
        pbE.bytes(2, pbE2.toBytes())

        val pb = ProtoBuilder()
        pb.string(
            1, RandomStringFromAlphabetGenerator.generate(
                CONTENT_PLAYBACK_NONCE_ALPHABET, 11, numberGenerator
            )
        )
        pb.varint(5, Clock.System.now().epochSeconds / 1000 - numberGenerator.nextInt(600000))
        pb.bytes(6, pbE.toBytes())
        return pb.toUrlencodedBase64()
    }

    /**
     * Parses the duration string of the video expecting ":" or "." as separators
     *
     * @return the duration in seconds
     * @throws ParsingException when more than 3 separators are found
     */

    @Throws(ParsingException::class, NumberFormatException::class)
    fun parseDurationString(input: String): Int {
        // If time separator : is not detected, try . instead
        val splitInput = if (input.contains(":"))
            input.split(":".toRegex())
                .dropLastWhile { it.isEmpty() }.toTypedArray() else
            input.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        val units = intArrayOf(24, 60, 60, 1)
        val offset = units.size - splitInput.size
        if (offset < 0) {
            throw ParsingException("Error duration string with unknown format: $input")
        }
        var duration = 0
        for (i in splitInput.indices) {
            duration = units[i + offset] * (duration + convertDurationToInt(
                splitInput[i]
            ))
        }
        return duration
    }

    /**
     * Tries to convert a duration string to an integer without throwing an exception.
     * <br></br>
     * Helper method for [.parseDurationString].
     * <br></br>
     * Note: This method is also used as a workaround for NewPipe#8034 (YT shorts no longer
     * display any duration in channels).
     *
     * @param input The string to process
     * @return The converted integer or 0 if the conversion failed.
     */
    private fun convertDurationToInt(input: String?): Int {
        if (input == null || input.isEmpty()) {
            return 0
        }

        val clearedInput = Utils.removeNonDigitCharacters(input)
        return try {
            clearedInput.toInt()
        } catch (ex: NumberFormatException) {
            0
        }
    }


    fun getFeedUrlFrom(channelIdOrUser: String): String {
        return if (channelIdOrUser.startsWith("user/")) {
            FEED_BASE_USER + channelIdOrUser.replace("user/", "")
        } else if (channelIdOrUser.startsWith("channel/")) {
            FEED_BASE_CHANNEL_ID + channelIdOrUser.replace("channel/", "")
        } else {
            FEED_BASE_CHANNEL_ID + channelIdOrUser
        }
    }

    /**
     * Checks if the given playlist id is a YouTube Mix (auto-generated playlist)
     * Ids from a YouTube Mix start with "RD"
     *
     * @param playlistId the playlist id
     * @return Whether given id belongs to a YouTube Mix
     */

    fun isYoutubeMixId(playlistId: String): Boolean {
        return playlistId.startsWith("RD")
    }

    /**
     * Checks if the given playlist id is a YouTube My Mix (auto-generated playlist)
     * Ids from a YouTube My Mix start with "RDMM"
     *
     * @param playlistId the playlist id
     * @return Whether given id belongs to a YouTube My Mix
     */
    fun isYoutubeMyMixId(playlistId: String): Boolean {
        return playlistId.startsWith("RDMM")
    }

    /**
     * Checks if the given playlist id is a YouTube Music Mix (auto-generated playlist)
     * Ids from a YouTube Music Mix start with "RDAMVM" or "RDCLAK"
     *
     * @param playlistId the playlist id
     * @return Whether given id belongs to a YouTube Music Mix
     */
    fun isYoutubeMusicMixId(playlistId: String): Boolean {
        return playlistId.startsWith("RDAMVM") || playlistId.startsWith("RDCLAK")
    }

    /**
     * Checks if the given playlist id is a YouTube Channel Mix (auto-generated playlist)
     * Ids from a YouTube channel Mix start with "RDCM"
     *
     * @return Whether given id belongs to a YouTube Channel Mix
     */

    fun isYoutubeChannelMixId(playlistId: String): Boolean {
        return playlistId.startsWith("RDCM")
    }

    /**
     * Checks if the given playlist id is a YouTube Genre Mix (auto-generated playlist)
     * Ids from a YouTube Genre Mix start with "RDGMEM"
     *
     * @return Whether given id belongs to a YouTube Genre Mix
     */
    fun isYoutubeGenreMixId(playlistId: String): Boolean {
        return playlistId.startsWith("RDGMEM")
    }

    /**
     * @param playlistId the playlist id to parse
     * @return the [PlaylistInfo.PlaylistType] extracted from the playlistId (mix playlist
     * types included)
     * @throws ParsingException if the playlistId is null or empty, if the playlistId is not a mix,
     * if it is a mix but it's not based on a specific stream (this is the
     * case for channel or genre mixes)
     */


    fun extractVideoIdFromMixId(playlistId: String): String {
        if (Utils.isNullOrEmpty(playlistId)) {
            throw ParsingException("Video id could not be determined from empty playlist id")
        } else if (isYoutubeMyMixId(playlistId)) {
            return playlistId.substring(4)
        } else if (isYoutubeMusicMixId(playlistId)) {
            return playlistId.substring(6)
        } else if (isYoutubeChannelMixId(playlistId)) {
            // Channel mixes are of the form RMCM{channelId}, so videoId can't be determined
            throw ParsingException(
                "Video id could not be determined from channel mix id: "
                        + playlistId
            )
        } else if (isYoutubeGenreMixId(playlistId)) {
            // Genre mixes are of the form RDGMEM{garbage}, so videoId can't be determined
            throw ParsingException(
                "Video id could not be determined from genre mix id: "
                        + playlistId
            )
        } else if (isYoutubeMixId(playlistId)) { // normal mix
            if (playlistId.length != 13) {
                // Stream YouTube mixes are of the form RD{videoId}, but if videoId is not exactly
                // 11 characters then it can't be a video id, hence we are dealing with a different
                // type of mix (e.g. genre mixes handled above, of the form RDGMEM{garbage})
                throw ParsingException(
                    "Video id could not be determined from mix id: "
                            + playlistId
                )
            }
            return playlistId.substring(2)
        } else { // not a mix
            throw ParsingException(
                "Video id could not be determined from playlist id: "
                        + playlistId
            )
        }
    }


    private fun getInitialData(html: String): JsonObject {
        try {
            return JsonParser.`object`().from(
                Utils.getStringResultFromRegexArray(
                    html,
                    INITIAL_DATA_REGEXES, 1
                )
            )
        } catch (e: JsonParserException) {
            throw ParsingException("Could not get ytInitialData", e)
        } catch (e: RegexException) {
            throw ParsingException("Could not get ytInitialData", e)
        }
    }


    private suspend fun extractClientVersionFromSwJs() {
        if (clientVersionExtracted) {
            return
        }
        val url = "https://www.youtube.com/sw.js"
        val headers = getOriginReferrerHeaders("https://www.youtube.com")
        val response = NewPipe.downloader.get(url, headers).responseBody()
        try {
            clientVersion = Utils.getStringResultFromRegexArray(
                response,
                INNERTUBE_CONTEXT_CLIENT_VERSION_REGEXES, 1
            )
        } catch (e: RegexException) {
            throw ParsingException(
                "Could not extract YouTube WEB InnerTube client version "
                        + "from sw.js", e
            )
        }
        clientVersionExtracted = true
    }


    private suspend fun extractClientVersionFromHtmlSearchResultsPage() {
        // Don't extract the InnerTube client version if it has been already extracted
        if (clientVersionExtracted) {
            return
        }

        // Don't provide a search term in order to have a smaller response
        val url = "https://www.youtube.com/results?search_query=&ucbcb=1"
        val html = NewPipe.downloader.get(
            url,
            cookieHeader
        ).responseBody()
        val initialData = getInitialData(html)
        val serviceTrackingParams = initialData.getObject("responseContext")
            .getArray("serviceTrackingParams")

        // Try to get version from initial data first
        val serviceTrackingParamsStream = serviceTrackingParams
            .filterIsInstance<JsonObject>()

        clientVersion = getClientVersionFromServiceTrackingParam(
            serviceTrackingParamsStream, "CSI", "cver"
        )


        if (clientVersion == null) {
            try {
                clientVersion = Utils.getStringResultFromRegexArray(
                    html,
                    INNERTUBE_CONTEXT_CLIENT_VERSION_REGEXES, 1
                )
            } catch (ignored: RegexException) {
            }
        }

        // Fallback to get a shortened client version which does not contain the last two
        // digits
        if (Utils.isNullOrEmpty(clientVersion)) {
            clientVersion = getClientVersionFromServiceTrackingParam(
                serviceTrackingParamsStream, "ECATCHER", "client.version"
            )
        }

        if (clientVersion == null) {
            throw ParsingException( // CHECKSTYLE:OFF
                "Could not extract YouTube WEB InnerTube client version from HTML search results page"
            )
            // CHECKSTYLE:ON
        }

        clientVersionExtracted = true
    }


    private fun getClientVersionFromServiceTrackingParam(
        serviceTrackingParams: List<JsonObject>,
        serviceName: String,
        clientVersionKey: String
    ): String? {
        return serviceTrackingParams
            .filter { it.getString("service", "") == serviceName }
            .flatMap { it.getArray("params").toList() }
            .filterIsInstance<JsonObject>()
            .firstOrNull { it.getString("key", "") == clientVersionKey }
            ?.getString("value")
            ?.takeIf { !Utils.isNullOrEmpty(it) }
    }


    /**
     * Get the client version used by YouTube website on InnerTube requests.
     */


    suspend fun getClientVersion(): String? {
        if (!Utils.isNullOrEmpty(clientVersion)) {
            return clientVersion
        }

        // Always extract the latest client version, by trying first to extract it from the
        // JavaScript service worker, then from HTML search results page as a fallback, to prevent
        // fingerprinting based on the client version used
        try {
            extractClientVersionFromSwJs()
        } catch (e: Exception) {
            extractClientVersionFromHtmlSearchResultsPage()
        }

        if (clientVersionExtracted) {
            return clientVersion
        }

        // Fallback to the hardcoded one if it is valid
        if (isHardcodedClientVersionValid()) {
            clientVersion = ClientsConstants.WEB_HARDCODED_CLIENT_VERSION
            return clientVersion
        }

        throw ExtractionException("Could not get YouTube WEB client version")
    }

    /**
     *
     *
     * **Only used in tests.**
     *
     *
     *
     *
     * Quick-and-dirty solution to reset global state in between test classes.
     *
     *
     *
     * This is needed for the mocks because in order to reach that state a network request has to
     * be made. If the global state is not reset and the RecordingDownloader is used,
     * then only the first test class has that request recorded. Meaning running the other
     * tests with mocks will fail, because the mock is missing.
     *
     */
    fun resetClientVersion() {
        clientVersion = null
        clientVersionExtracted = false
    }

    /**
     *
     *
     * **Only used in tests.**
     *
     */
    fun setNumberGenerator(random: Random) {
        numberGenerator = random
    }


    private var _isHardcodedYoutubeMusicClientVersionValid: Boolean? = null

    suspend fun isHardcodedYoutubeMusicClientVersionValid(): Boolean {
        if (_isHardcodedYoutubeMusicClientVersionValid != null) {
            return _isHardcodedYoutubeMusicClientVersionValid!!
        }

        val url =
            "https://music.youtube.com/youtubei/v1/music/get_search_suggestions?$DISABLE_PRETTY_PRINT_PARAMETER"

        val json = JsonWriter.string()
            .`object`()
            .`object`("context")
            .`object`("client")
            .value("clientName", ClientsConstants.WEB_REMIX_CLIENT_NAME)
            .value("clientVersion", ClientsConstants.WEB_REMIX_HARDCODED_CLIENT_VERSION)
            .value("hl", "en-GB")
            .value("gl", "GB")
            .value("platform", ClientsConstants.DESKTOP_CLIENT_PLATFORM)
            .value("utcOffsetMinutes", 0)
            .end()
            .`object`("request")
            .array("internalExperimentFlags")
            .end()
            .value("useSsl", true)
            .end()
            .`object`("user")
            .value("lockedSafetyMode", false)
            .end()
            .end()
            .value("input", "")
            .end().done().toByteArray(Charsets.UTF_8)

        val headers = HashMap(getOriginReferrerHeaders(YOUTUBE_MUSIC_URL)).apply {
            putAll(
                getClientHeaders(
                    ClientsConstants.WEB_REMIX_CLIENT_ID,
                    ClientsConstants.WEB_HARDCODED_CLIENT_VERSION
                )
            )
        }

        val response = NewPipe.downloader.postWithContentTypeJson(url, headers, json)
        _isHardcodedYoutubeMusicClientVersionValid =
            response.responseBody().length > 500 && response.responseCode() == 200

        return _isHardcodedYoutubeMusicClientVersionValid!!
    }

    @Throws(
        IOException::class, ReCaptchaException::class, RegexException::class,
        CancellationException::class
    )
    suspend fun getYoutubeMusicClientVersion(): String {
        if (!Utils.isNullOrEmpty(youtubeMusicClientVersion)) {
            return youtubeMusicClientVersion!!
        }
        if (isHardcodedYoutubeMusicClientVersionValid()) {
            youtubeMusicClientVersion = ClientsConstants.WEB_REMIX_HARDCODED_CLIENT_VERSION
            return youtubeMusicClientVersion!!
        }

        try {
            val url = "https://music.youtube.com/sw.js"
            val headers = getOriginReferrerHeaders(YOUTUBE_MUSIC_URL)
            val response = NewPipe.downloader.get(url, headers).responseBody()

            youtubeMusicClientVersion = Utils.getStringResultFromRegexArray(
                response,
                INNERTUBE_CONTEXT_CLIENT_VERSION_REGEXES, 1
            )
        } catch (e: Exception) {
            val url = "https://music.youtube.com/?ucbcb=1"
            val html = NewPipe.downloader.get(
                url,
                cookieHeader
            ).responseBody()

            youtubeMusicClientVersion = Utils.getStringResultFromRegexArray(
                html,
                INNERTUBE_CONTEXT_CLIENT_VERSION_REGEXES, 1
            )
        }

        return youtubeMusicClientVersion!!
    }


    fun getUrlFromNavigationEndpoint(
        navigationEndpoint: JsonObject
    ): String? {
        if (navigationEndpoint.has("urlEndpoint")) {
            var internUrl = navigationEndpoint.getObject("urlEndpoint")
                .getString("url") ?: return null

            if (internUrl.startsWith("https://www.youtube.com/redirect?")) {
                // remove https://www.youtube.com part to fall in the next if block
                internUrl = internUrl.substring(23)
            }

            if (internUrl.startsWith("/redirect?")) {
                // q parameter can be the first parameter
                internUrl = internUrl.substring(10)
                val params =
                    internUrl.split("&".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                for (param in params) {
                    if (param.split("=".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()[0] == "q") {
                        return Utils.decodeUrlUtf8(
                            param.split("=".toRegex())
                                .dropLastWhile { it.isEmpty() }.toTypedArray()[1]
                        )
                    }
                }
            } else if (internUrl.startsWith("http")) {
                return internUrl
            } else if (internUrl.startsWith("/channel") || internUrl.startsWith("/user")
                || internUrl.startsWith("/watch")
            ) {
                return "https://www.youtube.com$internUrl"
            }
        }

        if (navigationEndpoint.has("browseEndpoint")) {
            val browseEndpoint = navigationEndpoint.getObject("browseEndpoint")
            val canonicalBaseUrl = browseEndpoint.getString("canonicalBaseUrl")
            val browseId = browseEndpoint.getString("browseId")

            if (browseId != null) {
                if (browseId.startsWith("UC")) {
                    // All channel IDs are prefixed with UC
                    return "https://www.youtube.com/channel/$browseId"
                } else if (browseId.startsWith("VL")) {
                    // All playlist IDs are prefixed with VL, which needs to be removed from the
                    // playlist ID
                    return "https://www.youtube.com/playlist?list=" + browseId.substring(2)
                }
            }

            if (!Utils.isNullOrEmpty(canonicalBaseUrl)) {
                return "https://www.youtube.com$canonicalBaseUrl"
            }
        }

        if (navigationEndpoint.has("watchEndpoint")) {
            val url = StringBuilder()
            url.append("https://www.youtube.com/watch?v=")
                .append(
                    navigationEndpoint.getObject("watchEndpoint")
                        .getString(VIDEO_ID)
                )
            if (navigationEndpoint.getObject("watchEndpoint").has("playlistId")) {
                url.append("&list=").append(
                    navigationEndpoint.getObject("watchEndpoint")
                        .getString("playlistId")
                )
            }
            if (navigationEndpoint.getObject("watchEndpoint").has("startTimeSeconds")) {
                url.append("&t=")
                    .append(
                        navigationEndpoint.getObject("watchEndpoint")
                            .getInt("startTimeSeconds")
                    )
            }
            return url.toString()
        }

        if (navigationEndpoint.has("watchPlaylistEndpoint")) {
            return ("https://www.youtube.com/playlist?list="
                    + navigationEndpoint.getObject("watchPlaylistEndpoint")
                .getString("playlistId"))
        }

        if (navigationEndpoint.has("commandMetadata")) {
            val metadata = navigationEndpoint.getObject("commandMetadata")
                .getObject("webCommandMetadata")
            if (metadata.has("url")) {
                return "https://www.youtube.com" + metadata.getString("url")
            }
        }

        return null
    }

    /**
     * Get the text from a JSON object that has either a `simpleText` or a `runs`
     * array.
     *
     * @param textObject JSON object to get the text from
     * @param html       whether to return HTML, by parsing the `navigationEndpoint`
     * @return text in the JSON object or `null`
     */

    fun htmlEscape(text: String): String {
        return buildString {
            for (c in text) {
                when (c) {
                    '<' -> append("&lt;")
                    '>' -> append("&gt;")
                    '&' -> append("&amp;")
                    '"' -> append("&quot;")
                    '\'' -> append("&#39;")
                    else -> append(c)
                }
            }
        }
    }

    fun getTextFromObject(textObject: JsonObject, html: Boolean): String? {
        if (Utils.isNullOrEmpty(textObject)) {
            return null
        }

        if (textObject.has("simpleText")) {
            return textObject.getString("simpleText")
        }

        val runs = textObject.getArray("runs")
        if (runs.isEmpty()) {
            return null
        }

        val textBuilder = StringBuilder()
        for (o in runs) {
            val run = o as JsonObject
            var text = run.getString("text")

            if (html) {
                if (run.has("navigationEndpoint")) {
                    val url = getUrlFromNavigationEndpoint(
                        run.getObject("navigationEndpoint")
                    )
                    if (!url.isNullOrEmpty()) {
                        text = """<a href="${htmlEscape(url)}">${htmlEscape(text ?: "")}</a>"""
                    }
                }

                val bold = run.has("bold")
                        && run.getBoolean("bold")
                val italic = run.has("italics")
                        && run.getBoolean("italics")
                val strikethrough = run.has("strikethrough")
                        && run.getBoolean("strikethrough")

                if (bold) {
                    textBuilder.append("<b>")
                }
                if (italic) {
                    textBuilder.append("<i>")
                }
                if (strikethrough) {
                    textBuilder.append("<s>")
                }

                textBuilder.append(text)

                if (strikethrough) {
                    textBuilder.append("</s>")
                }
                if (italic) {
                    textBuilder.append("</i>")
                }
                if (bold) {
                    textBuilder.append("</b>")
                }
            } else {
                textBuilder.append(text)
            }
        }

        var text = textBuilder.toString()

        if (html) {
            text = text.replace("\\n".toRegex(), "<br>")
            text = text.replace(" {2}".toRegex(), " &nbsp;")
        }

        return text
    }


    fun getTextFromObjectOrThrow(textObject: JsonObject, error: String): String {
        val result = getTextFromObject(textObject)
            ?: throw ParsingException("Could not extract text: $error")
        return result
    }


    fun getTextFromObject(textObject: JsonObject): String? {
        return getTextFromObject(textObject, false)
    }


    fun getUrlFromObject(textObject: JsonObject): String? {
        if (Utils.isNullOrEmpty(textObject)) {
            return null
        }

        val runs = textObject.getArray("runs")
        if (runs.isEmpty()) {
            return null
        }

        for (textPart in runs) {
            val url = getUrlFromNavigationEndpoint(
                (textPart as JsonObject)
                    .getObject("navigationEndpoint")
            )
            if (!Utils.isNullOrEmpty(url)) {
                return url
            }
        }

        return null
    }


    fun getTextAtKey(jsonObject: JsonObject, theKey: String?): String? {
        return if (jsonObject.isString(theKey)) {
            jsonObject.getString(theKey)
        } else {
            getTextFromObject(jsonObject.getObject(theKey))
        }
    }


    fun fixThumbnailUrl(thumbnailUrl: String): String {
        var result = thumbnailUrl
        if (result.startsWith("//")) {
            result = result.substring(2)
        }

        if (result.startsWith(Utils.HTTP)) {
            result = Utils.replaceHttpWithHttps(result)
        } else if (!result.startsWith(Utils.HTTPS)) {
            result = "https://$result"
        }

        return result
    }

    /**
     * Get thumbnails from a [JsonObject] representing a YouTube
     * [InfoItem].
     *
     *
     *
     * Thumbnails are got from the `thumbnails` [JsonArray] inside the `thumbnail`
     * [JsonObject] of the YouTube [InfoItem],
     * using [.getImagesFromThumbnailsArray].
     *
     *
     * @param infoItem a YouTube [InfoItem]
     * @return an unmodifiable list of [Image]s found in the `thumbnails`
     * [JsonArray]
     * @throws ParsingException if an exception occurs when
     * [.getImagesFromThumbnailsArray] is executed
     */


    fun getThumbnailsFromInfoItem(infoItem: JsonObject): List<Image> {
        try {
            return getImagesFromThumbnailsArray(
                infoItem.getObject("thumbnail")
                    .getArray("thumbnails")
            )
        } catch (e: Exception) {
            throw ParsingException("Could not get thumbnails from InfoItem", e)
        }
    }

    /**
     * Get images from a YouTube `thumbnails` [JsonArray].
     *
     *
     *
     * The properties of the [Image]s created will be set using the corresponding ones of
     * thumbnail items.
     *
     *
     * @param thumbnails a YouTube `thumbnails` [JsonArray]
     * @return an unmodifiable list of [Image]s extracted from the given [JsonArray]
     */

    fun getImagesFromThumbnailsArray(thumbnails: JsonArray): List<Image> {
        return thumbnails
            .filterIsInstance<JsonObject>()
            .mapNotNull { thumbnail ->
                val url = thumbnail.getString("url")
                if (url.isNullOrEmpty()) return@mapNotNull null

                val height = thumbnail.getInt("height", Image.HEIGHT_UNKNOWN)
                Image(
                    fixThumbnailUrl(url),
                    height,
                    thumbnail.getInt("width", Image.WIDTH_UNKNOWN),
                    fromHeight(height)
                )
            }
    }

    @Throws(ParsingException::class, IllegalArgumentException::class)
    fun getValidJsonResponseBody(response: Response): String {
        if (response.responseCode() == 404) {
            throw ContentNotAvailableException(
                ("Not found"
                        + " (\"" + response.responseCode() + " " + response.responseMessage() + "\")")
            )
        }

        val responseBody = response.responseBody()
        if (responseBody.length < 50) { // Ensure to have a valid response
            throw ParsingException("JSON response is too short")
        }

        // Check if the request was redirected to the error page.
        val latestUrl = Url(response.latestUrl())
        if (latestUrl.host.equals("www.youtube.com", ignoreCase = true)) {
            val path = latestUrl.encodedPath
            if (path.equals("/oops", ignoreCase = true) || path.equals(
                    "/error",
                    ignoreCase = true
                )
            ) {
                throw ContentNotAvailableException("Content unavailable")
            }
        }

        val responseContentType = response.getHeader("Content-Type")
        if (responseContentType != null
            && responseContentType.lowercase().contains("text/html")
        ) {
            throw ParsingException(
                ("Got HTML document, expected JSON response"
                        + " (latest url was: \"" + response.latestUrl() + "\")")
            )
        }

        return responseBody
    }

    suspend fun getJsonPostResponse(
        endpoint: String,
        body: ByteArray?,
        localization: Localization?
    ): JsonObject {
        val headers =
            youTubeHeaders()

        return JsonUtils.toJsonObject(
            getValidJsonResponseBody(
                NewPipe.downloader.postWithContentTypeJson(
                    (YOUTUBEI_V1_URL + endpoint + "?"
                            + DISABLE_PRETTY_PRINT_PARAMETER), headers, body, localization
                )
            )
        )
    }

    suspend fun prepareDesktopJsonBuilder(
        localization: Localization,
        contentCountry: ContentCountry
    ): JsonBuilder<JsonObject> {
        // @formatter:off
        return JsonObject.builder()
            .`object`("context")
            .`object`("client")
            .value("hl", localization.localizationCode)
            .value("gl", contentCountry.countryCode)
            .value("clientName", ClientsConstants.WEB_CLIENT_NAME)
            .value("clientVersion", getClientVersion())
            .value("originalUrl", "https://www.youtube.com")
            .value("platform", ClientsConstants.DESKTOP_CLIENT_PLATFORM)
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
        // @formatter:on
    }

    /**
     * Get the user-agent string used as the user-agent for InnerTube requests with the Android
     * client.
     *
     *
     *
     * If the [Localization] provided is `null`, fallbacks to
     * [the default one][Localization.DEFAULT].
     *
     *
     * @param localization the [Localization] to set in the user-agent
     * @return the Android user-agent used for InnerTube requests with the Android client,
     * depending on the [Localization] provided
     */

    fun getAndroidUserAgent(localization: Localization?): String {
        return ("com.google.android.youtube/" + ClientsConstants.ANDROID_CLIENT_VERSION
                + " (Linux; U; Android 15; "
                + (localization ?: Localization.DEFAULT).countryCode
                + ") gzip")
    }

    /**
     * Get the user-agent string used as the user-agent for InnerTube requests with the iOS
     * client.
     *
     *
     *
     * If the [Localization] provided is `null`, fallbacks to
     * [the default one][Localization.DEFAULT].
     *
     *
     * @param localization the [Localization] to set in the user-agent
     * @return the iOS user-agent used for InnerTube requests with the iOS client, depending on the
     * [Localization] provided
     */

    fun getIosUserAgent(localization: Localization?): String {
        return ("com.google.ios.youtube/" + ClientsConstants.IOS_CLIENT_VERSION + "(" + ClientsConstants.IOS_DEVICE_MODEL
                + "; U; CPU iOS " + ClientsConstants.IOS_USER_AGENT_VERSION + " like Mac OS X; "
                + (localization ?: Localization.DEFAULT).countryCode
                + ")")
    }


    val tvHtml5UserAgent: String
        /**
         * Get the user-agent string used as the user-agent for InnerTube requests with the HTML5 TV
         * client.
         *
         * @return the user-agent used for InnerTube requests with the TVHTML5 client
         */
        get() = ClientsConstants.TVHTML5_USER_AGENT

    val youtubeMusicHeaders: Map<String, List<String>>
        /**
         * Returns a [Map] containing the required YouTube Music headers.
         */
        get() {
            val headers =
                HashMap(
                    getOriginReferrerHeaders(YOUTUBE_MUSIC_URL)
                )
            headers.putAll(
                getClientHeaders(
                    ClientsConstants.WEB_REMIX_CLIENT_ID,
                    youtubeMusicClientVersion!!
                )
            )
            return headers
        }

    suspend fun youTubeHeaders(): Map<String, List<String>> {
        val headers = clientInfoHeaders()
        headers["Cookie"] = listOf(generateConsentCookie())
        return headers
    }

    suspend fun clientInfoHeaders(): MutableMap<String, List<String>> {
        val headers = HashMap(getOriginReferrerHeaders("https://www.youtube.com"))
        headers.putAll(
            getClientHeaders(
                ClientsConstants.WEB_CLIENT_ID,
                getClientVersion()!! // suspend 가능
            )
        )
        return headers
    }

    /**
     * Returns an unmodifiable [Map] containing the `Origin` and `Referer`
     * headers set to the given URL.
     *
     * @param url The URL to be set as the origin and referrer.
     */

    fun getOriginReferrerHeaders(url: String): Map<String, List<String>> {
        val urlList = listOf(url)
        return mapOf("Origin" to urlList, "Referer" to urlList)
    }

    /**
     * Returns an unmodifiable [Map] containing the `X-YouTube-Client-Name` and
     * `X-YouTube-Client-Version` headers.
     *
     * @param name The X-YouTube-Client-Name value.
     * @param version X-YouTube-Client-Version value.
     */

    fun getClientHeaders(
        name: String,
        version: String
    ): Map<String, List<String>> {
        return mapOf(
            "X-YouTube-Client-Name" to listOf(name),
            "X-YouTube-Client-Version" to listOf(version)
        )
    }

    val cookieHeader: Map<String, List<String>>
        /**
         * Create a map with the required cookie header.
         * @return A singleton map containing the header.
         */
        get() = mapOf(
            "Cookie" to
                    listOf(generateConsentCookie())
        )


    fun generateConsentCookie(): String {
        return "SOCS=" + (if (isConsentAccepted // CAISAiAD means that the user configured manually cookies YouTube, regardless of
        // the consent values
        // This value surprisingly allows to extract mixes and some YouTube Music playlists
        // in the same way when a user allows all cookies
        )
            "CAISAiAD" // CAE= means that the user rejected all non-necessary cookies with the "Reject
        // all" button on the consent page
        else
            "CAE=")
    }


    fun extractCookieValue(
        cookieName: String,
        response: Response
    ): String {
        val cookies = response.responseHeaders()["set-cookie"] ?: return ""

        var result = ""
        for (cookie in cookies) {
            val startIndex = cookie.indexOf(cookieName)
            if (startIndex != -1) {
                result = cookie.substring(
                    startIndex + cookieName.length + "=".length,
                    cookie.indexOf(";", startIndex)
                )
            }
        }
        return result
    }

    /**
     * Shared alert detection function, multiple endpoints return the error similarly structured.
     *
     *
     * Will check if the object has an alert of the type "ERROR".
     *
     *
     * @param initialData the object which will be checked if an alert is present
     * @throws ContentNotAvailableException if an alert is detected
     */
    /**
     * Sometimes, YouTube provides URLs which use Google's cache. They look like
     * `https://webcache.googleusercontent.com/search?q=cache:CACHED_URL`
     *
     * @param url the URL which might refer to the Google's webcache
     * @return the URL which is referring to the original site
     */

    fun extractCachedUrlIfNeeded(url: String?): String? {
        if (url == null) {
            return null
        }
        if (url.contains("webcache.googleusercontent.com")) {
            return url.split("cache:".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
        }
        return url
    }

    /**
     * Generate a content playback nonce (also called `cpn`), sent by YouTube clients in
     * playback requests (and also for some clients, in the player request body).
     *
     * @return a content playback nonce string
     */

    fun generateContentPlaybackNonce(): String {
        return RandomStringFromAlphabetGenerator.generate(
            CONTENT_PLAYBACK_NONCE_ALPHABET, 16, numberGenerator
        )
    }

    /**
     * Try to generate a `t` parameter, sent by mobile clients as a query of the player
     * request.
     *
     *
     *
     * Some researches needs to be done to know how this parameter, unique at each request, is
     * generated.
     *
     *
     * @return a 12 characters string to try to reproduce the `` parameter
     */

    fun generateTParameter(): String {
        return RandomStringFromAlphabetGenerator.generate(
            CONTENT_PLAYBACK_NONCE_ALPHABET, 12, numberGenerator
        )
    }

    /**
     * Check if the streaming URL is from the YouTube `WEB` client.
     *
     * @param url the streaming URL to be checked.
     * @return true if it's a `WEB` streaming URL, false otherwise
     */

    fun isWebStreamingUrl(url: String): Boolean {
        return Parser.isMatch(C_WEB_PATTERN, url)
    }

    /**
     * Check if the streaming URL is from the YouTube `WEB_EMBEDDED_PLAYER` client.
     *
     * @param url the streaming URL to be checked.
     * @return true if it's a `WEB_EMBEDDED_PLAYER` streaming URL, false otherwise
     */

    fun isWebEmbeddedPlayerStreamingUrl(url: String): Boolean {
        return Parser.isMatch(C_WEB_EMBEDDED_PLAYER_PATTERN, url)
    }

    /**
     * Check if the streaming URL is a URL from the YouTube `TVHTML5` client.
     *
     * @param url the streaming URL on which check if it's a `TVHTML5`
     * streaming URL.
     * @return true if it's a `TVHTML5` streaming URL, false otherwise
     */

    fun isTvHtml5StreamingUrl(url: String): Boolean {
        return Parser.isMatch(C_TVHTML5_PLAYER_PATTERN, url)
    }

    /**
     * Check if the streaming URL is a URL from the YouTube `ANDROID` client.
     *
     * @param url the streaming URL to be checked.
     * @return true if it's a `ANDROID` streaming URL, false otherwise
     */

    fun isAndroidStreamingUrl(url: String): Boolean {
        return Parser.isMatch(C_ANDROID_PATTERN, url)
    }

    /**
     * Check if the streaming URL is a URL from the YouTube `IOS` client.
     *
     * @param url the streaming URL on which check if it's a `IOS` streaming URL.
     * @return true if it's a `IOS` streaming URL, false otherwise
     */

    fun isIosStreamingUrl(url: String): Boolean {
        return Parser.isMatch(C_IOS_PATTERN, url)
    }

    /**
     * Extract the audio track type from a YouTube stream URL.
     *
     *
     * The track type is parsed from the `xtags` URL parameter
     * (Example: `acont=original:lang=en`).
     *
     * @param streamUrl YouTube stream URL
     * @return [AudioTrackType] or `null` if no track type was found
     */

    fun extractAudioTrackType(streamUrl: String): AudioTrackType? {
        val xtags: String?
        try {
            xtags = Utils.getQueryValue(streamUrl, "xtags")
        } catch (e: IllegalArgumentException) {
            return null
        }
        if (xtags == null) {
            return null
        }

        var atype: String? = null
        for (param in xtags.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
            val kv = param.split("=".toRegex(), limit = 2).toTypedArray()
            if (kv.size > 1 && kv[0] == "acont") {
                atype = kv[1]
                break
            }
        }
        if (atype == null) {
            return null
        }

        return when (atype) {
            "original" -> AudioTrackType.ORIGINAL
            "dubbed", "dubbed-auto" -> AudioTrackType.DUBBED
            "descriptive" -> AudioTrackType.DESCRIPTIVE
            "secondary" -> AudioTrackType.SECONDARY
            else -> null
        }
    }


    suspend fun getVisitorDataFromInnertube(
        innertubeClientRequestInfo: InnertubeClientRequestInfo,
        localization: Localization,
        contentCountry: ContentCountry,
        httpHeaders: Map<String, List<String>>,
        innertubeDomainAndVersionEndpoint: String,
        embedUrl: String?,
        useGuideEndpoint: Boolean
    ): String {
        val builder = prepareJsonBuilder(
            localization, contentCountry, innertubeClientRequestInfo, embedUrl
        )

        val body = JsonWriter.string(builder.done())
            .toByteArray(Charsets.UTF_8)

        val visitorData = JsonUtils.toJsonObject(
            getValidJsonResponseBody(
                NewPipe.downloader
                    .postWithContentTypeJson(
                        (innertubeDomainAndVersionEndpoint
                                + (if (useGuideEndpoint) "guide" else "visitor_id") + "?"
                                + DISABLE_PRETTY_PRINT_PARAMETER),
                        httpHeaders, body
                    )
            )
        )
            .getObject("responseContext")
            .getString("visitorData")


        requireNotNull(visitorData) {
            ParsingException("Could not get visitorData")
        }

        return visitorData
    }


    fun prepareJsonBuilder(
        localization: Localization,
        contentCountry: ContentCountry,
        innertubeClientRequestInfo: InnertubeClientRequestInfo,
        embedUrl: String?
    ): JsonBuilder<JsonObject> {
        val builder = JsonObject.builder()
            .`object`("context")
            .`object`("client")
            .value("clientName", innertubeClientRequestInfo.clientInfo.clientName)
            .value("clientVersion", innertubeClientRequestInfo.clientInfo.clientVersion)
            .value("clientScreen", innertubeClientRequestInfo.clientInfo.clientScreen)
            .value("platform", innertubeClientRequestInfo.deviceInfo.platform)

        if (innertubeClientRequestInfo.clientInfo.visitorData != null) {
            builder.value("visitorData", innertubeClientRequestInfo.clientInfo.visitorData)
        }

        if (innertubeClientRequestInfo.deviceInfo.deviceMake != null) {
            builder.value("deviceMake", innertubeClientRequestInfo.deviceInfo.deviceMake)
        }
        if (innertubeClientRequestInfo.deviceInfo.deviceModel != null) {
            builder.value("deviceModel", innertubeClientRequestInfo.deviceInfo.deviceModel)
        }
        if (innertubeClientRequestInfo.deviceInfo.osName != null) {
            builder.value("osName", innertubeClientRequestInfo.deviceInfo.osName)
        }
        if (innertubeClientRequestInfo.deviceInfo.osVersion != null) {
            builder.value("osVersion", innertubeClientRequestInfo.deviceInfo.osVersion)
        }
        if (innertubeClientRequestInfo.deviceInfo.androidSdkVersion > 0) {
            builder.value(
                "androidSdkVersion",
                innertubeClientRequestInfo.deviceInfo.androidSdkVersion
            )
        }

        builder.value("hl", localization.localizationCode)
            .value("gl", contentCountry.countryCode)
            .value("utcOffsetMinutes", 0)
            .end()

        if (embedUrl != null) {
            builder.`object`("thirdParty")
                .value("embedUrl", embedUrl)
                .end()
        }

        builder.`object`("request")
            .array("internalExperimentFlags")
            .end()
            .value("useSsl", true)
            .end()
            .`object`("user") // TODO: provide a way to enable restricted mode with:
            //  .value("enableSafetyMode", boolean)
            .value("lockedSafetyMode", false)
            .end()
            .end()

        return builder
    }
}
