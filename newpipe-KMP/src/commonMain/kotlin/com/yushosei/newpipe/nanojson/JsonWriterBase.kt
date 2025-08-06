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
package com.yushosei.newpipe.nanojson

import kotlinx.io.IOException

/**
 * Internal class that handles emitting to an [Appendable]. Users only see
 * the public subclasses, [JsonStringWriter] and
 * [JsonAppendableWriter].
 *
 * @param <SELF>
 * A subclass of [JsonWriterBase].
</SELF> */
internal open class JsonWriterBase<SELF : JsonWriterBase<SELF>> : JsonSink<SELF> {
    protected val appendable: Appendable?
    protected val utf8: Boolean
    private val buffer: StringBuilder?
    private val bb: ByteArray?
    private var bo = 0

    // BitSet 대신 MutableList<Boolean>을 사용
    private val states = mutableListOf<Boolean>()

    private var stateIndex = 0
    private var first = true
    private var inObject = false

    /**
     * Sequence to use for indenting.
     */
    private val indentString: String?

    /**
     * Current indent amount.
     */
    private var indent = 0

    constructor(appendable: Appendable?, indent: String?) {
        this.appendable = appendable
        this.indentString = indent
        utf8 = false
        buffer = StringBuilder(BUFFER_SIZE)
        bb = null
    }


    /**
     * This is guaranteed to be safe as the type of "this" will always be the
     * type of "SELF".
     */
    private fun castThis(): SELF {
        return this as SELF
    }

    override fun array(c: Collection<*>): SELF {
        return array(null, c)
    }

    override fun array(key: String?, c: Collection<*>): SELF {
        if (key == null) array()
        else array(key)

        for (o in c) {
            value(o)
        }

        return end()
    }

    override fun `object`(map: Map<*, *>): SELF {
        return `object`(null, map)
    }

    override fun `object`(key: String?, map: Map<*, *>): SELF {
        if (key == null) `object`()
        else `object`(key)

        for ((key1, value) in map) {
            val o = value!!
            if (key1 !is String) throw JsonWriterException(
                "Invalid key type for map: "
                        + (if (key1 == null) "null" else key1::class)
            )
            value(key1, o)
        }

        return end()
    }

    override fun nul(): SELF {
        preValue()
        raw(NULL)
        return castThis()
    }

    override fun nul(key: String): SELF {
        preValue(key)
        raw(NULL)
        return castThis()
    }

    override fun value(o: Any?): SELF {
        when (o) {
            null -> return nul()
            is String -> return value(o)
            is Number -> return value(o)
            is Boolean -> return value(o)
            is Collection<*> -> return array(o)
            is Map<*, *> -> return `object`(o)
            is JsonArray -> {
                array()
                o.forEach { value(it) }
                return end()
            }

            else -> throw JsonWriterException(
                "Unable to handle type: "
                        + o::class
            )
        }
    }

    override fun value(key: String, o: Any?): SELF {
        when (o) {
            null -> return nul(key)
            is String -> return value(key, o)
            is Number -> return value(key, o)
            is Boolean -> return value(key, o)
            is Collection<*> -> return array(key, o)
            is Map<*, *> -> return `object`(key, o)
            is JsonArray -> {
                array(key)
                for (element in o) {
                    value(element)
                }
                return end()
            }

            else -> throw JsonWriterException(
                "Unable to handle type: "
                        + o::class
            )
        }
    }

    override fun value(s: String?): SELF {
        if (s == null) return nul()
        preValue()
        emitStringValue(s)
        return castThis()
    }

    override fun value(i: Int): SELF {
        preValue()
        raw(i.toString())
        return castThis()
    }

    override fun value(l: Long): SELF {
        preValue()
        raw(l.toString())
        return castThis()
    }

    override fun value(b: Boolean): SELF {
        preValue()
        raw(if (b) TRUE else FALSE)
        return castThis()
    }

    override fun value(d: Double): SELF {
        preValue()
        raw(d.toString())
        return castThis()
    }

    override fun value(d: Float): SELF {
        preValue()
        raw(d.toString())
        return castThis()
    }

    override fun value(n: Number?): SELF {
        preValue()
        if (n == null) raw(NULL)
        else raw(n.toString())
        return castThis()
    }

    override fun value(key: String, s: String?): SELF {
        if (s == null) return nul(key)
        preValue(key)
        emitStringValue(s)
        return castThis()
    }

    override fun value(key: String, i: Int): SELF {
        preValue(key)
        raw(i.toString())
        return castThis()
    }

    override fun value(key: String, l: Long): SELF {
        preValue(key)
        raw(l.toString())
        return castThis()
    }

