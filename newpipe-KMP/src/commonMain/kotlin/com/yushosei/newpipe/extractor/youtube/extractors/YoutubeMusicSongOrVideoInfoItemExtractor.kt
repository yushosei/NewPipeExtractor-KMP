package com.yushosei.newpipe.extractor.youtube.extractors

import com.yushosei.newpipe.extractor.Image
import com.yushosei.newpipe.extractor.exceptions.ParsingException
import com.yushosei.newpipe.extractor.youtube.YoutubeParsingHelper.getImagesFromThumbnailsArray
import com.yushosei.newpipe.extractor.youtube.YoutubeParsingHelper.getTextFromObject
import com.yushosei.newpipe.extractor.youtube.YoutubeParsingHelper.parseDurationString
import com.yushosei.newpipe.extractor.stream.StreamInfoItemExtractor
import com.yushosei.newpipe.extractor.stream.StreamType
import com.yushosei.newpipe.extractor.utils.Utils
import com.yushosei.newpipe.nanojson.JsonArray
import com.yushosei.newpipe.nanojson.JsonObject

internal class YoutubeMusicSongOrVideoInfoItemExtractor(
    private val songOrVideoInfoItem: JsonObject,
    private val descriptionElements: JsonArray,
    private val searchType: String
) : StreamInfoItemExtractor {
    
    override val url: String
        get() {
            val id =
                songOrVideoInfoItem.getObject("playlistItemData").getString("videoId")
            if (!Utils.isNullOrEmpty(id)) {
                return "https://music.youtube.com/watch?v=$id"
            }
            throw ParsingException("Could not get URL")
        }

    override val uploaderName: String
        get() {
            val name = descriptionElements.getObject(0).getString("text")
            if (!Utils.isNullOrEmpty(name)) {
                return name!!
            }
            throw ParsingException("Could not get uploader name")
        }

    
    override val name: String
        get() {
            val name = getTextFromObject(
                songOrVideoInfoItem.getArray("flexColumns")
                    .getObject(0)
                    .getObject("musicResponsiveListItemFlexColumnRenderer")
                    .getObject("text")
            )
            if (!Utils.isNullOrEmpty(name)) {
                return name!!
            }
            throw ParsingException("Could not get name")
        }

    override val streamType: StreamType
        get() = StreamType.VIDEO_STREAM

    override val duration: Long
        get() {
            val duration = descriptionElements.getObject(descriptionElements.size - 1)
                .getString("text")
            if (!Utils.isNullOrEmpty(duration)) {
                return parseDurationString(duration!!).toLong()
            }
            throw ParsingException("Could not get duration")
        }

    
    override val thumbnails: List<Image>
        get() {
            try {
                return getImagesFromThumbnailsArray(
                    songOrVideoInfoItem.getObject("thumbnail")
                        .getObject("musicThumbnailRenderer")
                        .getObject("thumbnail")
                        .getArray("thumbnails")
                )
            } catch (e: Exception) {
                throw ParsingException("Could not get thumbnails", e)
            }
        }
}
