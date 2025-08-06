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

//@formatter:off

 /**
 * JSON writer that emits JSON to a [String].
 *
 * Create this class using [JsonWriter.string].
 *
 * <pre>
 * String json = JsonEmitter
 * .indent("  ")
 * .string()
 * .object()
 * .array("a")
 * .value(1)
 * .value(2)
 * .end()
 * .value("b", false)
 * .value("c", true)
 * .end()
 * .done();
</pre> *
 */
 //@formatter:on
 internal class JsonStringWriter internal constructor(indent: String?) :
     JsonWriterBase<JsonStringWriter>(StringBuilder(), indent) {
     /**
      * Completes this JSON writing session and returns the internal representation as a [String].
      */
     fun done(): String {
         super.doneInternal()
         return appendable.toString()
     }
 }
