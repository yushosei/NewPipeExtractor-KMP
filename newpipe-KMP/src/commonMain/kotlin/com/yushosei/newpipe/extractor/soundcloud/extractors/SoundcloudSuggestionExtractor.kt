package com.yushosei.newpipe.extractor.soundcloud.extractors

import com.yushosei.newpipe.extractor.StreamingService
import com.yushosei.newpipe.extractor.exceptions.ExtractionException
import com.yushosei.newpipe.extractor.NewPipe
import com.yushosei.newpipe.extractor.soundcloud.SoundcloudParsingHelper
import com.yushosei.newpipe.extractor.soundcloud.SoundcloudParsingHelper.SOUNDCLOUD_API_V2_URL
import com.yushosei.newpipe.extractor.suggestion.SuggestionExtractor
import com.yushosei.newpipe.extractor.utils.Utils
import com.yushosei.newpipe.nanojson.JsonObject
import com.yushosei.newpipe.nanojson.JsonParser
import com.yushosei.newpipe.nanojson.JsonParserException

class SoundcloudSuggestionExtractor(service: StreamingService) : SuggestionExtractor(service) {
    override suspend fun suggestionList(query: String): List<String> {
        if (query.isBlank()) {
            return emptyList()
        }

        val clientId = SoundcloudParsingHelper.clientId()
        val url = "$SOUNDCLOUD_API_V2_URL" +
                "search/queries?q=${Utils.encodeUrlUtf8(query)}" +
                "&client_id=$clientId&limit=10"
        val response = NewPipe.downloader.get(url, extractorLocalization).responseBody()

        try {
            val collection = JsonParser.`object`().from(response).getArray("collection")
            return collection
                .mapNotNull { it as? JsonObject }
                .map { it.getString("query", "") }
                .filter { it.isNotBlank() }
        } catch (e: JsonParserException) {
            throw ExtractionException("Could not parse json response", e)
        }
    }
}
