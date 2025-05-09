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


/**
 * Extends an [ArrayList] with helper methods to determine the underlying JSON type of the list element.
 */
class JsonArray(
    private val items: MutableList<Any?> = mutableListOf()
) : Iterable<Any?> by items {

    fun add(value: Any?): Boolean = items.add(value)

    operator fun get(index: Int): Any? = items.getOrNull(index)

    fun getOrNull(index: Int): Any? = items.getOrNull(index)

    fun isEmpty(): Boolean = items.isEmpty()

    val indices: IntRange
        get() = items.indices

    fun size(): Int = items.size

    val size: Int
        get() = items.size

    fun has(index: Int): Boolean = index in items.indices
    fun isNull(index: Int): Boolean = index in items.indices && items[index] == null
    fun isBoolean(index: Int): Boolean = get(index) is Boolean
    fun isNumber(index: Int): Boolean = get(index) is Number
    fun isString(index: Int): Boolean = get(index) is String

    fun getBoolean(index: Int, default_: Boolean = false): Boolean =
        (get(index) as? Boolean) ?: default_

    fun getDouble(index: Int, default_: Double = 0.0): Double =
        (get(index) as? Number)?.toDouble() ?: default_

    fun getFloat(index: Int, default_: Float = 0f): Float =
        (get(index) as? Number)?.toFloat() ?: default_

    fun getInt(index: Int, default_: Int = 0): Int =
        (get(index) as? Number)?.toInt() ?: default_

    fun getLong(index: Int, default_: Long = 0L): Long =
        (get(index) as? Number)?.toLong() ?: default_

    fun getNumber(index: Int, default_: Number? = null): Number? =
        (get(index) as? Number) ?: default_

    fun getString(index: Int, default_: String? = null): String? =
        (get(index) as? String) ?: default_

    fun getObject(index: Int, default_: JsonObject = JsonObject()): JsonObject =
        (get(index) as? JsonObject) ?: default_

    fun getArray(index: Int, default_: JsonArray = JsonArray()): JsonArray =
        (get(index) as? JsonArray) ?: default_

    fun toList(): List<Any?> = items.toList()

    companion object {
        fun from(vararg contents: Any?): JsonArray {
            val array = JsonArray()
            contents.forEach { array.add(it) }
            return array
        }

        fun builder(): JsonBuilder<JsonArray> {
            return JsonBuilder(JsonArray())
        }
    }
}