package com.yushosei.newpipe.extractor

import com.yushosei.newpipe.extractor.exceptions.ParsingException
import com.yushosei.newpipe.extractor.stream.StreamInfoItemExtractor
import com.yushosei.newpipe.extractor.stream.StreamInfoItemsCollector

/*
* Created by Christian Schabesberger on 12.02.17.
*
* Copyright (C) 2017 Christian Schabesberger <chris.schabesberger@mailbox.org>
* InfoItemsSearchCollector.java is part of NewPipe Extractor.
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
 * A collector that can handle many extractor types, to be used when a list contains items of
 * different types (e.g. search)
 *
 *
 * This collector can handle the following extractor types:
 *
 *  * [StreamInfoItemExtractor]
 *
 * Calling [.extract] or [.commit] with any
 * other extractor type will raise an exception.
 */
class MultiInfoItemsCollector(serviceId: Int) :
    InfoItemsCollector<InfoItem, InfoItemExtractor>(serviceId) {
    private val streamCollector = StreamInfoItemsCollector(serviceId)

    override var errors: MutableList<Throwable>
        get() {
            val errors: MutableList<Throwable> =
                ArrayList(super.errors)
            errors.addAll(streamCollector.errors)

            return errors
        }
        set(errors) {
            super.errors = errors
        }

    override fun reset() {
        super.reset()
        streamCollector.reset()
    }

    
    override fun extract(extractor: InfoItemExtractor): InfoItem {
        // Use the corresponding collector for each item extractor type
        if (extractor is StreamInfoItemExtractor) {
            return streamCollector.extract(extractor)
        } else {
            throw IllegalArgumentException("Invalid extractor type: $extractor")
        }
    }
}
