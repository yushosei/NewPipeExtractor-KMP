/*
 * Created by Christian Schabesberger on 26.08.15.
 *
 * Copyright (C) 2016 Christian Schabesberger <chris.schabesberger@mailbox.org>
 * StreamInfo.java is part of NewPipe Extractor.
 *
 * NewPipe Extractor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NewPipe Extractor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NewPipe Extractor.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.yushosei.newpipe.extractor.stream

import com.yushosei.newpipe.extractor.Image
import com.yushosei.newpipe.extractor.Info
import com.yushosei.newpipe.extractor.MetaInfo
import com.yushosei.newpipe.extractor.NewPipe.getServiceByUrl
import com.yushosei.newpipe.extractor.StreamingService
import com.yushosei.newpipe.extractor.exceptions.ContentNotAvailableException
import com.yushosei.newpipe.extractor.exceptions.ContentNotSupportedException
import com.yushosei.newpipe.extractor.exceptions.ExtractionException
import com.yushosei.newpipe.extractor.locale.Locale
import com.yushosei.newpipe.extractor.utils.Utils

/**
 * Info object for opened contents, i.e. the content ready to play.
 */
class StreamInfo(
    serviceId: Int,
    url: String,
    originalUrl: String,
    /**
     * Get the stream type
     *
     * @return the stream type
     */
    var streamType: StreamType,
    id: String,
    name: String,
    var ageLimit: Int
) :
    Info(serviceId, id, url, originalUrl, name) {
    class StreamExtractException internal constructor(message: String?) :
        ExtractionException(message)

    /**
     * Get the thumbnail url
     *
     * @return the thumbnail url as a string
     */
    var thumbnails: List<Image> = listOf()

    /**
     * Get the duration in seconds
     *
     * @return the duration in seconds
     */
    var duration: Long = -1
    var description: Description? = null

    var uploaderName: String = ""
    var uploaderUrl: String = ""

    var audioStreams: List<AudioStream> = listOf()

    var dashMpdUrl: String = ""
    var hlsUrl: String = ""

    var startPosition: Long = 0

    var category: String = ""
    var languageInfo: Locale? = null
    var tags: List<String> = listOf()
    var streamSegments: List<StreamSegment> = listOf()
    var metaInfo: List<MetaInfo> = listOf()

    /**
     * Preview frames, e.g. for the storyboard / seekbar thumbnail preview
     */


    companion object {

        fun getInfo(url: String): StreamInfo {
            return getInfo(getServiceByUrl(url), url)
        }


        fun getInfo(
            service: StreamingService,
            url: String
        ): StreamInfo {
            return getInfo(service.getStreamExtractor(url))
        }


        fun getInfo(extractor: StreamExtractor): StreamInfo {
            extractor.fetchPage()
            val streamInfo: StreamInfo
            try {
                streamInfo = extractImportantData(extractor)
                extractStreams(streamInfo, extractor)
                extractOptionalData(streamInfo, extractor)
                return streamInfo
            } catch (e: ExtractionException) {
                // Currently, YouTube does not distinguish between age restricted videos and videos
                // blocked by country. This means that during the initialisation of the extractor, the
                // extractor will assume that a video is age restricted while in reality it is blocked
                // by country.
                //
                // We will now detect whether the video is blocked by country or not.

                val errorMessage = extractor.errorMessage
                if (Utils.isNullOrEmpty(errorMessage)) {
                    throw e
                } else {
                    throw ContentNotAvailableException(errorMessage, e)
                }
            }
        }


        private fun extractImportantData(extractor: StreamExtractor): StreamInfo {
            // Important data, without it the content can't be displayed.
            // If one of these is not available, the frontend will receive an exception directly.

            val url = extractor.url
            val streamType = extractor.streamType
            val id = extractor.id
            val name = extractor.name
            val ageLimit = extractor.ageLimit

            // Suppress always-non-null warning as here we double-check it really is not null
            if (streamType == StreamType.NONE || Utils.isNullOrEmpty(url)
                || Utils.isNullOrEmpty(id)
                || name == null /* but it can be empty of course */ || ageLimit == -1
            ) {
                throw ExtractionException("Some important stream information was not given.")
            }

            return StreamInfo(
                extractor.serviceId, url!!, extractor.originalUrl,
                streamType, id!!, name, ageLimit
            )
        }


        private fun extractStreams(
            streamInfo: StreamInfo,
            extractor: StreamExtractor
        ) {
            /* ---- Stream extraction goes here ---- */
            // At least one type of stream has to be available, otherwise an exception will be thrown
            // directly into the frontend.

            try {
                streamInfo.dashMpdUrl = extractor.dashMpdUrl
            } catch (e: Exception) {
                streamInfo.addError(ExtractionException("Couldn't get DASH manifest", e))
            }

            try {
                streamInfo.hlsUrl = extractor.hlsUrl
            } catch (e: Exception) {
                streamInfo.addError(ExtractionException("Couldn't get HLS manifest", e))
            }

            try {
                streamInfo.audioStreams = extractor.audioStreams
            } catch (e: ContentNotSupportedException) {
                throw e
            } catch (e: Exception) {
                streamInfo.addError(ExtractionException("Couldn't get audio streams", e))
            }
        }

        private fun extractOptionalData(
            streamInfo: StreamInfo,
            extractor: StreamExtractor
        ) {
            /* ---- Optional data goes here: ---- */
            // If one of these fails, the frontend needs to handle that they are not available.
            // Exceptions are therefore not thrown into the frontend, but stored into the error list,
            // so the frontend can afterwards check where errors happened.

            try {
                streamInfo.thumbnails = extractor.thumbnails
            } catch (e: Exception) {
                streamInfo.addError(e)
            }
            try {
                streamInfo.duration = extractor.length
            } catch (e: Exception) {
                streamInfo.addError(e)
            }
            try {
                streamInfo.uploaderName = extractor.uploaderName
            } catch (e: Exception) {
                streamInfo.addError(e)
            }

            try {
                streamInfo.description = extractor.description
            } catch (e: Exception) {
                streamInfo.addError(e)
            }
            try {
                streamInfo.startPosition = extractor.timeStamp
            } catch (e: Exception) {
                streamInfo.addError(e)
            }
            try {
                streamInfo.category = extractor.category
            } catch (e: Exception) {
                streamInfo.addError(e)
            }
            try {
                streamInfo.languageInfo = extractor.languageInfo
            } catch (e: Exception) {
                streamInfo.addError(e)
            }
            try {
                streamInfo.tags = extractor.tags
            } catch (e: Exception) {
                streamInfo.addError(e)
            }
            try {
                streamInfo.streamSegments = extractor.streamSegments
            } catch (e: Exception) {
                streamInfo.addError(e)
            }
            try {
                streamInfo.metaInfo = extractor.metaInfo
            } catch (e: Exception) {
                streamInfo.addError(e)
            }
        }
    }
}