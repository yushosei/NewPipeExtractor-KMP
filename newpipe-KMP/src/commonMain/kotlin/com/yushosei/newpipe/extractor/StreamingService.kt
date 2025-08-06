package com.yushosei.newpipe.extractor

import com.yushosei.newpipe.extractor.StreamingService.ServiceInfo.MediaCapability
import com.yushosei.newpipe.extractor.exceptions.ExtractionException
import com.yushosei.newpipe.extractor.exceptions.ParsingException
import com.yushosei.newpipe.extractor.linkhandler.LinkHandler
import com.yushosei.newpipe.extractor.linkhandler.LinkHandlerFactory
import com.yushosei.newpipe.extractor.linkhandler.SearchQueryHandler
import com.yushosei.newpipe.extractor.linkhandler.SearchQueryHandlerFactory
import com.yushosei.newpipe.extractor.localization.ContentCountry
import com.yushosei.newpipe.extractor.localization.Localization
import com.yushosei.newpipe.extractor.search.SearchExtractor
import com.yushosei.newpipe.extractor.stream.StreamExtractor
import com.yushosei.newpipe.extractor.suggestion.SuggestionExtractor
import com.yushosei.newpipe.extractor.utils.Utils

/*
* Copyright (C) 2018 Christian Schabesberger <chris.schabesberger@mailbox.org>
* StreamingService.java is part of NewPipe Extractor.
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
abstract class StreamingService(
    val serviceId: Int,
    name: String,
    capabilities: List<MediaCapability>
) {
    /**
     * This class holds meta information about the service implementation.
     */
    class ServiceInfo(val name: String, val mediaCapabilities: List<MediaCapability>) {

        enum class MediaCapability {
            AUDIO, VIDEO, LIVE, COMMENTS
        }
    }

    /**
     * LinkType will be used to determine which type of URL you are handling, and therefore which
     * part of NewPipe should handle a certain URL.
     */
    enum class LinkType {
        NONE,
        STREAM,
        CHANNEL,
        PLAYLIST
    }


    val serviceInfo: ServiceInfo =
        ServiceInfo(name, capabilities)

    override fun toString(): String {
        return serviceId.toString() + ":" + serviceInfo.name
    }

    abstract val baseUrl: String?

    /*//////////////////////////////////////////////////////////////////////////
    // Url Id handler
    ////////////////////////////////////////////////////////////////////////// */
    /**
     * Must return a new instance of an implementation of LinkHandlerFactory for streams.
     * @return an instance of a LinkHandlerFactory for streams
     */
    abstract val streamLHFactory: LinkHandlerFactory

    /**
     * Must return an instance of an implementation of SearchQueryHandlerFactory.
     * @return an instance of a SearchQueryHandlerFactory
     */
    abstract val searchQHFactory: SearchQueryHandlerFactory

    /*//////////////////////////////////////////////////////////////////////////
    // Extractors
    ////////////////////////////////////////////////////////////////////////// */
    /**
     * Must create a new instance of a SearchExtractor implementation.
     * @param queryHandler specifies the keyword lock for, and the filters which should be applied.
     * @return a new SearchExtractor instance
     */
    abstract fun getSearchExtractor(queryHandler: SearchQueryHandler): SearchExtractor

    /**
     * Must create a new instance of a SuggestionExtractor implementation.
     * @return a new SuggestionExtractor instance
     */
    abstract val suggestionExtractor: SuggestionExtractor


    
    abstract fun getStreamExtractor(linkHandler: LinkHandler): StreamExtractor


    
    fun getSearchExtractor(
        query: String,
        contentFilter: List<String>,
        sortFilter: String
    ): SearchExtractor {
        return getSearchExtractor(
            searchQHFactory
                .fromQuery(query, contentFilter, sortFilter)
        )
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Short extractors overloads
    ////////////////////////////////////////////////////////////////////////// */
    
    fun getSearchExtractor(query: String): SearchExtractor {
        return getSearchExtractor(searchQHFactory.fromQuery(query))
    }


    
    fun getStreamExtractor(url: String): StreamExtractor {
        return getStreamExtractor(streamLHFactory.fromUrl(url))
    }


    /*//////////////////////////////////////////////////////////////////////////
    // newpipe.timeago.raw.Utils
    ////////////////////////////////////////////////////////////////////////// */
    /**
     * Figures out where the link is pointing to (a channel, a video, a playlist, etc.)
     * @param url the url on which it should be decided of which link type it is
     * @return the link type of url
     */
    
    fun getLinkTypeByUrl(url: String): LinkType {
        val polishedUrl = Utils.followGoogleRedirectIfNeeded(url)

        val sH = streamLHFactory

        return if (sH != null && sH.acceptUrl(polishedUrl)) {
            LinkType.STREAM
        } else {
            LinkType.NONE
        }
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Localization
    ////////////////////////////////////////////////////////////////////////// */
    open val supportedLocalizations: List<Localization>
        /**
         * Returns a list of localizations that this service supports.
         */
        get() = listOf(Localization.DEFAULT)

    open val supportedCountries: List<ContentCountry?>
        /**
         * Returns a list of countries that this service supports.<br></br>
         */
        get() = listOf(ContentCountry.DEFAULT)

    val localization: Localization
        /**
         * Returns the localization that should be used in this service. It will get which localization
         * the user prefer (using [NewPipe.getPreferredLocalization]), then it will:
         *
         *  * Check if the exactly localization is supported by this service.
         *  * If not, check if a less specific localization is available, using only the language
         * code.
         *  * Fallback to the [default][Localization.DEFAULT] localization.
         *
         */
        get() {
            val preferredLocalization = NewPipe.getPreferredLocalization()

            // Check the localization's language and country
            if (supportedLocalizations.contains(preferredLocalization)) {
                return preferredLocalization
            }

            // Fallback to the first supported language that matches the preferred language
            for (supportedLanguage in supportedLocalizations) {
                if (supportedLanguage.languageCode
                    == preferredLocalization.languageCode
                ) {
                    return supportedLanguage
                }
            }

            return Localization.DEFAULT
        }

    val contentCountry: ContentCountry
        /**
         * Returns the country that should be used to fetch content in this service. It will get which
         * country the user prefer (using [NewPipe.getPreferredContentCountry]), then it will:
         *
         *  * Check if the country is supported by this service.
         *  * If not, fallback to the [default][ContentCountry.DEFAULT] country.
         *
         */
        get() {
            val preferredContentCountry = NewPipe.getPreferredContentCountry()

            if (supportedCountries.contains(preferredContentCountry)) {
                return preferredContentCountry
            }

            return ContentCountry.DEFAULT
        }

    /**
     * Get an instance of the time ago parser using the patterns related to the passed localization.
     * <br></br><br></br>
     * Just like [.getLocalization], it will also try to fallback to a less specific
     * localization if the exact one is not available/supported.
     *
     * @throws IllegalArgumentException if the localization is not supported (parsing patterns are
     * not present).
     */
}
