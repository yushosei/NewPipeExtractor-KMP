package com.yushosei.newpipe.ktx

fun Long.millisToDuration(): String {
    val totalSeconds = this / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    return "${minutes}:${seconds.toString().padStart(2, '0')}"
}

fun List<String?>.interpunctize(interpunct: String = " ꞏ ") = joinToString(interpunct)