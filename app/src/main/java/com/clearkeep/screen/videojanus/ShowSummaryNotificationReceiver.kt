package com.clearkeep.screen.videojanus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.clearkeep.db.clearkeep.model.User
import com.clearkeep.db.clearkeep.model.UserPreference
import com.clearkeep.domain.repository.GroupRepository
import com.clearkeep.domain.repository.MessageRepository
import com.clearkeep.domain.repository.UserPreferenceRepository
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

    @Inject
    lateinit var userPreferenceRepository: UserPreferenceRepository

    override fun onReceive(context: Context, intent: Intent) {
        val groupId = intent.getLongExtra(EXTRA_GROUP_ID, 0)
        val ownerClientId = intent.getStringExtra(EXTRA_OWNER_CLIENT) ?: ""
        val ownerDomain = intent.getStringExtra(EXTRA_OWNER_DOMAIN) ?: ""
        if (ownerClientId.isNotBlank() && ownerDomain.isNotBlank() && groupId != 0L) {
            handleShowMessageSummary(context, groupId, ownerClientId, ownerDomain)
        }
    }

    private fun handleShowMessageSummary(
        context: Context,
        groupId: Long,
        ownerClientId: String,
        ownerDomain: String
    ) {
        GlobalScope.launch {
            val unreadMessages =
                messageRepository.getUnreadMessage(groupId, ownerDomain, ownerClientId)
            val group = groupRepository.getGroupByID(groupId, ownerDomain, ownerClientId)
            if (group?.data != null && unreadMessages.isNotEmpty()) {
                val me = group.data.clientList.find { it.userId == ownerClientId }
                    ?: User(userId = ownerClientId, userName = "me", domain = ownerDomain)
                val userPreference =
                    userPreferenceRepository.getUserPreference(ownerDomain, ownerClientId)
                showMessageNotificationToSystemBar(
                    context,
                    me,
                    group.data,
                    unreadMessages,
                    userPreference ?: UserPreference.getDefaultUserPreference("", "", false)
                )
            }
        }
    }
}