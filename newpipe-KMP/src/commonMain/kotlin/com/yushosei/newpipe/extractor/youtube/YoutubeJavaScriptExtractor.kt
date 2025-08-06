package com.yushosei.newpipe.extractor.youtube

import io.ktor.http.Url
import com.yushosei.newpipe.extractor.NewPipe
import com.yushosei.newpipe.extractor.exceptions.ParsingException
import com.yushosei.newpipe.extractor.localization.Localization
import com.yushosei.newpipe.extractor.utils.Parser
import com.yushosei.newpipe.extractor.utils.Parser.RegexException

/**
 * The extractor of YouTube's base JavaScript player file.
 *
 *
 *
 * This class handles fetching of this base JavaScript player file in order to allow other classes
 * to extract the needed data.
 *
 *
 *
 *
 * It will try to get the player URL from YouTube's IFrame resource first, and from a YouTube embed
 * watch page as a fallback.
 *
 */
internal object YoutubeJavaScriptExtractor {
    private const val HTTPS = "https:"
    private const val BASE_JS_PLAYER_URL_FORMAT =
        "https://www.youtube.com/s/player/%s/player_ias.vflset/en_GB/base.js"
    private val IFRAME_RES_JS_BASE_PLAYER_HASH_PATTERN: Regex = Regex(
        "player\\\\/([a-z0-9]{8})\\\\/"
    )
    private val EMBEDDED_WATCH_PAGE_JS_BASE_PLAYER_URL_PATTERN: Regex = Regex(
        "\"jsUrl\":\"(/s/player/[A-Za-z0-9]+/player_ias\\.vflset/[A-Za-z_-]+/base\\.js)\""
    )

    /**
     * Extracts the JavaScript base player file.
     *
     * @param videoId the video ID used to get the JavaScript base player file (an empty one can be
     * passed, even it is not recommend in order to spoof better official YouTube
     * clients)
     * @return the whole JavaScript base player file as a string
     * @throws ParsingException if the extraction of the file failed
     */
    
    fun extractJavaScriptPlayerCode(videoId: String): String {
        var url: String
        try {
            url = extractJavaScriptUrlWithIframeResource()
            val playerJsUrl = cleanJavaScriptUrl(url)

            // Assert that the URL we extracted and built is valid
            Url(playerJsUrl)

            return downloadJavaScriptCode(playerJsUrl)
        } catch (e: Exception) {
            url = extractJavaScriptUrlWithEmbedWatchPage(videoId)
            val playerJsUrl = cleanJavaScriptUrl(url)

            try {
                // Assert that the URL we extracted and built is valid
                Url(playerJsUrl)
            } catch (exception: IllegalArgumentException) {
                throw ParsingException(
                    "The extracted and built JavaScript URL is invalid", exception
                )
            }

            return downloadJavaScriptCode(playerJsUrl)
        }
    }


    
    fun extractJavaScriptUrlWithIframeResource(): String {
        val iframeUrl: String
        val iframeContent: String
        try {
            iframeUrl = "https://www.youtube.com/iframe_api"
            iframeContent = NewPipe.downloader
                .get(iframeUrl, Localization.DEFAULT)
                .responseBody()
        } catch (e: Exception) {
            throw ParsingException("Could not fetch IFrame resource", e)
        }

        try {
            val hash = Parser.matchGroup1(
                IFRAME_RES_JS_BASE_PLAYER_HASH_PATTERN, iframeContent
            )
            // String.format(BASE_JS_PLAYER_URL_FORMAT, hash)
            return "https://www.youtube.com/s/player/$hash/player_ias.vflset/en_GB/base.js"
        } catch (e: RegexException) {
            throw ParsingException(
                "IFrame resource didn't provide JavaScript base player's hash", e
            )
        }
    }

    
    fun extractJavaScriptUrlWithEmbedWatchPage(videoId: String): String {
        val embedUrl = "https://www.youtube.com/embed/$videoId"
        val embedPageContent = try {
            NewPipe.downloader
                .get(embedUrl, Localization.DEFAULT)
                .responseBody()
        } catch (e: Exception) {
            throw ParsingException("Could not fetch embedded watch page", e)
        }

        // 1️⃣ Regex로 <script ... src="...base.js"...> 추출 시도
        val regex = Regex("""<script[^>]+src=["']([^"']*base\.js[^"']*)["']""")
        val match = regex.find(embedPageContent)
        if (match != null) {
            return match.groupValues[1]
        }

        // 2️⃣ fallback: 기존 parser를 통한 추출
        try {
            return Parser.matchGroup1(
                EMBEDDED_WATCH_PAGE_JS_BASE_PLAYER_URL_PATTERN,
                embedPageContent
            )
        } catch (e: RegexException) {
            throw ParsingException(
                "Embedded watch page didn't provide JavaScript base player's URL", e
            )
        }
    }

    private fun cleanJavaScriptUrl(javaScriptPlayerUrl: String): String {
        return if (javaScriptPlayerUrl.startsWith("//")) {
            // https part has to be added manually if the URL is protocol-relative
            HTTPS + javaScriptPlayerUrl
        } else if (javaScriptPlayerUrl.startsWith("/")) {
            // https://www.youtube.com part has to be added manually if the URL is relative to
            // YouTube's domain
            HTTPS + "//www.youtube.com" + javaScriptPlayerUrl
        } else {
            javaScriptPlayerUrl
        }
    }


    
    private fun downloadJavaScriptCode(javaScriptPlayerUrl: String): String {
        try {
            return NewPipe.downloader
                .get(javaScriptPlayerUrl, Localization.DEFAULT)
                .responseBody()
        } catch (e: Exception) {
            throw ParsingException("Could not get JavaScript base player's code", e)
        }
    }
}
