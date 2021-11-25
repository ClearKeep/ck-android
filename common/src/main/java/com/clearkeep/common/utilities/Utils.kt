package com.clearkeep.common.utilities

import com.clearkeep.common.BuildConfig
import com.clearkeep.common.utilities.network.ProtoResponse
import com.clearkeep.common.utilities.network.TokenExpiredException
import java.util.*

fun isGroup(groupType: String): Boolean {
    return groupType == "group"
}

fun printlnCK(str: String) {
    if (BuildConfig.DEBUG) {
        println("CKLog_$str")
    }
}

fun getCurrentDateTime(): Date {
    return Calendar.getInstance().time
}

fun getUnableErrorMessage(message: String?): String {
    return if (BuildConfig.FLAVOR == "dev") {
        message ?: ""
    } else {
        ""
    }
}

fun ByteArray.toHexString(): String {
    return this.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }
}

fun String.decodeHex(): ByteArray {
    check(length % 2 == 0) { "Must have an even length" }

    val byteIterator = chunkedSequence(2)
        .map { it.toInt(16).toByte() }
        .iterator()

    return ByteArray(length / 2) { byteIterator.next() }
}

fun getFileNameFromUrl(url: String): String {
    val fileNameRegex = "(?:.(?!\\/))+\$".toRegex()
    val fileName = fileNameRegex.find(url)?.value?.replace(fileSizeRegex, "")
    return fileName?.substring(1 until fileName.length) ?: ""
}

fun getGroupType(isGroup: Boolean): String {
    return if (isGroup) "group" else "peer"
}

val fileSizeRegex = "\\|.+".toRegex()