package com.yushosei.newpipe.extractor.stream

import com.yushosei.newpipe.extractor.MediaFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class VideoStreamTest {
    @Test
    fun builderAllowsVideoStreamWithoutManifestUrl() {
        val stream = VideoStream.Builder()
            .setId("18")
            .setContent("https://example.com/video.mp4", true)
            .setMediaFormat(MediaFormat.MPEG_4)
            .setResolution("360p")
            .setIsVideoOnly(false)
            .build()

        assertEquals("https://example.com/video.mp4", stream.content)
        assertEquals("360p", stream.resolution)
        assertFalse(stream.isVideoOnly)
        assertNull(stream.manifestUrl)
    }
}
