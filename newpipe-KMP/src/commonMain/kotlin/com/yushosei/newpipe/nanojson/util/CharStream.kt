package com.yushosei.newpipe.nanojson.util

internal interface CharStream {
    fun read(buf: CharArray, off: Int = 0, len: Int = buf.size - off): Int
    fun close() = Unit
}

internal class StringCharStream(private val src: String) : CharStream {
    private var pos = 0
    override fun read(buf: CharArray, off: Int, len: Int): Int {
        if (pos >= src.length) return -1           // EOF
        val n = minOf(len, src.length - pos)
        repeat(n) { i -> buf[off + i] = src[pos + i] }
        pos += n
        return n
    }
}