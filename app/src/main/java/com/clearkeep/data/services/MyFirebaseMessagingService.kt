package com.clearkeep.data.services

import android.content.Intent
import android.util.Base64
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.clearkeep.common.utilities.*
import com.clearkeep.domain.repository.*
import com.clearkeep.data.remote.dynamicapi.Environment
import com.clearkeep.domain.model.CKSignalProtocolAddress
import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.model.UserPreference
import com.clearkeep.domain.usecase.call.BusyCallUseCase
import com.clearkeep.domain.usecase.chat.GetJoiningRoomUseCase
import com.clearkeep.domain.usecase.group.*
import com.clearkeep.domain.usecase.message.DecryptMessageUseCase
import com.clearkeep.domain.usecase.message.DeleteMessageUseCase
import com.clearkeep.domain.usecase.people.DeleteFriendUseCase
import com.clearkeep.domain.usecase.people.GetFriendUseCase
import com.clearkeep.domain.usecase.preferences.GetUserPreferenceUseCase
import com.clearkeep.domain.usecase.server.GetOwnerClientIdsUseCase
import com.clearkeep.domain.usecase.server.GetServerUseCase
import com.clearkeep.features.chat.presentation.home.MainActivity
import com.clearkeep.features.shared.presentation.AppCall
import com.clearkeep.features.shared.showMessagingStyleNotification
import com.google.common.reflect.TypeToken
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.google.protobuf.ByteString
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import java.util.HashMap

