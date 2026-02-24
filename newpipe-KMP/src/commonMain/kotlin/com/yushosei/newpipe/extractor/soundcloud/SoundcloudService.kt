package com.yushosei.newpipe.extractor.soundcloud

import com.yushosei.newpipe.extractor.StreamingService
import com.yushosei.newpipe.extractor.StreamingService.ServiceInfo.MediaCapability
import com.yushosei.newpipe.extractor.linkhandler.LinkHandler
import com.yushosei.newpipe.extractor.linkhandler.LinkHandlerFactory
import com.yushosei.newpipe.extractor.linkhandler.SearchQueryHandler
import com.yushosei.newpipe.extractor.linkhandler.SearchQueryHandlerFactory
import com.yushosei.newpipe.extractor.localization.ContentCountry
import com.yushosei.newpipe.extractor.search.SearchExtractor
import com.yushosei.newpipe.extractor.soundcloud.extractors.SoundcloudSearchExtractor
import com.yushosei.newpipe.extractor.soundcloud.extractors.SoundcloudStreamExtractor
import com.yushosei.newpipe.extractor.soundcloud.extractors.SoundcloudSuggestionExtractor
import com.yushosei.newpipe.extractor.soundcloud.linkHandler.SoundcloudSearchQueryHandlerFactory
import com.yushosei.newpipe.extractor.soundcloud.linkHandler.SoundcloudStreamLinkHandlerFactory
import com.yushosei.newpipe.extractor.stream.StreamExtractor
import com.yushosei.newpipe.extractor.suggestion.SuggestionExtractor

class SoundcloudService(id: Int) :
    StreamingService(
        id,
        "SoundCloud",
        listOf(
            MediaCapability.AUDIO,
            MediaCapability.COMMENTS
        )
    ) {
    override val baseUrl: String
        get() = "https://soundcloud.com"

    override val streamLHFactory: LinkHandlerFactory
        get() = SoundcloudStreamLinkHandlerFactory.instance

    override val searchQHFactory: SearchQueryHandlerFactory
        get() = SoundcloudSearchQueryHandlerFactory.instance

    override fun getStreamExtractor(linkHandler: LinkHandler): StreamExtractor {
        return SoundcloudStreamExtractor(this, linkHandler)
    }

    override fun getSearchExtractor(queryHandler: SearchQueryHandler): SearchExtractor {
        return SoundcloudSearchExtractor(this, queryHandler)
    }

    override val suggestionExtractor: SuggestionExtractor
        get() = SoundcloudSuggestionExtractor(this)

    override val supportedCountries: List<ContentCountry>
        get() = ContentCountry.listFrom("AU", "CA", "DE", "FR", "GB", "IE", "NL", "NZ", "US")
}
