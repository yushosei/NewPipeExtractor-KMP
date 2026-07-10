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
import com.yushosei.newpipe.extractor.stream.VideoStream
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

    private data class TranscodingContext(
        val preset: String,
        val protocol: String,
        val endpointUrl: String,
        val mimeType: String
    )

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

    override suspend fun videoStreams(): List<VideoStream> = emptyList()

    override suspend fun videoOnlyStreams(): List<VideoStream> = emptyList()

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
        val seenTranscodings = mutableListOf<String>()
        var firstFailure: ExtractionException? = null

        for (entry in transcodings) {
            val transcoding = entry as? JsonObject ?: continue

            val endpointUrl = transcoding.getString("url", "")
            val preset = transcoding.getString("preset", Stream.ID_UNKNOWN)
            val formatObject = transcoding.getObject("format")
            val protocol = formatObject.getString("protocol", "")
            val mimeType = formatObject.getString("mime_type", "")
            val context = TranscodingContext(
                preset = if (preset.isEmpty()) Stream.ID_UNKNOWN else preset,
                protocol = protocol,
                endpointUrl = endpointUrl,
                mimeType = mimeType
            )
            seenTranscodings.add(
                "preset=${context.preset}, protocol=${context.protocol.ifEmpty { "unknown" }}, " +
                        "endpoint=${context.endpointUrl.ifEmpty { "<missing>" }}"
            )

            if (endpointUrl.isEmpty()) {
                if (firstFailure == null) {
                    firstFailure = ExtractionException(
                        "SoundCloud transcoding is missing an endpoint URL: " +
                                transcodingContextLabel(context)
                    )
                }
                continue
            }

            if (protocol.contains("encrypted", ignoreCase = true)) {
                continue
            }

            try {
                val streamUrl = getTranscodingUrl(context)
                val builder = AudioStream.Builder()
                    .setId(context.preset)
                    .setContent(streamUrl, true)

                if (protocol == "hls") {
                    builder.setDeliveryMethod(DeliveryMethod.HLS)
                }

                if (!applyFormatMetadata(builder, context)) {
                    continue
                }

                val audioStream = builder.build()
                if (!Stream.containSimilarStream(audioStream, audioStreams)) {
                    audioStreams.add(audioStream)
                }
            } catch (e: ExtractionException) {
                if (firstFailure == null) {
                    firstFailure = e
                }
            } catch (e: Exception) {
                if (firstFailure == null) {
                    firstFailure = ExtractionException(
                        "Unexpected SoundCloud transcoding failure: ${transcodingContextLabel(context)}",
                        e
                    )
                }
            }
        }

        if (audioStreams.isEmpty()) {
            val summary = buildString {
                append("SoundCloud returned ")
                append(transcodings.size)
                append(" transcodings but no audio stream survived. Seen: ")
                append(seenTranscodings.joinToString(" | "))
            }
            if (firstFailure != null) {
                throw ExtractionException("$summary. First failure: ${firstFailure.message}", firstFailure)
            }
            throw ExtractionException(summary)
        }

        return audioStreams
    }

    private fun applyFormatMetadata(
        builder: AudioStream.Builder,
        context: TranscodingContext
    ): Boolean {
        val normalizedPreset = context.preset.lowercase()
        val normalizedMimeType = context.mimeType.lowercase()

        return when {
            normalizedPreset.contains("aac_160k") || normalizedMimeType.contains("audio/mp4") -> {
                builder.setMediaFormat(MediaFormat.M4A)
                builder.setAverageBitrate(160)
                true
            }

            normalizedPreset.contains("opus") || normalizedMimeType.contains("opus") -> {
                builder.setMediaFormat(MediaFormat.OPUS)
                builder.setAverageBitrate(64)
                true
            }

            normalizedPreset.contains("mp3") ||
                    normalizedPreset == "abr_sq" ||
                    normalizedMimeType.contains("audio/mpegurl") ||
                    normalizedMimeType.contains("audio/mpeg") -> {
                builder.setMediaFormat(MediaFormat.MP3)
                builder.setAverageBitrate(128)
                true
            }

            else -> false
        }
    }

    private suspend fun getTranscodingUrl(context: TranscodingContext): String {
        return requestTranscodingUrl(context, forceRefreshClientId = false)
    }

    private suspend fun requestTranscodingUrl(
        context: TranscodingContext,
        forceRefreshClientId: Boolean
    ): String {
        val trackPageUrl = track!!.getString("permalink_url", url ?: originalUrl).ifEmpty { originalUrl }
        val clientId = SoundcloudParsingHelper.clientId(trackPageUrl, forceRefresh = forceRefreshClientId)
        val requestUrl = buildTranscodingRequestUrl(context.endpointUrl, clientId)
        val response = try {
            downloader.get(requestUrl)
        } catch (e: Exception) {
            throw buildTranscodingFailure(
                context = context,
                statusCode = null,
                responseBody = null,
                prefix = "SoundCloud transcoding request failed",
                cause = e
            )
        }

        if (response.responseCode() in CLIENT_ID_RETRY_CODES) {
            if (!forceRefreshClientId) {
                SoundcloudParsingHelper.invalidateClientIdCache()
                return requestTranscodingUrl(context, forceRefreshClientId = true)
            }

            throw buildTranscodingFailure(
                context = context,
                statusCode = response.responseCode(),
                responseBody = response.responseBody(),
                prefix = "SoundCloud transcoding request failed"
            )
        }

        val urlObject = try {
            JsonParser.`object`().from(response.responseBody())
        } catch (e: JsonParserException) {
            if (!forceRefreshClientId) {
                SoundcloudParsingHelper.invalidateClientIdCache()
                return requestTranscodingUrl(context, forceRefreshClientId = true)
            }

            throw buildTranscodingFailure(
                context = context,
                statusCode = response.responseCode(),
                responseBody = response.responseBody(),
                prefix = "Could not parse SoundCloud stream URL response",
                cause = e
            )
        }

        val mediaUrl = urlObject.getString("url", "")
        if (mediaUrl.isEmpty()) {
            throw buildTranscodingFailure(
                context = context,
                statusCode = response.responseCode(),
                responseBody = response.responseBody(),
                prefix = "Could not extract SoundCloud stream URL"
            )
        }
        return mediaUrl
    }

    private fun buildTranscodingRequestUrl(endpointUrl: String, clientId: String): String {
        var requestUrl = SoundcloudParsingHelper.withClientId(endpointUrl, clientId)
        val trackAuthorization = track!!.getString("track_authorization", "")
        if (trackAuthorization.isNotEmpty()) {
            requestUrl += "&track_authorization=${Utils.encodeUrlUtf8(trackAuthorization)}"
        }
        return requestUrl
    }

    private fun buildTranscodingFailure(
        context: TranscodingContext,
        statusCode: Int?,
        responseBody: String?,
        prefix: String,
        cause: Throwable? = null
    ): ExtractionException {
        val message = buildString {
            append(prefix)
            append(": ")
            append(transcodingContextLabel(context))
            append(", status=")
            append(statusCode ?: "n/a")
            append(", bodySnippet=")
            append(bodySnippet(responseBody))
        }
        return if (cause == null) ExtractionException(message) else ExtractionException(message, cause)
    }

    private fun transcodingContextLabel(context: TranscodingContext): String {
        return "preset=${context.preset}, protocol=${context.protocol.ifEmpty { "unknown" }}, " +
                "endpoint=${context.endpointUrl.ifEmpty { "<missing>" }}"
    }

    private fun bodySnippet(responseBody: String?): String {
        if (responseBody.isNullOrBlank()) {
            return "<empty>"
        }

        return responseBody
            .replace("\\s+".toRegex(), " ")
            .take(BODY_SNIPPET_LENGTH)
    }

    private companion object {
        val CLIENT_ID_RETRY_CODES = setOf(401, 403, 404)
        const val BODY_SNIPPET_LENGTH = 160
    }
}
