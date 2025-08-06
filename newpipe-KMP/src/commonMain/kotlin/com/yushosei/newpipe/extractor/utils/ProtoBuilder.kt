package com.yushosei.newpipe.extractor.utils

import io.ktor.http.encodeURLParameter
import io.ktor.util.encodeBase64

class ProtoBuilder {
    private val byteBuffer = mutableListOf<Byte>()

    fun toBytes(): ByteArray = byteBuffer.toByteArray()

    fun toUrlencodedBase64(): String {
        val b64 = this.toBytes().encodeBase64()
            .replace("+", "-")
            .replace("/", "_")
            .replace("=", "")  // URL-safe
        return b64.encodeURLParameter()
    }

    private fun writeVarint(value: Long) {
        var v = value
        if (v == 0L) {
            byteBuffer.add(0)
            return
        }

        while (v != 0L) {
            var b = (v and 0x7F).toInt()
            v = v ushr 7
            if (v != 0L) {
                b = b or 0x80
            }
            byteBuffer.add(b.toByte())
        }
    }

    private fun field(field: Int, wire: Byte) {
        val fbits = field.toLong() shl 3
        val wbits = wire.toLong() and 0x07
        writeVarint(fbits or wbits)
    }

    fun varint(field: Int, value: Long) {
        field(field, 0)
        writeVarint(value)
    }

    fun string(field: Int, string: String) {
        bytes(field, string.encodeToByteArray())
    }

    fun bytes(field: Int, bytes: ByteArray) {
        field(field, 2)
        writeVarint(bytes.size.toLong())
        byteBuffer.addAll(bytes.toList())
    }
}
