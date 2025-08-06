/*
 * Copyright (C) 2016 Christian Schabesberger <chris.schabesberger@mailbox.org>
 * YoutubeStreamInfoItemExtractor.java is part of NewPipe Extractor.
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
package com.yushosei.newpipe.extractor.youtube.extractors

import com.yushosei.newpipe.extractor.Image
import com.yushosei.newpipe.extractor.exceptions.ParsingException
import com.yushosei.newpipe.extractor.youtube.YoutubeParsingHelper.getTextFromObject
import com.yushosei.newpipe.extractor.youtube.YoutubeParsingHelper.getThumbnailsFromInfoItem
import com.yushosei.newpipe.extractor.youtube.YoutubeParsingHelper.getUrlFromNavigationEndpoint
import com.yushosei.newpipe.extractor.youtube.YoutubeParsingHelper.parseDurationString
import com.yushosei.newpipe.extractor.youtube.linkHandler.YoutubeStreamLinkHandlerFactory.Companion.instance
import com.yushosei.newpipe.extractor.stream.StreamInfoItemExtractor
import com.yushosei.newpipe.extractor.stream.StreamType
import com.yushosei.newpipe.extractor.utils.Utils
import com.yushosei.newpipe.nanojson.JsonObject

internal class YoutubeStreamInfoItemExtractor
/**
 * Creates an extractor of StreamInfoItems from a YouTube page.
 *
 * @param videoInfoItem The JSON page element
 */(
    private val videoInfo: JsonObject,
) : StreamInfoItemExtractor {
    private var cachedStreamType: StreamType? = null
    private var isPremiere: Boolean? = null
        get() {
            if (field == null) {
                field = videoInfo.has("upcomingEventData")
            }
            return field
        }

    override val streamType: StreamType
        get() {
            if (cachedStreamType != null) {
                return cachedStreamType!!
            }

            val badges = videoInfo.getArray("badges")
            for (badge in badges) {
                if (badge !is JsonObject) {
                    continue
                }

                val badgeRenderer =
                    badge.getObject("metadataBadgeRenderer")
                if (badgeRenderer.getString("style", "") == "BADGE_STYLE_TYPE_LIVE_NOW"
                    || badgeRenderer.getString("label", "") == "LIVE NOW"
                ) {
                    cachedStreamType = StreamType.LIVE_STREAM
                    return cachedStreamType!!
                }
            }

            for (overlay in videoInfo.getArray("thumbnailOverlays")) {
                if (overlay !is JsonObject) {
                    continue
                }

                val style = overlay
                    .getObject("thumbnailOverlayTimeStatusRenderer")
                    .getString("style", "")
                if (style.equals("LIVE", ignoreCase = true)) {
                    cachedStreamType = StreamType.LIVE_STREAM
                    return cachedStreamType!!
                }
            }

            cachedStreamType = StreamType.VIDEO_STREAM
            return cachedStreamType!!
        }

    
    override val url: String
        get() {
            try {
                val videoId = videoInfo.getString("videoId")
                return instance.getUrl(videoId!!)
            } catch (e: Exception) {
                throw ParsingException("Could not get url", e)
            }
        }

    
    override val name: String
        get() {
            val name = getTextFromObject(videoInfo.getObject("title"))
            if (!Utils.isNullOrEmpty(name)) {
                return name!!
            }
            throw ParsingException("Could not get name")
        }

    
    override val duration: Long
        get() {
            if (streamType == StreamType.LIVE_STREAM) {
                return -1
            }

            var duration = getTextFromObject(videoInfo.getObject("lengthText"))

            if (duration.isNullOrEmpty()) {
                // Available in playlists for videos
                duration = videoInfo.getString("lengthSeconds")

                if (duration.isNullOrEmpty()) {
                    val timeOverlay = videoInfo.getArray("thumbnailOverlays")
                        .filterIsInstance<JsonObject>()
                        .firstOrNull { it.has("thumbnailOverlayTimeStatusRenderer") }

                    if (timeOverlay != null) {
                        duration = getTextFromObject(
                            timeOverlay
                                .getObject("thumbnailOverlayTimeStatusRenderer")
                                .getObject("text")
                        )
                    }
                }

                if (duration.isNullOrEmpty()) {
                    if (isPremiere == true) {
                        // Premieres can be livestreams, so the duration is not available in this case
                        return -1
                    }
                    throw ParsingException("Could not get duration")
                }
            }

            return parseDurationString(duration).toLong()
        }


    
    override val uploaderName: String
        get() {
            var name = getTextFromObject(videoInfo.getObject("longBylineText"))

            if (Utils.isNullOrEmpty(name)) {
                name = getTextFromObject(videoInfo.getObject("ownerText"))

                if (Utils.isNullOrEmpty(name)) {
                    name = getTextFromObject(videoInfo.getObject("shortBylineText"))

                    if (Utils.isNullOrEmpty(name)) {
                        throw ParsingException("Could not get uploader name")
                    }
                }
            }

            return name!!
        }

    
    val uploaderUrl: String
        get() {
            var url = getUrlFromNavigationEndpoint(
                videoInfo.getObject("longBylineText")
                    .getArray("runs").getObject(0).getObject("navigationEndpoint")
            )

            if (Utils.isNullOrEmpty(url)) {
                url = getUrlFromNavigationEndpoint(
                    videoInfo.getObject("ownerText")
                        .getArray("runs").getObject(0).getObject("navigationEndpoint")
                )

                if (Utils.isNullOrEmpty(url)) {
                    url = getUrlFromNavigationEndpoint(
                        videoInfo.getObject("shortBylineText")
                            .getArray("runs").getObject(0).getObject("navigationEndpoint")
                    )

                    if (Utils.isNullOrEmpty(url)) {
                        throw ParsingException("Could not get uploader url")
                    }
                }
            }

            return url!!
        }


    
    override val thumbnails: List<Image>
        get() = getThumbnailsFromInfoItem(videoInfo)

    companion object {
        private val ACCESSIBILITY_DATA_VIEW_COUNT_REGEX = Regex("([\\d,]+) views$")
        private const val NO_VIEWS_LOWERCASE = "no views"
    }
}
