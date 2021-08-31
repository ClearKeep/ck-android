package com.clearkeep.utilities

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
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
    val match = matcher.group()
    val port = "\\:\\d{1,5}".toRegex().find(match)?.value ?: ""
    val portNumber = port.replace(":", "").toIntOrNull()
    if (port.isNotBlank() && portNumber !in 1..65535) {
        return false
    }
    return !url.contains("http(s)?://".toRegex()) && match == url
}

val errorRegex = "\\{.+\\}".toRegex()