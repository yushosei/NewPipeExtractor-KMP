package com.yushosei.newpipe.extractor

import com.yushosei.newpipe.extractor.exceptions.ExtractionException
import com.yushosei.newpipe.extractor.linkhandler.LinkHandler

abstract class Info(
    val serviceId: Int,
    /**
     * Id of this Info object <br></br>
     * e.g. Youtube:  https://www.youtube.com/watch?v=RER5qCTzZ7     &gt;    RER5qCTzZ7
     */
    val id: String,
    /**
     * Different than the [.originalUrl] in the sense that it *may* be set as a cleaned
     * url.
     *
     * @see LinkHandler.getUrl
     * @see Extractor.getOriginalUrl
     */
    val url: String,
    /**
     * The url used to start the extraction of this [Info] object.
     *
     * @see Extractor.getOriginalUrl
     */
    var originalUrl: String,
    val name: String
) {
    // if you use an api and want to handle the website url
    // overriding original url is essential

    private val errors: MutableList<Throwable> = ArrayList()

    fun addError(throwable: Throwable) {
        errors.add(throwable)
    }

    fun addAllErrors(throwables: List<Throwable>) {
        errors.addAll(throwables)
    }

    constructor(serviceId: Int, linkHandler: LinkHandler, name: String) : this(
        serviceId,
        linkHandler.id,
        linkHandler.url,
        linkHandler.originalUrl,
        name
    )

    override fun toString(): String {
        val ifDifferentString = if (url == originalUrl) "" else " (originalUrl=\"$originalUrl\")"
        return "Info{" +
                "serviceId=$serviceId" +
                ", id='$id'" +
                ", url='$url'" +
                ifDifferentString +
                ", name='$name'" +
                ", errors=$errors" +
                '}'
    }

    val service: StreamingService
        get() {
            try {
                return NewPipe.getService(serviceId)
            } catch (e: ExtractionException) {
                // this should be unreachable, as serviceId certainly refers to a valid service
                throw RuntimeException("Info object has invalid service id", e)
            }
        }

    fun getErrors(): List<Throwable> {
        return errors
    }
}
