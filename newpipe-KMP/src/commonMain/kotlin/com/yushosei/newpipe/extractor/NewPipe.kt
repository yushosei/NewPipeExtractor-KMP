package com.yushosei.newpipe.extractor

import com.yushosei.newpipe.extractor.ServiceList.all
import com.yushosei.newpipe.extractor.downloader.Downloader
import com.yushosei.newpipe.extractor.exceptions.ExtractionException
import com.yushosei.newpipe.extractor.localization.ContentCountry
import com.yushosei.newpipe.extractor.localization.Localization
import com.yushosei.newpipe.util.DefaultDownloaderImpl

/*
* Created by Christian Schabesberger on 23.08.15.
*
* Copyright (C) 2015 Christian Schabesberger <chris.schabesberger@mailbox.org>
* NewPipe Extractor.java is part of NewPipe Extractor.
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

/**
 * Provides access to streaming services supported by NewPipe.
 */
object NewPipe {
    lateinit var downloader: Downloader

    private var preferredLocalization: Localization? = null
    private var preferredContentCountry: ContentCountry? = null


    fun init(
        d: Downloader = DefaultDownloaderImpl(),
        l: Localization = Localization.DEFAULT,
        c: ContentCountry? = if (l.countryCode.isNullOrEmpty())
            ContentCountry.DEFAULT
        else
            ContentCountry(l.countryCode)
    ) {
        downloader = d
        preferredLocalization = l
        preferredContentCountry = c
    }

    val services: List<StreamingService>
        /*//////////////////////////////////////////////////////////////////////////
             // newpipe.timeago.raw.Utils
             ////////////////////////////////////////////////////////////////////////// */
        get() = all()


    fun getService(serviceId: Int): StreamingService {
        return all().firstOrNull { service: StreamingService -> service.serviceId == serviceId }!!
    }


    fun getService(serviceName: String): StreamingService {
        return all().firstOrNull { service: StreamingService -> service.serviceInfo.name == serviceName }!!
    }


    fun getServiceByUrl(url: String): StreamingService {
        for (service in all()) {
            if (service.getLinkTypeByUrl(url) != StreamingService.LinkType.NONE) {
                return service
            }
        }
        throw ExtractionException("No service can handle the url = \"$url\"")
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Localization
    ////////////////////////////////////////////////////////////////////////// */

    fun setupLocalization(
        thePreferredLocalization: Localization,
        thePreferredContentCountry: ContentCountry? = null
    ) {
        preferredLocalization = thePreferredLocalization

        if (thePreferredContentCountry != null) {
            preferredContentCountry = thePreferredContentCountry
        } else {
            preferredContentCountry = if (thePreferredLocalization.countryCode.isEmpty())
                ContentCountry.DEFAULT
            else
                ContentCountry(thePreferredLocalization.countryCode)
        }
    }


    fun getPreferredLocalization(): Localization {
        return if (preferredLocalization == null) Localization.DEFAULT else preferredLocalization!!
    }

    fun setPreferredLocalization(preferredLocalization: Localization?) {
        NewPipe.preferredLocalization = preferredLocalization
    }


    fun getPreferredContentCountry(): ContentCountry {
        return if (preferredContentCountry == null) ContentCountry.DEFAULT else preferredContentCountry!!
    }

    fun setPreferredContentCountry(preferredContentCountry: ContentCountry?) {
        NewPipe.preferredContentCountry = preferredContentCountry
    }
}
