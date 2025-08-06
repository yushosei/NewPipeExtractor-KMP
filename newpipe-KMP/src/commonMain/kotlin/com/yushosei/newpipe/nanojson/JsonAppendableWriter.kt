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

internal class JsonAppendableWriter : JsonWriterBase<JsonAppendableWriter>,
    JsonSink<JsonAppendableWriter> {
    internal constructor(appendable: Appendable?, indent: String?) : super(appendable, indent)

    /*internal constructor(out: OutputStream?, indent: String?) : super(out, indent)*/

    /**
     * Closes this JSON writer and flushes the underlying [Appendable] if
     * it is also [Flushable].
     *
     * @throws JsonWriterException
     * if the underlying [Flushable] [Appendable] failed
     * to flush.
     */
    @Throws(JsonWriterException::class)
    fun done() {
        super.doneInternal()
        try {

        } catch (e: IOException) {
            throw JsonWriterException(e)
        }
    }
}
