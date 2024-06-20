package com.clearkeep

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.clearkeep.common.utilities.FILE_UPLOAD_CHANNEL_ID
import com.clearkeep.data.services.ChatService
import dagger.hilt.android.HiltAndroidApp
import net.gotev.uploadservice.UploadService
import net.gotev.uploadservice.UploadServiceConfig

@HiltAndroidApp
open class MyApplication : Application(), LifecycleObserver {
    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        UploadServiceConfig.initialize(
            context = this,
            defaultNotificationChannel = FILE_UPLOAD_CHANNEL_ID,
            debug = BuildConfig.DEBUG
        )

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                FILE_UPLOAD_CHANNEL_ID,
                getString(R.string.app_name),
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onApplicationClosed() {
        UploadService.stopAllUploads()
        ChatService.isBackGround = true
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onApplicationStart() {
        ChatService.isBackGround = false
    }
}
