package com.clearkeep.data.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.*
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import com.clearkeep.common.utilities.*
import com.clearkeep.domain.repository.*
import com.clearkeep.utilities.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import message.MessageOuterClass
import notification.NotifyOuterClass
import javax.inject.Inject
import com.clearkeep.data.remote.dynamicapi.Environment
import com.clearkeep.data.remote.dynamicapi.ParamAPI
import com.clearkeep.data.remote.dynamicapi.ParamAPIProvider
import com.clearkeep.data.remote.dynamicapi.subscriber.DynamicSubscriberAPIProvider
import com.clearkeep.domain.model.*
import com.clearkeep.data.services.utils.MessageChannelSubscriber
import com.clearkeep.data.services.utils.NotificationChannelSubscriber
import com.clearkeep.domain.usecase.chat.GetJoiningRoomUseCase
import com.clearkeep.domain.usecase.chat.ProcessPeerKeyUseCase
import com.clearkeep.domain.usecase.group.DeleteGroupUseCase
import com.clearkeep.domain.usecase.group.FetchGroupsUseCase
import com.clearkeep.domain.usecase.group.GetGroupByIdUseCase
import com.clearkeep.domain.usecase.message.DecryptMessageUseCase
import com.clearkeep.domain.usecase.message.UpdateMessageFromApiUseCase
import com.clearkeep.domain.usecase.people.GetFriendUseCase
import com.clearkeep.domain.usecase.preferences.GetUserPreferenceUseCase
import com.clearkeep.domain.usecase.server.GetOwnerClientIdsUseCase
import com.clearkeep.domain.usecase.server.GetServersUseCase
import com.clearkeep.features.shared.showMessagingStyleNotification

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
    lateinit var fetchGroupsUseCase: FetchGroupsUseCase

    @Inject
    lateinit var getGroupByIdUseCase: GetGroupByIdUseCase

    @Inject
    lateinit var getUserPreferenceUseCase: GetUserPreferenceUseCase

    @Inject
    lateinit var deleteGroupUseCase: DeleteGroupUseCase

    @Inject
    lateinit var processPeerKeyUseCase: ProcessPeerKeyUseCase

    @Inject
    lateinit var getJoiningRoomUseCase: GetJoiningRoomUseCase

    @Inject
    lateinit var decryptMessageUseCase: DecryptMessageUseCase

    @Inject
    lateinit var updateMessageFromApiUseCase: UpdateMessageFromApiUseCase

    @Inject
    lateinit var getOwnerClientIdsUseCase: GetOwnerClientIdsUseCase

    @Inject
    lateinit var getServersUseCase: GetServersUseCase

    @Inject
    lateinit var getFriendUseCase: GetFriendUseCase

    @Inject
    lateinit var dynamicAPIProvider: DynamicSubscriberAPIProvider

    @Inject
    lateinit var appStorage: UserRepository

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
        val servers = getServersUseCase()
        servers.forEach { server ->
            val domain = server.serverDomain
            val messageSubscriber = MessageChannelSubscriber(
                domain = domain,
                messageBlockingStub = apiProvider.provideMessageBlockingStub(
                    ParamAPI(
                        domain,
                        environment.getServer().accessKey,
                        environment.getServer().hashKey
                    )
                ),
                messageGrpc = apiProvider.provideMessageStub(
                    ParamAPI(
                        domain,
                        environment.getServer().accessKey,
                        environment.getServer().hashKey
                    )
                ),
                onMessageSubscriberListener = this,
                userManager = appStorage
            )
            val notificationSubscriber = NotificationChannelSubscriber(
                domain = domain,
                notifyBlockingStub = apiProvider.provideNotifyBlockingStub(
                    ParamAPI(
                        domain,
                        environment.getServer().accessKey,
                        environment.getServer().hashKey
                    )
                ),
                notifyStub = apiProvider.provideNotifyStub(
                    ParamAPI(
                        domain,
                        environment.getServer().accessKey,
                        environment.getServer().hashKey
                    )
                ),
                notificationChannelListener = this,
                userManager = appStorage
            )
            messageSubscriber.subscribeAndListen()
            notificationSubscriber.subscribeAndListen()
        }

    }

    override fun onMessageReceived(value: MessageOuterClass.MessageObjectResponse, domain: String) {
        scope.launch {
            printlnCK("chatService raw message ${value.message.toStringUtf8()}")
            environment.setUpTempDomain(
                Server(
                    null,
                    "",
                    domain,
                    value.clientId,
                    "",
                    0L,
                    "",
                    "",
                    "",
                    false,
                    Profile(null, value.clientId, "", "", "", 0L, "")
                )
            )
            val res = decryptMessageUseCase(
                value.id, value.groupId, value.groupType,
                value.fromClientId, value.fromClientWorkspaceDomain,
                value.createdAt, value.updatedAt,
                value.message,
                Owner(domain, value.clientId)
            )
            val isShowNotification =
                getOwnerClientIdsUseCase().contains(value.fromClientId)
            if (!isShowNotification) {
                val roomId = getJoiningRoomUseCase()
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
                    fetchGroupsUseCase()
                }
                "new-peer" -> {
                    fetchGroupsUseCase()
                }
                "peer-update-key" -> {
                    processPeerKeyUseCase(
                        value.refClientId,
                        value.refWorkspaceDomain,
                        value.clientId,
                        value.clientWorkspaceDomain
                    )
                }
                "member-add" -> {
                    fetchGroupsUseCase()
                    val updateGroupIntent = Intent(ACTION_ADD_REMOVE_MEMBER)
                    updateGroupIntent.putExtra(EXTRA_GROUP_ID, value.refGroupId)
                    sendBroadcast(updateGroupIntent)
                }

                "member-removal", "member-leave" -> {
                    getGroupByIdUseCase(value.refGroupId, domain, value.clientId)
                    deleteGroupUseCase(
                        value.refGroupId,
                        domain,
                        value.refClientId
                    )
                    val updateGroupIntent = Intent(ACTION_ADD_REMOVE_MEMBER)
                    updateGroupIntent.putExtra(EXTRA_GROUP_ID, value.refGroupId)
                    sendBroadcast(updateGroupIntent)

                }
                CALL_TYPE_VIDEO -> {
                    val switchIntent = Intent(ACTION_CALL_SWITCH_VIDEO)
                    switchIntent.putExtra(EXTRA_CALL_SWITCH_VIDEO, value.refGroupId)
                    sendBroadcast(switchIntent)
                }
                CALL_UPDATE_TYPE_CANCEL -> {
                    val groupAsyncRes = async {
                        getGroupByIdUseCase(
                            value.refGroupId,
                            domain,
                            value.clientId
                        )
                    }
                    val group = groupAsyncRes.await()
                    if (group != null) {
                        NotificationManagerCompat.from(applicationContext)
                            .cancel(null, INCOMING_NOTIFICATION_ID)
                        val endIntent = Intent(ACTION_CALL_CANCEL)
                        endIntent.putExtra(EXTRA_CALL_CANCEL_GROUP_ID, value.refGroupId.toString())
                        sendBroadcast(endIntent)
                    }
                }
            }
        }
    }

    private fun handleShowNotification(
        joiningRoomId: Long,
        groupId: Long,
        message: Message,
        domain: String,
        ownerClientId: String
    ) {
        scope.launch {
            printlnCK("handleShowNotification $groupId")
            val group = getGroupByIdUseCase(groupId = groupId, domain, ownerClientId)
            group?.data?.let {
                val currentServer = environment.getServer()
                if (joiningRoomId != groupId || currentServer.serverDomain != domain || currentServer.profile.userId != ownerClientId) {
                    val userPreference =
                        getUserPreferenceUseCase(domain, ownerClientId)
                            ?: UserPreference.getDefaultUserPreference("", "", false)
                    val senderUser = getFriendUseCase(
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
        fetchGroupsUseCase()
    }

    private suspend fun updateMessageAndKeyInOnlineRoom() {
        val roomId = getJoiningRoomUseCase()
        val currentServer = environment.getServer()
        if (roomId > 0 && currentServer != null) {
            val group = getGroupByIdUseCase(
                roomId,
                currentServer.serverDomain,
                currentServer.profile.userId
            )
            group?.data?.let {
                updateMessageFromApiUseCase(
                    it.groupId,
                    Owner(
                        currentServer.serverDomain,
                        currentServer.profile.userId
                    ),
                    it.lastMessageSyncTimestamp
                )

                if (!it.isGroup()) {
                    val receiver = it.clientList.firstOrNull { client ->
                        client.userId != currentServer.profile.userId
                    }
                    if (receiver != null) {
                        processPeerKeyUseCase(
                            receiver.userId,
                            receiver.domain,
                            currentServer.profile.userId,
                            currentServer.serverDomain
                        )
                    }
                }
            }
        }
    }

    private fun registerNetworkChange() {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
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