package com.yushosei.newpipe.extractor

import com.yushosei.newpipe.extractor.exceptions.FoundAdException
import com.yushosei.newpipe.extractor.exceptions.ParsingException

/*
* Created by Christian Schabesberger on 12.02.17.
*
* Copyright (C) 2017 Christian Schabesberger <chris.schabesberger@mailbox.org>
* InfoItemsCollector.java is part of NewPipe Extractor.
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
abstract class InfoItemsCollector<I : InfoItem, E : InfoItemExtractor>(
    /**
     * Get the service id
     * @return the service id
     */
    val serviceId: Int, private val comparator: Comparator<I>? = null
) :
    Collector<I, E> {
    private val itemList: MutableList<I> = ArrayList()
    override var errors: MutableList<Throwable> = ArrayList()

    /**
     * Create a new collector
     * @param serviceId the service id
     */

    override val items: List<I>
        get() {
            if (comparator != null) {
                itemList.sortWith(comparator)
            }
            return itemList
        }

    /*fun getErrors(): List<Throwable> {
        return errors.toList()
    }*/

    override fun reset() {
        itemList.clear()
        errors.clear()
    }

    /**
     * Add an error
     * @param error the error
     */
    protected fun addError(error: Exception) {
        errors.add(error)
    }

    /**
     * Add an item
     * @param item the item
     */
    protected fun addItem(item: I) {
        itemList.add(item)
    }

    override fun commit(extractor: E) {
        try {
            addItem(extract(extractor))
        } catch (ae: FoundAdException) {
            // found an ad. Maybe a debug line could be placed here
        } catch (e: ParsingException) {
            addError(e)
        }
    }
}
