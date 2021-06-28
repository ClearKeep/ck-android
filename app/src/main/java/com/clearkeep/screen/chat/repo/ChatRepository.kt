package com.clearkeep.screen.chat.repo

import com.clearkeep.screen.chat.signal_store.InMemorySenderKeyStore
import com.clearkeep.screen.chat.signal_store.InMemorySignalProtocolStore
import com.clearkeep.db.clear_keep.model.Owner
import com.clearkeep.db.signal_key.CKSignalProtocolAddress
import com.clearkeep.dynamicapi.DynamicAPIProvider
import com.clearkeep.screen.chat.utils.*
import com.clearkeep.utilities.*
import com.google.protobuf.ByteString
import kotlinx.coroutines.*
import message.MessageOuterClass
import org.whispersystems.libsignal.SessionCipher
import org.whispersystems.libsignal.groups.GroupCipher
import org.whispersystems.libsignal.groups.SenderKeyName
import org.whispersystems.libsignal.protocol.CiphertextMessage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    // network calls
    private val dynamicAPIProvider: DynamicAPIProvider,

    // data
    private val senderKeyStore: InMemorySenderKeyStore,
    private val signalProtocolStore: InMemorySignalProtocolStore,
    private val messageRepository: MessageRepository
) {
    val scope: CoroutineScope = CoroutineScope(Job() + Dispatchers.IO)

    private var roomId: Long = -1

    fun setJoiningRoomId(roomId: Long) {
        this.roomId = roomId
    }

    fun getJoiningRoomId() : Long {
        return roomId
    }

    suspend fun sendMessageInPeer(senderId: String, ownerWorkSpace: String, receiverId: String, receiverWorkspaceDomain: String, groupId: Long, plainMessage: String, isForceProcessKey: Boolean = false) : Boolean = withContext(Dispatchers.IO) {
        printlnCK("sendMessageInPeer: sender=$senderId + $ownerWorkSpace, receiver= $receiverId + $receiverWorkspaceDomain, groupId= $groupId")
        try {
            val signalProtocolAddress = CKSignalProtocolAddress(Owner(receiverWorkspaceDomain, receiverId), 111)

            if (isForceProcessKey || !signalProtocolStore.containsSession(signalProtocolAddress)) {
                val processSuccess = processPeerKey(receiverId, receiverWorkspaceDomain)
                if (!processSuccess) {
                    printlnCK("sendMessageInPeer, init session failed with message \"$plainMessage\"")
                    return@withContext false
                }
            }

            val sessionCipher = SessionCipher(signalProtocolStore, signalProtocolAddress)
            val message: CiphertextMessage =
                    sessionCipher.encrypt(plainMessage.toByteArray(charset("UTF-8")))

            val request = MessageOuterClass.PublishRequest.newBuilder()
                    .setClientId(receiverId)
                    .setFromClientId(senderId)
                    .setGroupId(groupId)
                    .setMessage(ByteString.copyFrom(message.serialize()))
                    .build()

            val response = dynamicAPIProvider.provideMessageBlockingStub().publish(request)
            messageRepository.saveNewMessage(messageRepository.convertMessageResponse(response, plainMessage, Owner(ownerWorkSpace, senderId)))

            printlnCK("send message success: $plainMessage")
        } catch (e: java.lang.Exception) {
            printlnCK("sendMessage: $e")
            return@withContext false
        }

        return@withContext true
    }

    suspend fun processPeerKey(receiverId: String, receiverWorkspaceDomain: String,): Boolean {
        val signalProtocolAddress = CKSignalProtocolAddress(Owner(receiverWorkspaceDomain, receiverId), 111)
        return messageRepository.initSessionUserPeer(
            signalProtocolAddress,
            dynamicAPIProvider.provideSignalKeyDistributionBlockingStub(),
            signalProtocolStore
        )
    }

    suspend fun sendMessageToGroup(senderId: String, ownerWorkSpace: String, groupId: Long, plainMessage: String) : Boolean = withContext(Dispatchers.IO) {
        printlnCK("sendMessageToGroup: sender $senderId to group $groupId, ownerWorkSpace = $ownerWorkSpace")
        try {
            val senderAddress = CKSignalProtocolAddress(Owner(ownerWorkSpace, senderId), 111)
            val groupSender  =  SenderKeyName(groupId.toString(), senderAddress)

            val aliceGroupCipher = GroupCipher(senderKeyStore, groupSender)
            val ciphertextFromAlice: ByteArray =
                    aliceGroupCipher.encrypt(plainMessage.toByteArray(charset("UTF-8")))

            val request = MessageOuterClass.PublishRequest.newBuilder()
                    .setGroupId(groupId)
                    .setFromClientId(senderAddress.owner.clientId)
                    .setMessage(ByteString.copyFrom(ciphertextFromAlice))
                    .build()
            val response = dynamicAPIProvider.provideMessageBlockingStub().publish(request)
            val message = messageRepository.convertMessageResponse(response, plainMessage, Owner(ownerWorkSpace, senderId))
            messageRepository.saveNewMessage(message)

            printlnCK("send message success: $plainMessage")
            return@withContext true
        } catch (e: Exception) {
            printlnCK("sendMessage: $e")
        }

        return@withContext false
    }
}