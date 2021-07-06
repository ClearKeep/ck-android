package com.clearkeep.screen.videojanus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.clearkeep.db.clear_keep.model.User
import com.clearkeep.screen.chat.repo.GroupRepository
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
    lateinit var groupRepository: GroupRepository

    override fun onReceive(context: Context, intent: Intent) {
        val groupId = intent.getLongExtra(EXTRA_GROUP_ID, 0)
        val ownerClientId = intent.getStringExtra(EXTRA_OWNER_CLIENT) ?: ""
        val ownerDomain = intent.getStringExtra(EXTRA_OWNER_DOMAIN) ?: ""
        if (ownerClientId.isNotBlank() && ownerDomain.isNotBlank() && groupId != 0L) {
            handleShowMessageSummary(context, groupId, ownerClientId, ownerDomain)
        }
    }

    private fun handleShowMessageSummary(context: Context, groupId: Long, ownerClientId: String, ownerDomain: String) {
        GlobalScope.launch {
            val unreadMessages = messageRepository.getUnreadMessage(groupId, ownerDomain, ownerClientId)
            val group = groupRepository.getGroupByID(groupId, ownerDomain, ownerClientId)
            if (group != null && unreadMessages.isNotEmpty()) {
                val me = group.clientList.find { it.id == ownerClientId } ?: User(ownerClientId, "me", ownerDomain)
                showMessageNotificationToSystemBar(context, me, group, unreadMessages)
            }
        }
    }
}