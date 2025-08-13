package com.yushosei.newpipe.extractor.locale


class Locale internal constructor(val languageTag: String) {

    // TODO

    companion object {
        /** Returns a [Locale] object which represents current locale */
        val current: Locale
            get() = current
    }

    /**
     * Create Locale object from a language tag.
     *
     * @param languageTag A [IETF BCP47](https://tools.ietf.org/html/bcp47) compliant language tag.
     * @return a locale object
     */

    /** The ISO 639 compliant language code. */
    val language: String = languageTag.substringBefore("-")

    /** The ISO 15924 compliant 4-letter script code. */
    val script: String =
        languageTag.substringAfter("-", "").substringBefore("-").takeIf { it.length == 4 } ?: ""

    /** The ISO 3166 compliant region code. */
    val region: String =
        languageTag.substringAfterLast("-").takeIf { it.length == 2 } ?: ""


    /**
     * Returns a IETF BCP47 compliant language tag representation of this Locale.
     *
     * @return A IETF BCP47 compliant language tag.
     */
    fun toLanguageTag(): String = languageTag

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is Locale) return false
        if (this === other) return true
        return toLanguageTag() == other.toLanguageTag()
    }

    // We don't use data class since we cannot offer copy function here.
    override fun hashCode(): Int = toLanguageTag().hashCode()

    override fun toString(): String = toLanguageTag()
}
