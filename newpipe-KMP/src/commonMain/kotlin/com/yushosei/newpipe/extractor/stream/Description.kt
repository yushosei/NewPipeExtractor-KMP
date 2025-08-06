package com.yushosei.newpipe.extractor.stream

class Description(content: String?, val type: Int) {
    private var content: String? = null

    init {
        if (content == null) {
            this.content = ""
        } else {
            this.content = content
        }
    }

    fun getContent(): String {
        return content!!
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o !is Description) {
            return false
        }
        return type == o.type && content == o.content
    }

    override fun hashCode(): Int {
        return 31 * type + (content?.hashCode() ?: 0)
    }

    companion object {
        const val HTML: Int = 1
        const val MARKDOWN: Int = 2
        const val PLAIN_TEXT: Int = 3
        val EMPTY_DESCRIPTION: Description = Description("", PLAIN_TEXT)
    }
}
