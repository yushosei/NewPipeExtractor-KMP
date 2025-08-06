package com.yushosei.newpipe.extractor.stream

import com.yushosei.newpipe.extractor.MediaFormat
import com.yushosei.newpipe.extractor.youtube.ItagItem
import com.yushosei.newpipe.extractor.stream.DeliveryMethod

/*
* Created by Christian Schabesberger on 04.03.16.
*
* Copyright (C) 2016 Christian Schabesberger <chris.schabesberger@mailbox.org>
* VideoStream.java is part of NewPipe Extractor.
*
* NewPipe Extractor is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* NewPipe Extractor is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with NewPipe Extractor. If not, see <https://www.gnu.org/licenses/>.
*/

class VideoStream private constructor(
    id: String,
    content: String,
    isUrl: Boolean,
    format: MediaFormat?,
    deliveryMethod: DeliveryMethod,
    resolution: String,
    isVideoOnly: Boolean,
    manifestUrl: String,
    itagItem: ItagItem?
) :
    Stream(id, content, isUrl, format, deliveryMethod, manifestUrl) {
    /**
     * Get the video resolution.
     *
     *
     *
     * It can be unknown for some streams, like for HLS master playlists. In this case,
     * [.RESOLUTION_UNKNOWN] is returned by this method.
     *
     *
     * @return the video resolution or [.RESOLUTION_UNKNOWN]
     */
    @Deprecated("Use {@link #getResolution()} instead. ")
    val resolution: String

    /**
     * Return whether the stream is video-only.
     *
     *
     *
     * Video-only streams have no audio.
     *
     *
     * @return `true` if this stream is video-only, `false` otherwise
     */
    @Deprecated("Use {@link #isVideoOnly()} instead. ")
    val isVideoOnly: Boolean

    /**
     * Get the itag identifier of the stream.
     *
     *
     *
     * Always equals to [.ITAG_NOT_AVAILABLE_OR_NOT_APPLICABLE] for other streams than the
     * ones of the YouTube service.
     *
     *
     * @return the number of the [ItagItem] passed in the constructor of the video stream.
     */
    // Fields for DASH
    var itag: Int = ITAG_NOT_AVAILABLE_OR_NOT_APPLICABLE
        private set

    /**
     * Get the bitrate of the stream.
     *
     * @return the bitrate set from the [ItagItem] passed in the constructor of the stream.
     */
    var bitrate: Int = 0
        private set

    /**
     * Get the initialization start of the stream.
     *
     * @return the initialization start value set from the [ItagItem] passed in the
     * constructor of the
     * stream.
     */
    var initStart: Int = 0
        private set

    /**
     * Get the initialization end of the stream.
     *
     * @return the initialization end value set from the [ItagItem] passed in the constructor
     * of the stream.
     */
    var initEnd: Int = 0
        private set

    /**
     * Get the index start of the stream.
     *
     * @return the index start value set from the [ItagItem] passed in the constructor of the
     * stream.
     */
    var indexStart: Int = 0
        private set

    /**
     * Get the index end of the stream.
     *
     * @return the index end value set from the [ItagItem] passed in the constructor of the
     * stream.
     */
    var indexEnd: Int = 0
        private set

    /**
     * Get the width of the video stream.
     *
     * @return the width set from the [ItagItem] passed in the constructor of the
     * stream.
     */
    var width: Int = 0
        private set

    /**
     * Get the height of the video stream.
     *
     * @return the height set from the [ItagItem] passed in the constructor of the
     * stream.
     */
    var height: Int = 0
        private set

    /**
     * Get the frames per second of the video stream.
     *
     * @return the frames per second set from the [ItagItem] passed in the constructor of the
     * stream.
     */
    var fps: Int = 0
        private set

    /**
     * Get the quality of the stream.
     *
     * @return the quality label set from the [ItagItem] passed in the constructor of the
     * stream.
     */
    var quality: String? = null
        private set

    /**
     * Get the codec of the stream.
     *
     * @return the codec set from the [ItagItem] passed in the constructor of the stream.
     */
    var codec: String? = null
        private set

    /**
     * {@inheritDoc}
     */
    override var itagItem: ItagItem? = null
        private set

    /**
     * Class to build [VideoStream] objects.
     */
    class Builder
    /**
     * Create a new [Builder] instance with its default values.
     */
    {
        private var id: String? = null
        private var content: String? = null
        private var isUrl = false
        private var deliveryMethod = DeliveryMethod.PROGRESSIVE_HTTP

        private var mediaFormat: MediaFormat? = null

        private var manifestUrl: String? = null

        // Use of the Boolean class instead of the primitive type needed for setter call check
        private var isVideoOnly: Boolean? = null
        private var resolution: String? = null

        private var itagItem: ItagItem? = null

        /**
         * Set the identifier of the [VideoStream].
         *
         *
         *
         * It must not be null, and should be non empty.
         *
         *
         *
         *
         * If you are not able to get an identifier, use the static constant [ ][Stream.ID_UNKNOWN] of the [Stream] class.
         *
         *
         * @param id the identifier of the [VideoStream], which must not be null
         * @return this [Builder] instance
         */
        fun setId(id: String?): Builder {
            this.id = id
            return this
        }

        /**
         * Set the content of the [VideoStream].
         *
         *
         *
         * It must not be null, and should be non empty.
         *
         *
         * @param content the content of the [VideoStream]
         * @param isUrl   whether the content is a URL
         * @return this [Builder] instance
         */
        fun setContent(
            content: String?,
            isUrl: Boolean
        ): Builder {
            this.content = content
            this.isUrl = isUrl
            return this
        }

        /**
         * Set the [MediaFormat] used by the [VideoStream].
         *
         *
         *
         * It should be one of the video [MediaFormat]s ([MPEG_4][MediaFormat.MPEG_4],
         * [v3GPP][MediaFormat.v3GPP], or [WEBM][MediaFormat.WEBM]) but can be `null` if the media format could not be determined.
         *
         *
         *
         *
         * The default value is `null`.
         *
         *
         * @param mediaFormat the [MediaFormat] of the [VideoStream], which can be null
         * @return this [Builder] instance
         */
        fun setMediaFormat(mediaFormat: MediaFormat?): Builder {
            this.mediaFormat = mediaFormat
            return this
        }

        /**
         * Set the [DeliveryMethod] of the [VideoStream].
         *
         *
         *
         * It must not be null.
         *
         *
         *
         *
         * The default delivery method is [DeliveryMethod.PROGRESSIVE_HTTP].
         *
         *
         * @param deliveryMethod the [DeliveryMethod] of the [VideoStream], which must
         * not be null
         * @return this [Builder] instance
         */
        fun setDeliveryMethod(deliveryMethod: DeliveryMethod): Builder {
            this.deliveryMethod = deliveryMethod
            return this
        }

        /**
         * Sets the URL of the manifest this stream comes from (if applicable, otherwise null).
         *
         * @param manifestUrl the URL of the manifest this stream comes from or `null`
         * @return this [Builder] instance
         */
        fun setManifestUrl(manifestUrl: String?): Builder {
            this.manifestUrl = manifestUrl
            return this
        }

        /**
         * Set whether the [VideoStream] is video-only.
         *
         *
         *
         * This property must be set before building the [VideoStream].
         *
         *
         * @param isVideoOnly whether the [VideoStream] is video-only
         * @return this [Builder] instance
         */
        fun setIsVideoOnly(isVideoOnly: Boolean): Builder {
            this.isVideoOnly = isVideoOnly
            return this
        }

        /**
         * Set the resolution of the [VideoStream].
         *
         *
         *
         * This resolution can be used by clients to know the quality of the video stream.
         *
         *
         *
         *
         * If you are not able to know the resolution, you should use [.RESOLUTION_UNKNOWN]
         * as the resolution of the video stream.
         *
         *
         *
         *
         * It must be set before building the builder and not null.
         *
         *
         * @param resolution the resolution of the [VideoStream]
         * @return this [Builder] instance
         */
        fun setResolution(resolution: String?): Builder {
            this.resolution = resolution
            return this
        }

        /**
         * Set the [ItagItem] corresponding to the [VideoStream].
         *
         *
         *
         * [ItagItem]s are YouTube specific objects, so they are only known for this service
         * and can be null.
         *
         *
         *
         *
         * The default value is `null`.
         *
         *
         * @param itagItem the [ItagItem] of the [VideoStream], which can be null
         * @return this [Builder] instance
         */
        fun setItagItem(itagItem: ItagItem?): Builder {
            this.itagItem = itagItem
            return this
        }

        /**
         * Build a [VideoStream] using the builder's current values.
         *
         *
         *
         * The identifier, the content (and so the `isUrl` boolean), the `isVideoOnly`
         * and the `resolution` properties must have been set.
         *
         *
         * @return a new [VideoStream] using the builder's current values
         * @throws IllegalStateException if `id`, `content` (and so `isUrl`),
         * `deliveryMethod`, `isVideoOnly` or `resolution` have been not set, or
         * have been set as `null`
         */
        fun build(): VideoStream {
            checkNotNull(id) {
                ("The identifier of the video stream has been not set or is null. If you "
                        + "are not able to get an identifier, use the static constant "
                        + "ID_UNKNOWN of the Stream class.")
            }

            checkNotNull(content) {
                ("The content of the video stream has been not set "
                        + "or is null. Please specify a non-null one with setContent.")
            }

            checkNotNull(deliveryMethod) {
                ("The delivery method of the video stream has been set as null, which is "
                        + "not allowed. Pass a valid one instead with setDeliveryMethod.")
            }

            checkNotNull(isVideoOnly) {
                ("The video stream has been not set as a "
                        + "video-only stream or as a video stream with embedded audio. Please "
                        + "specify this information with setIsVideoOnly.")
            }

            checkNotNull(resolution) {
                ("The resolution of the video stream has been not set. Please specify it "
                        + "with setResolution (use an empty string if you are not able to "
                        + "get it).")
            }

            return VideoStream(
                id!!, content!!, isUrl, mediaFormat, deliveryMethod, resolution!!,
                isVideoOnly!!, manifestUrl!!, itagItem
            )
        }
    }

    /**
     * Create a new video stream.
     *
     * @param id             the identifier which uniquely identifies the stream, e.g. for YouTube
     * this would be the itag
     * @param content        the content or the URL of the stream, depending on whether isUrl is
     * true
     * @param isUrl          whether content is the URL or the actual content of e.g. a DASH
     * manifest
     * @param format         the [MediaFormat] used by the stream, which can be null
     * @param deliveryMethod the [DeliveryMethod] of the stream
     * @param resolution     the resolution of the stream
     * @param isVideoOnly    whether the stream is video-only
     * @param itagItem       the [ItagItem] corresponding to the stream, which cannot be null
     * @param manifestUrl    the URL of the manifest this stream comes from (if applicable,
     * otherwise null)
     */
    init {
        if (itagItem != null) {
            this.itagItem = itagItem
            this.itag = itagItem.id
            this.bitrate = itagItem.bitrate
            this.initStart = itagItem.initStart
            this.initEnd = itagItem.initEnd
            this.indexStart = itagItem.indexStart
            this.indexEnd = itagItem.indexEnd
            this.codec = itagItem.codec
            this.height = itagItem.height
            this.width = itagItem.width
            this.quality = itagItem.quality
            this.fps = itagItem.fps
        }
        this.resolution = resolution
        this.isVideoOnly = isVideoOnly
    }

    /**
     * {@inheritDoc}
     */
    override fun equalStats(cmp: Stream): Boolean {
        return super.equalStats(cmp)
                && cmp is VideoStream
                && resolution == cmp.resolution
                && isVideoOnly == cmp.isVideoOnly
    }

    companion object {
        const val RESOLUTION_UNKNOWN: String = ""
    }
}
