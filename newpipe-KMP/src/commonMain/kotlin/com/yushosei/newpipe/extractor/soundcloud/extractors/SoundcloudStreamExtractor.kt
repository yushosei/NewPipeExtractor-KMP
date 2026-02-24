package com.yushosei.newpipe.extractor.soundcloud.extractors

import com.yushosei.newpipe.extractor.Image
import com.yushosei.newpipe.extractor.MediaFormat
import com.yushosei.newpipe.extractor.StreamingService
import com.yushosei.newpipe.extractor.downloader.Downloader
import com.yushosei.newpipe.extractor.exceptions.ContentNotAvailableException
import com.yushosei.newpipe.extractor.exceptions.ExtractionException
import com.yushosei.newpipe.extractor.exceptions.GeographicRestrictionException
import com.yushosei.newpipe.extractor.exceptions.SoundCloudGoPlusContentException
import com.yushosei.newpipe.extractor.linkhandler.LinkHandler
import com.yushosei.newpipe.extractor.soundcloud.SoundcloudParsingHelper
import com.yushosei.newpipe.extractor.stream.AudioStream
import com.yushosei.newpipe.extractor.stream.DeliveryMethod
import com.yushosei.newpipe.extractor.stream.Description
import com.yushosei.newpipe.extractor.stream.Stream
import com.yushosei.newpipe.extractor.stream.StreamExtractor
import com.yushosei.newpipe.extractor.stream.StreamType
import com.yushosei.newpipe.extractor.utils.Utils
import com.yushosei.newpipe.nanojson.JsonArray
import com.yushosei.newpipe.nanojson.JsonObject
import com.yushosei.newpipe.nanojson.JsonParser
import com.yushosei.newpipe.nanojson.JsonParserException

