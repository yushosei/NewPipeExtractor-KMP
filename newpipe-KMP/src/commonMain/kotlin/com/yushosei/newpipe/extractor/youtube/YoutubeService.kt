package com.yushosei.newpipe.extractor.youtube

import com.yushosei.newpipe.extractor.StreamingService
import com.yushosei.newpipe.extractor.StreamingService.ServiceInfo.MediaCapability
import com.yushosei.newpipe.extractor.linkhandler.LinkHandler
import com.yushosei.newpipe.extractor.linkhandler.LinkHandlerFactory
import com.yushosei.newpipe.extractor.linkhandler.SearchQueryHandler
import com.yushosei.newpipe.extractor.linkhandler.SearchQueryHandlerFactory
import com.yushosei.newpipe.extractor.localization.ContentCountry
import com.yushosei.newpipe.extractor.localization.Localization
import com.yushosei.newpipe.extractor.search.SearchExtractor
import com.yushosei.newpipe.extractor.youtube.extractors.YoutubeMusicSearchExtractor
import com.yushosei.newpipe.extractor.youtube.extractors.YoutubeSearchExtractor
import com.yushosei.newpipe.extractor.youtube.extractors.YoutubeStreamExtractor
import com.yushosei.newpipe.extractor.youtube.extractors.YoutubeSuggestionExtractor
import com.yushosei.newpipe.extractor.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import com.yushosei.newpipe.extractor.youtube.linkHandler.YoutubeStreamLinkHandlerFactory
import com.yushosei.newpipe.extractor.stream.StreamExtractor
import com.yushosei.newpipe.extractor.suggestion.SuggestionExtractor

/*
* Created by Christian Schabesberger on 23.08.15.
*
* Copyright (C) 2018 Christian Schabesberger <chris.schabesberger@mailbox.org>
* YoutubeService.java is part of NewPipe Extractor.
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
* along with NewPipe Extractor.  If not, see <http://www.gnu.org/licenses/>.
*/
class YoutubeService(id: Int) :
    StreamingService(
        id,
        "YouTube",
        listOf(
            MediaCapability.AUDIO,
            MediaCapability.VIDEO,
            MediaCapability.LIVE,
            MediaCapability.COMMENTS
        )
    ) {
    override val baseUrl: String
        get() = "https://youtube.com"

    override val streamLHFactory: LinkHandlerFactory
        get() = YoutubeStreamLinkHandlerFactory.instance

    override val searchQHFactory: SearchQueryHandlerFactory
        get() = YoutubeSearchQueryHandlerFactory.instance

    override fun getStreamExtractor(linkHandler: LinkHandler): StreamExtractor {
        return YoutubeStreamExtractor(this, linkHandler)
    }

    override fun getSearchExtractor(query: SearchQueryHandler): SearchExtractor {
        val contentFilters = query.contentFilters

        return if (contentFilters.isNotEmpty() && contentFilters[0].startsWith("music_")) {
            YoutubeMusicSearchExtractor(this, query)
        } else {
            YoutubeSearchExtractor(this, query)
        }
    }

    override val suggestionExtractor: SuggestionExtractor
        get() = YoutubeSuggestionExtractor(this)

    companion object {
        /*//////////////////////////////////////////////////////////////////////////
    // Localization
    ////////////////////////////////////////////////////////////////////////// */
        // https://www.youtube.com/picker_ajax?action_language_json=1
        val supportedLocalizations: List<Localization> = Localization.listFrom(
            "en-GB" /*"af", "am", "ar", "az", "be", "bg", "bn", "bs", "ca", "cs", "da", "de",
            "el", "en", "en-GB", "es", "es-419", "es-US", "et", "eu", "fa", "fi", "fil", "fr",
            "fr-CA", "gl", "gu", "hi", "hr", "hu", "hy", "id", "is", "it", "iw", "ja",
            "ka", "kk", "km", "kn", "ko", "ky", "lo", "lt", "lv", "mk", "ml", "mn",
            "mr", "ms", "my", "ne", "nl", "no", "pa", "pl", "pt", "pt-PT", "ro", "ru",
            "si", "sk", "sl", "sq", "sr", "sr-Latn", "sv", "sw", "ta", "te", "th", "tr",
            "uk", "ur", "uz", "vi", "zh-CN", "zh-HK", "zh-TW", "zu"*/
        )


        // https://www.youtube.com/picker_ajax?action_country_json=1
        val supportedCountries: List<ContentCountry> = ContentCountry.listFrom(
            "DZ", "AR", "AU", "AT", "AZ", "BH", "BD", "BY", "BE", "BO", "BA", "BR", "BG", "KH",
            "CA", "CL", "CO", "CR", "HR", "CY", "CZ", "DK", "DO", "EC", "EG", "SV", "EE", "FI",
            "FR", "GE", "DE", "GH", "GR", "GT", "HN", "HK", "HU", "IS", "IN", "ID", "IQ", "IE",
            "IL", "IT", "JM", "JP", "JO", "KZ", "KE", "KW", "LA", "LV", "LB", "LY", "LI", "LT",
            "LU", "MY", "MT", "MX", "ME", "MA", "NP", "NL", "NZ", "NI", "NG", "MK", "NO", "OM",
            "PK", "PA", "PG", "PY", "PE", "PH", "PL", "PT", "PR", "QA", "RO", "RU", "SA", "SN",
            "RS", "SG", "SK", "SI", "ZA", "KR", "ES", "LK", "SE", "CH", "TW", "TZ", "TH", "TN",
            "TR", "UG", "UA", "AE", "GB", "US", "UY", "VE", "VN", "YE", "ZW"
        )
    }
}
