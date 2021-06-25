package com.clearkeep.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.*
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import com.clearkeep.repo.ChatRepository
import com.clearkeep.repo.GroupRepository
import com.clearkeep.repo.MessageRepository
import com.clearkeep.screen.chat.utils.isGroup
import com.clearkeep.utilities.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import message.MessageOuterClass
import notification.NotifyOuterClass
import javax.inject.Inject
import com.clearkeep.db.clear_keep.model.Message
import com.clearkeep.dynamicapi.subscriber.DynamicSubscriberAPIProvider
import com.clearkeep.repo.PeopleRepository
import com.clearkeep.screen.videojanus.showMessagingStyleNotification
import com.clearkeep.services.utils.MessageChannelSubscriber
import com.clearkeep.services.utils.NotificationChannelSubscriber


@AndroidEntryPoint
class ChatService : Service(),
    MessageChannelSubscriber.MessageSubscriberListener,
    NotificationChannelSubscriber.NotificationSubscriberListener {
    @Inject
    lateinit var userManager: UserManager

    @Inject
    lateinit var chatRepository: ChatRepository

    @Inject
    lateinit var peopleRepository: PeopleRepository

    @Inject
    lateinit var messageRepository: MessageRepository

    @Inject
    lateinit var groupRepository: GroupRepository

    @Inject
    lateinit var dynamicAPIProvider: DynamicSubscriberAPIProvider

    private val scope: CoroutineScope = CoroutineScope(Job() + Dispatchers.IO)

    private var isShouldRecreateChannel = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            printlnCK("network available again")
            if (isShouldRecreateChannel) {
                isShouldRecreateChannel = false
                subscribeAndUpdateMessageData()
            }
        }

        override fun onLost(network: Network) {
            printlnCK("network lost")
            isShouldRecreateChannel = true
            printlnCK("ChatService, shut down channel")
        }
    }

    override fun onCreate() {
        printlnCK("ChatService, onCreate")
        super.onCreate()

        if (isOnline(applicationContext)) {
            isShouldRecreateChannel = false
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
        dynamicAPIProvider.shutDownAll()
        unregisterNetworkChange()
    }

    private suspend fun subscribe() {
        val domain = userManager.getWorkspaceDomain()
        val messageSubscriber = MessageChannelSubscriber(
            domain = userManager.getWorkspaceDomain(),
            clientId = userManager.getClientId(),
            messageBlockingStub = dynamicAPIProvider.provideMessageBlockingStub(domain),
            messageGrpc = dynamicAPIProvider.provideMessageStub(domain),
            onMessageSubscriberListener = this
        )
        val notificationSubscriber = NotificationChannelSubscriber(
            domain = userManager.getWorkspaceDomain(),
            clientId = userManager.getClientId(),
            notifyBlockingStub = dynamicAPIProvider.provideNotifyBlockingStub(domain),
            notifyStub = dynamicAPIProvider.provideNotifyStub(domain),
            notificationChannelListener = this
        )
        messageSubscriber.subscribeAndListen()
        notificationSubscriber.subscribeAndListen()
    }

    override fun onMessageReceived(value: MessageOuterClass.MessageObjectResponse, domain: String) {
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

    override fun onNotificationReceived(
        value: NotifyOuterClass.NotifyObjectResponse,
        domain: String
    ) {
        scope.launch {
            when (value.notifyType) {
                "new-group" -> {
                    groupRepository.fetchRoomsFromAPI()
                }
                "new-peer" -> {
                    groupRepository.fetchRoomsFromAPI()
                }
                "peer-update-key" -> {
                    val remoteUser = peopleRepository.getFriend(value.refClientId)
                    if (remoteUser != null) {
                        chatRepository.processPeerKey(remoteUser.id, remoteUser.workspace)
                    } else {
                        printlnCK("Warning, can not get user to peer update key")
                    }
                }
                CALL_TYPE_VIDEO -> {
                    val switchIntent = Intent(ACTION_CALL_SWITCH_VIDEO)
                    switchIntent.putExtra(EXTRA_CALL_SWITCH_VIDEO, value.refGroupId)
                    sendBroadcast(switchIntent)
                }
                CALL_UPDATE_TYPE_CANCEL -> {
                    val groupAsyncRes = async { groupRepository.getGroupByID(value.refGroupId) }
                    val group = groupAsyncRes.await()
                    if (group != null ) {
                        NotificationManagerCompat.from(applicationContext).cancel(null, INCOMING_NOTIFICATION_ID)
                        val endIntent = Intent(ACTION_CALL_CANCEL)
                        endIntent.putExtra(EXTRA_CALL_CANCEL_GROUP_ID, value.refGroupId.toString())
                        sendBroadcast(endIntent)
                    }
                }
            }
        }
    }

    private fun handleShowNotification(joiningRoomId: Long, groupId: Long, message: Message) {
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
                    chatRepository.processPeerKey(receiver.id, receiver.workspace)
                }
            }
        }
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
}