class SoundcloudStreamExtractor(
    service: StreamingService,
    linkHandler: LinkHandler
) : StreamExtractor(service, linkHandler) {
    private var track: JsonObject? = null
    private var isAvailable: Boolean = true

    override suspend fun onFetchPage(downloader: Downloader) {
        val resolvedUrl = SoundcloudParsingHelper.normalizeTrackUrl(url ?: originalUrl)
        track = SoundcloudParsingHelper.resolveFor(downloader, resolvedUrl)

        val policy = track!!.getString("policy", "")
        if (policy != "ALLOW" && policy != "MONETIZE") {
            isAvailable = false

            if (policy == "SNIP") {
                throw SoundCloudGoPlusContentException()
            }

            if (policy == "BLOCK") {
                throw GeographicRestrictionException(
                    "This track is not available in user's country"
                )
            }

            throw ContentNotAvailableException("Content not available: policy $policy")
        }
    }

    override val id: String
        get() {
            assertPageFetched()
            return track!!.getLong("id", 0L).toString()
        }

    override val name: String
        get() {
            assertPageFetched()
            return track!!.getString("title", "")
        }

    override val thumbnails: List<Image>
        get() {
            assertPageFetched()
            return SoundcloudParsingHelper.getAllImagesFromTrackObject(track!!)
        }

    override val description: Description
        get() {
            assertPageFetched()
            return Description(track!!.getString("description", ""), Description.PLAIN_TEXT)
        }

    override val length: Long
        get() {
            assertPageFetched()
            return track!!.getLong("duration", 0L) / 1000L
        }

    override val timeStamp: Long
        get() {
            val timestamp = getTimestampSeconds("(#t=\\d{0,3}h?\\d{0,3}m?\\d{1,3}s?)")
            return if (timestamp == -2L) 0L else timestamp
        }

    override val uploaderName: String
        get() {
            assertPageFetched()
            return SoundcloudParsingHelper.getUploaderName(track!!)
        }

    override suspend fun audioStreams(): List<AudioStream> {
        assertPageFetched()
        if (!isAvailable || !track!!.getBoolean("streamable", false)) {
            return emptyList()
        }

        val transcodings = track!!
            .getObject("media")
            .getArray("transcodings")
        if (transcodings.isEmpty()) {
            return emptyList()
        }

        return extractAudioStreams(transcodings)
    }

    override val dashMpdUrl: String
        get() = ""

    override val hlsUrl: String
        get() = ""

    override val streamType: StreamType
        get() = StreamType.AUDIO_STREAM

    override val category: String
        get() {
            assertPageFetched()
            return track!!.getString("genre", "")
        }

    override val tags: List<String>
        get() {
            assertPageFetched()
            val rawTagList = track!!.getString("tag_list", "")
            if (rawTagList.isEmpty()) {
                return emptyList()
            }

            val tags = mutableListOf<String>()
            val escapedTag = StringBuilder()
            var inEscapedTag = false

            for (tagPart in rawTagList.split(' ')) {
                if (tagPart.startsWith("\"")) {
                    escapedTag.clear()
                    escapedTag.append(tagPart.replace("\"", ""))
                    inEscapedTag = true
                } else if (inEscapedTag) {
                    if (tagPart.endsWith("\"")) {
                        escapedTag.append(" ").append(tagPart.replace("\"", ""))
                        inEscapedTag = false
                        tags.add(escapedTag.toString())
                    } else {
                        escapedTag.append(" ").append(tagPart)
                    }
                } else if (tagPart.isNotEmpty()) {
                    tags.add(tagPart)
                }
            }

            return tags
        }

    private suspend fun extractAudioStreams(transcodings: JsonArray): List<AudioStream> {
        val audioStreams = mutableListOf<AudioStream>()

        for (entry in transcodings) {
            val transcoding = entry as? JsonObject ?: continue

            val endpointUrl = transcoding.getString("url", "")
            if (endpointUrl.isEmpty()) {
                continue
            }

            val preset = transcoding.getString("preset", Stream.ID_UNKNOWN)
            val protocol = transcoding.getObject("format").getString("protocol", "")
            if (protocol.contains("encrypted", ignoreCase = true)) {
                continue
            }

            try {
                val streamUrl = getTranscodingUrl(endpointUrl)
                val builder = AudioStream.Builder()
                    .setId(if (preset.isEmpty()) Stream.ID_UNKNOWN else preset)
                    .setContent(streamUrl, true)

                if (protocol == "hls") {
                    builder.setDeliveryMethod(DeliveryMethod.HLS)
                }

                when {
                    preset.contains("mp3") -> {
                        builder.setMediaFormat(MediaFormat.MP3)
                        builder.setAverageBitrate(128)
                    }

                    preset.contains("opus") -> {
                        builder.setMediaFormat(MediaFormat.OPUS)
                        builder.setAverageBitrate(64)
                    }

                    preset.contains("aac_160k") -> {
                        builder.setMediaFormat(MediaFormat.M4A)
                        builder.setAverageBitrate(160)
                    }

                    else -> continue
                }

                val audioStream = builder.build()
                if (!Stream.containSimilarStream(audioStream, audioStreams)) {
                    audioStreams.add(audioStream)
                }
            } catch (_: Exception) {
                // Skip malformed transcodings and continue with the next one.
            }
        }

        return audioStreams
    }

    private suspend fun getTranscodingUrl(endpointUrl: String): String {
        var requestUrl = SoundcloudParsingHelper.withClientId(endpointUrl)

        val trackAuthorization = track!!.getString("track_authorization", "")
        if (trackAuthorization.isNotEmpty()) {
            requestUrl += "&track_authorization=${Utils.encodeUrlUtf8(trackAuthorization)}"
        }

        val responseBody = downloader.get(requestUrl).responseBody()
        val urlObject = try {
            JsonParser.`object`().from(responseBody)
        } catch (e: JsonParserException) {
            throw ExtractionException("Could not parse stream URL response", e)
        }

        val mediaUrl = urlObject.getString("url", "")
        if (mediaUrl.isEmpty()) {
            throw ExtractionException("Could not extract stream URL")
        }
        return mediaUrl
    }
}
