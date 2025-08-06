package com.yushosei.newpipe.nanojson

internal class JsonObject private constructor(
    private val backing: LinkedHashMap<String, Any?>   // 실제 저장소
) : MutableMap<String, Any?> by backing {

    /** 빈 객체 */
    constructor() : this(LinkedHashMap())

    /** Map 복사 생성자 */
    constructor(map: Map<out String, *>?) : this(
        LinkedHashMap<String, Any?>().apply { if (map != null) putAll(map) }
    )

    /** 용량 지정 생성자 */
    constructor(initialCapacity: Int) : this(LinkedHashMap(initialCapacity))

    /** 용량+로드팩터 지정 생성자 */
    constructor(initialCapacity: Int, loadFactor: Float)
            : this(LinkedHashMap(initialCapacity, loadFactor))

    /* ---------- 편의 메서드 ---------- */

    fun getArray(key: String?): JsonArray =
        (backing[key] as? JsonArray) ?: JsonArray()

    fun getBoolean(key: String?, default: Boolean = false): Boolean =
        (backing[key] as? Boolean) ?: default

    fun getDouble(key: String?, default: Double = 0.0): Double =
        (backing[key] as? Number)?.toDouble() ?: default

    fun getFloat(key: String?, default: Float = 0f): Float =
        (backing[key] as? Number)?.toFloat() ?: default

    fun getInt(key: String?, default: Int = 0): Int =
        (backing[key] as? Number)?.toInt() ?: default

    fun getLong(key: String?, default: Long = 0): Long =
        (backing[key] as? Number)?.toLong() ?: default

    fun getNumber(key: String?): Number? =
        backing[key] as? Number

    fun getObject(key: String?): JsonObject =
        (backing[key] as? JsonObject) ?: JsonObject()

    fun getObject(key: String?, default_: JsonObject?): JsonObject? {
        val o = backing[key]
        if (o is JsonObject)
            return o
        return default_
    }

    fun getString(key: String?, default: String = ""): String =
        backing[key] as? String ?: default

    /*fun getString(key: String?, default: String): String =
        backing[key] as? String ?: default
*/
    /* ---------- 타입 체크 ---------- */

    fun has(key: String) = backing.containsKey(key)
    fun isBoolean(key: String?) = backing[key] is Boolean
    fun isNull(key: String) = backing.containsKey(key) && backing[key] == null
    fun isNumber(key: String?) = backing[key] is Number
    fun isString(key: String?) = backing[key] is String

    companion object {
        fun builder(): JsonBuilder<JsonObject> = JsonBuilder(JsonObject())
    }
}
