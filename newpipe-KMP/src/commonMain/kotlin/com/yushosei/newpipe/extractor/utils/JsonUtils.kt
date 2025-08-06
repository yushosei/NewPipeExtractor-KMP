package com.yushosei.newpipe.extractor.utils

import com.yushosei.newpipe.extractor.exceptions.ParsingException
import com.yushosei.newpipe.nanojson.JsonArray
import com.yushosei.newpipe.nanojson.JsonObject
import com.yushosei.newpipe.nanojson.JsonParser.Companion.array
import com.yushosei.newpipe.nanojson.JsonParser.Companion.`object`
import com.yushosei.newpipe.nanojson.JsonParserException

internal object JsonUtils {

    /* ───────────────────────────── 기본 유틸 ───────────────────────────── */

    /** path 예: `"a.b.c"` */
    
    fun getValue(obj: JsonObject?, path: String): Any {
        val keys = path.split('.')
        // 마지막 key를 제외한 중간 객체 탐색
        val parent = if (keys.size == 1) obj
        else resolveObject(obj, keys.dropLast(1))
            ?: throw ParsingException("Unable to get $path")

        return parent?.get(keys.last())
            ?: throw ParsingException("Unable to get $path")
    }

    /** reified + 안전 캐스트로 타입 검사(멀티플랫폼 안전) */
    
    inline fun <reified T> getAs(obj: JsonObject?, path: String): T {
        val value = getValue(obj, path)
        return value as? T
            ?: throw ParsingException(
                "Wrong data type at path '$path' - expected ${T::class.simpleName}, got ${value::class.simpleName}"
            )
    }

    /* ─────────────────────────── 공개 API ─────────────────────────── */

    fun getString(obj: JsonObject?, path: String): String = getAs(obj, path)
    fun getBoolean(obj: JsonObject?, path: String): Boolean = getAs(obj, path)
    fun getNumber(obj: JsonObject?, path: String): Number = getAs(obj, path)
    fun getObject(obj: JsonObject?, path: String): JsonObject = getAs(obj, path)
    fun getArray(obj: JsonObject?, path: String): JsonArray = getAs(obj, path)

    /** JsonArray 안 객체들에서 동일 path 값을 모아 리스트로 반환 */
    
    fun getValues(array: JsonArray, path: String): List<Any> =
        List(array.size) { idx -> getValue(array.getObject(idx), path) }

    fun getStringListFromJsonArray(array: JsonArray): List<String> =
        array.filterIsInstance<String>()

    /* ───────────────────────── JSON 파싱 ───────────────────────── */

    
    fun toJsonArray(raw: String?): JsonArray =
        try {
            array().from(raw)
        } catch (e: JsonParserException) {
            throw ParsingException("Could not parse JSON array", e)
        }

    
    fun toJsonObject(raw: String?): JsonObject =
        try {
            `object`().from(raw)
        } catch (e: JsonParserException) {
            throw ParsingException("Could not parse JSON object", e)
        }

    /* ──────────────────────── 내부 헬퍼 ──────────────────────── */

    /** 중첩 key 목록을 따라가며 JsonObject를 반환 */
    private fun resolveObject(obj: JsonObject?, keys: List<String>): JsonObject? {
        var cur: JsonObject? = obj
        for (key in keys) {
            cur = cur?.getObject(key) ?: return null
        }
        return cur
    }
}
