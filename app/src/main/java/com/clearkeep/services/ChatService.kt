package com.clearkeep.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.*
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import com.clearkeep.db.clear_keep.model.*
import com.clearkeep.utilities.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import message.MessageOuterClass
import notification.NotifyOuterClass
import javax.inject.Inject
import com.clearkeep.dynamicapi.Environment
import com.clearkeep.dynamicapi.ParamAPI
import com.clearkeep.dynamicapi.ParamAPIProvider
import com.clearkeep.dynamicapi.subscriber.DynamicSubscriberAPIProvider
import com.clearkeep.repo.*
import com.clearkeep.screen.videojanus.showMessagingStyleNotification
import com.clearkeep.services.utils.MessageChannelSubscriber
import com.clearkeep.services.utils.NotificationChannelSubscriber

@AndroidEntryPoint
class ChatService : Service(),
    MessageChannelSubscriber.MessageSubscriberListener,
    NotificationChannelSubscriber.NotificationSubscriberListener {
    @Inject
    lateinit var environment: Environment

    @Inject
    lateinit var serverRepository: ServerRepository

    @Inject
    lateinit var chatRepository: ChatRepository

    @Inject
    lateinit var peopleRepository: PeopleRepository

    @Inject
    lateinit var messageRepository: MessageRepository

    @Inject
    lateinit var groupRepository: GroupRepository

    @Inject
    lateinit var userPreferenceRepository: UserPreferenceRepository

    @Inject
    lateinit var dynamicAPIProvider: DynamicSubscriberAPIProvider

    @Inject
    lateinit var appStorage: AppStorage

    @Inject
    lateinit var apiProvider: ParamAPIProvider

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
        val servers = serverRepository.getServers()
        servers.forEach { server ->
            val domain = server.serverDomain
            val messageSubscriber = MessageChannelSubscriber(
                domain = domain,
                clientId = server.profile.userId,
                messageBlockingStub = apiProvider.provideMessageBlockingStub(ParamAPI(domain,environment.getServer().accessKey,environment.getServer().hashKey)),
                messageGrpc = apiProvider.provideMessageStub(ParamAPI(domain,environment.getServer().accessKey,environment.getServer().hashKey)),
                onMessageSubscriberListener = this,
                appStorage
            )
            val notificationSubscriber = NotificationChannelSubscriber(
                domain = domain,
                clientId = server.profile.userId,
                notifyBlockingStub = apiProvider.provideNotifyBlockingStub(ParamAPI(domain,environment.getServer().accessKey,environment.getServer().hashKey)),
                notifyStub = apiProvider.provideNotifyStub(ParamAPI(domain,environment.getServer().accessKey,environment.getServer().hashKey)),
                notificationChannelListener = this,
                appStorage
            )
            messageSubscriber.subscribeAndListen()
            notificationSubscriber.subscribeAndListen()
        }

    }

    override fun onMessageReceived(value: MessageOuterClass.MessageObjectResponse, domain: String) {
        scope.launch {
            printlnCK("chatService raw message ${value.message.toStringUtf8()}")
            environment.setUpTempDomain(Server(null, "", domain, value.clientId, "", 0L, "", "", "", false, Profile(null, value.clientId, "", "", "", 0L, "")))
            val res = messageRepository.decryptMessage(
                value.id, value.groupId, value.groupType,
                value.fromClientId, value.fromClientWorkspaceDomain,
                value.createdAt, value.updatedAt,
                value.message,
                Owner(domain, value.clientId)
            )
            val isShowNotification =
                serverRepository.getOwnerClientIds().contains(value.fromClientId)
            if (!isShowNotification) {
                val roomId = chatRepository.getJoiningRoomId()
                val groupId = value.groupId
                handleShowNotification(
                    joiningRoomId = roomId,
                    groupId = groupId,
                    res,
                    domain,
                    value.clientId
                )
            }
        }
    }

    override fun onNotificationReceived(
        value: NotifyOuterClass.NotifyObjectResponse,
        domain: String
    ) {
        printlnCK("chatService onNotificationReceived")
        scope.launch {
            when (value.notifyType) {
                "new-group" -> {
                    // groupRepository.fetchNewGroup(value.refGroupId, Owner(value.clientWorkspaceDomain, value.clientId))
                    groupRepository.fetchGroups()
                }
                "new-peer" -> {
                    // groupRepository.fetchNewGroup(value.refGroupId, Owner(value.clientWorkspaceDomain, value.clientId))
                    groupRepository.fetchGroups()
                }
                "peer-update-key" -> {
                    chatRepository.processPeerKey(value.refClientId, value.refWorkspaceDomain,value.clientId,value.clientWorkspaceDomain)
                }
                "member-add" -> {
                    groupRepository.fetchGroups()
                    val updateGroupIntent = Intent(ACTION_ADD_REMOVE_MEMBER)
                    updateGroupIntent.putExtra(EXTRA_GROUP_ID, value.refGroupId)
                    sendBroadcast(updateGroupIntent)
                }

                "member-removal", "member-leave" -> {
                    groupRepository.getGroupFromAPIById(value.refGroupId,domain,value.clientId)
                    groupRepository.removeGroupOnWorkSpace(value.refGroupId,domain,value.refClientId)
                    val updateGroupIntent = Intent(ACTION_ADD_REMOVE_MEMBER)
                    updateGroupIntent.putExtra(EXTRA_GROUP_ID, value.refGroupId)
                    sendBroadcast(updateGroupIntent)

                }
                "notify_type" -> {
                    printlnCK("chatService Deactive account ref_client_id ${value.refClientId} ref_group_id ${value.refGroupId}")
                }
                CALL_TYPE_VIDEO -> {
                    val switchIntent = Intent(ACTION_CALL_SWITCH_VIDEO)
                    switchIntent.putExtra(EXTRA_CALL_SWITCH_VIDEO, value.refGroupId)
                    sendBroadcast(switchIntent)
                }
                CALL_UPDATE_TYPE_CANCEL -> {
                    val groupAsyncRes = async { groupRepository.getGroupByID(value.refGroupId, domain, value.clientId) }
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

    private fun handleShowNotification(joiningRoomId: Long, groupId: Long, message: Message, domain: String, ownerClientId: String) {
        scope.launch {
            printlnCK("handleShowNotification $groupId")
            val group = groupRepository.getGroupByID(groupId = groupId, domain, ownerClientId)
            group?.data?.let {
                val currentServer = environment.getServer()
                if (joiningRoomId != groupId || currentServer.serverDomain != domain || currentServer.profile.userId != ownerClientId) {
                    val userPreference = userPreferenceRepository.getUserPreference(domain, ownerClientId) ?: UserPreference.getDefaultUserPreference("", "", false)
                    val senderUser = peopleRepository.getFriendFromID(
                        message.senderId
                    )
                    val avatar = senderUser?.avatar
                    printlnCK("Notification service raw message $message")
                    showMessagingStyleNotification(
                        context = applicationContext,
                        chatGroup = it,
                        message,
                        userPreference,
                        avatar
                    )
                }
            }
        }
    }

    private suspend fun updateMessageHistory() {
        groupRepository.fetchGroups()
    }

    private suspend fun updateMessageAndKeyInOnlineRoom() {
        val roomId = chatRepository.getJoiningRoomId()
        val currentServer = environment.getServer()
        if (roomId > 0 && currentServer != null) {
            val group = groupRepository.getGroupByID(roomId, currentServer.serverDomain, currentServer.profile.userId)
            group?.data?.let {
                messageRepository.updateMessageFromAPI(it.groupId, Owner(currentServer.serverDomain, currentServer.profile.userId), it.lastMessageSyncTimestamp)

                if (!it.isGroup()) {
                    val receiver = it.clientList.firstOrNull { client ->
                        client.userId != currentServer.profile.userId
                    }
                    if (receiver != null) {
                        chatRepository.processPeerKey(receiver.userId, receiver.domain,currentServer.profile.userId,currentServer.serverDomain)
                    }
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