package com.clearkeep.utilities

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.util.Patterns
import com.clearkeep.BuildConfig
import java.text.SimpleDateFormat
import java.util.*
import android.net.NetworkCapabilities
import android.net.ConnectivityManager
import android.os.Build
import android.text.format.DateFormat
import android.view.View
import com.clearkeep.db.clear_keep.model.ProtoResponse
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.core.content.ContextCompat
import com.clearkeep.R
import com.clearkeep.screen.splash.SplashActivity
import com.google.gson.Gson
import io.grpc.StatusRuntimeException
import kotlin.system.exitProcess

fun restartToRoot(context: Context) {
    printlnCK("restartActivityToRoot")
    val intent = Intent(context, SplashActivity::class.java)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    context.startActivity(intent)
    exitProcess(2)
}

fun getCurrentDateTime(): Date {
    return Calendar.getInstance().time
}

/*fun getTimeAsString(timeMs: Long) : String {
    val formatter = SimpleDateFormat("EEE HH:mm")

    *//*return formatter.format(Date(timeMs))*//*
    return DateUtils.getRelativeTimeSpanString(timeMs).toString()
}*/

fun getTimeAsString(timeMs: Long): String {
    val nowTime = Calendar.getInstance()

    val inputTime = Calendar.getInstance()
    inputTime.timeInMillis = timeMs

    return if (
        inputTime[Calendar.YEAR] === nowTime[Calendar.YEAR]
        && inputTime[Calendar.MONTH] === nowTime[Calendar.MONTH]
        && inputTime[Calendar.WEEK_OF_MONTH] === nowTime[Calendar.WEEK_OF_MONTH]
    ) {
        when {
            nowTime[Calendar.DATE] === inputTime[Calendar.DATE] -> "Today"
            nowTime[Calendar.DATE] - inputTime[Calendar.DATE] === 1 -> "Yesterday"
            else -> {
                DateFormat.format("EEE", inputTime).toString()
            }
        }
    } else {
        DateFormat.format("yyyy/MM/dd", inputTime).toString()
    }
}

fun getHourTimeAsString(timeMs: Long) : String {
    val formatter = SimpleDateFormat("HH:mm")

    return formatter.format(Date(timeMs))
}

fun printlnCK(str: String) {
    println("CKLog_$str")
}

fun getUnableErrorMessage(message: String?): String {
    return if (BuildConfig.FLAVOR == "dev" || BuildConfig.FLAVOR == "staging") {
        message ?: ""
    } else {
        ""
    }
}

fun CharSequence?.isValidEmail() = !isNullOrEmpty() && Patterns.EMAIL_ADDRESS.matcher(this).matches()

fun isServiceRunning(context: Context, serviceName: String): Boolean {
    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    if (manager != null) {
        val services = manager.getRunningServices(Int.MAX_VALUE)
        for (service in services) {
            if (serviceName == service.service.className) {
                return true
            }
        }
    }
    return false
}

fun isOnline(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    if (connectivityManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
                if (capabilities != null) {
                    when {
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                            return true
                        }
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                            return true
                        }
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                            return true
                        }
                    }
                }
            } else {
                val activeNetworkInfo = connectivityManager.activeNetworkInfo
                if (activeNetworkInfo != null && activeNetworkInfo.isConnected) {
                    return true
                }
            }
    }
    return false
}

fun convertDpToPixel(dp: Float, context: Context): Float {
    return dp * (context.resources.displayMetrics.density)
}

fun convertPixelsToDp(px: Float, context: Context): Float {
    return px / (context.resources.displayMetrics.density)
}

fun dp2px(dpValue: Float): Int {
    val scale = Resources.getSystem().displayMetrics.density
    return (dpValue * scale + 0.5f).toInt()
}
fun View.gone() {
    if (visibility != View.GONE)
        visibility = View.GONE
}

fun View.visible() {
    if (visibility != View.VISIBLE)
        visibility = View.VISIBLE
}

fun View.invisible() {
    if (visibility != View.INVISIBLE)
        visibility = View.INVISIBLE
}



fun convertSecondsToHMmSs(seconds: Long): String {
    val s = seconds % 60
    val m = seconds / 60 % 60
    val h = seconds / (60 * 60) % 24
    if (h<1){
        return String.format("%02d:%02d",m, s)
    }
    return String.format("%02d:%02d:%02d", h, m, s)
}

fun isOdd(num: Int) : Boolean {
    return num % 2 != 0
}

fun parseError(e: StatusRuntimeException) : ProtoResponse {
    val rawError = errorRegex.find(e.message ?: "")?.value ?: ""
    return try {
        Gson().fromJson(rawError, ProtoResponse::class.java)
    } catch (e: Exception) {
        println("parseError exception rawError $rawError, exception ${e.message}")
        ProtoResponse(0, rawError)
    }
}

fun isValidServerUrl(url: String): Boolean {
    val matcher = Patterns.WEB_URL.matcher(url.trim())
    matcher.find()
    try {
        val match = matcher.group()
        val port = "\\:\\d{1,5}".toRegex().find(match)?.value ?: ""
        val portNumber = port.replace(":", "").toIntOrNull()
        if (port.isNotBlank() && portNumber !in 1..65535) {
            return false
        }
        return !url.contains("http(s)?://".toRegex()) && match == url
    } catch (e: Exception) {
        return false
    }
}

fun isPermissionGranted(context: Context, permission: String): Boolean {
    return when (PackageManager.PERMISSION_GRANTED) {
        ContextCompat.checkSelfPermission(context, permission) -> {
            true
        }
        else -> {
            false
        }
    }
}

fun isWriteFilePermissionGranted(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT <= Build.VERSION_CODES.P)
        return isPermissionGranted(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    return true
}

val errorRegex = "\\{.+\\}".toRegex()

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
fun Int.sdp() : Dp {
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