package com.yushosei.newpipe.extractor.soundcloud.linkHandler

import com.yushosei.newpipe.extractor.exceptions.ParsingException
import com.yushosei.newpipe.extractor.linkhandler.LinkHandlerFactory

class SoundcloudStreamLinkHandlerFactory private constructor() : LinkHandlerFactory() {
    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getUrl(id: String): String {
        return if (id.startsWith("http://") || id.startsWith("https://")) {
            id
        } else {
            "https://soundcloud.com/$id"
        }
    }

    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getId(url: String): String {
        val normalized = url.trim()
        API_URL_PATTERN.find(normalized)?.let { match ->
            return match.groupValues[1]
        }

        if (!URL_PATTERN.matches(normalized.lowercase())) {
            throw ParsingException("The URL is not a SoundCloud track URL")
        }

        return normalized.substringBefore("#")
    }

    override fun onAcceptUrl(url: String): Boolean {
        return URL_PATTERN.matches(url.lowercase())
    }

    companion object {
        val instance = SoundcloudStreamLinkHandlerFactory()

        private const val ON_URL_PATTERN = "^https?://on\\.soundcloud\\.com/[0-9a-zA-Z]+$"
        private val URL_PATTERN = Regex(
            "^https?://(?:www\\.|m\\.)?" +
                    "soundcloud\\.com/[0-9a-z_-]+" +
                    "/(?!(?:tracks|albums|sets|reposts|followers|following)/?$)" +
                    "[0-9a-z_-]+/?(?:[#?].*)?$|$ON_URL_PATTERN"
        )
        private val API_URL_PATTERN = Regex(
            "^https?://api-v2\\.soundcloud\\.com/" +
                    "(?:tracks|albums|sets|reposts|followers|following)/([0-9a-z_-]+)/?"
        )
    }
}
