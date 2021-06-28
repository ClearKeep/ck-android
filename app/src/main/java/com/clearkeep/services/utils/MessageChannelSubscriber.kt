package com.clearkeep.services.utils

import com.clearkeep.utilities.printlnCK
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import message.MessageGrpc
import message.MessageOuterClass

class MessageChannelSubscriber(
    private val domain: String,
    private val clientId: String,
    private val messageBlockingStub: MessageGrpc.MessageBlockingStub,
    private val messageGrpc: MessageGrpc.MessageStub,
    private val onMessageSubscriberListener: MessageSubscriberListener
) : ChannelSubscriber {

    interface MessageSubscriberListener {
        fun onMessageReceived(value: MessageOuterClass.MessageObjectResponse, domain: String)
    }

    override suspend fun subscribeAndListen() {
        subscribe()
    }

    override suspend fun shutdown() {
        TODO("Not yet implemented")
    }

    private suspend fun subscribe(): Boolean = withContext(Dispatchers.IO) {
        val request = MessageOuterClass.SubscribeRequest.newBuilder()
            .setClientId(clientId)
            .build()

        try {
            val res = messageBlockingStub.subscribe(request)
            if (res.success) {
                printlnCK("subscribeMessageChannel, success")
                listenMessageChannel()
            } else {
                printlnCK("subscribeMessageChannel, ${res.errors}")
            }
            return@withContext res.success
        } catch (e: Exception) {
            printlnCK("subscribeMessageChannel, $e")
            return@withContext false
        }
    }

    private fun listenMessageChannel() {
        val request = MessageOuterClass.ListenRequest.newBuilder()
            .setClientId(clientId)
            .build()

        messageGrpc.listen(request, object : StreamObserver<MessageOuterClass.MessageObjectResponse> {
                override fun onNext(value: MessageOuterClass.MessageObjectResponse) {
                    printlnCK("listenMessageChannel, Receive a message from : ${value.fromClientId}" +
                            ", from workspace = ${value.fromClientWorkspaceDomain}, groupId = ${value.groupId} to client id = ${value.clientId}, workspace = $domain")
                    onMessageSubscriberListener.onMessageReceived(value, domain)
                }

                override fun onError(t: Throwable?) {
                    printlnCK("Listen message error: ${t.toString()}")
                }

                override fun onCompleted() {
                    printlnCK("listenMessageChannel, listen success")
                }
            })
    }
}