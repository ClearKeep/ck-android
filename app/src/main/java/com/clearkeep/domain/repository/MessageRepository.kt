package com.clearkeep.domain.repository

import androidx.lifecycle.LiveData
import com.clearkeep.data.repository.MessagePagingResponse
import com.clearkeep.db.clearkeep.model.Message
import com.clearkeep.db.clearkeep.model.Note
import com.clearkeep.db.clearkeep.model.Owner
import com.clearkeep.db.signalkey.CKSignalProtocolAddress
import com.clearkeep.screen.chat.contact_search.MessageSearchResult
import com.clearkeep.data.local.signal.InMemorySignalProtocolStore
import com.google.protobuf.ByteString
import message.MessageOuterClass

interface MessageRepository {
    fun getMessagesAsState(groupId: Long, owner: Owner): LiveData<List<Message>>
    fun getNotesAsState(owner: Owner): LiveData<List<Note>>
    suspend fun getUnreadMessage(
        groupId: Long,
        domain: String,
        ourClientId: String
    ): List<Message>
    suspend fun updateMessageFromAPI(groupId: Long, owner: Owner, lastMessageAt: Long = 0, loadSize: Int = 20): MessagePagingResponse
    suspend fun updateNotesFromAPI(owner: Owner)
    suspend fun decryptMessage(
        messageId: String,
        groupId: Long,
        groupType: String,
        fromClientId: String,
        fromDomain: String,
        createdTime: Long,
        updatedTime: Long,
        encryptedMessage: ByteString,
        owner: Owner,
    ): Message
    fun getMessageByText(
        ownerDomain: String,
        ownerClientId: String,
        query: String
    ): LiveData<List<MessageSearchResult>>
    suspend fun saveNewMessage(message: Message): Message
    suspend fun clearTempMessage()
    suspend fun clearTempNotes()
    suspend fun saveNote(note: Note): Long
    suspend fun updateMessage(message: Message)
    suspend fun updateNote(note: Note)
    suspend fun saveMessage(message: Message): Int
    fun convertMessageResponse(
        value: MessageOuterClass.MessageObjectResponse,
        decryptedMessage: String,
        owner: Owner
    ): Message
    suspend fun initSessionUserPeer(
        signalProtocolAddress: CKSignalProtocolAddress,
        signalProtocolStore: InMemorySignalProtocolStore,
        owner: Owner
    ): Boolean
    suspend fun deleteMessageInGroup(groupId: Long, ownerDomain: String, ownerClientId: String)
    suspend fun clearMessageByDomain(domain: String, userId: String)
}