    override fun value(key: String, b: Boolean): SELF {
        preValue(key)
        raw(if (b) TRUE else FALSE)
        return castThis()
    }

    override fun value(key: String, d: Double): SELF {
        preValue(key)
        raw(d.toString())
        return castThis()
    }

    override fun value(key: String, d: Float): SELF {
        preValue(key)
        raw(d.toString())
        return castThis()
    }

    override fun value(key: String, n: Number?): SELF {
        if (n == null) return nul(key)
        preValue(key)
        raw(n.toString())
        return castThis()
    }

    override fun array(): SELF {
        preValue()
        // BitSet 대신 MutableList에 상태 저장
        ensureStateCapacity(stateIndex)
        states[stateIndex++] = inObject
        inObject = false
        first = true
        raw('[')
        return castThis()
    }

    override fun `object`(): SELF {
        preValue()
        // BitSet 대신 MutableList에 상태 저장
        ensureStateCapacity(stateIndex)
        states[stateIndex++] = inObject
        inObject = true
        first = true
        raw('{')
        if (indentString != null) {
            indent++
            appendNewLine()
        }
        return castThis()
    }

    override fun array(key: String): SELF {
        preValue(key)
        // BitSet 대신 MutableList에 상태 저장
        ensureStateCapacity(stateIndex)
        states[stateIndex++] = inObject
        inObject = false
        first = true
        raw('[')
        return castThis()
    }

    override fun `object`(key: String): SELF {
        preValue(key)
        // BitSet 대신 MutableList에 상태 저장
        ensureStateCapacity(stateIndex)
        states[stateIndex++] = inObject
        inObject = true
        first = true
        raw('{')
        if (indentString != null) {
            indent++
            appendNewLine()
        }
        return castThis()
    }

    // MutableList의 크기를 필요에 따라 증가시키는 도우미 함수
    private fun ensureStateCapacity(index: Int) {
        while (states.size <= index) {
            states.add(false)
        }
    }

    override fun end(): SELF {
        if (stateIndex == 0) throw JsonWriterException("Invalid call to end()")

        if (inObject) {
            if (indentString != null) {
                indent--
                appendNewLine()
                appendIndent()
            }
            raw('}')
        } else {
            raw(']')
        }

        first = false
        // MutableList에서 상태 가져오기
        inObject = states[--stateIndex]
        return castThis()
    }

    /**
     * Ensures that the object is in the finished state.
     *
     * @throws JsonWriterException
     * if the written JSON is not properly balanced, ie: all arrays
     * and objects that were started have been properly ended.
     */
    protected fun doneInternal() {
        if (stateIndex > 0) throw JsonWriterException(
            "Unclosed JSON objects and/or arrays when closing writer"
        )
        if (first) throw JsonWriterException(
            "Nothing was written to the JSON writer"
        )

        flush()
    }

    private fun appendIndent() {
        for (i in 0..<indent) {
            indentString?.let {
                raw(it)
            }
        }
    }

    private fun appendNewLine() {
        raw('\n')
    }

    private fun raw(s: String) {
        if (utf8) {
            val l = s.length
            if (bo + l > BUFFER_SIZE) flush()
            for (i in 0..<l) bb!![bo++] = s[i].code.toByte()
        } else {
            buffer!!.append(s)
            if (buffer.length > BUFFER_SIZE) {
                flush()
            }
        }
    }

    private fun raw(c: CharArray) {
        if (utf8) {
            val l = c.size
            if (bo + l > BUFFER_SIZE) flush()
            for (i in 0..<l) bb!![bo++] = c[i].code.toByte()
        } else {
            buffer!!.append(c)
            if (buffer.length > BUFFER_SIZE) {
                flush()
            }
        }
    }

    private fun raw(c: Char) {
        if (utf8) {
            if (bo + 1 > BUFFER_SIZE) flush()
            bb!![bo++] = c.code.toByte()
        } else {
            buffer!!.append(c)
            if (buffer.length > BUFFER_SIZE) {
                flush()
            }
        }
    }

    private fun flush() {
        try {
            if (utf8) {
                // out!!.write(bb, 0, bo)
                bo = 0
            } else {
                appendable!!.append(buffer.toString())
                buffer!!.setLength(0)
            }
        } catch (e: IOException) {
            throw JsonWriterException(e)
        }
    }

    private fun pre() {
        if (first) {
            first = false
        } else {
            if (stateIndex == 0) throw JsonWriterException(
                "Invalid call to emit a value in a finished JSON writer"
            )
            raw(',')
            if (indentString != null && inObject) {
                appendNewLine()
            }
        }
    }

