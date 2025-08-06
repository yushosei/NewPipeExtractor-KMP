package com.yushosei.newpipe.extractor.youtube

import com.yushosei.newpipe.extractor.MediaFormat
import com.yushosei.newpipe.extractor.exceptions.ParsingException
import com.yushosei.newpipe.extractor.locale.Locale
import com.yushosei.newpipe.extractor.stream.AudioTrackType

class ItagItem {

    /* ────────────────────────────────────────────────────────────────────────────
     * Types & constructors
     * ──────────────────────────────────────────────────────────────────────────── */

    enum class ItagType {
        AUDIO,
        VIDEO,
        VIDEO_ONLY
    }

    /** Default-fps (30) video/audio constructor */
    constructor(id: Int, type: ItagType, format: MediaFormat, resolution: String?) {
        this.id = id
        this.itagType = type
        this.mediaFormat = format
        this.resolutionString = resolution
        fps = 30
    }

    /** Explicit-fps video constructor */
    constructor(
        id: Int,
        type: ItagType,
        format: MediaFormat,
        resolution: String?,
        fps: Int
    ) {
        this.id = id
        this.itagType = type
        this.mediaFormat = format
        this.resolutionString = resolution
        this.fps = fps
    }

    /** Audio-only constructor (avg. bitrate supplied) */
    constructor(id: Int, type: ItagType, format: MediaFormat, avgBitrate: Int) {
        this.id = id
        this.itagType = type
        this.mediaFormat = format
        this.averageBitrate = avgBitrate
    }

    /** Copy constructor */
    constructor(other: ItagItem) {
        mediaFormat = other.mediaFormat
        id = other.id
        itagType = other.itagType
        averageBitrate = other.averageBitrate
        sampleRate = other.sampleRate
        audioChannels = other.audioChannels
        resolutionString = other.resolutionString
        fps = other.fps
        bitrate = other.bitrate
        width = other.width
        height = other.height
        initStart = other.initStart
        initEnd = other.initEnd
        indexStart = other.indexStart
        indexEnd = other.indexEnd
        quality = other.quality
        codec = other.codec
        targetDurationSec = other.targetDurationSec
        approxDurationMs = other.approxDurationMs
        contentLength = other.contentLength
        audioTrackId = other.audioTrackId
        audioTrackName = other.audioTrackName
        audioTrackType = other.audioTrackType
        audioLocale = other.audioLocale
    }

    /* ────────────────────────────────────────────────────────────────────────────
     * Basic immutable metadata
     * ──────────────────────────────────────────────────────────────────────────── */

    val mediaFormat: MediaFormat
    val id: Int
    val itagType: ItagType

    /* ────────────────────────────────────────────────────────────────────────────
     * Audio-specific fields
     * ──────────────────────────────────────────────────────────────────────────── */

    var averageBitrate: Int = AVERAGE_BITRATE_UNKNOWN

    var sampleRate: Int = SAMPLE_RATE_UNKNOWN
        set(value) {
            field = if (value > 0) value else SAMPLE_RATE_UNKNOWN
        }

    var audioChannels: Int = AUDIO_CHANNELS_NOT_APPLICABLE_OR_UNKNOWN
        set(value) {
            field = if (value > 0) value else AUDIO_CHANNELS_NOT_APPLICABLE_OR_UNKNOWN
        }

    var audioTrackId: String? = null
    var audioTrackName: String? = null
    var audioTrackType: AudioTrackType? = null
    var audioLocale: Locale? = null

    /* ────────────────────────────────────────────────────────────────────────────
     * Video-specific fields
     * ──────────────────────────────────────────────────────────────────────────── */

    var resolutionString: String? = null

    var fps: Int = FPS_NOT_APPLICABLE_OR_UNKNOWN
        set(value) {
            field = if (value > 0) value else FPS_NOT_APPLICABLE_OR_UNKNOWN
        }

    /* DASH/common stream metadata */
    var bitrate: Int = 0
    var width: Int = 0
    var height: Int = 0
    var initStart: Int = 0
    var initEnd: Int = 0
    var indexStart: Int = 0
    var indexEnd: Int = 0
    var quality: String? = null
    var codec: String? = null

