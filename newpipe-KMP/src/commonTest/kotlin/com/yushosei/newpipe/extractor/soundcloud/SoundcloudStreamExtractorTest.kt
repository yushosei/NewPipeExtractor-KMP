package com.yushosei.newpipe.extractor.soundcloud

import com.yushosei.newpipe.extractor.NewPipe
import com.yushosei.newpipe.extractor.ServiceList
import com.yushosei.newpipe.extractor.downloader.Downloader
import com.yushosei.newpipe.extractor.downloader.Request
import com.yushosei.newpipe.extractor.downloader.Response
import com.yushosei.newpipe.extractor.exceptions.ExtractionException
import com.yushosei.newpipe.extractor.soundcloud.extractors.SoundcloudStreamExtractor
import com.yushosei.newpipe.extractor.soundcloud.linkHandler.SoundcloudStreamLinkHandlerFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class SoundcloudStreamExtractorTest {
    @Test
    fun audioStreamsUseTrackHydrationClientIdAndAreNotEmpty() = runBlocking {
        SoundcloudParsingHelper.invalidateClientIdCache()
        val downloader = FakeSoundcloudDownloader(
            trackPageHtml = trackPageHtml(FRESH_CLIENT_ID, listOf(AAC_160K_TRANSCODING, ABR_SQ_TRANSCODING)),
            resolveBody = trackBody(listOf(AAC_160K_TRANSCODING, ABR_SQ_TRANSCODING)),
            endpointHandlers = mapOf(
                AAC_160K_ENDPOINT to { requestUrl ->
                    if (queryParameter(requestUrl, "client_id") == FRESH_CLIENT_ID) {
                        jsonResponse(requestUrl, 200, AAC_160K_RESPONSE)
                    } else {
                        jsonResponse(requestUrl, 401, "")
                    }
                },
                ABR_SQ_ENDPOINT to { requestUrl ->
                    jsonResponse(requestUrl, 404, "{}")
                }
            )
        )

        NewPipe.init(downloader)

        val streams = createExtractor().run {
            fetchPage()
            audioStreams()
        }

        assertTrue(streams.isNotEmpty(), "Expected at least one audio stream from the real track fixture")
        assertTrue(streams.any { it.id == "aac_160k" }, "Expected the AAC transcoding to survive extraction")
        assertEquals(listOf(FRESH_CLIENT_ID), downloader.clientIdsFor(AAC_160K_ENDPOINT))
        assertTrue(
            downloader.requestedUrls.none { it == HOMEPAGE_URL },
            "Track extraction should resolve client_id from the track page hydration before falling back to the homepage"
        )

        SoundcloudParsingHelper.invalidateClientIdCache()
    }

    @Test
    fun audioStreamsRecoverFromStaleClientId() = runBlocking {
        SoundcloudParsingHelper.invalidateClientIdCache()
        val downloader = FakeSoundcloudDownloader(
            homePageHtml = homePageHtml(STALE_CLIENT_ID),
            trackPageHtml = trackPageHtml(FRESH_CLIENT_ID, listOf(AAC_160K_TRANSCODING, ABR_SQ_TRANSCODING)),
            resolveBody = trackBody(listOf(AAC_160K_TRANSCODING, ABR_SQ_TRANSCODING)),
            endpointHandlers = mapOf(
                AAC_160K_ENDPOINT to { requestUrl ->
                    if (queryParameter(requestUrl, "client_id") == FRESH_CLIENT_ID) {
                        jsonResponse(requestUrl, 200, AAC_160K_RESPONSE)
                    } else {
                        jsonResponse(requestUrl, 401, "")
                    }
                },
                ABR_SQ_ENDPOINT to { requestUrl ->
                    jsonResponse(requestUrl, 404, "{}")
                }
            )
        )

        NewPipe.init(downloader)
        assertEquals(STALE_CLIENT_ID, SoundcloudParsingHelper.clientId())

        val streams = createExtractor().run {
            fetchPage()
            audioStreams()
        }

        assertTrue(streams.isNotEmpty(), "Expected stale client_id recovery to preserve audio streams")
        assertEquals(
            listOf(STALE_CLIENT_ID, FRESH_CLIENT_ID),
            downloader.clientIdsFor(AAC_160K_ENDPOINT),
            "The extractor should retry the first failed transcoding with a freshly resolved client_id"
        )
        assertTrue(
            downloader.requestedUrls.count { it == TRACK_URL } >= 1,
            "Refreshing the client_id should fetch the current track page hydration"
        )

        SoundcloudParsingHelper.invalidateClientIdCache()
    }

    @Test
    fun audioStreamsThrowWhenAllTranscodingsFail() = runBlocking {
        SoundcloudParsingHelper.invalidateClientIdCache()
        val downloader = FakeSoundcloudDownloader(
            trackPageHtml = trackPageHtml(FRESH_CLIENT_ID, listOf(ABR_SQ_TRANSCODING)),
            resolveBody = trackBody(listOf(ABR_SQ_TRANSCODING)),
            endpointHandlers = mapOf(
                ABR_SQ_ENDPOINT to { requestUrl ->
                    jsonResponse(requestUrl, 404, "{}")
                }
            )
        )

        NewPipe.init(downloader)

        val exception = assertFailsWith<ExtractionException> {
            createExtractor().run {
                fetchPage()
                audioStreams()
            }
        }

        val message = exception.message.orEmpty()
        assertTrue(message.contains("preset=abr_sq"), message)
        assertTrue(message.contains("protocol=hls"), message)
        assertTrue(message.contains("endpoint=$ABR_SQ_ENDPOINT"), message)
        assertTrue(message.contains("status=404"), message)
        assertTrue(message.contains("bodySnippet={}"), message)

        SoundcloudParsingHelper.invalidateClientIdCache()
    }

    private fun createExtractor(): SoundcloudStreamExtractor {
        return SoundcloudStreamExtractor(
            ServiceList.SoundCloud,
            SoundcloudStreamLinkHandlerFactory.instance.fromUrl(TRACK_URL)
        )
    }

    private class FakeSoundcloudDownloader(
        private val homePageHtml: String? = null,
        private val trackPageHtml: String,
        private val resolveBody: String,
        private val endpointHandlers: Map<String, (String) -> Response>
    ) : Downloader() {
        val requestedUrls = mutableListOf<String>()
        private val endpointClientIds = mutableMapOf<String, MutableList<String>>()

        override suspend fun execute(request: Request): Response {
            val requestUrl = request.url()
            requestedUrls += requestUrl

            return when {
                request.httpMethod() == "GET" && requestUrl == TRACK_URL ->
                    htmlResponse(requestUrl, trackPageHtml)

                request.httpMethod() == "GET" && requestUrl == HOMEPAGE_URL && homePageHtml != null ->
                    htmlResponse(requestUrl, homePageHtml)

                request.httpMethod() == "GET" && requestUrl.startsWith(RESOLVE_ENDPOINT_PREFIX) ->
                    jsonResponse(requestUrl, 200, resolveBody)

                request.httpMethod() == "GET" -> {
                    val endpointHandler = endpointHandlers.entries.firstOrNull { (endpointUrl, _) ->
                        requestUrl.startsWith(endpointUrl)
                    }?.let { (endpointUrl, handler) ->
                        endpointClientIds.getOrPut(endpointUrl) { mutableListOf() }
                            .add(queryParameter(requestUrl, "client_id").orEmpty())
                        handler
                    }

                    endpointHandler?.invoke(requestUrl)
                        ?: error("Unhandled request: ${request.httpMethod()} $requestUrl")
                }

                else -> error("Unhandled request: ${request.httpMethod()} $requestUrl")
            }
        }

        fun clientIdsFor(endpointUrl: String): List<String> {
            return endpointClientIds[endpointUrl].orEmpty()
        }
    }

    private companion object {
        const val TRACK_URL = "https://soundcloud.com/forss/flickermood"
        const val HOMEPAGE_URL = "https://soundcloud.com"
        const val RESOLVE_ENDPOINT_PREFIX = "https://api-v2.soundcloud.com/resolve?"

        const val STALE_CLIENT_ID = "stale-client-id"
        const val FRESH_CLIENT_ID = "fresh-hydration-client-id"
        const val AAC_160K_ENDPOINT =
            "https://api-v2.soundcloud.com/media/soundcloud:tracks:293/fa250033-f3ae-45f5-a97d-5f743bced495/stream/hls"
        const val ABR_SQ_ENDPOINT =
            "https://api-v2.soundcloud.com/media/soundcloud:tracks:293/302dc7fe-8690-4478-81b7-af8456d0fd6f/stream/hls"

        val AAC_160K_TRANSCODING = """
            {
              "url":"$AAC_160K_ENDPOINT",
              "preset":"aac_160k",
              "duration":213886,
              "snipped":false,
              "format":{"protocol":"hls","mime_type":"audio/mp4; codecs=\"mp4a.40.2\""},
              "quality":"sq",
              "is_legacy_transcoding":false
            }
        """.trimIndent()

        val ABR_SQ_TRANSCODING = """
            {
              "url":"$ABR_SQ_ENDPOINT",
              "preset":"abr_sq",
              "duration":213886,
              "snipped":false,
              "format":{"protocol":"hls","mime_type":"audio/mpegurl"},
              "quality":"sq",
              "is_legacy_transcoding":false
            }
        """.trimIndent()

        val AAC_160K_RESPONSE = """
            {
              "url":"https://playback.media-streaming.soundcloud.cloud/cWHNerOLlkUq/aac_160k/fa250033-f3ae-45f5-a97d-5f743bced495/playlist.m3u8?fixture=true"
            }
        """.trimIndent()
    }
}

