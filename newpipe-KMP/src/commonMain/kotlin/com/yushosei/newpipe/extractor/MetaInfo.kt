package com.yushosei.newpipe.extractor

import io.ktor.http.Url
import com.yushosei.newpipe.extractor.stream.Description

class MetaInfo {
    /**
     * @return Title of the info. Can be empty.
     */
    var title: String = ""
    var content: Description? = null
    private var urls: MutableList<Url> = ArrayList()
    private var urlTexts: MutableList<String> = ArrayList()

    constructor(
        title: String,
        content: Description?,
        urls: MutableList<Url>,
        urlTexts: MutableList<String>
    ) {
        this.title = title
        this.content = content
        this.urls = urls
        this.urlTexts = urlTexts
    }

    constructor()


    fun getUrls(): List<Url> {
        return urls
    }

    fun setUrls(urls: MutableList<Url>) {
        this.urls = urls
    }

    fun addUrl(url: Url) {
        urls.add(url)
    }


    fun getUrlTexts(): List<String> {
        return urlTexts
    }

    fun setUrlTexts(urlTexts: MutableList<String>) {
        this.urlTexts = urlTexts
    }

    fun addUrlText(urlText: String) {
        urlTexts.add(urlText)
    }
}
