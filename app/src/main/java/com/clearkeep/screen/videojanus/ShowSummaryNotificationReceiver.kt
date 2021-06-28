package com.clearkeep.screen.videojanus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.clearkeep.repo.MultiServerRepository
import com.clearkeep.screen.chat.repo.MessageRepository
import com.clearkeep.utilities.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ShowSummaryNotificationReceiver : BroadcastReceiver() {

    @Inject
    lateinit var messageRepository: MessageRepository

    @Inject
    lateinit var groupRepository: MultiServerRepository

    override fun onReceive(context: Context, intent: Intent) {
        val groupId = intent.getLongExtra(EXTRA_GROUP_ID, 0)
        handleShowMessageSummary(context, groupId)
    }

    private fun handleShowMessageSummary(context: Context, groupId: Long) {
        GlobalScope.launch {
            /*val unreadMessages = messageRepository.getUnreadMessage(groupId, "", "")
            val group = groupRepository.getGroupByID(groupId = groupId)!!
            val me = People("1234", "TODO", "")
            showMessageNotificationToSystemBar(context, me, group, unreadMessages)*/
        }
    }
}