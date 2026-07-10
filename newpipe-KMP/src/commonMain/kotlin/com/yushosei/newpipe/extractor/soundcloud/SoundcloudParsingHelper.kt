package com.yushosei.newpipe.extractor.soundcloud

import com.yushosei.newpipe.extractor.Image
import com.yushosei.newpipe.extractor.NewPipe
import com.yushosei.newpipe.extractor.downloader.Downloader
import com.yushosei.newpipe.extractor.exceptions.ExtractionException
import com.yushosei.newpipe.extractor.exceptions.ParsingException
import com.yushosei.newpipe.extractor.utils.JsonUtils
import com.yushosei.newpipe.extractor.utils.Parser
import com.yushosei.newpipe.extractor.utils.Utils
import com.yushosei.newpipe.nanojson.JsonObject
import com.yushosei.newpipe.nanojson.JsonParser
import com.yushosei.newpipe.nanojson.JsonParserException

internal object SoundcloudParsingHelper {
    const val SOUNDCLOUD_API_V2_URL = "https://api-v2.soundcloud.com/"

    private const val SOUNDCLOUD_URL = "https://soundcloud.com"
    private const val CLIENT_ID_PATTERN = ",client_id:\\\"(.*?)\\\""
    private const val API_CLIENT_HYDRATABLE = "apiClient"

