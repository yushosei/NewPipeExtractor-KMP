package com.yushosei.newpipe.extractor

import com.yushosei.newpipe.extractor.utils.Utils

class Page(
    val url: String?,
    val id: String? = null,
    val ids: List<String?>? = null,
    val cookies: Map<String, String>? = null,
    val body: ByteArray? = null
) {
    constructor(url: String?, id: String?, body: ByteArray?) : this(url, id, null, null, body)

    constructor(url: String?, body: ByteArray?) : this(url, null, null, null, body)

    constructor(url: String?, cookies: Map<String, String>?) : this(url, null, null, cookies, null)

    constructor(ids: List<String?>?) : this(null, null, ids, null, null)

    constructor(ids: List<String?>?, cookies: Map<String, String>?) : this(
        null,
        null,
        ids,
        cookies,
        null
    )

    companion object {
        fun isValid(page: Page?): Boolean {
            return page != null && (!Utils.isNullOrEmpty(page.url)
                    || !Utils.isNullOrEmpty(page.ids))
        }
    }
}
