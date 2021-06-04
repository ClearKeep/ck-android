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
import com.clearkeep.db.clear_keep.model.Message
import com.clearkeep.db.clear_keep.model.People
import com.clearkeep.screen.videojanus.showMessagingStyleNotification


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
                subscribeAndUpdateMessageData()
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
            subscribeAndUpdateMessageData()
        } else {
            isShouldRecreateChannel = true
        }


        registerNetworkChange()
    }

    private fun subscribeAndUpdateMessageData() {
        scope.launch {
            subscribe()
            updateMessageHistory()
            updateMessageAndKeyInOnlineRoom()
        }
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

    private suspend fun subscribe() {
        subscribeNotificationChannel()
        subscribeMessageChannel()
    }

    private suspend fun subscribeMessageChannel(): Boolean = withContext(Dispatchers.IO) {
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

    private suspend fun subscribeNotificationChannel(): Boolean = withContext(Dispatchers.IO) {
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

    private fun listenMessageChannel() {
        val request = MessageOuterClass.ListenRequest.newBuilder()
            .setClientId(userManager.getClientId())
            .build()

        MessageGrpc.newStub(networkChannel)
            .listen(request, object : StreamObserver<MessageOuterClass.MessageObjectResponse> {
                override fun onNext(value: MessageOuterClass.MessageObjectResponse) {
                    scope.launch {
                        if (!isGroup(value.groupType)) {
                            val res = chatRepository.decryptMessageFromPeer(value)
                            if (res != null) {
                                val roomId = chatRepository.getJoiningRoomId()
                                val groupId = value.groupId
                                handleShowNotification(joiningRoomId = roomId, groupId = groupId, res)
                            }
                        } else {
                            val res = chatRepository.decryptMessageFromGroup(value)
                            if (res != null) {
                                val roomId = chatRepository.getJoiningRoomId()
                                val groupId = value.groupId
                                handleShowNotification(joiningRoomId = roomId, groupId = groupId, res)
                            }
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

    fun handleShowNotification(joiningRoomId: Long, groupId: Long, message: Message) {
        scope.launch {
            val group = groupRepository.getGroupByID(groupId = groupId)
            group?.let {
                if (joiningRoomId != groupId) {
                    showMessagingStyleNotification(
                        context = applicationContext,
                        chatGroup = it,
                        message,
                    )
                }
            }
        }
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
                    when (value.notifyType) {
                        "new-group" -> {
                            groupRepository.fetchRoomsFromAPI()
                        }
                        "new-peer" -> {
                            groupRepository.fetchRoomsFromAPI()
                        }
                        "peer-update-key" -> {
                            chatRepository.processPeerKey(value.refClientId)
                        }
                        CALL_TYPE_VIDEO -> {
                            val switchIntent = Intent(ACTION_CALL_SWITCH_VIDEO)
                            switchIntent.putExtra(EXTRA_CALL_SWITCH_VIDEO, value.refGroupId)
                            sendBroadcast(switchIntent)
                        }
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

    private suspend fun updateMessageHistory() {
        groupRepository.fetchRoomsFromAPI()
    }

    private suspend fun updateMessageAndKeyInOnlineRoom() {
        val roomId = chatRepository.getJoiningRoomId()
        if (roomId > 0) {
            val group = groupRepository.getGroupByID(roomId)!!
            messageRepository.updateMessageFromAPI(group.id, group.lastMessageSyncTimestamp)

            if (!group.isGroup()) {
                val receiver = group.clientList.firstOrNull { client ->
                    client.id != userManager.getClientId()
                }
                if (receiver != null) {
                    chatRepository.processPeerKey(receiver.id)
                }
            }
        }
    }
}