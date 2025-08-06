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

/**
 * Streaming reader for JSON documents.
 */
internal class JsonReader internal constructor(private val tokener: JsonTokener) {
    private var token: Int
    private val states = mutableListOf<Boolean>()
    private var stateIndex = 0
    private var inObject = false
    private var first = true
    private val key = StringBuilder()

    /**
     * The type of value that the [JsonReader] is positioned over.
     */
    enum class Type {
        /**
         * An object.
         */
        OBJECT,

        /**
         * An array.
         */
        ARRAY,

        /**
         * A string.
         */
        STRING,

        /**
         * A number.
         */
        NUMBER,

        /**
         * A boolean value (true or false).
         */
        BOOLEAN,

        /**
         * A null value.
         */
        NULL,
    }

    /**
     * Internal constructor.
     */
    init {
        token = tokener.advanceToToken(false)
    }

    /**
     * Returns to the array or object structure above the current one, and
     * advances to the next key or value.
     */
    @Throws(JsonParserException::class)
    fun pop(): Boolean {
        // CHECKSTYLE_OFF: EmptyStatement
        while (!next());
        // CHECKSTYLE_ON: EmptyStatement
        first = false
        ensureStateCapacity(stateIndex)
        inObject = states[--stateIndex]
        return token != JsonTokener.TOKEN_EOF
    }

    private fun ensureStateCapacity(index: Int) {
        while (states.size <= index) {
            states.add(false)
        }
    }

    /**
     * Returns the current type of the value.
     */
    @Throws(JsonParserException::class)
    fun current(): Type {
        return when (token) {
            JsonTokener.TOKEN_TRUE, JsonTokener.TOKEN_FALSE -> Type.BOOLEAN
            JsonTokener.TOKEN_NULL -> Type.NULL
            JsonTokener.TOKEN_NUMBER -> Type.NUMBER
            JsonTokener.TOKEN_STRING -> Type.STRING
            JsonTokener.TOKEN_OBJECT_START -> Type.OBJECT
            JsonTokener.TOKEN_ARRAY_START -> Type.ARRAY
            else -> throw createTokenMismatchException(
                JsonTokener.TOKEN_NULL,
                JsonTokener.TOKEN_TRUE,
                JsonTokener.TOKEN_FALSE,
                JsonTokener.TOKEN_NUMBER,
                JsonTokener.TOKEN_STRING,
                JsonTokener.TOKEN_OBJECT_START,
                JsonTokener.TOKEN_ARRAY_START
            )
        }
    }

    /**
     * Starts reading an object at the current value.
     */
    @Throws(JsonParserException::class)
    fun `object`() {
        if (token != JsonTokener.TOKEN_OBJECT_START) throw createTokenMismatchException(JsonTokener.TOKEN_OBJECT_START)
        ensureStateCapacity(stateIndex)
        states[stateIndex++] = inObject
        inObject = true
        first = true
    }

    /**
     * Reads the key for the object at the current value. Does not advance to the next value.
     */
    @Throws(JsonParserException::class)
    fun key(): String {
        if (!inObject) throw tokener.createParseException(null, "Not reading an object", true)
        return key.toString()
    }

    /**
     * Starts reading an array at the current value.
     */
    @Throws(JsonParserException::class)
    fun array() {
        if (token != JsonTokener.TOKEN_ARRAY_START) throw createTokenMismatchException(JsonTokener.TOKEN_ARRAY_START)
        ensureStateCapacity(stateIndex)
        states[stateIndex++] = inObject
        inObject = false
        first = true
    }

    /**
     * Returns the current value.
     */
    @Throws(JsonParserException::class)
    fun value(): Any? {
        return when (token) {
            JsonTokener.TOKEN_TRUE -> true
            JsonTokener.TOKEN_FALSE -> false
            JsonTokener.TOKEN_NULL -> null
            JsonTokener.TOKEN_NUMBER -> number()
            JsonTokener.TOKEN_STRING -> string()
            else -> throw createTokenMismatchException(
                JsonTokener.TOKEN_NULL,
                JsonTokener.TOKEN_TRUE,
                JsonTokener.TOKEN_FALSE,
                JsonTokener.TOKEN_NUMBER,
                JsonTokener.TOKEN_STRING
            )
        }
    }

    /**
     * Parses the current value as a null.
     */
    @Throws(JsonParserException::class)
    fun nul() {
        if (token != JsonTokener.TOKEN_NULL) throw createTokenMismatchException(JsonTokener.TOKEN_NULL)
    }

    /**
     * Parses the current value as a string.
     */
    @Throws(JsonParserException::class)
    fun string(): String? {
        if (token == JsonTokener.TOKEN_NULL) return null
        if (token != JsonTokener.TOKEN_STRING) throw createTokenMismatchException(
            JsonTokener.TOKEN_NULL, JsonTokener.TOKEN_STRING
        )
        return tokener.reusableBuffer.toString()
    }

