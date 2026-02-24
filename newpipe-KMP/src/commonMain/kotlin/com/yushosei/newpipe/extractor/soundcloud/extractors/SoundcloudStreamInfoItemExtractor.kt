package com.yushosei.newpipe.extractor.soundcloud.extractors

import com.yushosei.newpipe.extractor.Image
import com.yushosei.newpipe.extractor.soundcloud.SoundcloudParsingHelper
import com.yushosei.newpipe.extractor.stream.StreamInfoItemExtractor
import com.yushosei.newpipe.extractor.stream.StreamType
import com.yushosei.newpipe.extractor.utils.Utils
import com.yushosei.newpipe.nanojson.JsonObject

internal class SoundcloudStreamInfoItemExtractor(
    private val itemObject: JsonObject
) : StreamInfoItemExtractor {
    override val url: String
        get() = Utils.replaceHttpWithHttps(itemObject.getString("permalink_url", ""))

    override val name: String
        get() = itemObject.getString("title", "")

    override val duration: Long
        get() = itemObject.getLong("duration", 0L) / 1000L

    override val uploaderName: String
        get() = itemObject.getObject("user").getString("username", "")

    override val thumbnails: List<Image>
        get() = SoundcloudParsingHelper.getAllImagesFromTrackObject(itemObject)

    override val streamType: StreamType
        get() = StreamType.AUDIO_STREAM
}
