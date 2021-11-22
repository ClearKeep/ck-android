package com.clearkeep.domain.repository

import androidx.lifecycle.LiveData
import com.clearkeep.data.local.signal.CKSignalProtocolAddress
import com.clearkeep.data.repository.MessagePagingResponse
import com.clearkeep.domain.model.Message
import com.clearkeep.domain.model.Note
import com.clearkeep.domain.model.Owner
import com.clearkeep.data.local.signal.store.InMemorySignalProtocolStore
import com.clearkeep.domain.model.Server
import com.clearkeep.presentation.screen.chat.contactsearch.MessageSearchResult
import com.google.protobuf.ByteString
import message.MessageOuterClass

interface MessageRepository {
    fun getMessagesAsState(groupId: Long, owner: Owner): LiveData<List<Message>>
    suspend fun getUnreadMessage(
        groupId: Long,
        lastMessageSyncTimestamp: Long,
        domain: String,
        ourClientId: String
    ): List<Message>

    suspend fun updateMessageFromAPI(
        server: Server,
        groupId: Long,
        owner: Owner,
        lastMessageAt: Long = 0,
        loadSize: Int = 20
    ): MessageOuterClass.GetMessagesInGroupResponse?

    fun getMessageByText(
        ownerDomain: String,
        ownerClientId: String,
        query: String
    ): LiveData<List<Message>>

    suspend fun getGroupMessage(messageId: String, groupId: Long): Message?

    suspend fun clearTempMessage()
    suspend fun saveMessage(message: Message): Int
    suspend fun saveMessages(messages: List<Message>)
    suspend fun deleteMessageInGroup(groupId: Long, ownerDomain: String, ownerClientId: String)
    suspend fun deleteMessageByDomain(domain: String, userId: String)
}