package com.yushosei.newpipe.extractor.suggestion

import kotlinx.io.IOException
import com.yushosei.newpipe.extractor.StreamingService
import com.yushosei.newpipe.extractor.exceptions.ExtractionException
import com.yushosei.newpipe.extractor.localization.ContentCountry
import com.yushosei.newpipe.extractor.localization.Localization

abstract class SuggestionExtractor(val service: StreamingService) {
    private var forcedLocalization: Localization? = null
    private var forcedContentCountry: ContentCountry? = null

    
    abstract suspend fun suggestionList(query: String): List<String>

    val serviceId: Int
        get() = service.serviceId

    // TODO: Create a more general Extractor class
    fun forceLocalization(localization: Localization?) {
        this.forcedLocalization = localization
    }

    fun forceContentCountry(contentCountry: ContentCountry?) {
        this.forcedContentCountry = contentCountry
    }


    val extractorLocalization: Localization
        get() = if (forcedLocalization == null) service.localization else forcedLocalization!!


    val extractorContentCountry: ContentCountry
        get() = if (forcedContentCountry == null)
            service.contentCountry
        else
            forcedContentCountry!!
}
