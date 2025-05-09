/*
 * Copyright 2011 The nanojson Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.yushosei.nanojson

import kotlinx.io.IOException
import newpipe.nanojson.util.CharStream
import newpipe.nanojson.util.appendCodePointCompat
import kotlin.math.max

/**
 * Internal class for tokenizing JSON. Used by both [JsonParser] and [JsonReader].
 */
internal class JsonTokener {
    private var linePos = 1
    private var rowPos = 0
    private var charOffset = 0
    private var utf8adjust = 0
    private var tokenCharPos = 0
    private var tokenCharOffset = 0

    private var eof = false
    var index: Int = 0
    private val reader: CharStream
    private val buffer = CharArray(BUFFER_SIZE)
    private var bufferLength = 0

    private val utf8: Boolean

    var reusableBuffer: StringBuilder = StringBuilder()
    var isDouble: Boolean = false

    constructor(reader: CharStream) {
        this.reader = reader
        this.utf8 = false
        init()
    }

    @Throws(JsonParserException::class)
    private fun init() {
        eof = refillBuffer()
        consumeWhitespace()
    }

    /**
     * Expects a given string at the current position.
     */
    @Throws(JsonParserException::class)
    fun consumeKeyword(first: Char, expected: CharArray) {
        if (ensureBuffer(expected.size) < expected.size) {
            throw createHelpfulException(first, expected, 0)
        }

        for (i in expected.indices) if (buffer[index++] != expected[i]) throw createHelpfulException(
            first, expected, i
        )

        fixupAfterRawBufferRead()

        // The token shouldn't end with something other than an ASCII letter
        when (peekChar()) {
            ','.code, ':'.code, '{'.code, '}'.code, '['.code, ']'.code, ' '.code, '\n'.code, '\r'.code, '\t'.code -> {}
            else -> throw createHelpfulException(first, expected, expected.size)
        }
    }

    /**
     * Steps through to the end of the current number token (a non-digit token).
     */
    @Throws(JsonParserException::class)
    fun consumeTokenNumber(savedChar: Char) {
        reusableBuffer.setLength(0)
        reusableBuffer.append(savedChar)
        isDouble = false

        // Determine initial parser state based on the first character
        var state = when (savedChar) {
            '-' -> 1
            '0' -> 3
            in '1'..'9' -> 2
            else -> throw createParseException(null, "Invalid number start: $savedChar", true)
        }

        outer@ while (true) {
            val n = ensureBuffer(BUFFER_ROOM)
            if (n == 0) break@outer

            for (i in 0 until n) {
                val nc = buffer[index]
                if (!isDigitCharacter(nc.code)) break@outer

                val ns = when (state) {
                    1 -> when (nc) {
                        '0' -> 3
                        in '1'..'9' -> 2
                        else -> -1
                    }

                    2 -> when {
                        nc in '0'..'9' -> 2
                        nc == '.' -> {
                            isDouble = true
                            4
                        }

                        nc == 'e' || nc == 'E' -> {
                            isDouble = true
                            6
                        }

                        else -> -1
                    }

                    3 -> when {
                        nc == '.' -> {
                            isDouble = true
                            4
                        }

                        nc == 'e' || nc == 'E' -> {
                            isDouble = true
                            6
                        }

                        else -> -1
                    }

                    4, 5 -> when {
                        nc in '0'..'9' -> 5
                        (nc == 'e' || nc == 'E') && state == 5 -> {
                            isDouble = true
                            6
                        }

                        else -> -1
                    }

                    6 -> when (nc) {
                        '+', '-' -> 7
                        in '0'..'9' -> 8
                        else -> -1
                    }

                    7 -> when (nc) {
                        in '0'..'9' -> 8
                        else -> -1
                    }

                    8 -> if (nc in '0'..'9') 8 else -1

                    else -> throw createParseException(null, "Unexpected parser state", true)
                }

                reusableBuffer.append(nc)
                index++

                if (ns == -1) {
                    throw createParseException(null, "Malformed number: $reusableBuffer", true)
                }

                state = ns
            }
        }

        if (state !in listOf(2, 3, 5, 8)) {
            throw createParseException(null, "Malformed number: $reusableBuffer", true)
        }

        // Special case for -0 being a double
        if (state == 3 && savedChar == '-') {
            isDouble = true
        }

        fixupAfterRawBufferRead()
    }