private fun trackPageHtml(clientId: String, transcodings: List<String>): String {
    return """
        <!DOCTYPE html>
        <html lang="en">
        <head><title>Stream Flickermood by Forss | Listen online for free on SoundCloud</title></head>
        <body>
        <script>window.__sc_hydration = [{"hydratable":"apiClient","data":{"id":"$clientId","isExpiring":false}},{"hydratable":"sound","data":${trackBody(transcodings)}}];</script>
        </body>
        </html>
    """.trimIndent()
}

private fun homePageHtml(clientId: String): String {
    return """
        <!DOCTYPE html>
        <html lang="en">
        <body>
        <script>window.__sc_hydration = [{"hydratable":"apiClient","data":{"id":"$clientId","isExpiring":false}}];</script>
        </body>
        </html>
    """.trimIndent()
}

private fun trackBody(transcodings: List<String>): String {
    return """
        {
          "id":293,
          "title":"Flickermood",
          "permalink_url":"https://soundcloud.com/forss/flickermood",
          "streamable":true,
          "policy":"ALLOW",
          "track_authorization":"fixture-track-authorization",
          "media":{"transcodings":[${transcodings.joinToString(",")}]},
          "user":{"username":"Forss","permalink_url":"https://soundcloud.com/forss"}
        }
    """.trimIndent()
}

private fun htmlResponse(requestUrl: String, body: String): Response {
    return Response(
        responseCode = 200,
        responseMessage = "OK",
        responseHeaders = mapOf("Content-Type" to listOf("text/html")),
        responseBody = body,
        latestUrl = requestUrl
    )
}

private fun jsonResponse(requestUrl: String, statusCode: Int, body: String): Response {
    return Response(
        responseCode = statusCode,
        responseMessage = if (statusCode == 200) "OK" else "ERR",
        responseHeaders = mapOf("Content-Type" to listOf("application/json")),
        responseBody = body,
        latestUrl = requestUrl
    )
}

private fun queryParameter(url: String, name: String): String? {
    val query = url.substringAfter('?', "")
    if (query.isEmpty()) {
        return null
    }

    for (segment in query.split('&')) {
        val key = segment.substringBefore('=')
        if (key == name) {
            return segment.substringAfter('=', "")
        }
    }

    return null
}
