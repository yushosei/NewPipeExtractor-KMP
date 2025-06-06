package newpipe.nanojson.util

fun StringBuilder.appendCodePointCompat(codePoint: Int): StringBuilder {
    return if (isBmpCodePoint(codePoint)) {
        this.append(codePoint.toChar())
    } else if (isValidCodePoint(codePoint)) {
        this.append(highSurrogate(codePoint)).append(lowSurrogate(codePoint))
    } else {
        throw IllegalArgumentException("Invalid Unicode code point: $codePoint")
    }
}


/** U+0000 ~ U+FFFF */
fun isBmpCodePoint(codePoint: Int): Boolean = codePoint in 0x0000..0xFFFF

/** U+0000 ~ U+10FFFF, but excluding surrogates */
fun isValidCodePoint(codePoint: Int): Boolean = codePoint in 0x0000..0x10FFFF

fun highSurrogate(codePoint: Int): Char {
    val c = ((codePoint - 0x10000) shr 10) + 0xD800
    return c.toChar()
}

fun lowSurrogate(codePoint: Int): Char {
    val c = ((codePoint - 0x10000) and 0x3FF) + 0xDC00
    return c.toChar()
}
