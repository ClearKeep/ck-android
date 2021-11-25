package com.clearkeep.common.utilities

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.clearkeep.common.BuildConfig
import com.clearkeep.common.R
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

fun convertSecondsToHMmSs(seconds: Long): String {
    val s = seconds % 60
    val m = seconds / 60 % 60
    val h = seconds / (60 * 60) % 24
    if (h < 1) {
        return String.format("%02d:%02d", m, s)
    }
    return String.format("%02d:%02d:%02d", h, m, s)
}

@Composable
fun Int.toNonScalableTextSize(): TextUnit {
    return (this.toFloat() / 4).em
}

@Composable
fun Dp.toNonScalableTextSize(): TextUnit {
    return with(LocalDensity.current) {
        this@toNonScalableTextSize.toPx().em * 0.0856
    }
}

@Composable
fun defaultNonScalableTextSize(): TextUnit {
    return dimensionResource(R.dimen._14sdp).toNonScalableTextSize()
}

@Composable
fun Int.sdp(): Dp {
    return when (this) {
        0 -> 0.dp
        1 -> dimensionResource(R.dimen._1sdp)
        2 -> dimensionResource(R.dimen._2sdp)
        3 -> dimensionResource(R.dimen._3sdp)
        4 -> dimensionResource(R.dimen._4sdp)
        5 -> dimensionResource(R.dimen._5sdp)
        6 -> dimensionResource(R.dimen._6sdp)
        7 -> dimensionResource(R.dimen._7sdp)
        8 -> dimensionResource(R.dimen._8sdp)
        9 -> dimensionResource(R.dimen._9sdp)
        10 -> dimensionResource(R.dimen._10sdp)
        11 -> dimensionResource(R.dimen._11sdp)
        12 -> dimensionResource(R.dimen._12sdp)
        13 -> dimensionResource(R.dimen._13sdp)
        14 -> dimensionResource(R.dimen._14sdp)
        15 -> dimensionResource(R.dimen._15sdp)
        16 -> dimensionResource(R.dimen._16sdp)
        17 -> dimensionResource(R.dimen._17sdp)
        18 -> dimensionResource(R.dimen._18sdp)
        19 -> dimensionResource(R.dimen._19sdp)
        20 -> dimensionResource(R.dimen._20sdp)
        21 -> dimensionResource(R.dimen._21sdp)
        22 -> dimensionResource(R.dimen._22sdp)
        23 -> dimensionResource(R.dimen._23sdp)
        24 -> dimensionResource(R.dimen._24sdp)
        25 -> dimensionResource(R.dimen._25sdp)
        26 -> dimensionResource(R.dimen._26sdp)
        27 -> dimensionResource(R.dimen._27sdp)
        28 -> dimensionResource(R.dimen._28sdp)
        29 -> dimensionResource(R.dimen._29sdp)
        30 -> dimensionResource(R.dimen._30sdp)
        31 -> dimensionResource(R.dimen._31sdp)
        32 -> dimensionResource(R.dimen._32sdp)
        33 -> dimensionResource(R.dimen._33sdp)
        34 -> dimensionResource(R.dimen._34sdp)
        35 -> dimensionResource(R.dimen._35sdp)
        36 -> dimensionResource(R.dimen._36sdp)
        37 -> dimensionResource(R.dimen._37sdp)
        38 -> dimensionResource(R.dimen._38sdp)
        39 -> dimensionResource(R.dimen._39sdp)
        40 -> dimensionResource(R.dimen._40sdp)
        41 -> dimensionResource(R.dimen._41sdp)
        42 -> dimensionResource(R.dimen._42sdp)
        43 -> dimensionResource(R.dimen._43sdp)
        44 -> dimensionResource(R.dimen._44sdp)
        45 -> dimensionResource(R.dimen._45sdp)
        46 -> dimensionResource(R.dimen._46sdp)
        47 -> dimensionResource(R.dimen._47sdp)
        48 -> dimensionResource(R.dimen._48sdp)
        49 -> dimensionResource(R.dimen._49sdp)
        50 -> dimensionResource(R.dimen._50sdp)
        51 -> dimensionResource(R.dimen._51sdp)
        52 -> dimensionResource(R.dimen._52sdp)
        53 -> dimensionResource(R.dimen._53sdp)
        54 -> dimensionResource(R.dimen._54sdp)
        55 -> dimensionResource(R.dimen._55sdp)
        56 -> dimensionResource(R.dimen._56sdp)
        57 -> dimensionResource(R.dimen._57sdp)
        58 -> dimensionResource(R.dimen._58sdp)
        59 -> dimensionResource(R.dimen._59sdp)
        60 -> dimensionResource(R.dimen._60sdp)
        61 -> dimensionResource(R.dimen._61sdp)
        62 -> dimensionResource(R.dimen._62sdp)
        63 -> dimensionResource(R.dimen._63sdp)
        64 -> dimensionResource(R.dimen._64sdp)
        65 -> dimensionResource(R.dimen._65sdp)
        66 -> dimensionResource(R.dimen._66sdp)
        67 -> dimensionResource(R.dimen._67sdp)
        68 -> dimensionResource(R.dimen._68sdp)
        69 -> dimensionResource(R.dimen._69sdp)
        70 -> dimensionResource(R.dimen._70sdp)
        else -> this.dp
    }
}