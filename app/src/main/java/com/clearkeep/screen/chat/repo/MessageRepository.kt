package com.clearkeep.screen.chat.repo

import android.text.TextUtils
import com.clearkeep.db.clear_keep.dao.GroupDAO
import com.clearkeep.db.clear_keep.dao.MessageDAO
import com.clearkeep.db.clear_keep.model.ChatGroup
import com.clearkeep.db.clear_keep.model.Message
import com.clearkeep.db.clear_keep.model.Owner
import com.clearkeep.db.signal_key.CKSignalProtocolAddress
import com.clearkeep.dynamicapi.ParamAPI
import com.clearkeep.dynamicapi.ParamAPIProvider
import com.clearkeep.repo.ServerRepository
import com.clearkeep.screen.chat.signal_store.InMemorySenderKeyStore
import com.clearkeep.screen.chat.signal_store.InMemorySignalProtocolStore
import com.clearkeep.screen.chat.utils.isGroup
import com.clearkeep.utilities.getCurrentDateTime
import com.clearkeep.utilities.getUnableErrorMessage
import com.clearkeep.utilities.printlnCK
import com.google.protobuf.ByteString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import message.MessageOuterClass
import org.whispersystems.libsignal.DuplicateMessageException
import org.whispersystems.libsignal.IdentityKey
import org.whispersystems.libsignal.SessionBuilder
import org.whispersystems.libsignal.SessionCipher
import org.whispersystems.libsignal.groups.GroupCipher
import org.whispersystems.libsignal.groups.GroupSessionBuilder
import org.whispersystems.libsignal.groups.SenderKeyName
import org.whispersystems.libsignal.groups.state.SenderKeyRecord
import org.whispersystems.libsignal.protocol.PreKeySignalMessage
import org.whispersystems.libsignal.protocol.SenderKeyDistributionMessage
import org.whispersystems.libsignal.state.PreKeyBundle
import org.whispersystems.libsignal.state.PreKeyRecord
import org.whispersystems.libsignal.state.SignedPreKeyRecord
import signal.Signal
import signal.SignalKeyDistributionGrpc
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepository @Inject constructor(
    // dao
    private val groupDAO: GroupDAO,
    private val messageDAO: MessageDAO,

    // network calls
    private val apiProvider: ParamAPIProvider,

    private val senderKeyStore: InMemorySenderKeyStore,
    private val signalProtocolStore: InMemorySignalProtocolStore,
    private val serverRepository: ServerRepository,
    private val groupRepository: GroupRepository,
) {
    fun getMessagesAsState(groupId: Long, owner: Owner) = messageDAO.getMessagesAsState(groupId, owner.domain, owner.clientId)

    suspend fun getMessages(groupId: Long, owner: Owner) = messageDAO.getMessages(groupId, owner.domain, owner.clientId)

    suspend fun getMessage(messageId: String) = messageDAO.getMessage(messageId)

    suspend fun getUnreadMessage(groupId: Long, domain: String, ourClientId: String) : List<Message> {
        val group = groupDAO.getGroupById(groupId, domain, ourClientId)!!
        return messageDAO.getMessagesAfterTime(groupId, group.lastMessageSyncTimestamp, domain, ourClientId).dropWhile { it.senderId ==  ourClientId}
    }

    private suspend fun insertMessage(message: Message) = messageDAO.insert(message)

    suspend fun updateMessageFromAPI(groupId: Long, owner: Owner, lastMessageAt: Long, offSet: Int = 0) = withContext(Dispatchers.IO) {
        try {
            val server = serverRepository.getServerByOwner(owner) ?: return@withContext
            val messageGrpc = apiProvider.provideMessageBlockingStub(ParamAPI(server.serverDomain, server.accessKey, server.hashKey))
            val request = MessageOuterClass.GetMessagesInGroupRequest.newBuilder()
                    .setGroupId(groupId)
                    .setOffSet(offSet)
                    .setLastMessageAt(lastMessageAt)
                    .build()
            val responses = messageGrpc.getMessagesInGroup(request)
            val messages = responses.lstMessageList.map { parseMessageResponse(it, owner) }
            if (messages.isNotEmpty()) {
                messageDAO.insertMessages(messages)
                val lastMessage = messages.maxByOrNull { it.createdTime }
                if (lastMessage != null) {
                    updateLastSyncMessageTime(groupId, owner, lastMessage)
                }
            }
        } catch (exception: Exception) {
            printlnCK("fetchMessageFromAPI: $exception")
        }
    }

    private suspend fun updateLastSyncMessageTime(groupId: Long, owner: Owner, lastMessage: Message) {
        printlnCK("updateLastSyncMessageTime, groupId = $groupId")
        val group = groupDAO.getGroupById(groupId, owner.domain, owner.clientId)!!
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
            lastMessageSyncTimestamp = lastMessage.createdTime
        )
        groupDAO.update(updateGroup)
    }

    private suspend fun parseMessageResponse(response: MessageOuterClass.MessageObjectResponse, owner: Owner,): Message {
        val oldMessage = messageDAO.getMessage(response.id)
        if (oldMessage != null) {
            return oldMessage
        }
        return decryptMessage(
            response.id, response.groupId, response.groupType,
            response.fromClientId, response.fromClientWorkspaceDomain,
            response.createdAt, response.updatedAt,
            response.message,
            owner
        )
    }

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
    ): Message {
        var messageText: String
        try {
            val sender = Owner(fromDomain, fromClientId)
            messageText = if (!isGroup(groupType)) {
                decryptPeerMessage(sender, encryptedMessage, signalProtocolStore)
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
            val oldMessage = messageDAO.getMessage(messageId)
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

    suspend fun saveNewMessage(message: Message) : Message {
        printlnCK("saveNewMessage: ${message.messageId}, ${message.message}")
        insertMessage(message)

        val groupId = message.groupId
        var room: ChatGroup? = groupRepository.getGroupByID(groupId, message.ownerDomain, message.ownerClientId)

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
                lastMessageSyncTimestamp = room.lastMessageSyncTimestamp
            )
            groupDAO.update(updateRoom)
        } else {
            printlnCK("can not find owner group ${message.groupId} for this message")
        }

        return message
    }

    fun convertMessageResponse(value: MessageOuterClass.MessageObjectResponse, decryptedMessage: String, owner: Owner): Message {
        return Message(
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

    @Throws(java.lang.Exception::class, DuplicateMessageException::class)
    private suspend fun decryptPeerMessage(
        sender: Owner, message: ByteString,
        signalProtocolStore: InMemorySignalProtocolStore,
    ): String = withContext(Dispatchers.IO) {
        if (message.isEmpty) {
            return@withContext ""
        }

        val signalProtocolAddress = CKSignalProtocolAddress(sender, 222)
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

        val senderAddress = CKSignalProtocolAddress(sender, 222)
        val groupSender = SenderKeyName(groupId.toString(), senderAddress)
        val bobGroupCipher = GroupCipher(senderKeyStore, groupSender)

        printlnCK("decryptGroupMessage: groupId=$groupId, fromClientId=${sender.clientId}")
        initSessionUserInGroup(
            groupId, sender.clientId, groupSender, senderKeyStore, false, owner
        )
        var plaintextFromAlice = try {
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

    suspend fun initSessionUserPeer(
        signalProtocolAddress: CKSignalProtocolAddress,
        clientBlocking: SignalKeyDistributionGrpc.SignalKeyDistributionBlockingStub,
        signalProtocolStore: InMemorySignalProtocolStore,
    ): Boolean = withContext(Dispatchers.IO) {
        val remoteClientId = signalProtocolAddress.owner.clientId
        printlnCK("initSessionUserPeer with $remoteClientId, domain = ${signalProtocolAddress.owner.domain}")
        if (TextUtils.isEmpty(remoteClientId)) {
            return@withContext false
        }
        try {
            val requestUser = Signal.PeerGetClientKeyRequest.newBuilder()
                .setClientId(remoteClientId)
                .setWorkspaceDomain(signalProtocolAddress.owner.domain)
                .build()
            val remoteKeyBundle = clientBlocking.peerGetClientKey(requestUser)

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
        } catch (e: java.lang.Exception) {
            printlnCK("initSessionUserPeer: $e")
        }

        return@withContext false
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
                printlnCK("initSessionUserInGroup, process new session: group id = $groupId, server = ${server.serverDomain}")
                val signalGrpc = apiProvider.provideSignalKeyDistributionBlockingStub(ParamAPI(server.serverDomain, server.accessKey, server.hashKey))
                val request = Signal.GroupGetClientKeyRequest.newBuilder()
                    .setGroupId(groupId)
                    .setClientId(fromClientId)
                    .build()
                val senderKeyDistribution = signalGrpc.groupGetClientKey(request)
                val receivedAliceDistributionMessage =
                    SenderKeyDistributionMessage(senderKeyDistribution.clientKey.clientKeyDistribution.toByteArray())
                val bobSessionBuilder = GroupSessionBuilder(senderKeyStore)
                bobSessionBuilder.process(groupSender, receivedAliceDistributionMessage)
            } catch (e: java.lang.Exception) {
                printlnCK("initSessionUserInGroup: $e")
                return false
            }
        }
        return true
    }
}