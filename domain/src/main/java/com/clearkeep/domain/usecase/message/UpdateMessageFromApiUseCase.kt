package com.clearkeep.domain.usecase.message

import android.util.Log
import com.clearkeep.common.utilities.*
import com.clearkeep.domain.model.*
import com.clearkeep.domain.model.MessagePagingResponse
import com.clearkeep.domain.model.response.MessageObjectResponse
import com.clearkeep.domain.repository.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.whispersystems.libsignal.DuplicateMessageException
import org.whispersystems.libsignal.SessionCipher
import org.whispersystems.libsignal.groups.GroupCipher
import org.whispersystems.libsignal.groups.GroupSessionBuilder
import org.whispersystems.libsignal.groups.SenderKeyName
import org.whispersystems.libsignal.groups.state.SenderKeyRecord
import org.whispersystems.libsignal.protocol.PreKeySignalMessage
import org.whispersystems.libsignal.protocol.SenderKeyDistributionMessage
import java.nio.charset.StandardCharsets
import javax.inject.Inject

class UpdateMessageFromApiUseCase @Inject constructor(
    private val messageRepository: MessageRepository,
    private val serverRepository: ServerRepository,
    private val groupRepository: GroupRepository,
    private val senderKeyStore: SenderKeyStore,
    private val signalProtocolStore: SignalProtocolStore,
    private val signalKeyRepository: SignalKeyRepository,
) {
    suspend operator fun invoke(
        groupId: Long,
        owner: Owner,
        lastMessageAt: Long = 0,
        loadSize: Int = 20
    ): MessagePagingResponse = withContext(Dispatchers.IO) {
        val server = serverRepository.getServerByOwner(owner) ?: return@withContext MessagePagingResponse(
            isSuccess = false,
            endOfPaginationReached = true,
            newestMessageLoadedTimestamp = 0L
        )
        val responses =
            messageRepository.updateMessageFromAPI(server, groupId, owner, lastMessageAt, loadSize)
        if (responses != null) {
            val listMessage = arrayListOf<Message>()
            responses.lstMessage
                .sortedWith(compareBy(MessageObjectResponse::createdAt))
                .forEachIndexed { _, data ->
                    listMessage.add(parseMessageResponse(data, owner))
                }
            if (listMessage.isNotEmpty()) {
                messageRepository.saveMessages(listMessage)
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
        }
        return@withContext MessagePagingResponse(
            isSuccess = false,
            endOfPaginationReached = true,
            newestMessageLoadedTimestamp = lastMessageAt
        )
    }

    private suspend fun parseMessageResponse(
        response: MessageObjectResponse,
        owner: Owner
    ): Message {
        val oldMessage = messageRepository.getGroupMessage(response.id, response.groupId)
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

    private suspend fun decryptMessage(
        messageId: String,
        groupId: Long,
        groupType: String,
        fromClientId: String,
        fromDomain: String,
        createdTime: Long,
        updatedTime: Long,
        encryptedMessage: ByteArray,
        owner: Owner,
    ): Message {
        var messageText: String
        try {
            val sender = Owner(fromDomain, fromClientId)
            messageText = if (!isGroup(groupType)) {
                if (owner.clientId == sender.clientId) {
                    decryptPeerMessage(owner, encryptedMessage)
                } else {
                    decryptPeerMessage(sender, encryptedMessage)
                }
            } else {
                Log.d("antx: ", "UpdateMessageFromApiUseCase decryptMessage line = 125:decryptGroupMessage " );
                decryptGroupMessage(
                    sender,
                    groupId,
                    encryptedMessage,
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
            val oldMessage = messageRepository.getGroupMessage(messageId, groupId)
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

    private suspend fun updateLastSyncMessageTime(
        groupId: Long,
        owner: Owner,
        lastMessage: Message
    ) {
        val server = serverRepository.getServer(owner.domain, owner.clientId)
        val group = groupRepository.getGroupByID(groupId, owner.domain, owner.clientId, server, false).data
        group?.let {
            val updateGroup = ChatGroup(
                generateId = it.generateId,
                groupId = it.groupId,
                groupName = it.groupName,
                groupAvatar = it.groupAvatar,
                groupType = it.groupType,
                createBy = it.createBy,
                createdAt = it.createdAt,
                updateBy = it.updateBy,
                updateAt = it.updateAt,
                rtcToken = it.rtcToken,
                clientList = it.clientList,
                isJoined = it.isJoined,
                ownerDomain = it.ownerDomain,
                ownerClientId = it.ownerClientId,
                lastMessage = lastMessage,
                lastMessageAt = lastMessage.createdTime,
                // update
                lastMessageSyncTimestamp = lastMessage.createdTime,
                isDeletedUserPeer = it.isDeletedUserPeer
            )
            groupRepository.updateGroup(updateGroup)
        }
    }

    @Throws(java.lang.Exception::class, DuplicateMessageException::class)
    private suspend fun decryptGroupMessage(
        sender: Owner, groupId: Long, message: ByteArray,
        owner: Owner,
    ): String = withContext(Dispatchers.IO) {
        if (message.isEmpty()) {
            return@withContext ""
        }

        val bobGroupCipher: GroupCipher
        val groupSender: SenderKeyName
        if (sender.clientId == owner.clientId) {
            val senderAddress = CKSignalProtocolAddress(sender, SENDER_DEVICE_ID)
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
            bobGroupCipher.decrypt(message)
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
            try {
                bobGroupCipher.decrypt(message)
            } catch (e: Exception) {
                "".toByteArray()
            }
        }

        return@withContext String(plaintextFromAlice, StandardCharsets.UTF_8)
    }

    @Throws(java.lang.Exception::class, DuplicateMessageException::class)
    private suspend fun decryptPeerMessage(
        sender: Owner, message: ByteArray,
    ): String = withContext(Dispatchers.IO) {
        if (message.isEmpty()) {
            return@withContext ""
        }

        val signalProtocolAddress = CKSignalProtocolAddress(sender, RECEIVER_DEVICE_ID)
        val preKeyMessage = PreKeySignalMessage(message)

        val sessionCipher = SessionCipher(signalProtocolStore, signalProtocolAddress)
        val message = sessionCipher.decrypt(preKeyMessage)
        return@withContext String(message, StandardCharsets.UTF_8)
    }

    private suspend fun saveNewMessage(message: Message): Message {
        messageRepository.saveMessage(message)

        val groupId = message.groupId
        val server = serverRepository.getServer(message.ownerDomain, message.ownerClientId)
        val room: ChatGroup? =
            groupRepository.getGroupByID(
                groupId,
                message.ownerDomain,
                message.ownerClientId,
                server,
                true
            ).data

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
            groupRepository.updateGroup(updateRoom)
        } else {
            printlnCK("can not find owner group ${message.groupId} for this message")
        }

        return message
    }

    private suspend fun initSessionUserInGroup(
        groupId: Long, fromClientId: String,
        groupSender: SenderKeyName,
        senderKeyStore: SenderKeyStore,
        isForceProcess: Boolean,
        owner: Owner
    ): Boolean {
        val senderKeyRecord: SenderKeyRecord = signalKeyRepository.loadSenderKey(groupSender)
        if (senderKeyRecord.isEmpty || isForceProcess) {
            val server = serverRepository.getServerByOwner(owner)
            if (server == null) {
                printlnCK("initSessionUserInGroup: server must be not null")
                return false
            }
            val senderKeyDistribution =
                signalKeyRepository.getGroupClientKey(server, groupId, fromClientId)
            printlnCK("initSessionUserInGroup, process new session: group id = $groupId, server = ${server.serverDomain} ${senderKeyDistribution != null}")

            if (senderKeyDistribution != null) {
                printlnCK("")
                val receivedAliceDistributionMessage =
                    SenderKeyDistributionMessage(senderKeyDistribution.clientKey.clientKeyDistribution)
                Log.d("antx: ", "UpdateMessageFromApiUseCase initSessionUserInGroup line = 327:groupId ${groupId} ${senderKeyDistribution.clientKey.clientKeyDistribution} " );
                val bobSessionBuilder = GroupSessionBuilder(senderKeyStore)
                bobSessionBuilder.process(groupSender, receivedAliceDistributionMessage)
            } else {
                return false
            }
        }
        return true
    }
}