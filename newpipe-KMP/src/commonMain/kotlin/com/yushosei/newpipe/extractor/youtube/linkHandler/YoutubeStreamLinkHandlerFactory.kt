/*
 * Created by Christian Schabesberger on 02.02.16.
 *
 * Copyright (C) 2018 Christian Schabesberger <chris.schabesberger@mailbox.org>
 * YoutubeStreamLinkHandlerFactory.java is part of NewPipe Extractor.
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
 * along with NewPipe Extractor.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.yushosei.newpipe.extractor.youtube.linkHandler

import io.ktor.http.Url
import com.yushosei.newpipe.extractor.exceptions.FoundAdException
import com.yushosei.newpipe.extractor.exceptions.ParsingException
import com.yushosei.newpipe.extractor.linkhandler.LinkHandlerFactory
import com.yushosei.newpipe.extractor.youtube.YoutubeParsingHelper.isHooktubeURL
import com.yushosei.newpipe.extractor.youtube.YoutubeParsingHelper.isInvidiousURL
import com.yushosei.newpipe.extractor.youtube.YoutubeParsingHelper.isY2ubeURL
import com.yushosei.newpipe.extractor.youtube.YoutubeParsingHelper.isYoutubeServiceURL
import com.yushosei.newpipe.extractor.youtube.YoutubeParsingHelper.isYoutubeURL
import com.yushosei.newpipe.extractor.utils.Utils

class YoutubeStreamLinkHandlerFactory private constructor() : LinkHandlerFactory() {
    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getUrl(id: String): String {
        return "https://www.youtube.com/watch?v=$id"
    }

    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getId(theUrlString: String): String {
        var urlString = theUrlString
        try {
            val uri = Url(urlString)
            val scheme = uri.protocol.name

            if (scheme == "vnd.youtube" || scheme == "vnd.youtube.launch") {
                val schemeSpecificPart = uri.encodedPath
                if (schemeSpecificPart.startsWith("//")) {
                    val extractedId = extractId(schemeSpecificPart.removePrefix("//"))
                    if (extractedId != null) return extractedId
                    urlString = "https:$schemeSpecificPart"
                } else {
                    return assertIsId(schemeSpecificPart)
                }
            }
        } catch (ignored: IllegalArgumentException) {
            // Do nothing, fallback below
        }

        val url: Url = try {
            Url(urlString)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("The given URL is not valid: $urlString")
        }

        val host = url.host.lowercase()
        var path = url.encodedPath.removePrefix("/")

        if (!Utils.isHttp(url) || !(isYoutubeURL(url) || isYoutubeServiceURL(url)
                    || isHooktubeURL(url) || isInvidiousURL(url) || isY2ubeURL(url))
        ) {
            if (host.equals("googleads.g.doubleclick.net", ignoreCase = true)) {
                throw FoundAdException("Error: found ad: $urlString")
            }
            throw ParsingException("The URL is not a YouTube URL")
        }

        when (host.uppercase()) {
            "WWW.YOUTUBE-NOCOOKIE.COM" -> {
                if (path.startsWith("embed/")) {
                    return assertIsId(path.removePrefix("embed/"))
                }
            }

            "YOUTUBE.COM", "WWW.YOUTUBE.COM", "M.YOUTUBE.COM", "MUSIC.YOUTUBE.COM" -> {
                if (path == "attribution_link") {
                    val uQueryValue = Utils.getQueryValue(url, "u")
                    val decodedUrl = try {
                        Url("https://www.youtube.com$uQueryValue")
                    } catch (e: IllegalArgumentException) {
                        throw ParsingException("Error: no suitable URL: $urlString")
                    }
                    val viewQueryValue = Utils.getQueryValue(decodedUrl, "v")
                    return assertIsId(viewQueryValue)
                }

                getIdFromSubpathsInPath(path)?.let { return it }
                return assertIsId(Utils.getQueryValue(url, "v"))
            }

            "Y2U.BE", "YOUTU.BE" -> {
                Utils.getQueryValue(url, "v")?.let { return assertIsId(it) }
                return assertIsId(path)
            }

            in setOf(
                "HOOKTUBE.COM",
                "INVIDIO.US",
                "DEV.INVIDIO.US",
                "WWW.INVIDIO.US",
                "REDIRECT.INVIDIOUS.IO",
                "INVIDIOUS.SNOPYTA.ORG",
                "YEWTU.BE",
                "TUBE.CONNECT.CAFE",
                "TUBUS.EDUVID.ORG",
                "INVIDIOUS.KAVIN.ROCKS",
                "INVIDIOUS-US.KAVIN.ROCKS",
                "PIPED.KAVIN.ROCKS",
                "INVIDIOUS.SITE",
                "VID.MINT.LGBT",
                "INVIDIOU.SITE",
                "INVIDIOUS.FDN.FR",
                "INVIDIOUS.048596.XYZ",
                "INVIDIOUS.ZEE.LI",
                "VID.PUFFYAN.US",
                "YTPRIVATE.COM",
                "INVIDIOUS.NAMAZSO.EU",
                "INVIDIOUS.SILKKY.CLOUD",
                "INVIDIOUS.EXONIP.DE",
                "INV.RIVERSIDE.ROCKS",
                "INVIDIOUS.BLAMEFRAN.NET",
                "INVIDIOUS.MOOMOO.ME",
                "YTB.TROM.TF",
                "YT.CYBERHOST.UK",
                "Y.COM.CM"
            ) -> {
                if (path == "watch") {
                    Utils.getQueryValue(url, "v")?.let { return assertIsId(it) }
                }
                getIdFromSubpathsInPath(path)?.let { return it }
                Utils.getQueryValue(url, "v")?.let { return assertIsId(it) }
                return assertIsId(path)
            }
        }

        throw ParsingException("Error: no suitable URL: $urlString")
    }

    override fun onAcceptUrl(url: String): Boolean {
        try {
            getId(url)
            return true
        } catch (fe: FoundAdException) {
            throw fe
        } catch (e: ParsingException) {
            return false
        }
    }


    
    private fun getIdFromSubpathsInPath(path: String): String? {
        for (subpath in SUBPATHS) {
            if (path.startsWith(subpath)) {
                val id = path.substring(subpath.length)
                return assertIsId(id)
            }
        }
        return null
    }

    companion object {
        private val YOUTUBE_VIDEO_ID_REGEX_PATTERN
                : Regex = Regex("^([a-zA-Z0-9_-]{11})")
        val instance
                : YoutubeStreamLinkHandlerFactory = YoutubeStreamLinkHandlerFactory()
        private val SUBPATHS = listOf("embed/", "live/", "shorts/", "watch/", "v/", "w/")


        private fun extractId(id: String?): String? {
            return id?.let {
                YOUTUBE_VIDEO_ID_REGEX_PATTERN.find(it)?.groupValues?.getOrNull(1)
            }
        }


        
        private fun assertIsId(id: String?): String {
            val extractedId = extractId(id)!!
            if (extractedId != null) {
                return extractedId
            } else {
                throw ParsingException("The given string is not a YouTube video ID")
            }
        }
    }
}
