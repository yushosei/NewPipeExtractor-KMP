package com.yushosei.newpipe.extractor.utils

import com.yushosei.newpipe.extractor.locale.Locale


/**
 * This class contains a simple implementation of [Locale.forLanguageTag] for Android
 * API levels below 21 (Lollipop). This is needed as core library desugaring does not backport that
 * method as of this writing.
 * <br></br>
 * Relevant issue: https://issuetracker.google.com/issues/171182330
 */
object LocaleCompat {
    // Source: The AndroidX LocaleListCompat class's private forLanguageTagCompat() method.
    // Use Locale.forLanguageTag() on Android API level >= 21 / Java instead.
    fun forLanguageTag(tag: String): Locale? {
        return try {
            Locale(tag)
        } catch (e: Exception) {
            null // 잘못된 태그 처리
        }
    }
}