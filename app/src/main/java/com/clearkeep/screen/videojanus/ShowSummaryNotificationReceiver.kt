package com.clearkeep.screen.videojanus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.clearkeep.db.clear_keep.model.People
import com.clearkeep.repo.GroupRepository
import com.clearkeep.repo.MessageRepository
import com.clearkeep.utilities.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ShowSummaryNotificationReceiver : BroadcastReceiver() {
    @Inject
    lateinit var userManager: UserManager

    @Inject
    lateinit var messageRepository: MessageRepository

    @Inject
    lateinit var groupRepository: GroupRepository

    override fun onReceive(context: Context, intent: Intent) {
        val groupId = intent.getLongExtra(EXTRA_GROUP_ID, 0)
        handleShowMessageSummary(context, groupId)
    }

    private fun handleShowMessageSummary(context: Context, groupId: Long) {
        GlobalScope.launch {
            val unreadMessages = messageRepository.getUnreadMessage(groupId, userManager.getClientId())
            val group = groupRepository.getGroupByID(groupId = groupId)!!
            val me = People(userManager.getUserName(), userManager.getClientId())
            showMessageNotificationToSystemBar(context, me, group, unreadMessages)
        }
    }
}