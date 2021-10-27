package com.clearkeep

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import dagger.hilt.android.HiltAndroidApp
import net.gotev.uploadservice.UploadServiceConfig

@HiltAndroidApp
open class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        UploadServiceConfig.initialize(
            context = this,
            defaultNotificationChannel = notificationChannelID,
            debug = BuildConfig.DEBUG
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                notificationChannelID,
                "TestApp Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        // ID of the notification channel used by upload service. This is needed by Android API 26+
        // but you have to always specify it even if targeting lower versions, because it's handled
        // by AndroidX AppCompat library automatically
        const val notificationChannelID = "TestChannel"
    }
}
