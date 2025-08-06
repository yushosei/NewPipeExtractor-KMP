package com.yushosei.newpipe.extractor.stream

/**
 * An enum to represent the different delivery methods of [streams][Stream] which are returned
 * by the extractor.
 */
enum class DeliveryMethod {
    /**
     * Used for [Stream]s served using the progressive HTTP streaming method.
     */
    PROGRESSIVE_HTTP,

    /**
     * Used for [Stream]s served using the DASH (Dynamic Adaptive Streaming over HTTP)
     * adaptive streaming method.
     *
     * @see [the
     * Dynamic Adaptive Streaming over HTTP Wikipedia page](https://en.wikipedia.org/wiki/Dynamic_Adaptive_Streaming_over_HTTP) and [
 * DASH Industry Forum's website](https://dashif.org/) for more information about the DASH delivery method
     */
    DASH,

    /**
     * Used for [Stream]s served using the HLS (HTTP Live Streaming) adaptive streaming
     * method.
     *
     * @see [the HTTP Live Streaming
     * page](https://en.wikipedia.org/wiki/HTTP_Live_Streaming) and [Apple's developers website page
 * about HTTP Live Streaming](https://developer.apple.com/streaming) for more information about the HLS delivery method
     */
    HLS,

    /**
     * Used for [Stream]s served using the SmoothStreaming adaptive streaming method.
     *
     * @see [](https://en.wikipedia.org/wiki/Adaptive_bitrate_streaming.Microsoft_Smooth_Streaming_
    ) */
    SS,

    /**
     * Used for [Stream]s served via a torrent file.
     *
     * @see [Wikipedia's BitTorrent's page](https://en.wikipedia.org/wiki/BitTorrent),
     * [Wikipedia's page about torrent files
](https://en.wikipedia.org/wiki/Torrent_file) *  and [Bitorrent's website](https://www.bittorrent.org) for more information
     * about the BitTorrent protocol
     */
    TORRENT
}
