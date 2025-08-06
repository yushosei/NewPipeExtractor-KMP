package com.yushosei.newpipe.extractor.localization

/**
 * Represents a country that should be used when fetching content.
 *
 *
 * YouTube, for example, give different results in their feed depending on which country is
 * selected.
 *
 */
class ContentCountry(val countryCode: String) {
    override fun toString(): String {
        return countryCode
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o !is ContentCountry) {
            return false
        }

        return countryCode == o.countryCode
    }

    override fun hashCode(): Int {
        return countryCode.hashCode()
    }

    companion object {
        val DEFAULT: ContentCountry = ContentCountry(Localization.DEFAULT.countryCode)

        fun listFrom(vararg countryCodeList: String): List<ContentCountry> {
            val toReturn: MutableList<ContentCountry> = ArrayList()
            for (countryCode in countryCodeList) {
                toReturn.add(ContentCountry(countryCode))
            }
            return toReturn.toList()
        }
    }
}
