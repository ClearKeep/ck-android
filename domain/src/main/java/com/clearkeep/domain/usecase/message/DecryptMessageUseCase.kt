package com.clearkeep.domain.usecase.message

import android.util.Log
import com.clearkeep.common.utilities.*
import com.clearkeep.domain.model.CKSignalProtocolAddress
import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.repository.*
import com.google.protobuf.ByteString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.signal.libsignal.protocol.DuplicateMessageException
import org.signal.libsignal.protocol.SessionCipher
import org.signal.libsignal.protocol.groups.GroupCipher
import org.signal.libsignal.protocol.groups.GroupSessionBuilder
import org.signal.libsignal.protocol.groups.state.SenderKeyRecord
import org.signal.libsignal.protocol.message.PreKeySignalMessage
import org.signal.libsignal.protocol.message.SenderKeyDistributionMessage
import java.nio.charset.StandardCharsets
import javax.inject.Inject

class DecryptMessageUseCase @Inject constructor(
    private val messageRepository: MessageRepository,
    private val groupRepository: GroupRepository,
    private val senderKeyStore: SenderKeyStore,
    private val serverRepository: ServerRepository,
    private val signalKeyRepository: SignalKeyRepository,
    private val signalProtocolStore: SignalProtocolStore,
) {
    suspend operator fun invoke(
        messageId: String,
        groupId: Long,
        groupType: String,
        fromClientId: String,
        fromDomain: String,
        createdTime: Long,
        updatedTime: Long,
        encryptedMessage: ByteString,
        owner: Owner,
    ) = withContext(Dispatchers.IO) {
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
                return@withContext oldMessage
            } else {
                messageText = getUnableErrorMessage(e.message)
            }
        }
//        catch (e: Exception) {
//            printlnCK("decryptMessage error : $e")
//            messageText = getUnableErrorMessage(e.message)
//        }

        printlnCK("decryptMessage done: $messageText")
        return@withContext saveNewMessage(
            com.clearkeep.domain.model.Message(
                messageId = messageId, groupId = groupId, groupType = groupType,
                senderId = fromClientId, receiverId = owner.clientId, message = messageText,
                createdTime = createdTime, updatedTime = updatedTime,
                ownerDomain = owner.domain, ownerClientId = owner.clientId
            ),
        )
    }

    private suspend fun saveNewMessage(message: com.clearkeep.domain.model.Message): com.clearkeep.domain.model.Message {
        messageRepository.saveMessage(message)
        val server = serverRepository.getServer(message.ownerDomain, message.ownerClientId)

        val groupId = message.groupId
        val room: com.clearkeep.domain.model.ChatGroup? =
            groupRepository.getGroupByID(groupId, message.ownerDomain, message.ownerClientId, server, true).data

        printlnCK("saveNewMessage group $room")

        if (room != null) {
            // update last message in room
            val updateRoom = com.clearkeep.domain.model.ChatGroup(
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

    @Throws(java.lang.Exception::class, DuplicateMessageException::class)
    private suspend fun decryptGroupMessage(
        sender: Owner, groupId: Long, message: ByteString,
        owner: Owner,
    ): String = withContext(Dispatchers.IO) {
        if (message.isEmpty) {
            return@withContext ""
        }

        val bobGroupCipher: GroupCipher
        val senderAddress: CKSignalProtocolAddress
        if (sender.clientId == owner.clientId) {
            senderAddress = CKSignalProtocolAddress(sender, groupId, SENDER_DEVICE_ID)
            bobGroupCipher = GroupCipher(senderKeyStore, senderAddress)
        } else {
            senderAddress = CKSignalProtocolAddress(sender, groupId, RECEIVER_DEVICE_ID)
            bobGroupCipher = GroupCipher(senderKeyStore, senderAddress)
        }
        initSessionUserInGroup(
            groupId, sender.clientId, senderAddress, senderKeyStore, false, owner
        )

        val plaintextFromAlice = try {
            bobGroupCipher.decrypt(message.toByteArray())
        } catch (messageEx: DuplicateMessageException) {
            Log.d("antx: ", "DecryptMessageUseCase DuplicateMessageException line = 161: " );
            throw messageEx
        } catch (e: Exception) {
                        val initSessionAgain = initSessionUserInGroup(
               groupId, sender.clientId, senderAddress, senderKeyStore, true, owner
            )
            if (!initSessionAgain) {
                throw java.lang.Exception("can not init session in group $groupId")
            }
            bobGroupCipher.decrypt(message.toByteArray())

        }
        return@withContext String(plaintextFromAlice, StandardCharsets.UTF_8)
    }
    @Throws(java.lang.Exception::class, DuplicateMessageException::class)
    private suspend fun decryptPeerMessage(
        sender: Owner, message: ByteString,
    ): String = withContext(Dispatchers.IO) {
        if (message.isEmpty) {
            return@withContext ""
        }

        val signalProtocolAddress = CKSignalProtocolAddress(sender,null, RECEIVER_DEVICE_ID)
        Log.d("antx: ", "DecryptMessageUseCase decryptPeerMessage line = 185: $signalProtocolAddress" );
        val preKeyMessage = PreKeySignalMessage(message.toByteArray())
        val sessionCipher = SessionCipher(signalProtocolStore, signalProtocolAddress)
        val message = sessionCipher.decrypt(preKeyMessage)
        return@withContext String(message, StandardCharsets.UTF_8)
    }

    private suspend fun initSessionUserInGroup(
        groupId: Long, fromClientId: String,
        groupSender: CKSignalProtocolAddress,
        senderKeyStore: SenderKeyStore,
        isForceProcess: Boolean,
        owner: Owner
    ): Boolean {
        val senderKeyRecord: SenderKeyRecord? = signalKeyRepository.loadSenderKey(groupSender)
        if (senderKeyRecord == null || isForceProcess) {
            val server = serverRepository.getServerByOwner(owner)
            if (server == null) {
                printlnCK("initSessionUserInGroup: server must be not null")
                return false
            }
            val senderKeyDistribution =
                signalKeyRepository.getGroupClientKey(server, groupId, fromClientId)
            if (senderKeyDistribution != null) {
                printlnCK("process key")
                val receivedAliceDistributionMessage =
                    SenderKeyDistributionMessage(senderKeyDistribution.clientKey.clientKeyDistribution)
                val bobSessionBuilder = GroupSessionBuilder(senderKeyStore)
                bobSessionBuilder.process(groupSender, receivedAliceDistributionMessage)
            } else {
                return false
            }
        }
        return true
    }
}