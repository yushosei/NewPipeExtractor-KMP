package com.yushosei.newpipe.extractor.localization

import com.yushosei.newpipe.extractor.locale.Locale
import com.yushosei.newpipe.extractor.exceptions.ParsingException
import com.yushosei.newpipe.extractor.utils.LocaleCompat.forLanguageTag

class Localization(
    val languageCode: String,
    val countryCode: String
) {
    val localizationCode: String
        get() = if (countryCode.isNullOrBlank()) languageCode else "$languageCode-$countryCode"

    override fun toString(): String = "Localization[$localizationCode]"

    override fun equals(other: Any?): Boolean {
        return other is Localization &&
                languageCode == other.languageCode &&
                countryCode == other.countryCode
    }

    override fun hashCode(): Int {
        return languageCode.hashCode() * 31 + (countryCode?.hashCode() ?: 0)
    }

    companion object {
        val DEFAULT: Localization = Localization("en", "GB")

        /**
         * Parses a list of localization codes like "en-GB" into Localization objects.
         */
        fun listFrom(vararg localizationCodeList: String): List<Localization> =
            localizationCodeList.map {
                fromLocalizationCode(it)
                    ?: throw IllegalArgumentException("Not a valid localization code: $it")
            }

        /**
         * Attempts to parse a single localization code like "en-US".
         */
        fun fromLocalizationCode(localizationCode: String): Localization? {
            return forLanguageTag(localizationCode)?.let { fromLocale(it) }
        }

        /**
         * Converts Compose Multiplatform's Locale to our Localization model.
         */
        fun fromLocale(locale: Locale): Localization {
            return Localization(
                locale.language,
                locale.region.takeIf { it.isNotBlank() }.toString()
            )
        }

        /**
         * Compose Multiplatform does not provide ISO3 code conversion like Java,
         * so we fallback to a fixed table or basic map if needed (optional).
         */
        
        fun getLocaleFromThreeLetterCode(code: String): Locale {
            // Basic sample map (expand as needed)
            val known = mapOf(
                "eng" to "en",
                "kor" to "ko",
                "jpn" to "ja",
                "deu" to "de",
                "fra" to "fr"
            )
            val lang = known[code.lowercase()]
                ?: throw ParsingException("Unsupported three-letter code: $code")
            return Locale(lang)
        }
    }
}
