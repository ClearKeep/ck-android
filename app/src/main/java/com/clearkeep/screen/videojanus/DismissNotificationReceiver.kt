package com.clearkeep.screen.videojanus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.clearkeep.repo.VideoCallRepository
import com.clearkeep.screen.chat.utils.isGroup
import com.clearkeep.utilities.EXTRA_CALL_CANCEL_GROUP_ID
import com.clearkeep.utilities.EXTRA_CALL_CANCEL_GROUP_TYPE
import com.clearkeep.utilities.INCOMING_NOTIFICATION_ID
import com.clearkeep.utilities.printlnCK
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DismissNotificationReceiver : BroadcastReceiver() {
    @Inject
    lateinit var videoCallRepository: VideoCallRepository

    override fun onReceive(context: Context, intent: Intent) {
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
}