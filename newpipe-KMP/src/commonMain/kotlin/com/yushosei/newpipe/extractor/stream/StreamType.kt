package com.yushosei.newpipe.extractor.stream

/**
 * An enum representing the stream type of a [StreamInfo] extracted by a [ ].
 */
enum class StreamType {
    /**
     * Placeholder to check if the stream type was checked or not. It doesn't make sense to use this
     * enum constant outside of the extractor as it will never be returned by an [ ] and is only used internally.
     */
    NONE,

    /**
     * A normal video stream, usually with audio. Note that the [StreamInfo] **can also
     * provide audio-only [AudioStream]s** in addition to video or video-only [ ]s.
     */
    VIDEO_STREAM,

    /**
     * An audio-only stream. There should be no [VideoStream]s available! In order to prevent
     * unexpected behaviors, when [StreamExtractor]s return this stream type, they should
     * ensure that no video stream is returned in [StreamExtractor.getVideoStreams] and
     * [StreamExtractor.getVideoOnlyStreams].
     */
    AUDIO_STREAM,

    /**
     * A video live stream, usually with audio. Note that the [StreamInfo] **can also
     * provide audio-only [AudioStream]s** in addition to video or video-only [ ]s.
     */
    LIVE_STREAM,

    /**
     * An audio-only live stream. There should be no [VideoStream]s available! In order to
     * prevent unexpected behaviors, when [StreamExtractor]s return this stream type, they
     * should ensure that no video stream is returned in [StreamExtractor.getVideoStreams]
     * and [StreamExtractor.getVideoOnlyStreams].
     */
    AUDIO_LIVE_STREAM,

    /**
     * A video live stream that has just ended but has not yet been encoded into a normal video
     * stream. Note that the [StreamInfo] **can also provide audio-only [ ]s** in addition to video or video-only [VideoStream]s.
     *
     *
     *
     * Note that most of the content of an ended live video (or audio) may be extracted as [ ][.VIDEO_STREAM] (or [regular audio contents][.AUDIO_STREAM])
     * later, because the service may encode them again later as normal video/audio streams. That's
     * the case on YouTube, for example.
     *
     */
    POST_LIVE_STREAM,

    /**
     * An audio live stream that has just ended but has not yet been encoded into a normal audio
     * stream. There should be no [VideoStream]s available! In order to prevent unexpected
     * behaviors, when [StreamExtractor]s return this stream type, they should ensure that no
     * video stream is returned in [StreamExtractor.getVideoStreams] and
     * [StreamExtractor.getVideoOnlyStreams].
     *
     *
     *
     * Note that most of ended live audio streams extracted with this value are processed as
     * [regular audio streams][.AUDIO_STREAM] later, because the service may encode them
     * again later.
     *
     */
    POST_LIVE_AUDIO_STREAM
}