    private val onUrlPattern = Regex("^https?://on\\.soundcloud\\.com/[0-9a-zA-Z]+$")
    private val hydrationPattern = Regex(
        "window\\.__sc_hydration\\s*=\\s*(\\[.*?]);",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val scriptAssetPattern = Regex(
        "<script[^>]+src=[\"']([^\"']*sndcdn\\.com/assets/[^\"']+\\.js[^\"']*)[\"'][^>]*>",
        RegexOption.IGNORE_CASE
    )
    private val canonicalPatternA = Regex(
        "<link[^>]+rel=[\"']canonical[\"'][^>]+href=[\"']([^\"']+)[\"'][^>]*>",
        RegexOption.IGNORE_CASE
    )
    private val canonicalPatternB = Regex(
        "<link[^>]+href=[\"']([^\"']+)[\"'][^>]+rel=[\"']canonical[\"'][^>]*>",
        RegexOption.IGNORE_CASE
    )

    private var cachedClientId: String? = null

    private data class ArtworkVariant(
        val suffix: String,
        val width: Int,
        val height: Int,
        val level: Image.ResolutionLevel
    )

    private val artworkVariants = listOf(
        ArtworkVariant("t50x50", 50, 50, Image.ResolutionLevel.LOW),
        ArtworkVariant("large", 100, 100, Image.ResolutionLevel.LOW),
        ArtworkVariant("t200x200", 200, 200, Image.ResolutionLevel.MEDIUM),
        ArtworkVariant("t300x300", 300, 300, Image.ResolutionLevel.MEDIUM),
        ArtworkVariant("t500x500", 500, 500, Image.ResolutionLevel.MEDIUM)
    )

    suspend fun clientId(pageUrl: String? = null, forceRefresh: Boolean = false): String {
        if (!forceRefresh) {
            cachedClientId?.let { if (it.isNotEmpty()) return it }
        }

        val downloader = NewPipe.downloader
        val currentPageHtml = fetchPageHtmlOrNull(downloader, pageUrl)
        extractClientIdFromHydration(currentPageHtml)?.let { clientId ->
            cachedClientId = clientId
            return clientId
        }

        val homeHtml = when {
            currentPageHtml != null && isHomepage(pageUrl) -> currentPageHtml
            else -> fetchPageHtmlOrNull(downloader, SOUNDCLOUD_URL)
        }
        extractClientIdFromHydration(homeHtml)?.let { clientId ->
            cachedClientId = clientId
            return clientId
        }

        extractClientIdFromHomepageAssets(downloader, homeHtml)?.let { clientId ->
            cachedClientId = clientId
            return clientId
        }

        homeHtml?.let { html ->
            try {
                val extracted = Parser.matchGroup1(CLIENT_ID_PATTERN, html)
                if (extracted.isNotEmpty()) {
                    cachedClientId = extracted
                    return extracted
                }
            } catch (_: Exception) {
            }
        }

        throw ExtractionException(
            "Couldn't extract SoundCloud client id" +
                    if (pageUrl.isNullOrEmpty()) "" else " for $pageUrl"
        )
    }

    suspend fun withClientId(url: String): String {
        return withClientId(url, clientId())
    }

    internal fun invalidateClientIdCache() {
        cachedClientId = null
    }

    fun withClientId(url: String, clientId: String): String {
        if (url.contains("client_id=")) {
            return url
        }
        val separator = if (url.contains("?")) "&" else "?"
        return "$url${separator}client_id=$clientId"
    }

    suspend fun resolveFor(downloader: Downloader, trackUrl: String): JsonObject {
        val resolvedClientId = clientId(trackUrl)
        val apiUrl = withClientId(
            "$SOUNDCLOUD_API_V2_URL" +
                    "resolve?url=${Utils.encodeUrlUtf8(trackUrl)}",
            resolvedClientId
        )

        return try {
            JsonParser.`object`().from(downloader.get(apiUrl).responseBody())
        } catch (e: JsonParserException) {
            throw ParsingException("Could not parse json response", e)
        }
    }

    suspend fun normalizeTrackUrl(url: String): String {
        var fixedUrl = url
        if (onUrlPattern.matches(fixedUrl)) {
            fixedUrl = NewPipe.downloader.head(fixedUrl).latestUrl().substringBefore("?")
        }
        return if (fixedUrl.endsWith("/")) fixedUrl.dropLast(1) else fixedUrl
    }

    suspend fun resolveUrlWithEmbedPlayer(apiUrl: String): String {
        val response = NewPipe.downloader.get(
            "https://w.soundcloud.com/player/?url=${Utils.encodeUrlUtf8(apiUrl)}"
        ).responseBody()

        val resolved = canonicalPatternA.find(response)?.groupValues?.getOrNull(1)
            ?: canonicalPatternB.find(response)?.groupValues?.getOrNull(1)
            ?: throw ParsingException("Could not resolve SoundCloud canonical URL")

        return Utils.replaceHttpWithHttps(resolved)
    }

    suspend fun resolveIdWithWidgetApi(urlString: String): String {
        var fixedUrl = normalizeTrackUrl(urlString)
        fixedUrl = Utils.removeMAndWWWFromUrl(fixedUrl.lowercase())

        val widgetUrl = withClientId(
            "https://api-widget.soundcloud.com/resolve" +
                    "?url=${Utils.encodeUrlUtf8(Utils.stringToUrl(fixedUrl).toString())}" +
                    "&format=json"
        )

        return try {
            val response = NewPipe.downloader.get(widgetUrl).responseBody()
            val obj = JsonParser.`object`().from(response)
            JsonUtils.getValue(obj, "id").toString()
        } catch (e: JsonParserException) {
            throw ParsingException("Could not parse JSON response", e)
        }
    }

    fun getNextPageUrl(response: JsonObject, clientId: String?): String? {
        val nextHref = response.getString("next_href", "")
        if (nextHref.isEmpty()) {
            return null
        }

        return if (nextHref.contains("client_id=") || clientId.isNullOrEmpty()) {
            nextHref
        } else {
            withClientId(nextHref, clientId)
        }
    }

    fun getUploaderUrl(trackObject: JsonObject): String {
        return Utils.replaceHttpWithHttps(
            trackObject.getObject("user").getString("permalink_url", "")
        )
    }

    fun getAvatarUrl(trackObject: JsonObject): String {
        return Utils.replaceHttpWithHttps(
            trackObject.getObject("user").getString("avatar_url", "")
        )
    }

    fun getUploaderName(trackObject: JsonObject): String {
        return trackObject.getObject("user").getString("username", "")
    }

    fun getAllImagesFromTrackObject(trackObject: JsonObject): List<Image> {
        val artworkUrl = trackObject.getString("artwork_url", "")
        if (artworkUrl.isNotEmpty()) {
            return getAllImagesFromArtworkOrAvatarUrl(artworkUrl)
        }

        val avatarUrl = trackObject.getObject("user").getString("avatar_url", "")
        if (avatarUrl.isNotEmpty()) {
            return getAllImagesFromArtworkOrAvatarUrl(avatarUrl)
        }

        throw ParsingException("Could not get track or uploader thumbnails")
    }

    fun getAllImagesFromArtworkOrAvatarUrl(originalArtworkOrAvatarUrl: String?): List<Image> {
        if (originalArtworkOrAvatarUrl.isNullOrEmpty()) {
            return emptyList()
        }

        val normalized = Utils.replaceHttpWithHttps(originalArtworkOrAvatarUrl)
        if (!normalized.contains("-large.")) {
            return listOf(
                Image(
                    normalized,
                    Image.HEIGHT_UNKNOWN,
                    Image.WIDTH_UNKNOWN,
                    Image.ResolutionLevel.UNKNOWN
                )
            )
        }

        val baseFormat = normalized.replace("-large.", "-%s.")
        return artworkVariants.map { variant ->
            Image(
                baseFormat.replace("%s", variant.suffix),
                variant.height,
                variant.width,
                variant.level
            )
        }
    }

    private suspend fun fetchPageHtmlOrNull(downloader: Downloader, pageUrl: String?): String? {
        if (pageUrl.isNullOrBlank()) {
            return null
        }
        return try {
            downloader.get(pageUrl).responseBody()
        } catch (_: Exception) {
            null
        }
    }

    private fun extractClientIdFromHydration(pageHtml: String?): String? {
        if (pageHtml.isNullOrBlank()) {
            return null
        }

        val hydrationJson = hydrationPattern.find(pageHtml)?.groupValues?.getOrNull(1) ?: return null
        val hydration = try {
            JsonParser.array().from(hydrationJson)
        } catch (_: JsonParserException) {
            return null
        }

        for (entry in hydration) {
            val hydrationEntry = entry as? JsonObject ?: continue
            if (hydrationEntry.getString("hydratable", "") != API_CLIENT_HYDRATABLE) {
                continue
            }

            val hydratedClientId = hydrationEntry.getObject("data").getString("id", "")
            if (hydratedClientId.isNotEmpty()) {
                return hydratedClientId
            }
        }

        return null
    }

    private suspend fun extractClientIdFromHomepageAssets(
        downloader: Downloader,
        homeHtml: String?
    ): String? {
        if (homeHtml.isNullOrBlank()) {
            return null
        }

        val rangeHeaders = mapOf("Range" to listOf("bytes=0-50000"))
        val scriptUrls = scriptAssetPattern.findAll(homeHtml)
            .map { it.groupValues[1] }
            .toList()
            .asReversed()

        for (scriptUrl in scriptUrls) {
            val absoluteScriptUrl = when {
                scriptUrl.startsWith("//") -> "https:$scriptUrl"
                scriptUrl.startsWith("/") -> "$SOUNDCLOUD_URL$scriptUrl"
                else -> scriptUrl
            }

            try {
                val scriptBody = downloader.get(absoluteScriptUrl, rangeHeaders).responseBody()
                val extracted = Parser.matchGroup1(CLIENT_ID_PATTERN, scriptBody)
                if (extracted.isNotEmpty()) {
                    return extracted
                }
            } catch (_: Exception) {
                // Continue to the next script until one yields client_id.
            }
        }

        return null
    }

    private fun isHomepage(pageUrl: String?): Boolean {
        val normalizedPageUrl = pageUrl?.removeSuffix("/") ?: return false
        return normalizedPageUrl == SOUNDCLOUD_URL
    }
}
