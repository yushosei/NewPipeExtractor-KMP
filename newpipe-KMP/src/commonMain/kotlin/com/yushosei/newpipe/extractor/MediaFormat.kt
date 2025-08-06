package com.yushosei.newpipe.extractor

enum class MediaFormat(
    val id: Int,
    val name2: String,
    val suffix: String,
    val mimeType: String
) {
    // Video + audio formats
    MPEG_4(0x0, "MPEG-4", "mp4", "video/mp4"),
    v3GPP(0x10, "3GPP", "3gp", "video/3gpp"),
    WEBM(0x20, "WebM", "webm", "video/webm"),

    // Audio formats
    M4A(0x100, "m4a", "m4a", "audio/mp4"),
    WEBMA(0x200, "WebM", "webm", "audio/webm"),
    MP3(0x300, "MP3", "mp3", "audio/mpeg"),
    MP2(0x310, "MP2", "mp2", "audio/mpeg"),
    OPUS(0x400, "opus", "opus", "audio/opus"),
    OGG(0x500, "ogg", "ogg", "audio/ogg"),
    WEBMA_OPUS(0x200, "WebM Opus", "webm", "audio/webm"),
    AIFF(0x600, "AIFF", "aiff", "audio/aiff"),
    AIF(0x600, "AIFF", "aif", "audio/aiff"),
    WAV(0x700, "WAV", "wav", "audio/wav"),
    FLAC(0x800, "FLAC", "flac", "audio/flac"),
    ALAC(0x900, "ALAC", "alac", "audio/alac"),

    // Subtitle formats
    VTT(0x1000, "WebVTT", "vtt", "text/vtt"),
    TTML(0x2000, "Timed Text Markup Language", "ttml", "application/ttml+xml"),
    TRANSCRIPT1(0x3000, "TranScript v1", "srv1", "text/xml"),
    TRANSCRIPT2(0x4000, "TranScript v2", "srv2", "text/xml"),
    TRANSCRIPT3(0x5000, "TranScript v3", "srv3", "text/xml"),
    SRT(0x6000, "SubRip file format", "srt", "text/srt");

    companion object {

        private fun <T> getById(id: Int, field: (MediaFormat) -> T, orElse: T): T {
            return entries.firstOrNull { it.id == id }?.let(field) ?: orElse
        }

        fun getNameById(id: Int): String =
            getById(id, { it.name2 }, "")

        fun getSuffixById(id: Int): String =
            getById(id, { it.suffix }, "")

        fun getMimeById(id: Int): String? =
            getById(id, { it.mimeType }, null)

        fun getFormatById(id: Int): MediaFormat? =
            entries.firstOrNull { it.id == id }

        fun getFromMimeType(mimeType: String): MediaFormat? =
            entries.firstOrNull { it.mimeType == mimeType }

        fun getAllFromMimeType(mimeType: String): List<MediaFormat> =
            entries.filter { it.mimeType == mimeType }

        fun getFromSuffix(suffix: String): MediaFormat? =
            entries.firstOrNull { it.suffix == suffix }
    }
}
