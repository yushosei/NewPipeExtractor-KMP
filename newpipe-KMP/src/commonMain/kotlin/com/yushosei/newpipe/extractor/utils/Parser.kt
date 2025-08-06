package com.yushosei.newpipe.extractor.utils

import com.yushosei.newpipe.extractor.exceptions.ParsingException

/**
 * Regex‑helper utilities rewritten in **pure Kotlin** (no java.util.regex / streams).
 */
object Parser {

    /* ─────────────────────────── exceptions ─────────────────────────────── */

    class RegexException(message: String?) : ParsingException(message)

    /* ─────────────────────────── public API ──────────────────────────────── */

    
    fun matchGroup1(pattern: String, input: String): String =
        matchGroup(pattern, input, 1)

    
    fun matchGroup1(pattern: Regex, input: String): String =
        matchGroup(pattern, input, 1)

    
    fun matchGroup(pattern: String, input: String, group: Int): String =
        matchGroup(Regex(pattern), input, group)

    
    fun matchGroup(pattern: Regex, input: String, group: Int): String {
        val match = pattern.find(input)
        if (match != null && group in match.groupValues.indices) {
            return match.groupValues[group]
        }
        val msg = if (input.length > 1024) {
            "Failed to find pattern \"${pattern.pattern}\""
        } else {
            "Failed to find pattern \"${pattern.pattern}\" inside of \"$input\""
        }
        throw RegexException(msg)
    }

    /* ───────────────────── multiple‑pattern helpers ─────────────────────── */

    
    fun matchGroup1MultiplePatterns(patterns: List<Regex>, input: String): String =
        matchMultiplePatterns(patterns, input).groupValues[1]

    
    fun matchMultiplePatterns(patterns: List<Regex>, input: String): MatchResult {
        var firstError: RegexException? = null
        for (pattern in patterns) {
            val match = pattern.find(input)
            if (match != null) return match
            if (firstError == null) {
                firstError = RegexException(
                    if (input.length > 1024)
                        "Failed to find pattern \"${pattern.pattern}\""
                    else
                        "Failed to find pattern \"${pattern.pattern}\" inside of \"$input\""
                )
            }
        }
        throw firstError ?: RegexException("Empty patterns array passed to matchMultiplePatterns")
    }

    /* ───────────────────── convenience predicates ───────────────────────── */

    fun isMatch(pattern: String, input: String): Boolean =
        Regex(pattern).containsMatchIn(input)

    fun isMatch(pattern: Regex, input: String): Boolean =
        pattern.containsMatchIn(input)

    /* ───────────────────── query‑string helper ──────────────────────────── */

    /**
     * Legacy helper that parses a URL query‑string into a Map.
     * Behaviour is identical to the old Java stream/collector version:
     *   - keeps the *last* occurrence when duplicate keys exist.
     */
    fun compatParseMap(input: String): Map<String, String> =
        input.split('&')
            .asSequence()
            .map { it.split('=', limit = 2) }
            .filter { it.size == 2 }
            .fold(mutableMapOf<String, String>()) { acc, (k, v) ->
                acc[k] = Utils.decodeUrlUtf8(v)
                acc
            }
}
