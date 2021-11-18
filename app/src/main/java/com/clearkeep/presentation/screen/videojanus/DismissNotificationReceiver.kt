package com.clearkeep.presentation.screen.videojanus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.usecase.call.CancelCallUseCase
import com.clearkeep.presentation.screen.chat.utils.isGroup
import com.clearkeep.utilities.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DismissNotificationReceiver : BroadcastReceiver() {
    @Inject
    lateinit var cancelCallUseCase: CancelCallUseCase

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_CALL_CANCEL -> {
                val groupId = intent.getStringExtra(EXTRA_CALL_CANCEL_GROUP_ID)!!
                val domain = intent.getStringExtra(EXTRA_OWNER_DOMAIN)!!
                val clientId = intent.getStringExtra(EXTRA_OWNER_CLIENT)!!
                val groupType = intent.getStringExtra(EXTRA_CALL_CANCEL_GROUP_TYPE)!!
                NotificationManagerCompat.from(context).cancel(null, INCOMING_NOTIFICATION_ID)
                printlnCK("onReceive dismiss call notification, group id = $groupId")
                if (!isGroup(groupType) && domain.isNotBlank() && clientId.isNotBlank()) {
                    GlobalScope.launch {
                        cancelCallUseCase(groupId.toInt(), Owner(domain, clientId))
                    }
                }

            }
            ACTION_MESSAGE_CANCEL -> {
                printlnCK("onReceive action ACTION_MESSAGE_CANCEL ")
                val notificationId = intent.getIntExtra(MESSAGE_HEADS_UP_CANCEL_NOTIFICATION_ID, 0)
                NotificationManagerCompat.from(context).cancel(null, notificationId)
            }
        }
    }
}