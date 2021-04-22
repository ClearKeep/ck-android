package com.clearkeep.screen.videojanus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.clearkeep.repo.VideoCallRepository
import com.clearkeep.screen.chat.utils.isGroup
import com.clearkeep.utilities.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DismissNotificationReceiver : BroadcastReceiver() {
    @Inject
    lateinit var videoCallRepository: VideoCallRepository

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_CALL_CANCEL -> {
                val groupId = intent.getStringExtra(EXTRA_CALL_CANCEL_GROUP_ID)!!
                val groupType = intent.getStringExtra(EXTRA_CALL_CANCEL_GROUP_TYPE)!!
                NotificationManagerCompat.from(context).cancel(null, INCOMING_NOTIFICATION_ID)
                printlnCK("onReceive dismiss call notification, group id = $groupId")
                if (!isGroup(groupType)) {
                    GlobalScope.launch {
                        videoCallRepository.cancelCall(groupId.toInt())
                    }
                }

            }
            ACTION_MESSAGE_CANCEL -> {
                printlnCK("onReceive action ACTION_MESSAGE_CANCEL ")
                NotificationManagerCompat.from(context).cancel(null, MESSAGE_NOTIFICATION_ID)
            }
        }
    }
}