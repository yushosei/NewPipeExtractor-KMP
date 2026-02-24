package com.yushosei.newpipe.extractor.soundcloud.linkHandler

import com.yushosei.newpipe.extractor.exceptions.ParsingException
import com.yushosei.newpipe.extractor.linkhandler.SearchQueryHandlerFactory
import com.yushosei.newpipe.extractor.soundcloud.SoundcloudParsingHelper.SOUNDCLOUD_API_V2_URL
import com.yushosei.newpipe.extractor.utils.Utils

class SoundcloudSearchQueryHandlerFactory private constructor() : SearchQueryHandlerFactory() {
    @Throws(ParsingException::class, UnsupportedOperationException::class)
    override fun getUrl(
        query: String,
        contentFilter: List<String>,
        sortFilter: String
    ): String {
        var endpoint = "$SOUNDCLOUD_API_V2_URL" + "search"

        val requestedFilter = contentFilter.firstOrNull()
        if (requestedFilter == TRACKS) {
            endpoint += "/tracks"
        }

        return endpoint + "?q=" + Utils.encodeUrlUtf8(query) +
                "&limit=$ITEMS_PER_PAGE&offset=0"
    }

    override val availableContentFilter: List<String>
        get() = listOf(ALL, TRACKS)

    companion object {
        val instance = SoundcloudSearchQueryHandlerFactory()

        const val TRACKS = "tracks"
        const val USERS = "users"
        const val PLAYLISTS = "playlists"
        const val ALL = "all"

        const val ITEMS_PER_PAGE = 10
    }
}
