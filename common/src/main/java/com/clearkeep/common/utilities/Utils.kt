package com.clearkeep.common.utilities

import com.clearkeep.common.BuildConfig

fun isGroup(groupType: String): Boolean {
    return groupType == "group"
}

fun printlnCK(str: String) {
    if (BuildConfig.DEBUG) {
        println("CKLog_$str")
    }
}