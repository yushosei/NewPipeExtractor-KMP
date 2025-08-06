package com.yushosei.newpipe.extractor.stream




class StreamSegment(
    /**
     * Title of this segment
     */
    var title: String,
    /**
     * Timestamp of the starting point in seconds
     */
    var startTimeSeconds: Int
)  {
    /**
     * The channel or creator linked to this segment
     */
    var channelName: String? = null

    /**
     * Direct url to this segment. This can be null if the service doesn't provide such function.
     */
    var url: String? = null

    /**
     * Preview url for this segment. This can be null if the service doesn't provide such function
     * or there is no resource found.
     */
    var previewUrl: String? = null
}
