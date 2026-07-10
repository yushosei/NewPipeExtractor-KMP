package com.yushosei.newpipe.extractor

import com.yushosei.newpipe.extractor.stream.Stream
import com.yushosei.newpipe.util.DefaultDownloaderImpl
import com.yushosei.newpipe.util.ExtractorHelper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Opt-in smoke tests for the real services. Run with:
 *
 * `./gradlew :newpipe-KMP:desktopTest -Dnewpipe.liveTests=true`
 *
 * These tests are skipped by default so normal builds do not depend on external services.
 */
class LiveAudioStreamExtractionTest {
    @Test
    fun youtubeAudioStreamIsExtractedAndReachable() = runLiveTest {
        verifyAudioExtraction(
            serviceId = ServiceList.YouTube.serviceId,
            url = "https://www.youtube.com/watch?v=jNQXAC9IVRw"
        )
    }

    @Test
    fun soundcloudAudioStreamIsExtractedAndReachable() = runLiveTest {
        verifyAudioExtraction(
            serviceId = ServiceList.SoundCloud.serviceId,
            url = "https://soundcloud.com/forss/flickermood"
        )
    }

    @Test
    fun youtubeProgressiveVideoStreamIsExtractedAndReachable() = runLiveTest {
        val downloader = DefaultDownloaderImpl.initDefault()
        NewPipe.init(downloader)
        ExtractorHelper.clearCache()

        val url = "https://www.youtube.com/watch?v=jNQXAC9IVRw"
        val info = ExtractorHelper.getStreamInfo(
            ServiceList.YouTube.serviceId,
            url,
            forceLoad = true
        )

        assertTrue(info.videoStreams.isNotEmpty(), "No progressive video streams extracted for $url")
        assertTrue(info.videoOnlyStreams.isNotEmpty(), "No video-only streams extracted for $url")

        val progressiveResults = info.videoStreams.map { stream ->
            stream to requestFirstBytesWithMetadata(downloader, stream)
        }
        val videoOnlyResults = info.videoOnlyStreams.map { stream ->
            stream to requestFirstBytesWithMetadata(downloader, stream)
        }

        println("YouTube video: ${info.name}")
        progressiveResults.forEach { (stream, result) ->
            println(
                "progressive id=${stream.id}, resolution=${stream.resolution}, " +
                        "format=${stream.format}, fps=${stream.fps}, " +
                        "status=${result.statusCode}, contentType=${result.contentType}"
            )
        }
        videoOnlyResults.forEach { (stream, result) ->
            println(
                "video-only id=${stream.id}, resolution=${stream.resolution}, " +
                        "format=${stream.format}, fps=${stream.fps}, " +
                        "status=${result.statusCode}, contentType=${result.contentType}"
            )
        }

        assertTrue(
            progressiveResults.any { (_, result) -> result.isVideoResponse },
            "Extracted ${info.videoStreams.size} progressive video streams, but none was reachable"
        )
        assertTrue(
            videoOnlyResults.any { (_, result) -> result.isVideoResponse },
            "Extracted ${info.videoOnlyStreams.size} video-only streams, but none was reachable"
        )

        val progressiveMp4 = progressiveResults.first { (stream, result) ->
            stream.format == MediaFormat.MPEG_4 && result.isVideoResponse
        }.first
        val downloadedVideo = download(progressiveMp4.content)
        assertTrue(downloadedVideo.size > 100_000, "Downloaded MP4 is unexpectedly small")
        assertEquals("ftyp", downloadedVideo.copyOfRange(4, 8).decodeToString())
        assertTrue(downloadedVideo.containsAscii("moov"), "Downloaded MP4 has no moov atom")
        assertTrue(downloadedVideo.containsAscii("mdat"), "Downloaded MP4 has no mdat atom")
        println(
            "downloaded progressive MP4 bytes=${downloadedVideo.size}, " +
                    "atoms=ftyp/moov/mdat"
        )
    }

    private fun runLiveTest(block: suspend () -> Unit) {
        if (System.getProperty(LIVE_TEST_PROPERTY) != "true") return
        runBlocking { block() }
    }

    private suspend fun verifyAudioExtraction(serviceId: Int, url: String) {
        val downloader = DefaultDownloaderImpl.initDefault()
        NewPipe.init(downloader)
        ExtractorHelper.clearCache()

        val info = ExtractorHelper.getStreamInfo(serviceId, url, forceLoad = true)
        assertTrue(info.audioStreams.isNotEmpty(), "No audio streams extracted for $url")

        val successfulResponse = info.audioStreams.firstNotNullOfOrNull { stream ->
            requestFirstBytes(downloader, stream)
                .takeIf { it in 200..299 }
        }
        assertTrue(
            successfulResponse != null,
            "Extracted ${info.audioStreams.size} audio streams for $url, but none was reachable"
        )
    }

    private suspend fun requestFirstBytes(
        downloader: DefaultDownloaderImpl,
        stream: Stream
    ): Int = requestFirstBytesWithMetadata(downloader, stream).statusCode

    private suspend fun requestFirstBytesWithMetadata(
        downloader: DefaultDownloaderImpl,
        stream: Stream
    ): MediaResponse = try {
        val response =
        downloader.get(
            stream.content,
            headers = mapOf("Range" to listOf("bytes=0-0"))
        )
        MediaResponse(
            statusCode = response.responseCode(),
            contentType = response.getHeader("Content-Type").orEmpty()
        )
    } catch (_: Exception) {
        MediaResponse(statusCode = -1, contentType = "")
    }

    private data class MediaResponse(
        val statusCode: Int,
        val contentType: String
    ) {
        val isVideoResponse: Boolean
            get() = statusCode in 200..299 && contentType.startsWith("video/")
    }

    private fun download(url: String): ByteArray {
        val request = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(30))
            .GET()
            .build()
        val response = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()
            .send(request, HttpResponse.BodyHandlers.ofByteArray())
        assertTrue(response.statusCode() in 200..299, "Full MP4 download failed")
        assertTrue(
            response.headers().firstValue("Content-Type").orElse("").startsWith("video/mp4"),
            "Full download is not video/mp4"
        )
        return response.body()
    }

    private fun ByteArray.containsAscii(value: String): Boolean {
        val pattern = value.encodeToByteArray()
        return indices.any { start ->
            start + pattern.size <= size && pattern.indices.all { offset ->
                this[start + offset] == pattern[offset]
            }
        }
    }

    private companion object {
        const val LIVE_TEST_PROPERTY = "newpipe.liveTests"
    }
}
