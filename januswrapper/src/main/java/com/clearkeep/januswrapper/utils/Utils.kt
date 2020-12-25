package com.clearkeep.januswrapper.utils

import android.app.ActivityManager
import android.content.Context

object Utils {
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
}