@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {
    @Inject
    lateinit var getUserPreferenceUseCase: GetUserPreferenceUseCase

    @Inject
    lateinit var disableChatOfDeactivatedUserUseCase: DisableChatOfDeactivatedUserUseCase

    @Inject
    lateinit var fetchGroupsUseCase: FetchGroupsUseCase

    @Inject
    lateinit var getGroupByIdUseCase: GetGroupByIdUseCase

    @Inject
    lateinit var deleteGroupUseCase: DeleteGroupUseCase

    @Inject
    lateinit var getListClientInGroupUseCase: GetListClientInGroupUseCase

    @Inject
    lateinit var deleteMessageUseCase: DeleteMessageUseCase

    @Inject
    lateinit var decryptMessageUseCase: DecryptMessageUseCase

    @Inject
    lateinit var getOwnerClientIdsUseCase: GetOwnerClientIdsUseCase

    @Inject
    lateinit var getServerUseCase: GetServerUseCase

    @Inject
    lateinit var deleteFriendUseCase: DeleteFriendUseCase

    @Inject
    lateinit var getFriendUseCase: GetFriendUseCase

    @Inject
    lateinit var peopleRepository: PeopleRepository

    @Inject
    lateinit var busyCallUseCase: BusyCallUseCase

    @Inject
    lateinit var environment: Environment

    @Inject
    lateinit var senderKeyStore: SenderKeyStore

    @Inject
    lateinit var signalKeyRepository: SignalKeyRepository

    @Inject
    lateinit var getJoiningRoomUseCase: GetJoiningRoomUseCase

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        printlnCK("MyFirebaseMessagingService onMessageReceived: ${remoteMessage.data}")
        handleNotification(remoteMessage)
    }

    private fun handleNotification(remoteMessage: RemoteMessage) {
        val clientId = remoteMessage.data["client_id"] ?: ""
        val clientDomain = remoteMessage.data["client_workspace_domain"] ?: ""
        printlnCK("notification $clientId")
        GlobalScope.launch {
            val server = getServerUseCase(clientDomain, clientId)
            printlnCK("handleNotification server  clientDomain: $clientDomain clientId: $clientId notify_type ${remoteMessage.data["notify_type"]}")

            if (server != null) {
                environment.setUpTempDomain(server)
                when (remoteMessage.data["notify_type"]) {
                    "request_call" -> {
                        handleRequestCall(remoteMessage)
                    }
                    "cancel_request_call" -> {
                        handleCancelCall(remoteMessage)
                    }
                    "busy_request_call" -> {
                        handleBusyCall(remoteMessage)
                    }
                    "new_message" -> {
                        handleNewMessage(remoteMessage)
                    }
                    "member_leave", "new_member" -> {
                        handlerRequestAddRemoteMember(remoteMessage)
                    }
                    "deactive_account" -> {
                        printlnCK("Deactive account ref_client_id ${remoteMessage.data["ref_client_id"]} ref_group_id ${remoteMessage.data["ref_group_id"]}")
                        handleRequestDeactiveAccount(remoteMessage)
                        val intentNotiType = Intent(applicationContext, MainActivity::class.java)
                        intentNotiType.putExtra("notify_type", "deactive_account")
                        intentNotiType.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        applicationContext.startActivity(intentNotiType)
                    }
                    "reset_pincode" ->{
                        val intentNotiType = Intent(applicationContext, MainActivity::class.java)
                        intentNotiType.putExtra("notify_type", "deactive_account")
                        intentNotiType.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        applicationContext.startActivity(intentNotiType)
                    }
                    CALL_TYPE_VIDEO -> {
                        val switchIntent = Intent(ACTION_CALL_SWITCH_VIDEO)
                        val groupId = remoteMessage.data["group_id"]
                        switchIntent.putExtra(EXTRA_CALL_SWITCH_VIDEO, groupId)
                        sendBroadcast(switchIntent)
                    }
                }
            }
        }
    }

    private suspend fun handleRequestCall(remoteMessage: RemoteMessage) {
        val clientId = remoteMessage.data["client_id"] ?: ""
        val clientDomain = remoteMessage.data["client_workspace_domain"] ?: ""
        val groupId = remoteMessage.data["group_id"]
        val groupType = remoteMessage.data["group_type"] ?: ""
        val groupName = remoteMessage.data["group_name"] ?: ""
        val groupCallType = remoteMessage.data["call_type"]
        var avatar = remoteMessage.data["from_client_avatar"] ?: ""
        val fromClientName = remoteMessage.data["from_client_name"]
        val fromClientId = remoteMessage.data["from_client_id"] ?: ""
        val rtcToken = remoteMessage.data["group_rtc_token"] ?: ""
        val webRtcGroupId = remoteMessage.data["group_rtc_id"] ?: ""
        val webRtcUrl = remoteMessage.data["group_rtc_url"] ?: ""

        val turnConfigJson = remoteMessage.data["turn_server"] ?: ""
        val stunConfigJson = remoteMessage.data["stun_server"] ?: ""

        val turnConfigJsonObject = JSONObject(turnConfigJson)
        val stunConfigJsonObject = JSONObject(stunConfigJson)
        val turnUrl = turnConfigJsonObject.getString("server")
        val stunUrl = stunConfigJsonObject.getString("server")
        val turnUser = turnConfigJsonObject.getString("user")
        val turnPass = turnConfigJsonObject.getString("pwd")
        val isAudioMode = groupCallType == CALL_TYPE_AUDIO
        printlnCK("handleRequestCall avatar $avatar")
        if (avatar.isBlank() && !isGroup(groupType)) {
            getFriendUseCase(fromClientId)?.avatar?.let {
                avatar = it
            }
        }
        val currentUserName = if (isGroup(groupType)) "" else {
            val server = getServerUseCase(clientDomain, clientId)
            server?.profile?.userName ?: ""
        }
        val currentUserAvatar = if (isGroup(groupType)) "" else {
            val server = getServerUseCase(clientDomain, clientId)
            server?.profile?.avatar ?: ""
        }
        if (AppCall.listenerCallingState.value?.isCalling == false || AppCall.listenerCallingState.value?.isCalling == null) {
            val groupNameExactly = if (isGroup(groupType)) groupName else fromClientName
            AppCall.inComingCall(
                this, isAudioMode, rtcToken, groupId!!, groupType, groupNameExactly ?: "",
                clientDomain, clientId,
                fromClientName, avatar,
                turnUrl, turnUser, turnPass,
                webRtcGroupId, webRtcUrl,
                stunUrl, currentUserName, currentUserAvatar
            )
        } else {
            groupId?.let {
                busyCallUseCase(
                    it.toInt(),
                    Owner(clientDomain, clientId)
                )
            }
        }
    }

    private suspend fun handleNewMessage(remoteMessage: RemoteMessage) {
        val roomId = getJoiningRoomUseCase()
        val currentServer = environment.getServer()
        currentServer.serverDomain
        currentServer.profile.userId
        val data: Map<String, String> = Gson().fromJson(
            remoteMessage.data["data"], object : TypeToken<HashMap<String, String>>() {}.type
        )
        val clientId = data["client_id"] ?: ""
        val clientDomain = data["client_workspace_domain"] ?: ""
        val fromClientID = data["from_client_id"] ?: ""
        val fromDomain = data["from_client_workspace_domain"] ?: ""

        val messageId = data["id"] ?: ""
        val createdAt = data["created_at"]?.toLong() ?: 0
        val groupId = data["group_id"]?.toLong() ?: 0
        val groupType = data["group_type"] ?: ""
        val messageUTF8AsBytes = (data["message"] ?: "").toByteArray(StandardCharsets.UTF_8)
        val messageBase64 = Base64.decode(messageUTF8AsBytes, Base64.DEFAULT)
        printlnCK("Notification raw message messageUTF8AsBytes $messageUTF8AsBytes")
        printlnCK("Notification raw message messageBase64 $messageBase64")
        val messageContent: ByteString = ByteString.copyFrom(messageBase64)
        try {
            val decryptedMessage = decryptMessageUseCase(
                messageId, groupId, groupType,
                fromClientID, fromDomain,
                createdAt, createdAt,
                messageContent,
                Owner(clientDomain, clientId)
            )
            val isShowNotification = getOwnerClientIdsUseCase().contains(fromClientID)
            if (!isShowNotification && roomId != groupId) {
                val userPreference =
                    getUserPreferenceUseCase(clientDomain, clientId)
                val group = getGroupByIdUseCase(groupId, clientDomain, clientId)
                group?.data?.let {
                    val avatar = getFriendUseCase(fromClientID)?.avatar
                    showMessagingStyleNotification(
                        context = applicationContext,
                        chatGroup = group.data!!,
                        decryptedMessage,
                        userPreference ?: UserPreference.getDefaultUserPreference("", "", false),
                        avatar
                    )
                }
            }
        } catch (e: Exception) {
            printlnCK("onMessageReceived: error -> $e")
        }
    }

    private suspend fun handlerRequestAddRemoteMember(remoteMessage: RemoteMessage) {
        val data: Map<String, String> = Gson().fromJson(
            remoteMessage.data["data"], object : TypeToken<HashMap<String, String>>() {}.type
        )
        try {
            val clientId = data["client_id"] ?: ""
            val clientDomain = data["client_workspace_domain"] ?: ""
            val groupId = data["group_id"]?.toLong() ?: 0
            val removedMember = data["leave_member"] ?: ""
            printlnCK("handlerRequestAddRemoteMember clientId: $clientId  clientDomain: $clientDomain removedMember: $removedMember")
            if (!getOwnerClientIdsUseCase().contains(removedMember))
                getGroupByIdUseCase(groupId, clientDomain, clientId)
            if (removedMember.isNotEmpty()) {
                deleteFriendUseCase(removedMember)
                if (getOwnerClientIdsUseCase().contains(removedMember)) {
                    deleteMessageUseCase(groupId, clientDomain, removedMember)
                }
                if (getOwnerClientIdsUseCase().contains(removedMember)) {
                    getListClientInGroupUseCase(groupId, clientDomain)?.forEach {
                        Log.d("antx: ", "MyFirebaseMessagingService handlerRequestAddRemoteMember line = 255: $it");
                        val senderAddress2 = CKSignalProtocolAddress(
                            Owner(
                                clientDomain,
                                it
                            ),groupId, RECEIVER_DEVICE_ID
                        )
                        val senderAddress1 = CKSignalProtocolAddress(
                            Owner(
                                clientDomain,
                                it
                            ), groupId,SENDER_DEVICE_ID
                        )
                        senderKeyStore.deleteSenderKey(senderAddress2)
                        senderKeyStore.deleteSenderKey(senderAddress1)
                    }
                    deleteGroupUseCase(groupId, clientDomain, removedMember)
                } else {
                    Log.d("antx: ", "MyFirebaseMessagingService handlerRequestAddRemoteMember line = 274: ");
                    val senderAddress2 = CKSignalProtocolAddress(
                        Owner(
                            clientDomain,
                            removedMember
                        ), groupId,RECEIVER_DEVICE_ID
                    )
                    val senderAddress1 = CKSignalProtocolAddress(
                        Owner(
                            clientDomain,
                            removedMember
                        ), groupId,SENDER_DEVICE_ID
                    )

                    senderKeyStore.deleteSenderKey(senderAddress2)
                    senderKeyStore.deleteSenderKey(senderAddress1)
                    signalKeyRepository.deleteGroupSenderKey(groupId.toString(), senderAddress1.name)
                    signalKeyRepository.deleteGroupSenderKey(groupId.toString(), senderAddress2.name)

                }

            }
            if (!getOwnerClientIdsUseCase().contains(removedMember)) {
                val updateGroupIntent = Intent(ACTION_ADD_REMOVE_MEMBER)
                updateGroupIntent.putExtra(EXTRA_GROUP_ID, groupId)
                sendBroadcast(updateGroupIntent)
            }
        } catch (e: Exception) {
            printlnCK("handlerRequestAddRemoteMember Exception ${e.message}")
        }
    }

    private suspend fun handleRequestDeactiveAccount(remoteMessage: RemoteMessage) {
        fetchGroupsUseCase.invoke()
        val data: Map<String, String> = Gson().fromJson(
            remoteMessage.data["data"], object : TypeToken<HashMap<String, String>>() {}.type
        )
        val clientId = data["client_id"] ?: ""
        val clientDomain = remoteMessage.data["client_workspace_domain"] ?: ""
        val deactivatedAccountId = data["deactive_account_id"] ?: ""
        printlnCK("handleRequestDeactiveAccount clientId $clientId clientDomain $clientDomain deactivatedAccountId $deactivatedAccountId")
        disableChatOfDeactivatedUserUseCase(clientId, clientDomain, deactivatedAccountId)
    }

    private suspend fun handleCancelCall(remoteMessage: RemoteMessage) {
        val groupId = remoteMessage.data["group_id"]
        val clientId = remoteMessage.data["client_id"] ?: ""
        val clientDomain = remoteMessage.data["client_workspace_domain"] ?: ""
        if (groupId.isNullOrBlank()) {
            return
        }
        val group = getGroupByIdUseCase(groupId.toLong(), clientDomain, clientId)
        if (group != null) {
            NotificationManagerCompat.from(applicationContext)
                .cancel(null, INCOMING_NOTIFICATION_ID)
            val endIntent = Intent(ACTION_CALL_CANCEL)
            endIntent.putExtra(EXTRA_CALL_CANCEL_GROUP_ID, groupId)
            sendBroadcast(endIntent)
        }
    }

    private suspend fun handleBusyCall(remoteMessage: RemoteMessage) {
        val groupId = remoteMessage.data["group_id"]
        val clientId = remoteMessage.data["client_id"] ?: ""
        val clientDomain = remoteMessage.data["client_workspace_domain"] ?: ""
        if (groupId.isNullOrBlank()) {
            return
        }
        val group = getGroupByIdUseCase(groupId.toLong(), clientDomain, clientId)
        if (group != null) {
            val endIntent = Intent(ACTION_CALL_BUSY)
            endIntent.putExtra(EXTRA_CALL_CANCEL_GROUP_ID, groupId)
            sendBroadcast(endIntent)
        }
    }


    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }

    companion object {
        private const val TAG = "MyFirebaseService"
    }
}