package com.clearkeep.utilities

import android.app.ActivityManager
import android.content.Context
import android.util.Patterns
import com.clearkeep.BuildConfig
import java.text.SimpleDateFormat
import java.util.*

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