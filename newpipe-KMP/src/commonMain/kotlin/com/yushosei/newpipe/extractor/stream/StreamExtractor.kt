/*
 * Created by Christian Schabesberger on 10.08.18.
 *
 * Copyright (C) 2016 Christian Schabesberger <chris.schabesberger@mailbox.org>
 * StreamExtractor.java is part of NewPipe Extractor.
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

import com.yushosei.newpipe.extractor.locale.Locale
import kotlinx.io.IOException
import com.yushosei.newpipe.extractor.Extractor
import com.yushosei.newpipe.extractor.Image
import com.yushosei.newpipe.extractor.MetaInfo
import com.yushosei.newpipe.extractor.StreamingService
import com.yushosei.newpipe.extractor.exceptions.ExtractionException
import com.yushosei.newpipe.extractor.exceptions.ParsingException
import com.yushosei.newpipe.extractor.linkhandler.LinkHandler
import com.yushosei.newpipe.extractor.utils.Parser
import com.yushosei.newpipe.extractor.utils.Parser.RegexException

/**
 * Scrapes information from a video/audio streaming service (eg, YouTube).
 */
abstract class StreamExtractor(service: StreamingService, linkHandler: LinkHandler) :
    Extractor(service, linkHandler) {
    abstract val dashMpdUrl: String
    abstract val hlsUrl: String


    abstract val thumbnails: List<Image>


    open val description: Description
        /**
         * This is the stream description.
         *
         * @return The description of the stream/video or [Description.EMPTY_DESCRIPTION] if the
         * description is empty.
         */
        get() = Description.EMPTY_DESCRIPTION


    open val length: Long
        /**
         * This should return the length of a video in seconds.
         *
         * @return The length of the stream in seconds or 0 when it has no length (e.g. a livestream).
         */
        get() = 0


    open val timeStamp: Long
        /**
         * If the url you are currently handling contains a time stamp/seek, you can return the
         * position it represents here.
         * If the url has no time stamp simply return zero.
         *
         * @return the timestamp in seconds or 0 when there is no timestamp
         */
        get() = 0


    abstract val uploaderName: String

    abstract suspend fun audioStreams(): List<AudioStream>

    abstract val streamType: StreamType

    open val errorMessage: String?
        /**
         * Should analyse the webpage's document and extracts any error message there might be.
         *
         * @return Error message; `null` if there is no error message.
         */
        get() = null

    /**///////////////////////////////////////////////////////////////

    /**
     * Override this function if the format of timestamp in the url is not the same format as that
     * from youtube.
     *
     * @return the time stamp/seek for the video in seconds
     */

    protected fun getTimestampSeconds(regexPattern: String): Long {
        val timestamp: String
        try {
            timestamp = Parser.matchGroup1(regexPattern, originalUrl)
        } catch (e: RegexException) {
            // catch this instantly since a url does not necessarily have a timestamp

            // -2 because the testing system will consequently know that the regex failed
            // not good, I know

            return -2
        }

        if (!timestamp.isEmpty()) {
            try {
                var secondsString = ""
                var minutesString = ""
                var hoursString = ""
                try {
                    secondsString = Parser.matchGroup1("(\\d+)s", timestamp)
                    minutesString = Parser.matchGroup1("(\\d+)m", timestamp)
                    hoursString = Parser.matchGroup1("(\\d+)h", timestamp)
                } catch (e: Exception) {
                    // it could be that time is given in another method
                    if (secondsString.isEmpty() && minutesString.isEmpty()) {
                        // if nothing was obtained, treat as unlabelled seconds
                        secondsString = Parser.matchGroup1("t=(\\d+)", timestamp)
                    }
                }

                val seconds = if (secondsString.isEmpty()) 0 else secondsString.toInt()
                val minutes = if (minutesString.isEmpty()) 0 else minutesString.toInt()
                val hours = if (hoursString.isEmpty()) 0 else hoursString.toInt()

                return seconds + (60L * minutes) + (3600L * hours)
            } catch (e: ParsingException) {
                throw ParsingException("Could not get timestamp.", e)
            }
        } else {
            return 0
        }
    }


    open val category: String
        /**
         * The name of the category of the stream.
         * If the category is not available you can simply return an empty string.
         *
         * @return the category of the stream or an empty string.
         */
        get() = ""


    open val languageInfo: Locale?
        /**
         * The locale language of the stream.
         * If the language is not available you can simply return null.
         * If the language is provided by a language code, you can return
         * new Locale(language_code);
         *
         * @return the locale language of the stream or `null`.
         */
        get() = null


    open val tags: List<String>
        /**
         * The list of tags of the stream.
         * If the tag list is not available you can simply return an empty list.
         *
         * @return the list of tags of the stream or Collections.emptyList().
         */
        get() = emptyList<String>()


    open val streamSegments: List<StreamSegment>
        /**
         * The list of stream segments by timestamps for the stream.
         * If the segment list is not available you can simply return an empty list.
         *
         * @return The list of segments of the stream or an empty list.
         */
        get() = emptyList<StreamSegment>()


    open val metaInfo: List<MetaInfo>
        /**
         * Meta information about the stream.
         *
         *
         * This can be information about the stream creator (e.g. if the creator is a public
         * broadcaster) or further information on the topic (e.g. hints that the video might contain
         * conspiracy theories or contains information about a current health situation like the
         * Covid-19 pandemic).
         *
         * The meta information often contains links to external sources like Wikipedia or the WHO.
         *
         * @return The meta info of the stream or an empty list if not provided.
         */
        get() = emptyList<MetaInfo>()

    open val ageLimit: Int
        /**
         * The age limit of the stream.
         * If the age limit is not available you can simply return 0.
         *
         * @return the age limit of the stream or 0.
         */
        get() = NO_AGE_LIMIT

    enum class Privacy {
        PUBLIC,
        UNLISTED,
        PRIVATE,
        INTERNAL,
        OTHER
    }

    companion object {
        val NO_AGE_LIMIT: Int = 0
        val UNKNOWN_SUBSCRIBER_COUNT: Long = -1
    }
}
