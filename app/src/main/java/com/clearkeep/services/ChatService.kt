package com.clearkeep.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.*
import android.os.Build
import android.os.IBinder
import com.clearkeep.repo.ChatRepository
import com.clearkeep.repo.GroupRepository
import com.clearkeep.repo.MessageRepository
import com.clearkeep.screen.chat.utils.isGroup
import com.clearkeep.utilities.*
import dagger.hilt.android.AndroidEntryPoint
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.*
import message.MessageGrpc
import message.MessageOuterClass
import notification.NotifyGrpc
import notification.NotifyOuterClass
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class ChatService : Service() {
    @Inject
    lateinit var userManager: UserManager

    @Inject
    lateinit var chatRepository: ChatRepository

    @Inject
    lateinit var messageRepository: MessageRepository

    @Inject
    lateinit var groupRepository: GroupRepository

    val scope: CoroutineScope = CoroutineScope(Job() + Dispatchers.IO)

    private lateinit var networkChannel: ManagedChannel

    private var isShouldRecreateChannel = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            printlnCK("network available again")
            if (isShouldRecreateChannel) {
                isShouldRecreateChannel = false
                networkChannel = createNetworkChannel()
                subscribe()
                updateNewMessages()
            }
        }

        override fun onLost(network: Network) {
            printlnCK("network lost")
            isShouldRecreateChannel = true
            networkChannel.shutdownNow()
            printlnCK("ChatService, shut down channel")
        }
    }

    override fun onCreate() {
        printlnCK("ChatService, onCreate")
        super.onCreate()
        networkChannel = createNetworkChannel()

        if (isOnline(applicationContext)) {
            subscribe()
            updateNewMessages()
        } else {
            isShouldRecreateChannel = true
        }


        registerNetworkChange()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        printlnCK("ChatService, onDestroy")
        scope.cancel()
        if (!networkChannel.isShutdown) {
            networkChannel.shutdownNow()
        }
        unregisterNetworkChange()
    }

    private fun createNetworkChannel(): ManagedChannel {
        return ManagedChannelBuilder.forAddress(BASE_URL, PORT)
            .usePlaintext()
            .executor(Dispatchers.Default.asExecutor())
            .keepAliveTimeout(20, TimeUnit.SECONDS)
            .disableRetry()
            .build()
    }

    private fun registerNetworkChange() {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (connectivityManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                connectivityManager.registerDefaultNetworkCallback(networkCallback)
            } else {
                val request = NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()
                connectivityManager.registerNetworkCallback(request, networkCallback);
            }
        }
    }

    private fun unregisterNetworkChange() {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (connectivityManager != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }

    private fun subscribe() {
        scope.launch {
            subscribeNotificationChannel()
            subscribeMessageChannel()
        }
    }

    private suspend fun unsubscribe() {
        unsubscribeMessageChannel()
        unsubscribeNotificationChannel()
    }

    private suspend fun subscribeMessageChannel() : Boolean = withContext(Dispatchers.IO) {
        val request = MessageOuterClass.SubscribeRequest.newBuilder()
            .setClientId(userManager.getClientId())
            .build()

        try {
            val res = MessageGrpc.newBlockingStub(networkChannel).subscribe(request)
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

    private suspend fun unsubscribeMessageChannel() : Boolean = withContext(Dispatchers.IO) {
        try {
            val request = MessageOuterClass.UnSubscribeRequest.newBuilder()
                .setClientId(userManager.getClientId())
                .build()

            val res = MessageGrpc.newBlockingStub(networkChannel).unSubscribe(request)
            printlnCK("unsubscribeMessageChannel, ${res.success}")
            return@withContext res.success
        } catch (e: Exception) {
            printlnCK("unsubscribeMessageChannel, $e")
            return@withContext false
        }
    }

    private suspend fun subscribeNotificationChannel() : Boolean = withContext(Dispatchers.IO) {
        val request = NotifyOuterClass.SubscribeRequest.newBuilder()
            .setClientId(userManager.getClientId())
            .build()
        try {
            val res = NotifyGrpc.newBlockingStub(networkChannel).subscribe(request)
            if (res.success) {
                printlnCK("subscribeNotificationChannel, success")
                listenNotificationChannel()
            } else {
                printlnCK("subscribeNotificationChannel, ${res.errors}")
            }
            return@withContext res.success
        } catch (e: Exception) {
            printlnCK("subscribeNotificationChannel, $e")
            return@withContext false
        }
    }

    private suspend fun unsubscribeNotificationChannel() : Boolean = withContext(Dispatchers.IO) {
        try {
            val request = NotifyOuterClass.UnSubscribeRequest.newBuilder()
                .setClientId(userManager.getClientId())
                .build()

            val res = NotifyGrpc.newBlockingStub(networkChannel).unSubscribe(request)
            printlnCK("unsubscribeNotificationChannel, ${res.success}")
            return@withContext res.success
        } catch (e: Exception) {
            printlnCK("unsubscribeNotificationChannel, $e")
            return@withContext false
        }
    }

    private fun listenMessageChannel() {
        val request = MessageOuterClass.ListenRequest.newBuilder()
            .setClientId(userManager.getClientId())
            .build()

        MessageGrpc.newStub(networkChannel).listen(request, object : StreamObserver<MessageOuterClass.MessageObjectResponse> {
            override fun onNext(value: MessageOuterClass.MessageObjectResponse) {
                printlnCK("listenMessageChannel, Receive a message from : ${value.fromClientId}" +
                        ", groupId = ${value.groupId} groupType = ${value.groupType}")
                scope.launch {
                    // TODO
                    if (!isGroup(value.groupType)) {
                        chatRepository.decryptMessageFromPeer(value)
                    } else {
                        chatRepository.decryptMessageFromGroup(value)
                    }
                }
            }

            override fun onError(t: Throwable?) {
                printlnCK("Listen message error: ${t.toString()}")
            }

            override fun onCompleted() {
                printlnCK("listenMessageChannel, listen success")
            }
        })
    }

    private fun listenNotificationChannel() {
        val request = NotifyOuterClass.ListenRequest.newBuilder()
            .setClientId(userManager.getClientId())
            .build()

        NotifyGrpc.newStub(networkChannel).listen(request, object : StreamObserver<NotifyOuterClass.NotifyObjectResponse> {
            override fun onNext(value: NotifyOuterClass.NotifyObjectResponse) {
                printlnCK("listenNotificationChannel, Receive a notification from : ${value.refClientId}" +
                        ", groupId = ${value.refGroupId} groupType = ${value.notifyType}")
                scope.launch {
                    if(value.notifyType == "new-group") {
                        groupRepository.fetchRoomsFromAPI()
                    }
                }
            }

            override fun onError(t: Throwable?) {
                printlnCK("Listen notification error: ${t.toString()}")
            }

            override fun onCompleted() {
                printlnCK("listenNotificationChannel, listen success")
            }
        })
    }

    private fun updateNewMessages() {
        scope.launch {
            groupRepository.fetchRoomsFromAPI()
            val roomId = chatRepository.getJoiningRoomId()
            if (roomId > 0) {
                val group = groupRepository.getGroupByID(roomId)!!
                messageRepository.updateMessageFromAPI(group.id, group.lastMessageSyncTimestamp)
            }
        }
    }
}