    var targetDurationSec: Int = TARGET_DURATION_SEC_UNKNOWN
        set(value) {
            field = if (value > 0) value else TARGET_DURATION_SEC_UNKNOWN
        }

    var approxDurationMs: Long = APPROX_DURATION_MS_UNKNOWN
        set(value) {
            field = if (value > 0) value else APPROX_DURATION_MS_UNKNOWN
        }

    var contentLength: Long = CONTENT_LENGTH_UNKNOWN
        set(value) {
            field = if (value > 0) value else CONTENT_LENGTH_UNKNOWN
        }

    /* ────────────────────────────────────────────────────────────────────────────
     * Static helpers & constants
     * ──────────────────────────────────────────────────────────────────────────── */

    companion object {
        /** Full list of supported itags (excerpt kept as-is) */
        private val ITAG_LIST = arrayOf(
            /* VIDEO */
            ItagItem(17, ItagType.VIDEO, MediaFormat.v3GPP, "144p"),
            ItagItem(36, ItagType.VIDEO, MediaFormat.v3GPP, "240p"),
            ItagItem(18, ItagType.VIDEO, MediaFormat.MPEG_4, "360p"),
            ItagItem(34, ItagType.VIDEO, MediaFormat.MPEG_4, "360p"),
            ItagItem(35, ItagType.VIDEO, MediaFormat.MPEG_4, "480p"),
            ItagItem(59, ItagType.VIDEO, MediaFormat.MPEG_4, "480p"),
            ItagItem(78, ItagType.VIDEO, MediaFormat.MPEG_4, "480p"),
            ItagItem(22, ItagType.VIDEO, MediaFormat.MPEG_4, "720p"),
            ItagItem(37, ItagType.VIDEO, MediaFormat.MPEG_4, "1080p"),
            ItagItem(38, ItagType.VIDEO, MediaFormat.MPEG_4, "1080p"),
            ItagItem(43, ItagType.VIDEO, MediaFormat.WEBM, "360p"),
            ItagItem(44, ItagType.VIDEO, MediaFormat.WEBM, "480p"),
            ItagItem(45, ItagType.VIDEO, MediaFormat.WEBM, "720p"),
            ItagItem(46, ItagType.VIDEO, MediaFormat.WEBM, "1080p"),
            /* AUDIO */
            ItagItem(171, ItagType.AUDIO, MediaFormat.WEBMA, 128),
            ItagItem(172, ItagType.AUDIO, MediaFormat.WEBMA, 256),
            ItagItem(599, ItagType.AUDIO, MediaFormat.M4A, 32),
            ItagItem(139, ItagType.AUDIO, MediaFormat.M4A, 48),
            ItagItem(140, ItagType.AUDIO, MediaFormat.M4A, 128),
            ItagItem(141, ItagType.AUDIO, MediaFormat.M4A, 256),
            ItagItem(600, ItagType.AUDIO, MediaFormat.WEBMA_OPUS, 35),
            ItagItem(249, ItagType.AUDIO, MediaFormat.WEBMA_OPUS, 50),
            ItagItem(250, ItagType.AUDIO, MediaFormat.WEBMA_OPUS, 70),
            ItagItem(251, ItagType.AUDIO, MediaFormat.WEBMA_OPUS, 160),
            /* VIDEO ONLY */
            ItagItem(160, ItagType.VIDEO_ONLY, MediaFormat.MPEG_4, "144p"),
            ItagItem(394, ItagType.VIDEO_ONLY, MediaFormat.MPEG_4, "144p"),
            ItagItem(133, ItagType.VIDEO_ONLY, MediaFormat.MPEG_4, "240p"),
            ItagItem(395, ItagType.VIDEO_ONLY, MediaFormat.MPEG_4, "240p"),
            ItagItem(134, ItagType.VIDEO_ONLY, MediaFormat.MPEG_4, "360p"),
            ItagItem(396, ItagType.VIDEO_ONLY, MediaFormat.MPEG_4, "360p"),
            ItagItem(135, ItagType.VIDEO_ONLY, MediaFormat.MPEG_4, "480p"),
            ItagItem(212, ItagType.VIDEO_ONLY, MediaFormat.MPEG_4, "480p"),
            ItagItem(397, ItagType.VIDEO_ONLY, MediaFormat.MPEG_4, "480p"),
            ItagItem(136, ItagType.VIDEO_ONLY, MediaFormat.MPEG_4, "720p"),
            ItagItem(398, ItagType.VIDEO_ONLY, MediaFormat.MPEG_4, "720p"),
            ItagItem(298, ItagType.VIDEO_ONLY, MediaFormat.MPEG_4, "720p60", 60),
            ItagItem(137, ItagType.VIDEO_ONLY, MediaFormat.MPEG_4, "1080p"),
            ItagItem(399, ItagType.VIDEO_ONLY, MediaFormat.MPEG_4, "1080p"),
            ItagItem(299, ItagType.VIDEO_ONLY, MediaFormat.MPEG_4, "1080p60", 60),
            ItagItem(400, ItagType.VIDEO_ONLY, MediaFormat.MPEG_4, "1440p"),
            ItagItem(266, ItagType.VIDEO_ONLY, MediaFormat.MPEG_4, "2160p"),
            ItagItem(401, ItagType.VIDEO_ONLY, MediaFormat.MPEG_4, "2160p"),
            ItagItem(278, ItagType.VIDEO_ONLY, MediaFormat.WEBM, "144p"),
            ItagItem(242, ItagType.VIDEO_ONLY, MediaFormat.WEBM, "240p"),
            ItagItem(243, ItagType.VIDEO_ONLY, MediaFormat.WEBM, "360p"),
            ItagItem(244, ItagType.VIDEO_ONLY, MediaFormat.WEBM, "480p"),
            ItagItem(245, ItagType.VIDEO_ONLY, MediaFormat.WEBM, "480p"),
            ItagItem(246, ItagType.VIDEO_ONLY, MediaFormat.WEBM, "480p"),
            ItagItem(247, ItagType.VIDEO_ONLY, MediaFormat.WEBM, "720p"),
            ItagItem(248, ItagType.VIDEO_ONLY, MediaFormat.WEBM, "1080p"),
            ItagItem(271, ItagType.VIDEO_ONLY, MediaFormat.WEBM, "1440p"),
            ItagItem(272, ItagType.VIDEO_ONLY, MediaFormat.WEBM, "2160p"),
            ItagItem(302, ItagType.VIDEO_ONLY, MediaFormat.WEBM, "720p60", 60),
            ItagItem(303, ItagType.VIDEO_ONLY, MediaFormat.WEBM, "1080p60", 60),
            ItagItem(308, ItagType.VIDEO_ONLY, MediaFormat.WEBM, "1440p60", 60),
            ItagItem(313, ItagType.VIDEO_ONLY, MediaFormat.WEBM, "2160p"),
            ItagItem(315, ItagType.VIDEO_ONLY, MediaFormat.WEBM, "2160p60", 60)
        )

        fun isSupported(itag: Int): Boolean = ITAG_LIST.any { it.id == itag }


        fun getItag(itagId: Int): ItagItem =
            ITAG_LIST.firstOrNull { it.id == itagId }?.let { ItagItem(it) }
                ?: throw ParsingException("itag $itagId is not supported")

        /* Constants */
        const val AVERAGE_BITRATE_UNKNOWN = -1
        const val SAMPLE_RATE_UNKNOWN = -1
        const val FPS_NOT_APPLICABLE_OR_UNKNOWN = -1
        const val TARGET_DURATION_SEC_UNKNOWN = -1
        const val AUDIO_CHANNELS_NOT_APPLICABLE_OR_UNKNOWN = -1
        const val CONTENT_LENGTH_UNKNOWN = -1L
        const val APPROX_DURATION_MS_UNKNOWN = -1L
    }
}