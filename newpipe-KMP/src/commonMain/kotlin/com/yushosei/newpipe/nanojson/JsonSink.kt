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
 * Common interface for [JsonAppendableWriter], [JsonStringWriter] and [JsonBuilder].
 *
 * @param <SELF>
 * A subclass of [JsonSink].
</SELF> */
internal interface JsonSink<SELF : JsonSink<SELF>> {
    /**
     * Emits the start of an array.
     */
    fun array(c: Collection<*>): SELF

    /**
     * Emits the start of an array with a key.
     */
    fun array(key: String?, c: Collection<*>): SELF

    /**
     * Emits the start of an object.
     */
    fun `object`(map: Map<*, *>): SELF

    /**
     * Emits the start of an object with a key.
     */
    fun `object`(key: String?, map: Map<*, *>): SELF

    /**
     * Emits a 'null' token.
     */
    fun nul(): SELF

    /**
     * Emits a 'null' token with a key.
     */
    fun nul(key: String): SELF

    /**
     * Emits an object if it is a JSON-compatible type, otherwise throws an exception.
     */
    fun value(o: Any?): SELF

    /**
     * Emits an object with a key if it is a JSON-compatible type, otherwise throws an exception.
     */
    fun value(key: String, o: Any?): SELF

    /**
     * Emits a string value (or null).
     */
    fun value(s: String?): SELF

    /**
     * Emits an integer value.
     */
    fun value(i: Int): SELF

    /**
     * Emits a long value.
     */
    fun value(l: Long): SELF

    /**
     * Emits a boolean value.
     */
    fun value(b: Boolean): SELF

    /**
     * Emits a double value.
     */
    fun value(d: Double): SELF

    /**
     * Emits a float value.
     */
    fun value(f: Float): SELF

    /**
     * Emits a [Number] value.
     */
    fun value(n: Number?): SELF

    /**
     * Emits a string value (or null) with a key.
     */
    fun value(key: String, s: String?): SELF

    /**
     * Emits an integer value with a key.
     */
    fun value(key: String, i: Int): SELF

    /**
     * Emits a long value with a key.
     */
    fun value(key: String, l: Long): SELF

    /**
     * Emits a boolean value with a key.
     */
    fun value(key: String, b: Boolean): SELF

    /**
     * Emits a double value with a key.
     */
    fun value(key: String, d: Double): SELF

    /**
     * Emits a float value with a key.
     */
    fun value(key: String, f: Float): SELF

    /**
     * Emits a [Number] value with a key.
     */
    fun value(key: String, n: Number?): SELF

    /**
     * Starts an array.
     */
    fun array(): SELF

    /**
     * Starts an object.
     */
    fun `object`(): SELF

    /**
     * Starts an array within an object, prefixed with a key.
     */
    fun array(key: String): SELF

    /**
     * Starts an object within an object, prefixed with a key.
     */
    fun `object`(key: String): SELF

    /**
     * Ends the current array or object.
     */
    fun end(): SELF
}
