package com.clearkeep.domain.repository

import android.content.Context
import com.clearkeep.domain.model.Note
import com.clearkeep.utilities.network.Resource

interface ChatRepository {
    fun setJoiningRoomId(roomId: Long)
    fun getJoiningRoomId(): Long
    suspend fun sendMessageInPeer(
        senderId: String,
        ownerWorkSpace: String,
        receiverId: String,
        receiverWorkspaceDomain: String,
        groupId: Long,
        plainMessage: String,
        isForceProcessKey: Boolean = false,
        cachedMessageId: Int = 0
    ): Resource<Nothing>

    suspend fun processPeerKey(
        receiverId: String,
        receiverWorkspaceDomain: String,
        senderId: String,
        ownerWorkSpace: String
    ): Boolean

    suspend fun sendMessageToGroup(
        senderId: String,
        ownerWorkSpace: String,
        groupId: Long,
        plainMessage: String,
        cachedMessageId: Int = 0
    ): Resource<Nothing>

    suspend fun sendNote(note: Note, cachedNoteId: Long = 0): Boolean
    suspend fun uploadFile(
        context: Context,
        mimeType: String,
        fileName: String,
        fileUri: String
    ): Resource<String>

    fun downloadFile(fileName: String, url: String)
}