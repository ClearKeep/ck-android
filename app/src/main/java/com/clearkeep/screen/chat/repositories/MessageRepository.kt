package com.clearkeep.screen.chat.repositories

import com.clearkeep.db.MessageDAO
import com.clearkeep.db.model.Message
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepository @Inject constructor(
        private val messageDAO: MessageDAO,
) {
    fun getMessages(groupId: String) = messageDAO.getMessages(groupId)

    fun insert(message: Message) = messageDAO.insert(message)
}