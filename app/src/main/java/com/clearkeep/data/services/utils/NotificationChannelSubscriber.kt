package com.clearkeep.data.services.utils

import com.clearkeep.utilities.*
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import notification.NotifyGrpc
import notification.NotifyOuterClass

class NotificationChannelSubscriber(
    private val domain: String,
    private val notifyBlockingStub: NotifyGrpc.NotifyBlockingStub,
    private val notifyStub: NotifyGrpc.NotifyStub,
    private val notificationChannelListener: NotificationSubscriberListener,
    private val userManager: AppStorage
) {

    interface NotificationSubscriberListener {
        fun onNotificationReceived(value: NotifyOuterClass.NotifyObjectResponse, domain: String)
    }

    suspend fun subscribeAndListen() {
        subscribe()
    }

    private suspend fun subscribe(): Boolean = withContext(Dispatchers.IO) {
        val request = NotifyOuterClass.SubscribeRequest.newBuilder()
            .setDeviceId(userManager.getUniqueDeviceID())
            .build()
        try {
            val res = notifyBlockingStub.subscribe(request)
            if (res.error.isNullOrEmpty()) {
                printlnCK("subscribeNotificationChannel, success")
                listenNotificationChannel()
            } else {
                printlnCK("subscribeNotificationChannel, ${res.error}")
            }
            return@withContext res.error.isNullOrEmpty()
        } catch (e: Exception) {
            printlnCK("subscribeNotificationChannel, $e")
            return@withContext false
        }
    }

    private fun listenNotificationChannel() {
        val request = NotifyOuterClass.ListenRequest.newBuilder()
            .setDeviceId(userManager.getUniqueDeviceID())
            .build()

        notifyStub.listen(request, object : StreamObserver<NotifyOuterClass.NotifyObjectResponse> {
            override fun onNext(value: NotifyOuterClass.NotifyObjectResponse) {
                printlnCK(
                    "listenNotificationChannel, Receive a notification from : ${value.refClientId}" +
                            ", groupId = ${value.refGroupId} groupType = ${value.notifyType} to ${value.clientId} + ${value.clientWorkspaceDomain}"
                )
                notificationChannelListener.onNotificationReceived(value, domain)
            }

            override fun onError(t: Throwable?) {
                printlnCK("Listen notification error: ${t.toString()}")
            }

            override fun onCompleted() {
                printlnCK("listenNotificationChannel, listen success")
            }
        })
    }
}