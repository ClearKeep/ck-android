package com.clearkeep.utilities

import android.app.ActivityManager
import android.content.Context
import android.util.Patterns
import com.clearkeep.BuildConfig
import java.text.SimpleDateFormat
import java.util.*
import android.net.NetworkCapabilities

import android.net.ConnectivityManager
import android.os.Build
import android.util.DisplayMetrics





fun getCurrentDateTime(): Date {
    return Calendar.getInstance().time
}

fun getTimeAsString(timeMs: Long) : String {
    val formatter = SimpleDateFormat("EEE HH:mm")

    return formatter.format(Date(timeMs))
}

fun printlnCK(str: String) {
    println("CKLog_$str")
}

fun getUnableErrorMessage(message: String?): String {
    return if (BuildConfig.DEBUG) {
        message ?: "unable to decrypt this message"
    } else {
        "unable to decrypt this message"
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