package com.yushosei.newpipe.extractor.stream

import com.yushosei.newpipe.extractor.locale.Locale
import com.yushosei.newpipe.extractor.MediaFormat
import com.yushosei.newpipe.extractor.youtube.ItagItem
import com.yushosei.newpipe.extractor.stream.DeliveryMethod

/*
* Created by Christian Schabesberger on 04.03.16.
*
* Copyright (C) 2016 Christian Schabesberger <chris.schabesberger@mailbox.org>
* AudioStream.java is part of NewPipe Extractor.
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

class AudioStream private constructor(
    id: String,
    content: String,
    isUrl: Boolean,
    format: MediaFormat?,
    deliveryMethod: DeliveryMethod,
    averageBitrate: Int,
    manifestUrl: String?,
    audioTrackId: String?,
    audioTrackName: String?,
    audioLocale: Locale?,
    audioTrackType: AudioTrackType?,
    itagItem: ItagItem?
) :
    Stream(id, content, isUrl, format, deliveryMethod, manifestUrl) {
    /**
     * Get the average bitrate of the stream.
     *
     * @return the average bitrate or [.UNKNOWN_BITRATE] if it is unknown
     */
    val averageBitrate: Int

    /**
     * Get the itag identifier of the stream.
     *
     *
     *
     * Always equals to [.ITAG_NOT_AVAILABLE_OR_NOT_APPLICABLE] for other streams than the
     * ones of the YouTube service.
     *
     *
     * @return the number of the [ItagItem] passed in the constructor of the audio stream.
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
     * constructor of the stream.
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
     * Get the id of the audio track.
     *
     * @return the id of the audio track
     */
    // Fields about the audio track id/name
    val audioTrackId: String?

    /**
     * Get the name of the audio track, which may be `null` if this information is not
     * provided by the service.
     *
     * @return the name of the audio track or `null`
     */
    val audioTrackName: String?

    /**
     * Get the [Locale] of the audio representing the language of the stream, which is
     * `null` if the audio language of this stream is not known.
     *
     * @return the [Locale] of the audio or `null`
     */
    val audioLocale: Locale?

    /**
     * Get the [AudioTrackType] of the stream, which is `null` if the track type
     * is not known.
     *
     * @return the [AudioTrackType] of the stream or `null`
     */
    val audioTrackType: AudioTrackType?


    override var itagItem: ItagItem? = null

    /**
     * Class to build [AudioStream] objects.
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
        private var averageBitrate = UNKNOWN_BITRATE

        private var audioTrackId: String? = null

        private var audioTrackName: String? = null

        private var audioLocale: Locale? = null

        private var audioTrackType: AudioTrackType? = null

        private var itagItem: ItagItem? = null

        /**
         * Set the identifier of the [AudioStream].
         *
         *
         *
         * It **must not be null** and should be non empty.
         *
         *
         *
         *
         * If you are not able to get an identifier, use the static constant [ ][Stream.ID_UNKNOWN] of the [Stream] class.
         *
         *
         * @param id the identifier of the [AudioStream], which must not be null
         * @return this [Builder] instance
         */
        fun setId(id: String?): Builder {
            this.id = id
            return this
        }

        /**
         * Set the content of the [AudioStream].
         *
         *
         *
         * It must not be null, and should be non empty.
         *
         *
         * @param content the content of the [AudioStream]
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
         * Set the [MediaFormat] used by the [AudioStream].
         *
         *
         *
         * It should be one of the audio [MediaFormat]s ([M4A][MediaFormat.M4A],
         * [WEBMA][MediaFormat.WEBMA], [MP3][MediaFormat.MP3], [ OPUS][MediaFormat.OPUS], [OGG][MediaFormat.OGG], or [WEBMA_OPUS][MediaFormat.WEBMA_OPUS]) but
         * can be `null` if the media format could not be determined.
         *
         *
         *
         *
         * The default value is `null`.
         *
         *
         * @param mediaFormat the [MediaFormat] of the [AudioStream], which can be null
         * @return this [Builder] instance
         */
        fun setMediaFormat(mediaFormat: MediaFormat?): Builder {
            this.mediaFormat = mediaFormat
            return this
        }

        /**
         * Set the [DeliveryMethod] of the [AudioStream].
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
         * @param deliveryMethod the [DeliveryMethod] of the [AudioStream], which must
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
         * Set the average bitrate of the [AudioStream].
         *
         *
         *
         * The default value is [.UNKNOWN_BITRATE].
         *
         *
         * @param averageBitrate the average bitrate of the [AudioStream], which should
         * positive
         * @return this [Builder] instance
         */
        fun setAverageBitrate(averageBitrate: Int): Builder {
            this.averageBitrate = averageBitrate
            return this
        }

        /**
         * Set the audio track id of the [AudioStream].
         *
         *
         *
         * The default value is `null`.
         *
         *
         * @param audioTrackId the audio track id of the [AudioStream], which can be null
         * @return this [Builder] instance
         */
        fun setAudioTrackId(audioTrackId: String?): Builder {
            this.audioTrackId = audioTrackId
            return this
        }

        /**
         * Set the audio track name of the [AudioStream].
         *
         *
         *
         * The default value is `null`.
         *
         *
         * @param audioTrackName the audio track name of the [AudioStream], which can be null
         * @return this [Builder] instance
         */
        fun setAudioTrackName(audioTrackName: String?): Builder {
            this.audioTrackName = audioTrackName
            return this
        }

        /**
         * Set the [AudioTrackType] of the [AudioStream].
         *
         *
         *
         * The default value is `null`.
         *
         *
         * @param audioTrackType the audio track type of the [AudioStream], which can be null
         * @return this [Builder] instance
         */
        fun setAudioTrackType(audioTrackType: AudioTrackType?): Builder {
            this.audioTrackType = audioTrackType
            return this
        }

        /**
         * Set the [Locale] of the audio which represents its language.
         *
         *
         *
         * The default value is `null`, which means that the [Locale] is unknown.
         *
         *
         * @param audioLocale the [Locale] of the audio, which could be `null`
         * @return this [Builder] instance
         */
        fun setAudioLocale(audioLocale: Locale?): Builder {
            this.audioLocale = audioLocale
            return this
        }

        /**
         * Set the [ItagItem] corresponding to the [AudioStream].
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
         * @param itagItem the [ItagItem] of the [AudioStream], which can be null
         * @return this [Builder] instance
         */
        fun setItagItem(itagItem: ItagItem?): Builder {
            this.itagItem = itagItem
            return this
        }

        /**
         * Build an [AudioStream] using the builder's current values.
         *
         *
         *
         * The identifier and the content (and so the `isUrl` boolean) properties must have
         * been set.
         *
         *
         * @return a new [AudioStream] using the builder's current values
         * @throws IllegalStateException if `id`, `content` (and so `isUrl`) or
         * `deliveryMethod` have been not set, or have been set as `null`
         */
        fun build(): AudioStream {
            checkNotNull(id) {
                ("The identifier of the audio stream has been not set or is null. If you "
                        + "are not able to get an identifier, use the static constant "
                        + "ID_UNKNOWN of the Stream class.")
            }

            checkNotNull(content) {
                ("The content of the audio stream has been not set "
                        + "or is null. Please specify a non-null one with setContent.")
            }

            checkNotNull(deliveryMethod) {
                ("The delivery method of the audio stream has been set as null, which is "
                        + "not allowed. Pass a valid one instead with setDeliveryMethod.")
            }

            return AudioStream(
                id!!, content!!, isUrl, mediaFormat, deliveryMethod, averageBitrate,
                manifestUrl, audioTrackId, audioTrackName, audioLocale, audioTrackType,
                itagItem
            )
        }
    }


    /**
     * Create a new audio stream.
     *
     * @param id             the identifier which uniquely identifies the stream, e.g. for YouTube
     * this would be the itag
     * @param content        the content or the URL of the stream, depending on whether isUrl is
     * true
     * @param isUrl          whether content is the URL or the actual content of e.g. a DASH
     * manifest
     * @param format         the [MediaFormat] used by the stream, which can be null
     * @param deliveryMethod the [DeliveryMethod] of the stream
     * @param averageBitrate the average bitrate of the stream (which can be unknown, see
     * [.UNKNOWN_BITRATE])
     * @param audioTrackId   the id of the audio track
     * @param audioTrackName the name of the audio track
     * @param audioLocale    the [Locale] of the audio stream, representing its language
     * @param itagItem       the [ItagItem] corresponding to the stream, which cannot be null
     * @param manifestUrl    the URL of the manifest this stream comes from (if applicable,
     * otherwise null)
     */
    init {
        if (itagItem != null) {
            this.itagItem = itagItem
            this.itag = itagItem.id
            this.quality = itagItem.quality
            this.bitrate = itagItem.bitrate
            this.initStart = itagItem.initStart
            this.initEnd = itagItem.initEnd
            this.indexStart = itagItem.indexStart
            this.indexEnd = itagItem.indexEnd
            this.codec = itagItem.codec
        }
        this.averageBitrate = averageBitrate
        this.audioTrackId = audioTrackId
        this.audioTrackName = audioTrackName
        this.audioLocale = audioLocale
        this.audioTrackType = audioTrackType
    }

    /**
     * {@inheritDoc}
     */
    override fun equalStats(cmp: Stream): Boolean {
        return super.equalStats(cmp) && cmp is AudioStream
                && averageBitrate == cmp.averageBitrate && audioTrackId == cmp.audioTrackId
                && audioTrackType == cmp.audioTrackType && audioLocale == cmp.audioLocale
    }

    companion object {
        const val UNKNOWN_BITRATE: Int = -1
    }
}
