package com.clearkeep.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import com.clearkeep.data.remote.service.MessageService
import com.clearkeep.data.remote.service.NoteService
import com.clearkeep.data.remote.service.SignalKeyDistributionService
import com.clearkeep.data.local.clearkeep.dao.GroupDAO
import com.clearkeep.data.local.clearkeep.dao.MessageDAO
import com.clearkeep.data.local.clearkeep.dao.NoteDAO
import com.clearkeep.data.local.signal.CKSignalProtocolAddress
import com.clearkeep.domain.model.Message
import com.clearkeep.domain.model.Note
import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.repository.GroupRepository
import com.clearkeep.domain.repository.MessageRepository
import com.clearkeep.domain.repository.ServerRepository
import com.clearkeep.data.local.signal.store.InMemorySenderKeyStore
import com.clearkeep.data.local.signal.store.InMemorySignalProtocolStore
import com.clearkeep.domain.model.ChatGroup
import com.clearkeep.presentation.screen.chat.contactsearch.MessageSearchResult
import com.clearkeep.presentation.screen.chat.utils.isGroup
import com.clearkeep.utilities.*
import com.google.protobuf.ByteString
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import message.MessageOuterClass
import note.NoteOuterClass
import org.whispersystems.libsignal.*
import org.whispersystems.libsignal.groups.GroupCipher
import org.whispersystems.libsignal.groups.GroupSessionBuilder
import org.whispersystems.libsignal.groups.SenderKeyName
import org.whispersystems.libsignal.groups.state.SenderKeyRecord
import org.whispersystems.libsignal.protocol.PreKeySignalMessage
import org.whispersystems.libsignal.protocol.SenderKeyDistributionMessage
import java.nio.charset.StandardCharsets
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val groupDAO: GroupDAO,
    private val messageDAO: MessageDAO,
    private val noteDAO: NoteDAO,
    private val senderKeyStore: InMemorySenderKeyStore,
    private val signalProtocolStore: InMemorySignalProtocolStore,
    private val serverRepository: ServerRepository,
    private val groupRepository: GroupRepository,
    private val signalKeyDistributionService: SignalKeyDistributionService,
    private val messageService: MessageService,
    private val noteService: NoteService,
): MessageRepository {
    override fun getMessagesAsState(groupId: Long, owner: Owner) =
        messageDAO.getMessagesAsState(groupId, owner.domain, owner.clientId)

    override suspend fun getUnreadMessage(
        groupId: Long,
        domain: String,
        ourClientId: String
    ): List<Message> {
        val group = groupDAO.getGroupById(groupId, domain, ourClientId)!!
        return messageDAO.getMessagesAfterTime(
            groupId,
            group.lastMessageSyncTimestamp,
            domain,
            ourClientId
        ).dropWhile { it.senderId == ourClientId }
    }

    private suspend fun insertMessage(message: Message) = messageDAO.insert(message)

    private suspend fun insertNote(note: Note) = noteDAO.insert(note)

    //Return true if there's no more message
    override suspend fun updateMessageFromAPI(groupId: Long, owner: Owner, lastMessageAt: Long, loadSize: Int): MessagePagingResponse = withContext(Dispatchers.IO) {
        try {
            val server = serverRepository.getServerByOwner(owner) ?: return@withContext MessagePagingResponse(
                isSuccess = false,
                endOfPaginationReached = true,
                newestMessageLoadedTimestamp = 0L
            )
            val responses = messageService.getMessage(server, groupId, loadSize, lastMessageAt)
            printlnCK("updateMessageFromAPI! lastMessageAt $lastMessageAt loadSize $loadSize")
            val listMessage = arrayListOf<Message>()
            responses.lstMessageList
                .sortedWith(compareBy(MessageOuterClass.MessageObjectResponse::getCreatedAt))
                .forEachIndexed { _, data ->
                    printlnCK("updateMessageFromAPI! timestamp ${data.createdAt}")
                    listMessage.add(parseMessageResponse(data, owner))
                }
            if (listMessage.isNotEmpty()) {
                messageDAO.insertMessages(listMessage)
                val lastMessage = listMessage.maxByOrNull { it.createdTime }
                val lastMessageOther = listMessage.minByOrNull { it.createdTime }

                if (lastMessage != null) {
                    updateLastSyncMessageTime(groupId, owner, lastMessage)
                }
                return@withContext MessagePagingResponse(
                    isSuccess = true,
                    endOfPaginationReached = false,
                    newestMessageLoadedTimestamp = lastMessageOther?.createdTime ?: 0L
                )
            }
            return@withContext MessagePagingResponse(
                isSuccess = true,
                endOfPaginationReached = true,
                newestMessageLoadedTimestamp = lastMessageAt
            )
        } catch (e: StatusRuntimeException) {
            val parsedError = parseError(e)

            val message = when (parsedError.code) {
                1000, 1077 -> {
                    printlnCK("inviteToGroupFromAPI token expired")
                    serverRepository.isLogout.postValue(true)
                    parsedError.message
                }
                else -> parsedError.message
            }

            return@withContext MessagePagingResponse(
                isSuccess = false,
                endOfPaginationReached = true,
                newestMessageLoadedTimestamp = lastMessageAt
            )
        } catch (exception: Exception) {
            printlnCK("fetchMessageFromAPI: $exception")
            return@withContext MessagePagingResponse(
                isSuccess = false,
                endOfPaginationReached = true,
                newestMessageLoadedTimestamp = lastMessageAt
            )
        }
    }

    override suspend fun updateNotesFromAPI(owner: Owner) = withContext(Dispatchers.IO) {
        try {
            val server = serverRepository.getServerByOwner(owner) ?: return@withContext
            val responses = noteService.getNotes(server)
            val notes = responses.userNotesList.map { parseNoteResponse(it, owner) }
            if (notes.isNotEmpty()) {
                noteDAO.insertNotes(notes)
            }
        } catch (e: StatusRuntimeException) {
            printlnCK("updateNotesFromAPI: $e")
        } catch (e: Exception) {
            printlnCK("updateNotesFromAPI: $e")
        }
    }

    private suspend fun updateLastSyncMessageTime(
        groupId: Long,
        owner: Owner,
        lastMessage: Message
    ) {
        printlnCK("updateLastSyncMessageTime, groupId = $groupId")
        val group = groupDAO.getGroupById(groupId, owner.domain, owner.clientId)!!
        printlnCK("updateLastSyncMessageTime group $group")
        val updateGroup = ChatGroup(
            generateId = group.generateId,
            groupId = group.groupId,
            groupName = group.groupName,
            groupAvatar = group.groupAvatar,
            groupType = group.groupType,
            createBy = group.createBy,
            createdAt = group.createdAt,
            updateBy = group.updateBy,
            updateAt = group.updateAt,
            rtcToken = group.rtcToken,
            clientList = group.clientList,
            isJoined = group.isJoined,
            ownerDomain = group.ownerDomain,
            ownerClientId = group.ownerClientId,
            lastMessage = lastMessage,
            lastMessageAt = lastMessage.createdTime,
            // update
            lastMessageSyncTimestamp = lastMessage.createdTime,
            isDeletedUserPeer = group.isDeletedUserPeer
        )
        groupDAO.updateGroup(updateGroup)
    }

    private suspend fun parseMessageResponse(
        response: MessageOuterClass.MessageObjectResponse,
        owner: Owner
    ): Message {
        val oldMessage = messageDAO.getMessage(response.id, response.groupId)
        if (oldMessage != null) {
            return oldMessage
        }
        if (owner.clientId == response.fromClientId) {
            return decryptMessage(
                response.id, response.groupId, response.groupType,
                response.fromClientId, response.fromClientWorkspaceDomain,
                response.createdAt, response.updatedAt,
                response.senderMessage,
                owner
            )
        }
        return decryptMessage(
            response.id, response.groupId, response.groupType,
            response.fromClientId, response.fromClientWorkspaceDomain,
            response.createdAt, response.updatedAt,
            response.message,
            owner
        )
    }

    private suspend fun parseNoteResponse(
        response: NoteOuterClass.UserNoteResponse,
        owner: Owner
    ): Note {
        val oldNote = noteDAO.getNote(response.createdAt)
        if (oldNote != null) {
            return oldNote
        }
        return Note(
            null,
            response.content.toString(Charsets.UTF_8),
            response.createdAt,
            owner.domain,
            owner.clientId
        )
    }

    override suspend fun decryptMessage(
        messageId: String,
        groupId: Long,
        groupType: String,
        fromClientId: String,
        fromDomain: String,
        createdTime: Long,
        updatedTime: Long,
        encryptedMessage: ByteString,
        owner: Owner,
    ): Message {
        var messageText: String
        try {
            val sender = Owner(fromDomain, fromClientId)
            messageText = if (!isGroup(groupType)) {
                //decryptPeerMessage(owner, encryptedMessage, signalProtocolStore)
                if (owner.clientId == sender.clientId) {
                    decryptPeerMessage(owner, encryptedMessage, signalProtocolStore)
                } else {
                    decryptPeerMessage(sender, encryptedMessage, signalProtocolStore)
                }
            } else {
                decryptGroupMessage(
                    sender,
                    groupId,
                    encryptedMessage,
                    senderKeyStore,
                    owner
                )
            }
        } catch (e: DuplicateMessageException) {
            printlnCK("decryptMessage, maybe this message decrypted, waiting 1,5s and check again")
            /**
             * To fix case: both load message and receive message from socket at the same time
             * Need wait 1.5s to load old message before save unableDecryptMessage
             */
            delay(1500)
            val oldMessage = messageDAO.getMessage(messageId, groupId)
            if (oldMessage != null) {
                printlnCK("decryptMessage, exactly old message: ${oldMessage.message}")
                return oldMessage
            } else {
                messageText = getUnableErrorMessage(e.message)
            }
        } catch (e: Exception) {
            printlnCK("decryptMessage error : $e")
            messageText = getUnableErrorMessage(e.message)
        }

        printlnCK("decryptMessage done: $messageText")
        return saveNewMessage(
            Message(
                messageId = messageId, groupId = groupId, groupType = groupType,
                senderId = fromClientId, receiverId = owner.clientId, message = messageText,
                createdTime = createdTime, updatedTime = updatedTime,
                ownerDomain = owner.domain, ownerClientId = owner.clientId
            ),
        )
    }

    override fun getMessageByText(
        ownerDomain: String,
        ownerClientId: String,
        query: String
    ): LiveData<List<MessageSearchResult>> {
        printlnCK("getMessageByText() ")
        return messageDAO.getMessageByText(ownerDomain, ownerClientId, "%$query%").asFlow().map {
            val groupsList = it.map { it.groupId }.distinct().map {
                groupDAO.getGroupById(it, ownerDomain, ownerClientId)
            }
            val clientList =
                groupsList.map { it?.clientList ?: emptyList() }.flatten().distinctBy { it.userId }
            val result = it.map { message ->
                MessageSearchResult(
                    message,
                    clientList.find { message.senderId == it.userId },
                    groupsList.find { message.groupId == it?.groupId })
            }
            printlnCK("====================== result get message")
            result
        }.asLiveData()
    }

    override suspend fun saveNewMessage(message: Message): Message {
        printlnCK("saveMessage: ${message.messageId}, ${message.message}")
        insertMessage(message)

        val groupId = message.groupId
        val room: ChatGroup? =
            groupRepository.getGroupByID(groupId, message.ownerDomain, message.ownerClientId).data

        printlnCK("saveNewMessage group $room")

        if (room != null) {
            // update last message in room
            val updateRoom = ChatGroup(
                generateId = room.generateId,
                groupId = room.groupId,
                groupName = room.groupName,
                groupAvatar = room.groupAvatar,
                groupType = room.groupType,
                createBy = room.createBy,
                createdAt = room.createdAt,
                updateBy = message.senderId,
                updateAt = getCurrentDateTime().time,
                rtcToken = room.rtcToken,
                clientList = room.clientList,

                // update
                isJoined = room.isJoined,
                ownerDomain = room.ownerDomain,
                ownerClientId = room.ownerClientId,

                lastMessage = message,
                lastMessageAt = message.createdTime,
                lastMessageSyncTimestamp = room.lastMessageSyncTimestamp,
                isDeletedUserPeer = room.isDeletedUserPeer
            )
            groupDAO.updateGroup(updateRoom)
        } else {
            printlnCK("can not find owner group ${message.groupId} for this message")
        }

        return message
    }

    override suspend fun clearTempMessage() {
        withContext(Dispatchers.IO) {
            messageDAO.deleteTempMessages()
        }
    }

    override suspend fun clearTempNotes() {
        withContext(Dispatchers.IO) {
            noteDAO.deleteTempNotes()
        }
    }

    override suspend fun saveNote(note: Note): Long {
        return insertNote(note)
    }

    override suspend fun saveMessage(message: Message): Int {
        return messageDAO.insert(message).toInt()
    }

    @Throws(java.lang.Exception::class, DuplicateMessageException::class)
    private suspend fun decryptPeerMessage(
        sender: Owner, message: ByteString,
        signalProtocolStore: InMemorySignalProtocolStore,
    ): String = withContext(Dispatchers.IO) {
        printlnCK("decryptPeerMessage!")
        if (message.isEmpty) {
            return@withContext ""
        }

        val signalProtocolAddress = CKSignalProtocolAddress(sender, RECEIVER_DEVICE_ID)
        val preKeyMessage = PreKeySignalMessage(message.toByteArray())

        val sessionCipher = SessionCipher(signalProtocolStore, signalProtocolAddress)
        val message = sessionCipher.decrypt(preKeyMessage)
        return@withContext String(message, StandardCharsets.UTF_8)
    }

    @Throws(java.lang.Exception::class, DuplicateMessageException::class)
    private suspend fun decryptGroupMessage(
        sender: Owner, groupId: Long, message: ByteString,
        senderKeyStore: InMemorySenderKeyStore,
        owner: Owner,
    ): String = withContext(Dispatchers.IO) {
        if (message.isEmpty) {
            return@withContext ""
        }

        val bobGroupCipher: GroupCipher
        val groupSender: SenderKeyName
        if (sender.clientId == owner.clientId) {
            val senderAddress = CKSignalProtocolAddress(sender, 111)
            groupSender = SenderKeyName(groupId.toString(), senderAddress)
            bobGroupCipher = GroupCipher(senderKeyStore, groupSender)
        } else {
            val senderAddress = CKSignalProtocolAddress(sender, RECEIVER_DEVICE_ID)
            groupSender = SenderKeyName(groupId.toString(), senderAddress)
            bobGroupCipher = GroupCipher(senderKeyStore, groupSender)
        }
        initSessionUserInGroup(
            groupId, sender.clientId, groupSender, senderKeyStore, false, owner
        )

        val plaintextFromAlice = try {
            bobGroupCipher.decrypt(message.toByteArray())
        } catch (messageEx: DuplicateMessageException) {
            throw messageEx
        } catch (ex: Exception) {
            printlnCK("decryptGroupMessage, $ex")
            val initSessionAgain = initSessionUserInGroup(
                groupId, sender.clientId, groupSender, senderKeyStore, true, owner
            )
            if (!initSessionAgain) {
                throw java.lang.Exception("can not init session in group $groupId")
            }
            bobGroupCipher.decrypt(message.toByteArray())
        }

        return@withContext String(plaintextFromAlice, StandardCharsets.UTF_8)
    }

    private suspend fun initSessionUserInGroup(
        groupId: Long, fromClientId: String,
        groupSender: SenderKeyName,
        senderKeyStore: InMemorySenderKeyStore, isForceProcess: Boolean,
        owner: Owner
    ): Boolean {
        val senderKeyRecord: SenderKeyRecord = senderKeyStore.loadSenderKey(groupSender)
        if (senderKeyRecord.isEmpty || isForceProcess) {
            try {
                val server = serverRepository.getServerByOwner(owner)
                if (server == null) {
                    printlnCK("initSessionUserInGroup: server must be not null")
                    return false
                }
                printlnCK("initSessionUserInGroup, process new session: group id = $groupId, server = ${server.serverDomain} $fromClientId")
                val senderKeyDistribution = signalKeyDistributionService.getGroupClientKey(server, groupId, fromClientId)
                printlnCK("")
                val receivedAliceDistributionMessage =
                    SenderKeyDistributionMessage(senderKeyDistribution.clientKey.clientKeyDistribution.toByteArray())
                val bobSessionBuilder = GroupSessionBuilder(senderKeyStore)
                bobSessionBuilder.process(groupSender, receivedAliceDistributionMessage)
            } catch (e: StatusRuntimeException) {
                val parsedError = parseError(e)
                return false
            } catch (e: java.lang.Exception) {
                printlnCK("initSessionUserInGroup:${fromClientId} ${e.message}")
                return false
            }
        }
        return true
    }

    override suspend fun deleteMessageInGroup(groupId: Long, ownerDomain: String, ownerClientId: String) {
        val index = messageDAO.deleteMessageFromGroupId(groupId, ownerDomain, ownerClientId)
        printlnCK("deleteMessageInGroup: $index")
    }

    override suspend fun deleteMessageByDomain(domain: String, userId: String) {
        messageDAO.deleteMessageByDomain(domain, userId)
    }
}

data class MessagePagingResponse(
    val isSuccess: Boolean,
    val endOfPaginationReached: Boolean,
    val newestMessageLoadedTimestamp: Long
)