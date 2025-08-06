/*
 * Created by Christian Schabesberger on 28.02.16.
 *
 * Copyright (C) 2016 Christian Schabesberger <chris.schabesberger@mailbox.org>
 * StreamInfoItemsCollector.java is part of NewPipe Extractor.
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
package com.yushosei.newpipe.extractor.stream

import com.yushosei.newpipe.extractor.InfoItemsCollector
import com.yushosei.newpipe.extractor.exceptions.FoundAdException
import com.yushosei.newpipe.extractor.exceptions.ParsingException

class StreamInfoItemsCollector

    : InfoItemsCollector<StreamInfoItem, StreamInfoItemExtractor> {
    constructor(serviceId: Int) : super(serviceId)

    constructor(
        serviceId: Int,
        comparator: Comparator<StreamInfoItem>
    ) : super(serviceId, comparator)

    
    override fun extract(extractor: StreamInfoItemExtractor): StreamInfoItem {
        /*if (extractor.isAd) {
            throw FoundAdException("Found ad")
        }*/

        val resultItem = StreamInfoItem(
            serviceId, extractor.url, extractor.name, extractor.streamType!!
        )

        // optional information
        try {
            resultItem.duration = extractor.duration
        } catch (e: Exception) {
            addError(e)
        }
        try {
            resultItem.uploaderName = extractor.uploaderName
        } catch (e: Exception) {
            addError(e)
        }
        try {
            resultItem.thumbnails = extractor.thumbnails
        } catch (e: Exception) {
            addError(e)
        }
        return resultItem
    }

    override fun commit(extractor: StreamInfoItemExtractor) {
        try {
            addItem(extract(extractor))
        } catch (ignored: FoundAdException) {
        } catch (e: Exception) {
            addError(e)
        }
    }
}
