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
 * Thrown when the [JsonParser] encounters malformed JSON.
 */
class JsonParserException internal constructor(
    e: Exception?, message: String?,
    /**
     * Gets the 1-based line position of the error.
     */
    val linePosition: Int,
    /**
     * Gets the 1-based character position of the error.
     */
    val charPosition: Int,
    /**
     * Gets the 0-based character offset of the error from the beginning of the string.
     */
    val charOffset: Int
) : Exception(message, e) {
    companion object {
        private const val serialVersionUID = 1L
    }
}
