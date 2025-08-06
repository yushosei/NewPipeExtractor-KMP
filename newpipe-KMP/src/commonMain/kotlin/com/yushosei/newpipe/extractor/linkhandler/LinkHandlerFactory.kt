package com.yushosei.newpipe.extractor.linkhandler

import com.yushosei.newpipe.extractor.exceptions.ParsingException
import com.yushosei.newpipe.extractor.utils.Utils

/*
* Created by Christian Schabesberger on 26.07.16.
*
* Copyright (C) 2016 Christian Schabesberger <chris.schabesberger@mailbox.org>
* LinkHandlerFactory.java is part of NewPipe Extractor.
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
abstract class LinkHandlerFactory {

    @Throws(ParsingException::class, UnsupportedOperationException::class)
    abstract fun getId(url: String): String

    @Throws(ParsingException::class, UnsupportedOperationException::class)
    abstract fun getUrl(id: String): String

    
    abstract fun onAcceptUrl(url: String): Boolean

    @Throws(ParsingException::class, UnsupportedOperationException::class)
    open fun getUrl(id: String, baseUrl: String): String {
        return getUrl(id)
    }

    
    open fun fromUrl(url: String): LinkHandler {
        require(!Utils.isNullOrEmpty(url)) { "The url is null or empty" }
        val polishedUrl = Utils.followGoogleRedirectIfNeeded(url)
        val baseUrl = Utils.getBaseUrl(polishedUrl)
        return fromUrl(polishedUrl, baseUrl)
    }

    /**
     * Builds a [LinkHandler] from an URL and a base URL. The URL is expected to be already
     * polished from Google search redirects (otherwise how could `baseUrl` have been
     * extracted?).<br></br>
     * So do not call [Utils.followGoogleRedirectIfNeeded] on the URL if overriding
     * this function, since that should be done in [.fromUrl].
     *
     * @param url     the URL without Google search redirects to extract id from
     * @param baseUrl the base URL
     * @return a [LinkHandler] complete with information
     */
    
    open fun fromUrl(url: String, baseUrl: String): LinkHandler {
        if (!acceptUrl(url)) {
            throw ParsingException("URL not accepted: $url")
        }

        val id = getId(url)
        return LinkHandler(url, getUrl(id, baseUrl), id)
    }

    
    open fun fromId(id: String): LinkHandler {
        val url = getUrl(id)
        return LinkHandler(url, url, id)
    }

    
    open fun fromId(id: String, baseUrl: String): LinkHandler {
        val url = getUrl(id, baseUrl)
        return LinkHandler(url, url, id)
    }

    /**
     * When a VIEW_ACTION is caught this function will test if the url delivered within the calling
     * Intent was meant to be watched with this Service.
     * Return false if this service shall not allow to be called through ACTIONs.
     */
    
    fun acceptUrl(url: String): Boolean {
        return onAcceptUrl(url)
    }
}
