package com.clearkeep.screen.videojanus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.clearkeep.utilities.INCOMING_NOTIFICATION_ID

class DismissNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NotificationManagerCompat.from(context).cancel(null, INCOMING_NOTIFICATION_ID)
    }
}