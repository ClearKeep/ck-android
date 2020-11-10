package com.clearkeep.chat.repositories

import com.clearkeep.chat.signal_store.InMemorySenderKeyStore
import com.clearkeep.db.MessageDAO
import com.clearkeep.db.RoomDAO
import com.clearkeep.db.model.Message
import com.clearkeep.login.LoginRepository
import com.clearkeep.utilities.getCurrentDateTime
import com.clearkeep.utilities.printlnCK
import com.google.protobuf.ByteString
import io.grpc.stub.StreamObserver
import org.whispersystems.libsignal.SignalProtocolAddress
import org.whispersystems.libsignal.groups.GroupCipher
import org.whispersystems.libsignal.groups.GroupSessionBuilder
import org.whispersystems.libsignal.groups.SenderKeyName
import org.whispersystems.libsignal.groups.state.SenderKeyRecord
import org.whispersystems.libsignal.protocol.SenderKeyDistributionMessage
import signalc_group.GroupSenderKeyDistributionGrpc
import signalc_group.SignalcGroup
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupChatRepository @Inject constructor(
    private val client: GroupSenderKeyDistributionGrpc.GroupSenderKeyDistributionStub,
    private val clientBlocking: GroupSenderKeyDistributionGrpc.GroupSenderKeyDistributionBlockingStub,
    private val myStore: InMemorySenderKeyStore,
    private val loginRepository: LoginRepository,
    private val messageDAO: MessageDAO,
    private val roomDAO: RoomDAO
) {
    init {
        subscribe()
    }

    private fun subscribe() {
        val request = SignalcGroup.GroupSubscribeAndListenRequest.newBuilder()
                .setClientId(loginRepository.getGroupClientId())
                .build()

        client.subscribe(request, object : StreamObserver<SignalcGroup.BaseResponse> {
            override fun onNext(response: SignalcGroup.BaseResponse?) {
                printlnCK("subscribe onNext ${response?.message}")
            }

            override fun onError(t: Throwable?) {
            }

            override fun onCompleted() {
                printlnCK("subscribe onCompleted")
                listen()
            }
        })
    }

    private fun listen() {
        val request = SignalcGroup.GroupSubscribeAndListenRequest.newBuilder()
            .setClientId(loginRepository.getGroupClientId())
            .build()
        client.listen(request, object : StreamObserver<SignalcGroup.GroupPublication> {
            override fun onNext(value: SignalcGroup.GroupPublication) {
                printlnCK("Receive a message from : ${value.senderId}")
                decryptMessage(value)
            }

            override fun onError(t: Throwable?) {
                printlnCK("listen message occurs error: ${t.toString()}")
            }

            override fun onCompleted() {
            }
        })
    }

    private fun decryptMessage(value: SignalcGroup.GroupPublication) {
        try {
            val senderAddress = SignalProtocolAddress(value.senderId, 111)
            val groupSender = SenderKeyName(value.groupId, senderAddress)
            val bobGroupCipher = GroupCipher(myStore, groupSender)

            val initSession = initSession(value, groupSender)
            if (!initSession) {
                return
            }

            val plaintextFromAlice = bobGroupCipher.decrypt(value.message.toByteArray())
            val decryptMessage = String(value.message.toByteArray(), StandardCharsets.UTF_8)
            val result = String(plaintextFromAlice, StandardCharsets.UTF_8)
            val roomId = roomDAO.getRoomId(value.groupId) ?: 0
            printlnCK("before decrypt: $decryptMessage")
            printlnCK("after decrypt: $result, roomId = $roomId")
            messageDAO.insert(Message(
                    value.senderId, result,
                    decryptMessage,
                    roomId, getCurrentDateTime().time)
            )
        } catch (e: Exception) {
            printlnCK("decrypt error : $e")
        }
    }

    private fun initSession(value: SignalcGroup.GroupPublication, groupSender: SenderKeyName): Boolean {
        val senderKeyRecord: SenderKeyRecord = myStore.loadSenderKey(groupSender)
        if (senderKeyRecord.isEmpty) {
            try {
                val request = SignalcGroup.GroupGetSenderKeyRequest.newBuilder()
                        .setGroupId(value.groupId)
                        .setSenderId(value.senderId)
                        .build()
                val senderKeyDistribution = clientBlocking.getSenderKeyInGroup(request)
                val receivedAliceDistributionMessage = SenderKeyDistributionMessage(senderKeyDistribution.senderKey.senderKeyDistribution.toByteArray())
                val bobSessionBuilder = GroupSessionBuilder(myStore)
                bobSessionBuilder.process(groupSender, receivedAliceDistributionMessage)
            } catch (e: Exception) {
                printlnCK("initSession: $e")
            }
        }
        return true
    }

    fun getMyClientId() = loginRepository.getGroupClientId()

    fun getMessagesFromRoom(roomId: Int) = messageDAO.getMessages(roomId = roomId)

    fun sendMessage(roomId: Int, groupId: String, msg: String) : Boolean {
        try {
            val senderAddress = SignalProtocolAddress(loginRepository.getGroupClientId(), 111)
            val groupSender  =  SenderKeyName(groupId, senderAddress)
            val aliceGroupCipher = GroupCipher(myStore, groupSender)
            val ciphertextFromAlice: ByteArray =
                    aliceGroupCipher.encrypt(msg.toByteArray(charset("UTF-8")))
            val messageAfterEncrypted = String(ciphertextFromAlice, StandardCharsets.UTF_8)

            val request = SignalcGroup.GroupPublishRequest.newBuilder()
                    .setGroupId(groupId)
                    .setSenderId(loginRepository.getGroupClientId())
                    .setMessage(ByteString.copyFrom(ciphertextFromAlice))
                    .build()
            clientBlocking.publish(request)
            messageDAO.insert(Message(getMyClientId(), msg, messageAfterEncrypted, roomId, getCurrentDateTime().time))

            printlnCK("send message success: $msg, encrypted: $messageAfterEncrypted")
            return true
        } catch (e: Exception) {
            printlnCK("sendMessage: $e")
        }

        return false
    }
}