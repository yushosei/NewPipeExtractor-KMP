/*
 * Created by Christian Schabesberger on 28.09.16.
 *
 * Copyright (C) 2015 Christian Schabesberger <chris.schabesberger@mailbox.org>
 * YoutubeSuggestionExtractor.java is part of NewPipe Extractor.
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
package com.yushosei.newpipe.extractor.youtube.extractors

import kotlinx.io.IOException
import com.yushosei.newpipe.extractor.NewPipe
import com.yushosei.newpipe.extractor.StreamingService
import com.yushosei.newpipe.extractor.exceptions.ExtractionException
import com.yushosei.newpipe.extractor.exceptions.ParsingException
import com.yushosei.newpipe.extractor.suggestion.SuggestionExtractor
import com.yushosei.newpipe.extractor.utils.Utils
import com.yushosei.newpipe.nanojson.JsonArray
import com.yushosei.newpipe.nanojson.JsonParser
import com.yushosei.newpipe.nanojson.JsonParserException

class YoutubeSuggestionExtractor(service: StreamingService) : SuggestionExtractor(service) {
    
    override fun suggestionList(query: String): List<String> {
        val url = ("https://suggestqueries-clients6.youtube.com/complete/search"
                + "?client=" + "youtube"
                + "&ds=" + "yt"
                + "&gl=" + Utils.encodeUrlUtf8(extractorContentCountry.countryCode)
                + "&q=" + Utils.encodeUrlUtf8(query)
                + "&xhr=t")

        val headers: MutableMap<String, List<String>> = HashMap()
        headers["Origin"] = listOf("https://www.youtube.com")
        headers["Referer"] = listOf("https://www.youtube.com")

        val response = NewPipe.downloader
            .get(url, headers, extractorLocalization)

        val contentTypeHeader = response.getHeader("Content-Type")
        if (Utils.isNullOrEmpty(contentTypeHeader) || !contentTypeHeader!!.contains("application/json")) {
            throw ExtractionException(
                ("Invalid response type (got \"" + contentTypeHeader
                        + "\", excepted a JSON response) (response code "
                        + response.responseCode() + ")")
            )
        }

        val responseBody = response.responseBody()

        if (responseBody.isEmpty()) {
            throw ExtractionException("Empty response received")
        }

        try {
            val suggestions = JsonParser.array()
                .from(responseBody)
                .getArray(1) // 0: search query, 1: search suggestions, 2: tracking data?

            return suggestions
                .filterIsInstance<JsonArray>() // JsonArray 인스턴스만 필터링
                .mapNotNull { it.getString(0) } // 첫 번째 인덱스의 문자열
                .filter { !Utils.isBlank(it) } // 빈 문자열 제거
        } catch (e: JsonParserException) {
            throw ParsingException("Could not parse JSON response", e)
        }
    }
}
