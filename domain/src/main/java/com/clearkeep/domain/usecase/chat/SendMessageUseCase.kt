package com.clearkeep.domain.usecase.chat

import android.text.TextUtils
import com.clearkeep.data.local.signal.CKSignalProtocolAddress
import com.clearkeep.data.local.signal.store.InMemorySenderKeyStore
import com.clearkeep.domain.model.Owner
import com.clearkeep.utilities.SENDER_DEVICE_ID
import com.clearkeep.common.utilities.network.Resource
import com.clearkeep.utilities.printlnCK
import org.whispersystems.libsignal.SessionCipher
import org.whispersystems.libsignal.protocol.CiphertextMessage
import com.clearkeep.data.local.signal.store.InMemorySignalProtocolStore
import com.clearkeep.data.remote.service.SignalKeyDistributionService
import com.clearkeep.domain.model.ChatGroup
import com.clearkeep.domain.model.Message
import com.clearkeep.domain.repository.*
import com.clearkeep.utilities.AppStorage
import com.clearkeep.utilities.getCurrentDateTime
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import message.MessageOuterClass
import org.whispersystems.libsignal.IdentityKey
import org.whispersystems.libsignal.SessionBuilder
import org.whispersystems.libsignal.groups.GroupCipher
import org.whispersystems.libsignal.groups.SenderKeyName
import org.whispersystems.libsignal.state.PreKeyBundle
import org.whispersystems.libsignal.state.PreKeyRecord
import org.whispersystems.libsignal.state.SignedPreKeyRecord
import java.lang.Exception
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val messageRepository: MessageRepository,
    private val serverRepository: ServerRepository,
    private val signalProtocolStore: InMemorySignalProtocolStore,
    private val senderKeyStore: InMemorySenderKeyStore,
    private val signalKeyDistributionService: SignalKeyDistributionService,
    private val groupRepository: GroupRepository,
    private val userManager: AppStorage
) {
    suspend fun toPeer(
        senderId: String,
        ownerWorkSpace: String,
        receiverId: String,
        receiverWorkspaceDomain: String,
        groupId: Long,
        plainMessage: String,
        isForceProcessKey: Boolean = false,
        cachedMessageId: Int = 0
    ): Resource<Nothing> {
        val server = serverRepository.getServerByOwner(
            com.clearkeep.domain.model.Owner(
                ownerWorkSpace,
                senderId
            )
        )
        if (server == null) {
            printlnCK("sendMessageInPeer: server must be not null")
            return Resource.error("", null)
        }

        val signalProtocolAddress =
            CKSignalProtocolAddress(
                com.clearkeep.domain.model.Owner(receiverWorkspaceDomain, receiverId),
                SENDER_DEVICE_ID
            )

        if (isForceProcessKey || !signalProtocolStore.containsSession(signalProtocolAddress)) {
            val processSuccess =
                processPeerKey(receiverId, receiverWorkspaceDomain, senderId, ownerWorkSpace)
            if (!processSuccess) {
                printlnCK("sendMessageInPeer, init session failed with message \"$plainMessage\"")
                return Resource.error("init session failed", null)
            }
        }
        val signalProtocolAddressPublishRequest =
            CKSignalProtocolAddress(com.clearkeep.domain.model.Owner(ownerWorkSpace, senderId), SENDER_DEVICE_ID)
        val sessionCipher = SessionCipher(signalProtocolStore, signalProtocolAddress)
        val message: CiphertextMessage =
            sessionCipher.encrypt(plainMessage.toByteArray(charset("UTF-8")))

        val sessionCipherSender =
            SessionCipher(signalProtocolStore, signalProtocolAddressPublishRequest)
        val messageSender: CiphertextMessage =
            sessionCipherSender.encrypt(plainMessage.toByteArray(charset("UTF-8")))

        val response = chatRepository.sendMessageInPeer(server, receiverId, userManager.getUniqueDeviceID(), groupId, message.serialize(), messageSender.serialize())

        if (response.isSuccess() && response.data != null) {
            val responseMessage = convertMessageResponse(
                response.data,
                plainMessage,
                com.clearkeep.domain.model.Owner(ownerWorkSpace, senderId)
            )
            if (cachedMessageId == 0) {
                saveNewMessage(responseMessage)
            } else {
                chatRepository.updateMessage(responseMessage.copy(generateId = cachedMessageId))
            }
            return Resource.success(null)
        }
        return Resource.error(response.message ?: "", null, response.errorCode, response.error)
    }

    suspend fun toGroup(
        senderId: String,
        ownerWorkSpace: String,
        groupId: Long,
        plainMessage: String,
        cachedMessageId: Int = 0
    ): Resource<Nothing> =
        withContext(Dispatchers.IO) {
            val senderAddress =
                CKSignalProtocolAddress(com.clearkeep.domain.model.Owner(ownerWorkSpace, senderId), SENDER_DEVICE_ID)
            val groupSender = SenderKeyName(groupId.toString(), senderAddress)
            printlnCK("sendMessageToGroup: senderAddress : $senderAddress  groupSender: $groupSender")
            val aliceGroupCipher = GroupCipher(senderKeyStore, groupSender)
            val ciphertextFromAlice: ByteArray =
                aliceGroupCipher.encrypt(plainMessage.toByteArray(charset("UTF-8")))

            val server = serverRepository.getServerByOwner(
                com.clearkeep.domain.model.Owner(
                    ownerWorkSpace,
                    senderId
                )
            )
            if (server == null) {
                printlnCK("sendMessageToGroup: server must be not null")
                return@withContext Resource.error("server must be not null", null)
            }

            val response = chatRepository.sendMessageToGroup(
                server,
                userManager.getUniqueDeviceID(),
                groupId,
                ciphertextFromAlice
            )

            if (response.isSuccess() && response.data != null) {
                val message = convertMessageResponse(
                    response.data,
                    plainMessage,
                    com.clearkeep.domain.model.Owner(ownerWorkSpace, senderId)
                )

                if (cachedMessageId == 0) {
                    saveNewMessage(message)
                } else {
                    chatRepository.updateMessage(message.copy(generateId = cachedMessageId))
                }
                return@withContext Resource.success(null)
            }
            return@withContext Resource.error(response.message ?: "", null, response.errorCode, response.error)
        }

    private fun convertMessageResponse(
        value: MessageOuterClass.MessageObjectResponse,
        decryptedMessage: String,
        owner: com.clearkeep.domain.model.Owner
    ): com.clearkeep.domain.model.Message {
        return com.clearkeep.domain.model.Message(
            messageId = value.id,
            groupId = value.groupId,
            groupType = value.groupType,
            senderId = value.fromClientId,
            receiverId = value.clientId,
            message = decryptedMessage,
            createdTime = value.createdAt,
            updatedTime = value.updatedAt,
            ownerDomain = owner.domain,
            ownerClientId = owner.clientId
        )
    }

    private suspend fun processPeerKey(
        receiverId: String,
        receiverWorkspaceDomain: String,
        senderId: String,
        ownerWorkSpace: String
    ): Boolean {
        val signalProtocolAddress =
            CKSignalProtocolAddress(
                com.clearkeep.domain.model.Owner(
                    receiverWorkspaceDomain,
                    receiverId
                ), SENDER_DEVICE_ID)
        val signalProtocolAddress2 =
            CKSignalProtocolAddress(com.clearkeep.domain.model.Owner(ownerWorkSpace, senderId), SENDER_DEVICE_ID)
        initSessionUserPeer(
            signalProtocolAddress2,
            signalProtocolStore,
            owner = com.clearkeep.domain.model.Owner(ownerWorkSpace, senderId)
        )
        return initSessionUserPeer(
            signalProtocolAddress,
            signalProtocolStore,
            owner = com.clearkeep.domain.model.Owner(ownerWorkSpace, senderId)
        )
    }

    private suspend fun initSessionUserPeer(
        signalProtocolAddress: CKSignalProtocolAddress,
        signalProtocolStore: InMemorySignalProtocolStore,
        owner: com.clearkeep.domain.model.Owner
    ): Boolean = withContext(Dispatchers.IO) {
        val remoteClientId = signalProtocolAddress.owner.clientId
        printlnCK("initSessionUserPeer with $remoteClientId, domain = ${signalProtocolAddress.owner.domain}")
        if (TextUtils.isEmpty(remoteClientId)) {
            return@withContext false
        }
        try {
            val server = serverRepository.getServerByOwner(owner)
            if (server == null) {
                printlnCK("initSessionUserPeer: server must be not null")
                return@withContext false
            }

            val remoteKeyBundle = signalKeyDistributionService.getPeerClientKey(server, remoteClientId, signalProtocolAddress.owner.domain)

            val preKey = PreKeyRecord(remoteKeyBundle.preKey.toByteArray())
            val signedPreKey = SignedPreKeyRecord(remoteKeyBundle.signedPreKey.toByteArray())
            val identityKeyPublic = IdentityKey(remoteKeyBundle.identityKeyPublic.toByteArray(), 0)

            val retrievedPreKey = PreKeyBundle(
                remoteKeyBundle.registrationId,
                signalProtocolAddress.deviceId,
                preKey.id,
                preKey.keyPair.publicKey,
                remoteKeyBundle.signedPreKeyId,
                signedPreKey.keyPair.publicKey,
                signedPreKey.signature,
                identityKeyPublic
            )

            val sessionBuilder = SessionBuilder(signalProtocolStore, signalProtocolAddress)

            // Build a session with a PreKey retrieved from the server.
            sessionBuilder.process(retrievedPreKey)
            printlnCK("initSessionUserPeer: success")
            return@withContext true
        } catch (e: StatusRuntimeException) {
            printlnCK("initSessionUserPeer: $e")
        } catch (e: Exception) {
            e.printStackTrace()
            printlnCK("initSessionUserPeer: $e")
        }

        return@withContext false
    }

    private suspend fun saveNewMessage(message: com.clearkeep.domain.model.Message): com.clearkeep.domain.model.Message {
        messageRepository.saveMessage(message)

        val groupId = message.groupId
        val server = serverRepository.getServer(message.ownerDomain, message.ownerClientId)
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
}