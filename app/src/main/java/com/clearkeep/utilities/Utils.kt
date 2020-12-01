package com.clearkeep.utilities

import java.text.SimpleDateFormat
import java.util.*

fun getCurrentDateTime(): Date {
    return Calendar.getInstance().time
}

fun getTimeAsString(timeMs: Long) : String {
    val formatter = SimpleDateFormat("EEE HH:mm")

    return formatter.format(Date(timeMs * 1000))
}

fun printlnCK(str: String) {
    println("CKLog_$str")
}