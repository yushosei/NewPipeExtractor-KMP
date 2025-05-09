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

import newpipe.nanojson.util.StringCharStream
import kotlin.jvm.JvmStatic

/**
 * Simple JSON parser.
 *
 * <pre>
 * Object json = [JsonParser].any().from("{\"a\":[true,false], \"b\":1}");
 * Number json = ([Number])[JsonParser].any().from("123.456e7");
 * JsonObject json = [JsonParser].object().from("{\"a\":[true,false], \"b\":1}");
 * JsonArray json = [JsonParser].array().from("[1, {\"a\":[true,false], \"b\":1}]");
</pre> *
 */
class JsonParser internal constructor(
    private val tokener: JsonTokener,
    private val lazyNumbers: Boolean
) {
    private var value: Any? = null
    private var token = 0

    /**
     * Returns a type-safe parser context for a [JsonObject], [JsonArray] or "any" type from which you can
     * parse a [String] or a [Reader].
     *
     * @param <T> The parsed type.
    </T> */
    class JsonParserContext<T> internal constructor() {
        private var lazyNumbers = false

        /**
         * Parses numbers lazily, allowing us to defer some of the cost of
         * number construction until later.
         */
        fun withLazyNumbers(): JsonParserContext<T> {
            lazyNumbers = true
            return this
        }

        /**
         * Parses the current JSON type from a [String].
         */
        @Throws(JsonParserException::class)
        fun from(s: String?): T {
            return JsonParser(JsonTokener(StringCharStream(s.toString())), lazyNumbers).parse()
        }
    }

    /**
     * Parse a single JSON value from the string, expecting an EOF at the end.
     */
    @Throws(JsonParserException::class)
    fun <T : Any> parse(): T {
        advanceToken(false, false)
        val parsed = currentValue()

        if (advanceToken(false, false) != JsonTokener.TOKEN_EOF) {
            throw tokener.createParseException(
                null,
                "Expected end of input, got $token", true
            )
        }

        @Suppress("UNCHECKED_CAST")
        return parsed as? T ?: throw tokener.createParseException(
            null,
            "Failed to cast parsed result to expected type",
            true
        )
    }

    /**
     * Starts parsing a JSON value at the current token position.
     */
    @Throws(JsonParserException::class)
    private fun currentValue(): Any? {
        // Only a value start token should appear when we're in the context of parsing a JSON value
        if (token >= JsonTokener.TOKEN_VALUE_MIN) return value
        throw tokener.createParseException(null, "Expected JSON value, got $token", true)
    }

    /**
     * Consumes a token, first eating up any whitespace ahead of it. Note that number tokens are not necessarily valid
     * numbers.
     */
    @Throws(JsonParserException::class)
    private fun advanceToken(allowSemiString: Boolean, old: Boolean): Int {
        if (old) tokener.index--
        token = tokener.advanceToToken(allowSemiString)
        when (token) {
            JsonTokener.TOKEN_ARRAY_START -> {
                val list = JsonArray()
                if (advanceToken(false, false) != JsonTokener.TOKEN_ARRAY_END) while (true) {
                    list.add(currentValue())
                    if (token == JsonTokener.TOKEN_SEMI_STRING) throw tokener.createParseException(
                        null,
                        "Semi-string is not allowed in array",
                        true
                    )
                    if (advanceToken(false, false) == JsonTokener.TOKEN_ARRAY_END) break
                    if (token != JsonTokener.TOKEN_COMMA) throw tokener.createParseException(
                        null,
                        "Expected a comma or end of the array instead of $token", true
                    )
                    if (advanceToken(
                            false,
                            false
                        ) == JsonTokener.TOKEN_ARRAY_END
                    ) throw tokener.createParseException(
                        null,
                        "Trailing comma found in array",
                        true
                    )
                }
                value = list
                return JsonTokener.TOKEN_ARRAY_START.also { token = it }
            }

            JsonTokener.TOKEN_OBJECT_START -> {
                val map = JsonObject()
                if (advanceToken(true, false) != JsonTokener.TOKEN_OBJECT_END) while (true) {
                    when (token) {
                        JsonTokener.TOKEN_NULL, JsonTokener.TOKEN_TRUE, JsonTokener.TOKEN_FALSE -> value =
                            value.toString()

                        JsonTokener.TOKEN_STRING, JsonTokener.TOKEN_SEMI_STRING -> {}
                        else -> throw tokener.createParseException(
                            null,
                            "Expected STRING, got $token", true
                        )
                    }
                    val key = value as String?
                    if (token == JsonTokener.TOKEN_SEMI_STRING) {
                        if (advanceToken(
                                false,
                                true
                            ) != JsonTokener.TOKEN_COLON
                        ) throw tokener.createParseException(
                            null,
                            "Expected COLON, got $token", true
                        )
                    } else if (advanceToken(
                            false,
                            false
                        ) != JsonTokener.TOKEN_COLON
                    ) throw tokener.createParseException(
                        null,
                        "Expected COLON, got $token", true
                    )
                    advanceToken(false, false)
                    map[key!!] = currentValue()
                    if (advanceToken(false, false) == JsonTokener.TOKEN_OBJECT_END) break
                    if (token != JsonTokener.TOKEN_COMMA) throw tokener.createParseException(
                        null,
                        "Expected a comma or end of the object instead of $token", true
                    )
                    if (advanceToken(
                            true,
                            false
                        ) == JsonTokener.TOKEN_OBJECT_END
                    ) throw tokener.createParseException(
                        null,
                        "Trailing object found in array",
                        true
                    )
                }
                value = map
                return JsonTokener.TOKEN_OBJECT_START.also { token = it }
            }

            JsonTokener.TOKEN_TRUE -> value = true
            JsonTokener.TOKEN_FALSE -> value = false
            JsonTokener.TOKEN_NULL -> value = null
            JsonTokener.TOKEN_STRING, JsonTokener.TOKEN_SEMI_STRING -> value =
                tokener.reusableBuffer.toString()

            JsonTokener.TOKEN_NUMBER -> value = if (lazyNumbers) {
                JsonLazyNumber(tokener.reusableBuffer.toString(), tokener.isDouble)
            } else {
                parseNumber()
            }

            else -> {}
        }

        return token
    }

    @Throws(JsonParserException::class)
    private fun parseNumber(): Number {
        val number = tokener.reusableBuffer.toString()

        try {
            if (tokener.isDouble) return number.toDouble()

            // Quick parse for single-digits
            if (number.length == 1) {
                return number[0].code - '0'.code
            } else if (number.length == 2 && number[0] == '-') {
                return '0'.code - number[1].code
            }

            // HACK: Attempt to parse using the approximate best type for this
            val firstMinus = number[0] == '-'
            val length = if (firstMinus) number.length - 1 else number.length
            // CHECKSTYLE_OFF: MagicNumber
            if (length < 10 || (length == 10 && number[if (firstMinus) 1 else 0] < '2'))  // 2 147 483 647
                return number.toInt()
            if (length < 19 || (length == 19 && number[if (firstMinus) 1 else 0] < '9'))  // 9 223 372 036 854 775 807
                return number.toLong()
            // CHECKSTYLE_ON: MagicNumber
            return JsonLazyNumber(number, false)
        } catch (e: NumberFormatException) {
            throw tokener.createParseException(e, "Malformed number: $number", true)
        }
    }

    companion object {
        /**
         * Parses a [JsonObject] from a source.
         *
         * <pre>
         * JsonObject json = [JsonParser].object().from("{\"a\":[true,false], \"b\":1}");
        </pre> *
         */
        @JvmStatic
        fun `object`(): JsonParserContext<JsonObject> {
            return JsonParserContext()
        }

        /**
         * Parses a [JsonArray] from a source.
         *
         * <pre>
         * JsonArray json = [JsonParser].array().from("[1, {\"a\":[true,false], \"b\":1}]");
        </pre> *
         */
        @JvmStatic
        fun array(): JsonParserContext<JsonArray> {
            return JsonParserContext()
        }

        /**
         * Parses any object from a source. For any valid JSON, returns either a null (for the JSON string 'null'), a
         * [String], a [Number], a [Boolean], a [JsonObject] or a [JsonArray].
         *
         * <pre>
         * Object json = [JsonParser].any().from("{\"a\":[true,false], \"b\":1}");
         * Number json = ([Number])[JsonParser].any().from("123.456e7");
        </pre> *
         */
        fun any(): JsonParserContext<Any> {
            return JsonParserContext()
        }
    }
}