    private fun preValue() {
        if (inObject) throw JsonWriterException(
            "Invalid call to emit a keyless value while writing an object"
        )

        pre()
    }

    private fun preValue(key: String) {
        if (!inObject) throw JsonWriterException(
            "Invalid call to emit a key value while not writing an object"
        )

        pre()

        if (indentString != null) {
            appendIndent()
        }
        emitStringValue(key)
        raw(':')
    }

    /**
     * Emits a quoted string value, escaping characters that are required to be
     * escaped.
     */
    @OptIn(ExperimentalStdlibApi::class)
    private fun emitStringValue(s: String) {
        raw('"')
        var b = 0.toChar()
        var c = 0.toChar()
        var i = 0
        while (i < s.length) {
            b = c
            c = s[i]

            when (c) {
                '\\', '"' -> {
                    raw('\\')
                    raw(c)
                }

                '/' -> {
                    // Special case to ensure that </script> doesn't appear in JSON
                    // output
                    if (b == '<') raw('\\')
                    raw(c)
                }

                '\b' -> raw("\\b")
                '\t' -> raw("\\t")
                '\n' -> raw("\\n")
                '\u000c' -> raw("\\f")
                '\r' -> raw("\\r")
                else -> if (shouldBeEscaped(c)) {
                    if (c.code < 0x100) {
                        raw(UNICODE_SMALL)
                        raw(HEX[(c.code shr 4) and 0xf])
                        raw(HEX[c.code and 0xf])
                    } else {
                        raw(UNICODE_LARGE)
                        raw(HEX[(c.code shr 12) and 0xf])
                        raw(HEX[(c.code shr 8) and 0xf])
                        raw(HEX[(c.code shr 4) and 0xf])
                        raw(HEX[c.code and 0xf])
                    }
                } else {
                    if (utf8) {
                        if (bo + 4 > BUFFER_SIZE)  // 4 is the max char size
                            flush()
                        if (c.code < 0x80) {
                            bb!![bo++] = c.code.toByte()
                        } else if (c.code < 0x800) {
                            bb!![bo++] = (0xc0 or (c.code shr 6)).toByte()
                            bb[bo++] = (0x80 or (c.code and 0x3f)).toByte()
                        } else if (c.code < 0xd800) {
                            bb!![bo++] = (0xe0 or (c.code shr 12)).toByte()
                            bb[bo++] = (0x80 or ((c.code shr 6) and 0x3f)).toByte()
                            bb[bo++] = (0x80 or (c.code and 0x3f)).toByte()
                        } else if (c.code < 0xdfff) {
                            // TODO: bad surrogates
                            i++

                            val fc = toCodePoint(c, s[i])
                            if (fc < 0x1fffff) {
                                bb!![bo++] = (0xf0 or (fc shr 18)).toByte()
                                bb[bo++] = (0x80 or ((fc shr 12) and 0x3f)).toByte()
                                bb[bo++] = (0x80 or ((fc shr 6) and 0x3f)).toByte()
                                bb[bo++] = (0x80 or (fc and 0x3f)).toByte()
                            } else {
                                throw JsonWriterException(
                                    "Unable to encode character 0x"
                                            + fc.toHexString()
                                )
                            }
                        } else {
                            bb!![bo++] = (0xe0 or (c.code shr 12)).toByte()
                            bb[bo++] = (0x80 or ((c.code shr 6) and 0x3f)).toByte()
                            bb[bo++] = (0x80 or (c.code and 0x3f)).toByte()
                        }
                    } else {
                        raw(c)
                    }
                }
            }
            i++
        }

        raw('"')
    }

    /**
     * json.org spec says that all control characters must be escaped.
     */
    private fun shouldBeEscaped(c: Char): Boolean {
        return c < ' ' || (c >= '\u0080' && c < '\u00a0')
                || (c >= '\u2000' && c < '\u2100')
    }

    private fun toCodePoint(high: Char, low: Char): Int {
        return (high.code shl 10) + low.code + (-56613888) // 0x35fdc00 = -56613888
    }

    companion object {
        const val BUFFER_SIZE: Int = 10 * 1024
        private val NULL = charArrayOf('n', 'u', 'l', 'l')
        private val TRUE = charArrayOf('t', 'r', 'u', 'e')
        private val FALSE = charArrayOf('f', 'a', 'l', 's', 'e')
        private val HEX = charArrayOf(
            '0', '1', '2', '3', '4', '5', '6', '7', '8',
            '9', 'a', 'b', 'c', 'd', 'e', 'f'
        )
        private val UNICODE_SMALL = charArrayOf('\\', 'u', '0', '0')
        private val UNICODE_LARGE = charArrayOf('\\', 'u')
    }
}