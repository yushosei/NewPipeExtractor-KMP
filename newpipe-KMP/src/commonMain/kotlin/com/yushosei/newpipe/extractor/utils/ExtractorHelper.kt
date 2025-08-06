package com.yushosei.newpipe.extractor.utils

import com.yushosei.newpipe.extractor.Info
import com.yushosei.newpipe.extractor.InfoItem
import com.yushosei.newpipe.extractor.ListExtractor
import com.yushosei.newpipe.extractor.ListExtractor.InfoItemsPage
import com.yushosei.newpipe.extractor.ListExtractor.InfoItemsPage.Companion.emptyPage

object ExtractorHelper {
    fun <T : InfoItem> getItemsPageOrLogError(
        info: Info, extractor: ListExtractor<T>
    ): InfoItemsPage<T> {
        try {
            val page = extractor.initialPage
            info.addAllErrors(page.errors)

            return page
        } catch (e: Exception) {
            info.addError(e)
            return emptyPage()
        }
    }
}
