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
 * Lazily-parsed number for performance.
 */

internal class JsonLazyNumber(
    private val value: String,
    private val isDouble: Boolean
) : Number() {

    override fun toDouble(): Double = value.toDouble()

    override fun toFloat(): Float = value.toFloat()

    override fun toInt(): Int = if (isDouble) value.toDouble().toInt() else value.toInt()

    override fun toLong(): Long = if (isDouble) value.toDouble().toLong() else value.toLong()

    override fun toByte(): Byte = toInt().toByte()

    override fun toShort(): Short = toInt().toShort()
}
