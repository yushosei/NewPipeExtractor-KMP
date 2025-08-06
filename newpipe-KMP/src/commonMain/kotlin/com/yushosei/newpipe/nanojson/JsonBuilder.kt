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
 * Builds a [JsonObject] or [JsonArray].
 *
 * @param <T>
 * The type of JSON object to build.
</T> */
internal class JsonBuilder<T>(private val root: T) : JsonSink<JsonBuilder<T>> {
    private val json = ArrayDeque<Any>()

    init {
        json.addLast(root as Any)
    }

    fun done(): T = root

    override fun array(c: Collection<*>): JsonBuilder<T> = value(c)
    override fun array(key: String?, c: Collection<*>): JsonBuilder<T> = value(key!!, c)
    override fun `object`(map: Map<*, *>): JsonBuilder<T> = value(map)
    override fun `object`(key: String?, map: Map<*, *>): JsonBuilder<T> = value(key!!, map)
    override fun nul(): JsonBuilder<T> = value(null as Any?)
    override fun nul(key: String): JsonBuilder<T> = value(key, null as Any?)

    override fun value(o: Any?): JsonBuilder<T> {
        arr().add(o)
        return this
    }

    override fun value(key: String, o: Any?): JsonBuilder<T> {
        obj()[key] = o
        return this
    }

    override fun value(s: String?): JsonBuilder<T> = value(s as Any?)
    override fun value(i: Int): JsonBuilder<T> = value(i as Any)
    override fun value(l: Long): JsonBuilder<T> = value(l as Any)
    override fun value(b: Boolean): JsonBuilder<T> = value(b as Any)
    override fun value(d: Double): JsonBuilder<T> = value(d as Any)
    override fun value(f: Float): JsonBuilder<T> = value(f as Any)
    override fun value(n: Number?): JsonBuilder<T> = value(n as Any?)

    override fun value(key: String, s: String?): JsonBuilder<T> = value(key, s as Any?)
    override fun value(key: String, i: Int): JsonBuilder<T> = value(key, i as Any)
    override fun value(key: String, l: Long): JsonBuilder<T> = value(key, l as Any)
    override fun value(key: String, b: Boolean): JsonBuilder<T> = value(key, b as Any)
    override fun value(key: String, d: Double): JsonBuilder<T> = value(key, d as Any)
    override fun value(key: String, f: Float): JsonBuilder<T> = value(key, f as Any)
    override fun value(key: String, n: Number?): JsonBuilder<T> = value(key, n as Any?)

    override fun array(): JsonBuilder<T> {
        val a = JsonArray()
        value(a)
        json.addLast(a)
        return this
    }

    override fun `object`(): JsonBuilder<T> {
        val o = JsonObject()
        value(o)
        json.addLast(o)
        return this
    }

    override fun array(key: String): JsonBuilder<T> {
        val a = JsonArray()
        value(key, a)
        json.addLast(a)
        return this
    }

    override fun `object`(key: String): JsonBuilder<T> {
        val o = JsonObject()
        value(key, o)
        json.addLast(o)
        return this
    }

    override fun end(): JsonBuilder<T> {
        if (json.size <= 1) throw JsonWriterException("Cannot end the root object or array")
        json.removeLast()
        return this
    }

    private fun obj(): JsonObject {
        return json.lastOrNull() as? JsonObject
            ?: throw JsonWriterException("Attempted to write a keyed value to a JsonArray")
    }

    private fun arr(): JsonArray {
        return json.lastOrNull() as? JsonArray
            ?: throw JsonWriterException("Attempted to write a non-keyed value to a JsonObject")
    }
}
