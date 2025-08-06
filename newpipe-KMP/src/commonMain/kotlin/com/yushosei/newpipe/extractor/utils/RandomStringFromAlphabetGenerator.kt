package com.yushosei.newpipe.extractor.utils

import kotlin.random.Random

/**
 * Generates a random string from a predefined alphabet.
 */
object RandomStringFromAlphabetGenerator {
    /**
     * Generate a random string from an alphabet.
     *
     * @param alphabet the characters' alphabet to use
     * @param length   the length of the returned string (greater than 0)
     * @param random   [Random] (or better [java.security.SecureRandom]) used for
     * generating the random string
     * @return a random string of the requested length made of only characters from the provided
     * alphabet
     */
    fun generate(
        alphabet: String,
        length: Int,
        random: Random
    ): String {
        val stringBuilder = StringBuilder(length)
        for (i in 0..<length) {
            stringBuilder.append(alphabet[random.nextInt(alphabet.length)])
        }
        return stringBuilder.toString()
    }
}