    /**
     * Parses the current value as a boolean.
     */
    @Throws(JsonParserException::class)
    fun bool(): Boolean {
        return if (token == JsonTokener.TOKEN_TRUE) true
        else if (token == JsonTokener.TOKEN_FALSE) false
        else throw createTokenMismatchException(JsonTokener.TOKEN_TRUE, JsonTokener.TOKEN_FALSE)
    }

    /**
     * Parses the current value as a [Number].
     */
    @Throws(JsonParserException::class)
    fun number(): Number? {
        if (token == JsonTokener.TOKEN_NULL) return null
        return JsonLazyNumber(tokener.reusableBuffer.toString(), tokener.isDouble)
    }

    /**
     * Parses the current value as a long.
     */
    @Throws(JsonParserException::class)
    fun longVal(): Long {
        val s = tokener.reusableBuffer.toString()
        return if (tokener.isDouble) s.toDouble().toLong() else s.toLong()
    }

    /**
     * Parses the current value as an integer.
     */
    @Throws(JsonParserException::class)
    fun intVal(): Int {
        val s = tokener.reusableBuffer.toString()
        return if (tokener.isDouble) s.toDouble().toInt() else s.toInt()
    }

    /**
     * Parses the current value as a float.
     */
    @Throws(JsonParserException::class)
    fun floatVal(): Float {
        val s = tokener.reusableBuffer.toString()
        return s.toFloat()
    }

    /**
     * Parses the current value as a double.
     */
    @Throws(JsonParserException::class)
    fun doubleVal(): Double {
        val s = tokener.reusableBuffer.toString()
        return s.toDouble()
    }

    /**
     * Advance to the next value in this array or object. If no values remain,
     * return to the parent array or object.
     *
     * @return true if we still have values to read in this array or object,
     * false if we have completed this object (and implicitly moved back
     * to the parent array or object)
     */
    @Throws(JsonParserException::class)
    fun next(): Boolean {
        if (stateIndex == 0) {
            throw tokener.createParseException(null, "Unabled to call next() at the root", true)
        }

        token = tokener.advanceToToken(false)

        if (inObject) {
            if (token == JsonTokener.TOKEN_OBJECT_END) {
                inObject = states[--stateIndex]
                first = false
                return false
            }

            if (!first) {
                if (token != JsonTokener.TOKEN_COMMA) throw createTokenMismatchException(
                    JsonTokener.TOKEN_COMMA, JsonTokener.TOKEN_OBJECT_END
                )
                token = tokener.advanceToToken(false)
            }

            if (token != JsonTokener.TOKEN_STRING) throw createTokenMismatchException(JsonTokener.TOKEN_STRING)
            key.setLength(0)
            key.append(tokener.reusableBuffer) // reduce string garbage 
            if ((tokener.advanceToToken(false)
                    .also { token = it }) != JsonTokener.TOKEN_COLON
            ) throw createTokenMismatchException(JsonTokener.TOKEN_COLON)
            token = tokener.advanceToToken(false)
        } else {
            if (token == JsonTokener.TOKEN_ARRAY_END) {
                inObject = states[--stateIndex]
                first = false
                return false
            }
            if (!first) {
                if (token != JsonTokener.TOKEN_COMMA) throw createTokenMismatchException(
                    JsonTokener.TOKEN_COMMA, JsonTokener.TOKEN_ARRAY_END
                )
                token = tokener.advanceToToken(false)
            }
        }

        if (token != JsonTokener.TOKEN_NULL && token != JsonTokener.TOKEN_STRING && token != JsonTokener.TOKEN_NUMBER && token != JsonTokener.TOKEN_TRUE && token != JsonTokener.TOKEN_FALSE && token != JsonTokener.TOKEN_OBJECT_START && token != JsonTokener.TOKEN_ARRAY_START) throw createTokenMismatchException(
            JsonTokener.TOKEN_NULL,
            JsonTokener.TOKEN_STRING,
            JsonTokener.TOKEN_NUMBER,
            JsonTokener.TOKEN_TRUE,
            JsonTokener.TOKEN_FALSE,
            JsonTokener.TOKEN_OBJECT_START,
            JsonTokener.TOKEN_ARRAY_START
        )

        first = false

        return true
    }

    private fun createTokenMismatchException(vararg t: Int): JsonParserException {
        return tokener.createParseException(
            null, ("token mismatch (expected " + t.contentToString() + ", was " + token + ")"), true
        )
    }

    companion object {
        /**
         * Create a [JsonReader] from an [InputStream].
        @Throws(JsonParserException::class)
        fun from(`in`: InputStream?): JsonReader {
        return JsonReader(JsonTokener(`in`))
        }

         */
        /**
         * Create a [JsonReader] from a [String].
         *//*
        @Throws(JsonParserException::class)
        fun from(s: String?): JsonReader {
            return JsonReader(JsonTokener(StringReader(s)))
        }*/
    }
}