    /**
     * Steps through to the end of the current string token (the unescaped double quote).
     */
    @Throws(JsonParserException::class)
    fun consumeTokenString(cc: Int) {
        reusableBuffer.setLength(0)


        // Assume no escapes or UTF-8 in the string to start (fast path)
        start@ while (true) {
            val n = ensureBuffer(BUFFER_ROOM)
            if (n == 0) throw createParseException(
                null, "String was not terminated before end of input", true
            )

            for (i in 0..<n) {
                val c = stringChar()
                if (c.code == cc) {
                    // Use the index before we fixup
                    reusableBuffer.appendRange(buffer, index - i - 1, index - i - 1 + i)
                    fixupAfterRawBufferRead()
                    return
                }
                if (c == '\\' || (utf8 && (c.code and 0x80) != 0)) {
                    reusableBuffer.appendRange(buffer, index - i - 1, index - i - 1 + i)
                    index--
                    break@start
                }
            }

            val offset = index - n
            reusableBuffer.appendRange(buffer, offset, offset + n)
        }

        outer@ while (true) {
            var n = ensureBuffer(BUFFER_ROOM)
            if (n == 0) throw createParseException(
                null, "String was not terminated before end of input", true
            )

            var end = index + n
            while (index < end) {
                val c = stringChar()

                if (utf8 && (c.code and 0x80) != 0) {
                    // If it's a UTF-8 codepoint, we know it won't have special meaning
                    consumeTokenStringUtf8Char(c)
                    continue@outer
                }

                when (c) {
                    '"', '\'' -> if (c.code == cc) {
                        fixupAfterRawBufferRead()
                        return
                    } else {
                        reusableBuffer.append(c)
                        break
                    }

                    '\\' -> {
                        // Ensure that we have at least MAX_ESCAPE here in the buffer
                        if (end - index < MAX_ESCAPE) {
                            // Re-adjust the buffer end, unlikely path
                            n = ensureBuffer(MAX_ESCAPE)
                            end = index + n
                            // Make sure that there's enough chars for a \\uXXXX escape
                            if (buffer[index] == 'u' && n < MAX_ESCAPE) {
                                index = bufferLength // Reset index to last valid location
                                throw createParseException(
                                    null, "EOF encountered in the middle of a string escape", false
                                )
                            }
                        }
                        val escape = buffer[index++]
                        when (escape) {
                            'b' -> reusableBuffer.append('\b')
                            'f' -> reusableBuffer.append('\u000c')
                            'n' -> reusableBuffer.append('\n')
                            'r' -> reusableBuffer.append('\r')
                            't' -> reusableBuffer.append('\t')
                            '"', '\'', '/', '\\' -> reusableBuffer.append(escape)
                            'u' -> {
                                var escaped = 0

                                var j = 0
                                while (j < 4) {
                                    escaped = escaped shl 4
                                    val digit = buffer[index++].code
                                    escaped = if (digit >= '0'.code && digit <= '9'.code) {
                                        escaped or (digit - '0'.code)
                                    } else if (digit >= 'A'.code && digit <= 'F'.code) {
                                        escaped or (digit - 'A'.code) + 10
                                    } else if (digit >= 'a'.code && digit <= 'f'.code) {
                                        escaped or (digit - 'a'.code) + 10
                                    } else {
                                        throw createParseException(
                                            null,
                                            ("Expected unicode hex escape character: " + digit.toChar() + " (" + digit + ")"),
                                            false
                                        )
                                    }
                                    j++
                                }

                                reusableBuffer.append(escaped.toChar())
                            }

                            else -> throw createParseException(
                                null, "Invalid escape: \\$escape", false
                            )
                        }
                    }

                    else -> reusableBuffer.append(c)
                }
            }

            if (index > bufferLength) {
                index = bufferLength // Reset index to last valid location
                throw createParseException(
                    null, "EOF encountered in the middle of a string escape", false
                )
            }
        }
    }

