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
 * Factory for JSON writers that target [String]s and [Appendable]s.
 * 
 * Creates writers that write JSON to a [String], an [OutputStream], or an
 * [Appendable] such as a [StringBuilder], a [Writer] a [PrintStream] or a [CharBuffer].
 * 
 * <pre>
 * String json = JsonWriter
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
 internal object JsonWriter {
     //@formatter:off
 /**
 * Creates a [JsonWriter] source that will write indented output with the given indent.
 * 
 * <pre>
 * String json = JsonWriter.indent("  ").string()
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
 fun indent(indent: String): JsonWriterContext {
     requireNotNull(indent) { "indent must be non-null" }

     for (i in 0..<indent.length) {
         require(!(indent[i] != ' ' && indent[i] != '\t')) { "Only tabs and spaces are allowed for indent." }
     }

     return JsonWriterContext(indent)
 }

     //@formatter:off
 /**
 * Creates a new [JsonStringWriter].
 * 
 * <pre>
 * String json = JsonWriter.string()
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
 fun string(): JsonStringWriter {
     return JsonStringWriter(null)
 }

     /**
      * Emits a single value (a JSON primitive such as a [Number],
      * [Boolean], [String], a [Map] or [JsonObject], or
      * a [Collection] or [JsonArray].
      *
      * Emit a [String], JSON-escaped:
      *
      * <pre>
      * JsonWriter.string(&quot;abc\n\&quot;&quot;) // &quot;\&quot;abc\\n\\&quot;\&quot;&quot;
     </pre> *
      *
      * <pre>
      * JsonObject obj = new JsonObject();
      * obj.put("abc", 1);
      * JsonWriter.string(obj) // "{\"abc\":1}"
     </pre> *
      */
     fun string(value: Any?): String {
         return JsonStringWriter(null).value(value).done()
     }

     /**
      * Creates a [JsonAppendableWriter] that can output to an
      * [Appendable] subclass, such as a [StringBuilder], a
      * [Writer] a [PrintStream] or a [CharBuffer].
      */
     //@formatter:off
 /**
 * Creates a [JsonAppendableWriter] that can output to an [PrintStream] subclass.
 * 
 * <pre>
 * JsonWriter.on(System.out)
 * .object()
 * .value(&quot;a&quot;, 1)
 * .value(&quot;b&quot;, 2)
 * .end()
 * .done();
</pre> * 
 */
 //@formatter:on
     /*fun on(appendable: PrintStream?): JsonAppendableWriter {
         return JsonAppendableWriter(appendable as Appendable?, null)
     }*/

     //@formatter:off
 /**
 * Creates a [JsonAppendableWriter] that can output to an [OutputStream] subclass. Uses the UTF-8
 * [Charset]. To specify a different charset, use the [JsonWriter.on] method with an
 * [OutputStreamWriter].
 * 
 * <pre>
 * JsonWriter.on(System.out)
 * .object()
 * .value(&quot;a&quot;, 1)
 * .value(&quot;b&quot;, 2)
 * .end()
 * .done();
</pre> * 
 */
 //@formatter:on
     /*fun on(out: OutputStream?): JsonAppendableWriter {
         return JsonAppendableWriter(out, null)
     }*/

     /**
      * Escape a string value.
      *
      * @param value
      * @return the escaped JSON value
      */
     fun escape(value: String?): String {
         val s = string(value)
         return s.substring(1, s.length - 1)
     }

     /**
      * Allows for additional configuration of the [JsonWriter].
      */
     class JsonWriterContext internal constructor(private val indent: String) {
         //@formatter:off
 /**
 * Creates a new [JsonStringWriter].
 * 
 * <pre>
 * String json = JsonWriter.indent("  ").string()
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


         //@formatter:off
 /**
 * Creates a [JsonAppendableWriter] that can output to an [PrintStream] subclass.
 * 
 * <pre>
 * JsonWriter.indent("  ").on(System.out)
 * .object()
 * .value(&quot;a&quot;, 1)
 * .value(&quot;b&quot;, 2)
 * .end()
 * .done();
</pre> * 
 */
 //@formatter:on


         //@formatter:off
 /**
 * Creates a [JsonAppendableWriter] that can output to an [OutputStream] subclass. Uses the UTF-8
 * [Charset]. To specify a different charset, use the [JsonWriter.on] method with an
 * [OutputStreamWriter].
 * 
 * <pre>
 * JsonWriter.indent("  ").on(System.out)
 * .object()
 * .value(&quot;a&quot;, 1)
 * .value(&quot;b&quot;, 2)
 * .end()
 * .done();
</pre> * 
 */
 //@formatter:on

     }
 }
