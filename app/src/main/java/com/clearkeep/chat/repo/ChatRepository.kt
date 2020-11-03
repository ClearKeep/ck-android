package com.clearkeep.chat.repo

import android.text.TextUtils
import com.clearkeep.db.MessageDAO
import com.clearkeep.db.model.Message
import com.clearkeep.login.LoginRepository
import com.clearkeep.chat.signal_store.InMemorySignalProtocolStore
import com.google.protobuf.ByteString
import io.grpc.stub.StreamObserver
import org.whispersystems.libsignal.IdentityKey
import org.whispersystems.libsignal.SessionBuilder
import org.whispersystems.libsignal.SessionCipher
import org.whispersystems.libsignal.SignalProtocolAddress
import org.whispersystems.libsignal.protocol.CiphertextMessage
import org.whispersystems.libsignal.protocol.PreKeySignalMessage
import org.whispersystems.libsignal.state.PreKeyBundle
import org.whispersystems.libsignal.state.PreKeyRecord
import org.whispersystems.libsignal.state.SignedPreKeyRecord
import signalc.SignalKeyDistributionGrpc
import signalc.Signalc
import java.lang.Exception
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val client: SignalKeyDistributionGrpc.SignalKeyDistributionStub,
    private val clientBlocking: SignalKeyDistributionGrpc.SignalKeyDistributionBlockingStub,
    private val myStore: InMemorySignalProtocolStore,
    private val loginRepository: LoginRepository,
    private val messageDAO: MessageDAO
) {
    init {
        subscribe()
        listen()
    }

    private fun subscribe() {
        val request = Signalc.SubscribeAndListenRequest.newBuilder()
                .setClientId(loginRepository.getClientId())
                .build()

        client.subscribe(request, object : StreamObserver<Signalc.BaseResponse> {
            override fun onNext(response: Signalc.BaseResponse?) {
                println("onNext ${response?.message}")
            }

            override fun onError(t: Throwable?) {
            }

            override fun onCompleted() {
            }
        })
    }

    private fun listen() {
        val request = Signalc.SubscribeAndListenRequest.newBuilder()
            .setClientId(loginRepository.getClientId())
            .build()
        client.listen(request, object : StreamObserver<Signalc.Publication> {
            override fun onNext(value: Signalc.Publication) {
                println("Receive a message from : ${value.senderId}")
                val signalProtocolAddress = SignalProtocolAddress(value.senderId, 111)
                val preKeyMessage = PreKeySignalMessage(value.message.toByteArray())
                val sessionCipher = SessionCipher(myStore, signalProtocolAddress)
                val message = sessionCipher.decrypt(preKeyMessage)
                val result = String(message, StandardCharsets.UTF_8)
                messageDAO.insert(Message(value.senderId, result))
            }

            override fun onError(t: Throwable?) {
                println("listen message occurs error: ${t.toString()}")
            }

            override fun onCompleted() {
            }
        })
    }

    fun getMyName() = loginRepository.getClientId()

    fun getMessagesFromSender(senderId: String) = messageDAO.getMessages(senderId = senderId)

    fun sendMessage(receiver: String, msg: String) : Boolean {
        val signalProtocolAddress = SignalProtocolAddress(receiver, 111)

        if (!myStore.containsSession(signalProtocolAddress)) {
            val initSuccess = initSessionWithReceiver(receiver, signalProtocolAddress)
            if (!initSuccess) {
                return false
            }
        }

        val sessionCipher = SessionCipher(myStore, signalProtocolAddress)
        val message: CiphertextMessage =
            sessionCipher.encrypt(msg.toByteArray(charset("UTF-8")))

        val request = Signalc.PublishRequest.newBuilder()
            .setReceiveId(receiver)
            .setSenderId(loginRepository.getClientId())
            .setMessage(ByteString.copyFrom(message.serialize()))
            .build()

        try {
            clientBlocking.publish(request)
        } catch (e: Exception) {
            println("sendMessage: ${e.toString()}")
            return false
        }

        return true
    }

    private fun initSessionWithReceiver(receiver: String, signalProtocolAddress: SignalProtocolAddress) : Boolean {
        if (TextUtils.isEmpty(receiver)) {
            return false
        }
        try {
            val requestUser = Signalc.SignalKeysUserRequest.newBuilder()
                .setClientId(receiver)
                .build()
            val remoteKeyBundle = clientBlocking.getKeyBundleByUserId(requestUser)

            val preKey = PreKeyRecord(remoteKeyBundle.preKey.toByteArray())
            val signedPreKey = SignedPreKeyRecord(remoteKeyBundle.signedPreKey.toByteArray())
            val identityKeyPublic = IdentityKey(remoteKeyBundle.identityKeyPublic.toByteArray(), 0)

            val retrievedPreKey = PreKeyBundle(
                remoteKeyBundle.registrationId,
                remoteKeyBundle.deviceId,
                preKey.id,
                preKey.keyPair.publicKey,
                remoteKeyBundle.signedPreKeyId,
                signedPreKey.keyPair.publicKey,
                signedPreKey.signature,
                identityKeyPublic
            )

            val sessionBuilder = SessionBuilder(myStore, signalProtocolAddress)

            // Build a session with a PreKey retrieved from the server.
            sessionBuilder.process(retrievedPreKey)
            return true
        } catch (e: Exception) {
            println("initSessionWithReceiver: ${e.toString()}")
        }

        return false
    }
}