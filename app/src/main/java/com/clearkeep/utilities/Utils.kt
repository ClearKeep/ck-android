package com.clearkeep.utilities

import java.util.*

fun getCurrentDateTime(): Date {
    return Calendar.getInstance().time
}

fun printlnCK(str: String) {
    println("CKLog_$str")
}