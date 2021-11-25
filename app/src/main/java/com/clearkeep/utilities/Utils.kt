package com.clearkeep.utilities

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.util.Patterns
import java.text.SimpleDateFormat
import java.util.*
import android.net.NetworkCapabilities
import android.net.ConnectivityManager
import android.os.Build
import android.text.format.DateFormat
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.core.content.ContextCompat
import com.clearkeep.R
import com.clearkeep.common.utilities.printlnCK
import com.clearkeep.features.splash.presentation.SplashActivity
import kotlin.system.exitProcess

fun restartToRoot(context: Context) {
    printlnCK("restartActivityToRoot")
    val intent = Intent(context, com.clearkeep.features.splash.presentation.SplashActivity::class.java)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    context.startActivity(intent)
    exitProcess(2)
}

fun getTimeAsString(timeMs: Long, includeTime: Boolean = false): String {
    val nowTime = Calendar.getInstance()

    val inputTime = Calendar.getInstance()
    inputTime.timeInMillis = timeMs

    val time = if (includeTime) " at ${
        SimpleDateFormat(
            "hh:mm aa",
            Locale.US
        ).format(inputTime.time)
    }" else ""

    return if (
        inputTime[Calendar.YEAR] == nowTime[Calendar.YEAR]
        && inputTime[Calendar.MONTH] == nowTime[Calendar.MONTH]
        && inputTime[Calendar.WEEK_OF_MONTH] == nowTime[Calendar.WEEK_OF_MONTH]
    ) {
        when {
            nowTime[Calendar.DATE] == inputTime[Calendar.DATE] -> {
                "Today$time"
            }
            nowTime[Calendar.DATE] - inputTime[Calendar.DATE] == 1 -> {
                "Yesterday$time"
            }
            else -> {
                DateFormat.format("EEE", inputTime).toString()
            }
        }
    } else {
        DateFormat.format("yyyy/MM/dd", inputTime).toString()
    }
}

fun getHourTimeAsString(timeMs: Long): String {
    val formatter = SimpleDateFormat("HH:mm")

    return formatter.format(Date(timeMs))
}

fun getDateAsString(timeMs: Long): String {
    val formatter = SimpleDateFormat("dd/MM")

    return formatter.format(Date(timeMs))
}

fun CharSequence?.isValidEmail() =
    !isNullOrEmpty() && Patterns.EMAIL_ADDRESS.matcher(this).matches()

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
            val capabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
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

fun isOdd(num: Int): Boolean {
    return num % 2 != 0
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