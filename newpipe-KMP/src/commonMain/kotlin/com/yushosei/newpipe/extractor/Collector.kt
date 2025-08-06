package com.yushosei.newpipe.extractor

import com.yushosei.newpipe.extractor.exceptions.ParsingException

/**
 * Collectors are used to simplify the collection of information
 * from extractors
 * @param <I> the item type
 * @param <E> the extractor type
</E></I> */
interface Collector<I, E> {
    /**
     * Try to add an extractor to the collection
     * @param extractor the extractor to add
     */
    fun commit(extractor: E)

    /**
     * Try to extract the item from an extractor without adding it to the collection
     * @param extractor the extractor to use
     * @return the item
     * @throws ParsingException thrown if there is an error extracting the
     * **required** fields of the item.
     */
    
    fun extract(extractor: E): I

    /**
     * Get all items
     * @return the items
     */
    val items: List<I>?

    /**
     * Get all errors
     * @return the errors
     */
    val errors: MutableList<Throwable>

    /**
     * Reset all collected items and errors
     */
    fun reset()
}
