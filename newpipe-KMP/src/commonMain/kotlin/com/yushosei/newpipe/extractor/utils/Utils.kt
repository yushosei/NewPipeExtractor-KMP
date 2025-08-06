package com.yushosei.newpipe.extractor.utils

import io.ktor.http.*
import com.yushosei.newpipe.extractor.exceptions.ParsingException
import com.yushosei.newpipe.extractor.utils.Parser.RegexException

object Utils {

    /* ─── constants ─── */
    const val HTTP = "http://"
    const val HTTPS = "https://"

    private val mRegex = Regex("""^(https?)?://m\.""", RegexOption.IGNORE_CASE)
    private val wwwRegex = Regex("""^(https?)?://www\.""", RegexOption.IGNORE_CASE)

    /* ─── URL encode / decode (UTF-8) ─── */

    fun encodeUrlUtf8(raw: String): String = raw.encodeURLParameter()        // ktor util
    fun decodeUrlUtf8(raw: String): String = raw.decodeURLPart()

    /* ─── misc string helpers ─── */

    fun removeNonDigitCharacters(s: String): String = s.replace(Regex("\\D+"), "")

    
    fun mixedNumberWordToLong(numberWord: String): Long {
        val countStr = Regex("""([\d]+([\.,][\d]+)?)""")
            .find(numberWord)
            ?.value?.replace(',', '.') ?: throw ParsingException("No number in $numberWord")
        val count = countStr.toDouble()

        val multiplier = Regex("""[KMBkmb]""").find(numberWord)?.value?.uppercase() ?: ""
        val factor = when (multiplier) {
            "K" -> 1e3
            "M" -> 1e6
            "B" -> 1e9
            else -> 1.0
        }
        return (count * factor).toLong()
    }

    fun replaceHttpWithHttps(url: String?): String =
        url?.let { if (it.startsWith(HTTP)) HTTPS + it.removePrefix(HTTP) else it }.toString()

    /* ─── URL & query helpers (ktor) ─── */

    fun stringToUrl(raw: String): Url =
        try {
            Url(raw)
        }                       // 정상
        catch (e: IllegalArgumentException) {  // 프로토콜 없을 때
            Url("$HTTPS$raw")
        }

    fun isHttp(url: Url): Boolean =
        url.protocol in setOf(URLProtocol.HTTP, URLProtocol.HTTPS) &&
                (url.port == url.protocol.defaultPort || url.port == 0)

    fun getQueryValue(rawUrl: String, name: String): String? =
        Url(rawUrl).parameters[name]

    fun getQueryValue(rawUrl: Url, name: String): String? =
        rawUrl.parameters[name]

    fun getBaseUrl(rawUrl: String): String {
        val url = stringToUrl(rawUrl)
        return "${url.protocol.name}://${url.host}"
    }

    fun followGoogleRedirectIfNeeded(raw: String): String =
        Url(raw).let { u ->
            if (u.host.contains("google") && u.encodedPath == "/url")
                u.parameters["url"]?.decodeURLPart() ?: raw
            else raw
        }

    /* ─── simple string utils ─── */

    fun removeMAndWWWFromUrl(url: String): String =
        url.replace(mRegex, "$1://").replace(wwwRegex, "$1://")

    fun removeUTF8BOM(s: String): String = s.trim('\uFEFF')

    fun isNullOrEmpty(s: String?): Boolean = s.isNullOrEmpty()
    fun <T> isNullOrEmpty(c: Collection<T>?): Boolean = c.isNullOrEmpty()
    fun <K, V> isNullOrEmpty(m: Map<K, V>?): Boolean = m.isNullOrEmpty()
    fun isBlank(s: String?): Boolean = s.isNullOrBlank()

    fun join(
        delimiter: String,
        mapJoin: String,
        elements: Map<out CharSequence, CharSequence>
    ): String =
        elements.entries.joinToString(delimiter) { "${it.key}$mapJoin${it.value}" }

    fun nonEmptyAndNullJoin(
        delimiter: CharSequence,
        vararg elements: String
    ): String =
        elements.asSequence()
            .filter { it.isNotEmpty() && it != "null" }
            .joinToString(delimiter)

    /* ─── regex helpers (pure Kotlin Regex) ─── */

    
    fun getStringResultFromRegexArray(
        input: String,
        regexes: List<Any>,
        group: Int = 0
    ): String {
        regexes.forEach { rx ->
            val regex = when (rx) {
                is String -> Regex(rx)
                is Regex -> rx
                else -> throw IllegalArgumentException("Unsupported regex type: ${rx::class}")
            }
            regex.find(input)?.let { m ->
                if (group in m.groupValues.indices) return m.groupValues[group]
            }
        }
        throw RegexException("No regex matched the input on group $group")
    }
}