    @Throws(JsonParserException::class)
    fun consumeTokenSemiString() {
        reusableBuffer.setLength(0)

        start@ while (true) {
            val n = ensureBuffer(BUFFER_ROOM)
            if (n == 0) throw createParseException(
                null, "String was not terminated before end of input", true
            )

            for (i in 0..<n) {
                val c = stringChar()
                if (isWhitespace(c.code) || c == ':') {
                    // Use the index before we fixup
                    val offset = index - i - 1
                    reusableBuffer.appendRange(buffer, offset, offset + i)
                    fixupAfterRawBufferRead()
                    return
                }
                if (c == '\\' || (utf8 && (c.code and 0x80) != 0)) {
                    val offset = index - i - 1
                    reusableBuffer.appendRange(buffer, offset, offset + i)
                    index--
                    break@start
                }
                if (c == '[' || c == ']' || c == '{' || c == '}' || c == ',') {
                    throw createParseException(null, "Invalid character in semi-string: $c", false)
                }
            }

            val offset = index - n
            reusableBuffer.appendRange(buffer, offset, offset + n)
        }

        outer@ while (true) {
            var n = ensureBuffer(BUFFER_ROOM)
            if (n == 0) throw createParseException(
                null, "String was not terminated before end of input", true
            )

            var end = index + n
            while (index < end) {
                val c = stringChar()

                if (utf8 && (c.code and 0x80) != 0) {
                    // If it's a UTF-8 codepoint, we know it won't have special meaning
                    consumeTokenStringUtf8Char(c)
                    continue@outer
                }

                when (c) {
                    ' ', '\n', '\r', '\t', ':' -> {
                        fixupAfterRawBufferRead()
                        return
                    }

                    '[', ']', '{', '}', ',' -> throw createParseException(
                        null, "Invalid character in semi-string: $c", false
                    )

                    '\\' -> {
                        // Ensure that we have at least MAX_ESCAPE here in the buffer
                        if (end - index < MAX_ESCAPE) {
                            // Re-adjust the buffer end, unlikely path
                            n = ensureBuffer(MAX_ESCAPE)
                            end = index + n
                            // Make sure that there's enough chars for a \\uXXXX escape
                            if (buffer[index] == 'u' && n < MAX_ESCAPE) {
                                index = bufferLength // Reset index to last valid location
                                throw createParseException(
                                    null, "EOF encountered in the middle of a string escape", false
                                )
                            }
                        }
                        val escape = buffer[index++]
                        when (escape) {
                            'b' -> reusableBuffer.append('\b')
                            'f' -> reusableBuffer.append('\u000c')
                            'n' -> reusableBuffer.append('\n')
                            'r' -> reusableBuffer.append('\r')
                            't' -> reusableBuffer.append('\t')
                            '"', '/', '\\' -> reusableBuffer.append(escape)
                            'u' -> {
                                var escaped = 0

                                var j = 0
                                while (j < 4) {
                                    escaped = escaped shl 4
                                    val digit = buffer[index++].code
                                    escaped = if (digit >= '0'.code && digit <= '9'.code) {
                                        escaped or (digit - '0'.code)
                                    } else if (digit >= 'A'.code && digit <= 'F'.code) {
                                        escaped or (digit - 'A'.code) + 10
                                    } else if (digit >= 'a'.code && digit <= 'f'.code) {
                                        escaped or (digit - 'a'.code) + 10
                                    } else {
                                        throw createParseException(
                                            null,
                                            ("Expected unicode hex escape character: " + digit.toChar() + " (" + digit + ")"),
                                            false
                                        )
                                    }
                                    j++
                                }

                                reusableBuffer.append(escaped.toChar())
                            }

                            else -> throw createParseException(
                                null, "Invalid escape: \\$escape", false
                            )
                        }
                    }

                    else -> reusableBuffer.append(c)
                }
            }

            if (index > bufferLength) {
                index = bufferLength // Reset index to last valid location
                throw createParseException(
                    null, "EOF encountered in the middle of a string escape", false
                )
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Throws(JsonParserException::class)
    private fun consumeTokenStringUtf8Char(c: Char) {
        var c = c
        ensureBuffer(5)

        // Hand-UTF8-decoding
        when (c.code and 0xf0) {
            0x80, 0x90, 0xa0, 0xb0 -> throw createParseException(
                null,
                "Illegal UTF-8 continuation byte: 0x" + (c.code and 0xff).toHexString(),
                false
            )

            0xc0 -> {
                // Check for illegal C0 and C1 bytes
                if ((c.code and 0xe) == 0) throw createParseException(
                    null, "Illegal UTF-8 byte: 0x" + (c.code and 0xff).toHexString(), false
                )
                c = ((c.code and 0x1f) shl 6 or (buffer[index++].code and 0x3f)).toChar()
                reusableBuffer.append(c)
                utf8adjust++
            }

            0xd0 -> {
                c = ((c.code and 0x1f) shl 6 or (buffer[index++].code and 0x3f)).toChar()
                reusableBuffer.append(c)
                utf8adjust++
            }

            0xe0 -> {
                c =
                    ((c.code and 0x0f) shl 12 or ((buffer[index++].code and 0x3f) shl 6) or (buffer[index++].code and 0x3f)).toChar()
                utf8adjust += 2
                // Check for illegally-encoded surrogate - http://unicode.org/faq/utf_bom.html#utf8-4
                if ((c >= '\ud800' && c <= '\udbff') || (c >= '\udc00' && c <= '\udfff')) throw createParseException(
                    null, "Illegal UTF-8 codepoint: 0x" + (c.code).toHexString(), false
                )
                reusableBuffer.append(c)
            }

            0xf0 -> {
                if ((c.code and 0xf) >= 5) throw createParseException(
                    null, "Illegal UTF-8 byte: 0x" + (c.code and 0xff).toHexString(), false
                )

                // Extended char
                when ((c.code and 0xc) shr 2) {
                    0, 1 -> {
                        reusableBuffer.appendCodePointCompat(
                            (c.code and 7) shl 18 or ((buffer[index++].code and 0x3f) shl 12) or ((buffer[index++].code and 0x3f) shl 6) or (buffer[index++].code and 0x3f)
                        )
                        utf8adjust += 3
                    }

                    2 -> {
                        // TODO: \uFFFD (replacement char)
                        val codepoint =
                            ((c.code and 3) shl 24 or ((buffer[index++].code and 0x3f) shl 18) or ((buffer[index++].code and 0x3f) shl 12) or ((buffer[index++].code and 0x3f) shl 6) or (buffer[index++].code and 0x3f))
                        throw createParseException(
                            null,
                            ("Unable to represent codepoint 0x" + codepoint.toHexString() + " in a Java string"),
                            false
                        )
                    }

                    3 -> {
                        val codepoint =
                            ((c.code and 1) shl 30 or ((buffer[index++].code and 0x3f) shl 24) or ((buffer[index++].code and 0x3f) shl 18) or ((buffer[index++].code and 0x3f) shl 12) or ((buffer[index++].code and 0x3f) shl 6) or (buffer[index++].code and 0x3f))
                        throw createParseException(
                            null,
                            ("Unable to represent codepoint 0x" + codepoint.toHexString() + " in a Java string"),
                            false
                        )
                    }

                    else -> check(false) { "Impossible" }
                }
            }

            else -> {}
        }
        if (index > bufferLength) throw createParseException(
            null, "UTF-8 codepoint was truncated", false
        )
    }

    /**
     * Advances a character, throwing if it is illegal in the context of a JSON string.
     */
    @Throws(JsonParserException::class)
    private fun stringChar(): Char {
        val c = buffer[index++]
        if (c.code < 32) throwControlCharacterException(c)
        return c
    }

    @Throws(JsonParserException::class)
    private fun throwControlCharacterException(c: Char) {
        // Need to ensure that we position this at the correct location for the error
        if (c == '\n') {
            linePos++
            rowPos = index + 1 + charOffset
            utf8adjust = 0
        }
        throw createParseException(
            null, "Strings may not contain control characters: 0x$c", false
        )
    }

    /**
     * Quick test for digit characters.
     */
    private fun isDigitCharacter(c: Int): Boolean {
        return (c >= '0'.code && c <= '9'.code) || c == 'e'.code || c == 'E'.code || c == '.'.code || c == '+'.code || c == '-'.code
    }

    /**
     * Quick test for whitespace characters.
     */
    fun isWhitespace(c: Int): Boolean {
        return c == ' '.code || c == '\n'.code || c == '\r'.code || c == '\t'.code
    }

    /**
     * Quick test for ASCII letter characters.
     */
    fun isAsciiLetter(c: Int): Boolean {
        return (c >= 'A'.code && c <= 'Z'.code) || (c >= 'a'.code && c <= 'z'.code)
    }

    /**
     * Returns true if EOF.
     */
    @Throws(JsonParserException::class)
    private fun refillBuffer(): Boolean {
        try {
            val r = reader.read(buffer, 0, buffer.size)
            if (r <= 0) {
                return true
            }
            charOffset += bufferLength
            index = 0
            bufferLength = r
            return false
        } catch (e: IOException) {
            throw createParseException(e, "IOException", true)
        }
    }

    /**
     * Peek one char ahead, don't advance, returns `EOF` (-1) on end of input.
     */
    private fun peekChar(): Int {
        return if (eof) -1 else buffer[index].code
    }

    /**
     * Ensures that there is enough room in the buffer to directly access the next N chars via buffer[].
     */
    @Throws(JsonParserException::class)
    fun ensureBuffer(n: Int): Int {
        // 이미 충분한 버퍼가 있는 경우
        if (bufferLength - n >= index) {
            return n
        }

        // 아직 읽을 게 남은 경우, 앞쪽 내용을 밀기
        if (index > 0) {
            charOffset += index
            bufferLength -= index
            buffer.copyInto(buffer, 0, index, index + bufferLength)
            index = 0
        }

        try {
            while (buffer.size > bufferLength) {
                val readLen = reader.read(buffer, bufferLength, buffer.size - bufferLength)
                if (readLen <= 0) {
                    return bufferLength - index // EOF
                }
                bufferLength += readLen
                if (bufferLength - index >= n) return bufferLength - index
            }
            throw Exception()
        } catch (e: Exception) {
            throw createParseException(e, "Stream read error", true)
        }
    }


    /**
     * Advance one character ahead, or return `EOF` (-1) on end of input.
     */
    @Throws(JsonParserException::class)
    private fun advanceChar(): Int {
        if (eof) return -1

        val c = buffer[index].code
        if (c == '\n'.code) {
            linePos++
            rowPos = index + 1 + charOffset
            utf8adjust = 0
        }

        index++

        // Prepare for next read
        if (index >= bufferLength) eof = refillBuffer()

        return c
    }

    fun advanceCharFast(): Int {
        val c = buffer[index].code
        if (c == '\n'.code) {
            linePos++
            rowPos = index + 1 + charOffset
            utf8adjust = 0
        }

        index++
        return c
    }

    @Throws(JsonParserException::class)
    private fun consumeWhitespace() {
        var n: Int
        do {
            n = ensureBuffer(BUFFER_ROOM)
            for (i in 0..<n) {
                val c = buffer[index]
                if (!isWhitespace(c.code)) {
                    fixupAfterRawBufferRead()
                    return
                }
                if (c == '\n') {
                    linePos++
                    rowPos = index + 1 + charOffset
                    utf8adjust = 0
                }
                index++
            }
        } while (n > 0)
        eof = true
    }

    /**
     * Consumes a token, first eating up any whitespace ahead of it. Note that number tokens are not necessarily valid
     * numbers.
     */
    @Throws(JsonParserException::class)
    fun advanceToToken(allowSemiString: Boolean): Int {
        var c = advanceChar()
        while (isWhitespace(c)) c = advanceChar()

        tokenCharPos = index + charOffset - rowPos - utf8adjust
        tokenCharOffset = charOffset + index

        val oldIndex = index
        var token: Int
        when (c) {
            -1 -> return TOKEN_EOF
            '['.code -> token = TOKEN_ARRAY_START
            ']'.code -> token = TOKEN_ARRAY_END
            ','.code -> token = TOKEN_COMMA
            ':'.code -> token = TOKEN_COLON
            '{'.code -> token = TOKEN_OBJECT_START
            '}'.code -> token = TOKEN_OBJECT_END
            't'.code -> try {
                consumeKeyword(c.toChar(), TRUE)
                token = TOKEN_TRUE
            } catch (e: JsonParserException) {
                if (allowSemiString) {
                    index = oldIndex - 1
                    consumeTokenSemiString()
                    token = TOKEN_SEMI_STRING
                } else throw e
            }

            'f'.code -> try {
                consumeKeyword(c.toChar(), FALSE)
                token = TOKEN_FALSE
            } catch (e: JsonParserException) {
                if (allowSemiString) {
                    index = oldIndex - 1
                    consumeTokenSemiString()
                    token = TOKEN_SEMI_STRING
                } else throw e
            }

            'n'.code -> try {
                consumeKeyword(c.toChar(), NULL)
                token = TOKEN_NULL
            } catch (e: JsonParserException) {
                if (allowSemiString) {
                    index = oldIndex - 1
                    consumeTokenSemiString()
                    token = TOKEN_SEMI_STRING
                } else throw e
            }

            '"'.code, '\''.code -> {
                consumeTokenString(c)
                token = TOKEN_STRING
            }

            '-'.code, '0'.code, '1'.code, '2'.code, '3'.code, '4'.code, '5'.code, '6'.code, '7'.code, '8'.code, '9'.code -> {
                consumeTokenNumber(c.toChar())
                token = TOKEN_NUMBER
            }

            '+'.code, '.'.code -> throw createParseException(
                null, "Numbers may not start with '" + c.toChar() + "'", true
            )

            else -> if (allowSemiString) {
                index--
                consumeTokenSemiString()
                token = TOKEN_SEMI_STRING
            } else {
                if (isAsciiLetter(c)) throw createHelpfulException(c.toChar(), null, 0)

                throw createParseException(null, "Unexpected character: " + c.toChar(), true)
            }
        }


//		consumeWhitespace();
        return token
    }

    @Throws(JsonParserException::class)
    fun tokenChar(): Int {
        var c = advanceChar()
        while (isWhitespace(c)) c = advanceChar()
        return c
    }

    /**
     * Helper function to fixup eof after reading buffer directly.
     */
    @Throws(JsonParserException::class)
    fun fixupAfterRawBufferRead() {
        if (index >= bufferLength) eof = refillBuffer()
    }

    /**
     * Throws a helpful exception based on the current alphanumeric token.
     */
    @Throws(JsonParserException::class)
    fun createHelpfulException(
        first: Char, expected: CharArray?, failurePosition: Int
    ): JsonParserException {
        // Build the first part of the token
        val errorToken = StringBuilder(
            first.toString() + (expected?.concatToString(
                0,
                0 + failurePosition
            ) ?: "")
        )

        // Consume the whole pseudo-token to make a better error message
        while (isAsciiLetter(peekChar()) && errorToken.length < 15) errorToken.append(advanceChar().toChar())

        return createParseException(
            null,
            ("Unexpected token '$errorToken'" + (if (expected == null) "" else ". Did you mean '$first" + expected
                .concatToString() + "'?")),
            true
        )
    }

    /**
     * Creates a [JsonParserException] and fills it from the current line and char position.
     */
    fun createParseException(
        e: Exception?, message: String, tokenPos: Boolean
    ): JsonParserException {
        if (tokenPos) return JsonParserException(
            e,
            "$message on line $linePos, char $tokenCharPos",
            linePos,
            tokenCharPos,
            tokenCharOffset
        )
        else {
            val charPos = max(1.0, (index + charOffset - rowPos - utf8adjust).toDouble()).toInt()
            return JsonParserException(
                e, "$message on line $linePos, char $charPos", linePos, charPos, index + charOffset
            )
        }
    }

    companion object {
        // Used by tests
        const val BUFFER_SIZE: Int = 32 * 1024

        const val BUFFER_ROOM: Int = 256
        const val MAX_ESCAPE: Int = 5 // uXXXX (don't need the leading slash)

        val TRUE: CharArray = charArrayOf('r', 'u', 'e')
        val FALSE: CharArray = charArrayOf('a', 'l', 's', 'e')
        val NULL: CharArray = charArrayOf('u', 'l', 'l')

        const val TOKEN_EOF: Int = 0
        const val TOKEN_COMMA: Int = 1
        const val TOKEN_COLON: Int = 2
        const val TOKEN_OBJECT_END: Int = 3
        const val TOKEN_ARRAY_END: Int = 4
        const val TOKEN_NULL: Int = 5
        const val TOKEN_TRUE: Int = 6
        const val TOKEN_FALSE: Int = 7
        const val TOKEN_STRING: Int = 8
        const val TOKEN_NUMBER: Int = 9
        const val TOKEN_OBJECT_START: Int = 10
        const val TOKEN_ARRAY_START: Int = 11
        const val TOKEN_SEMI_STRING: Int = 12
        const val TOKEN_VALUE_MIN: Int = TOKEN_NULL
    }
}
