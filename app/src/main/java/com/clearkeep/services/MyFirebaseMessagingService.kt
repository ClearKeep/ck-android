package com.clearkeep.services

import android.content.Intent
import android.util.Base64
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.clearkeep.db.clear_keep.model.Owner
import com.clearkeep.db.clear_keep.model.UserPreference
import com.clearkeep.dynamicapi.Environment
import com.clearkeep.repo.ServerRepository
import com.clearkeep.screen.chat.repo.*
import com.clearkeep.screen.chat.utils.isGroup
import com.clearkeep.screen.videojanus.AppCall
import com.clearkeep.screen.videojanus.showMessagingStyleNotification
import com.clearkeep.utilities.*
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
    lateinit var chatRepository: ChatRepository

    @Inject
    lateinit var groupRepository: GroupRepository

    @Inject
    lateinit var serverRepository: ServerRepository

    @Inject
    lateinit var messageRepository: MessageRepository

    @Inject
    lateinit var userPreferenceRepository: UserPreferenceRepository

    @Inject
    lateinit var peopleRepository: PeopleRepository

    @Inject
    lateinit var videoCallRepository: VideoCallRepository

    @Inject
    lateinit var  environment: Environment


    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        printlnCK("onMessageReceived: ${remoteMessage.data}")
        handleNotification(remoteMessage)
    }

    private fun handleNotification(remoteMessage: RemoteMessage) {
        val clientId = remoteMessage.data["client_id"] ?: ""
        val clientDomain = remoteMessage.data["client_workspace_domain"] ?: ""
        printlnCK("notification  $clientId")
        GlobalScope.launch {
            val server = serverRepository.getServer(clientDomain, clientId)
            printlnCK("handleNotification server  clientDomain: ${clientDomain} clientId: $clientId")

            if (server != null) {
                environment.setUpTempDomain(server)
                when (remoteMessage.data["notify_type"]) {
                    "request_call" -> {
                        handleRequestCall(remoteMessage)
                    }
                    "cancel_request_call" -> {
                        handleCancelCall(remoteMessage)
                    }
                    "busy_request_call" ->{
                        handleBusyCall(remoteMessage)
                    }
                    "new_message" -> {
                        handleNewMessage(remoteMessage)
                    }
                    "member_leave","new_member" -> {
                        handlerRequestAddRemoteMember(remoteMessage)
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
        val groupType = remoteMessage.data["group_type"]?: ""
        val groupName = remoteMessage.data["group_name"]?: ""
        val groupCallType = remoteMessage.data["call_type"]
        var avatar = remoteMessage.data["from_client_avatar"] ?: ""
        val fromClientName = remoteMessage.data["from_client_name"]
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
        if(!isGroup(groupType)){
            peopleRepository.getFriendFromID(clientId)?.avatar?.let {
                avatar=it
            }
        }
        val currentUserName = if (isGroup(groupType)) "" else {
            val server = serverRepository.getServer(clientDomain, clientId)
            server?.profile?.userName ?: ""
        }
        if (AppCall.listenerCallingState.value?.isCalling == false || AppCall.listenerCallingState.value?.isCalling == null) {
            val groupNameExactly = if (isGroup(groupType)) groupName else fromClientName
            AppCall.inComingCall(this, isAudioMode, rtcToken, groupId!!, groupType, groupNameExactly ?:"",
                clientDomain, clientId,
                fromClientName, avatar,
                turnUrl, turnUser, turnPass,
                webRtcGroupId, webRtcUrl,
                stunUrl, currentUserName)
        }else {
            groupId?.let { videoCallRepository.busyCall(it.toInt(), Owner(clientDomain, clientId)) }
        }
    }

    private suspend fun handleNewMessage(remoteMessage: RemoteMessage) {
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
            val decryptedMessage = messageRepository.decryptMessage(
                messageId, groupId, groupType,
                fromClientID, fromDomain,
                createdAt, createdAt,
                messageContent,
                Owner(clientDomain, clientId)
            )
            val isShowNotification = serverRepository.getOwnerClientIds().contains(fromClientID)
            if (!isShowNotification) {
                val userPreference =
                    userPreferenceRepository.getUserPreference(clientDomain, clientId)
                val group = groupRepository.getGroupByID(groupId, clientDomain, clientId)
                group?.data?.let {
                    val avatar = peopleRepository.getFriendFromID(fromClientID)?.avatar
                    showMessagingStyleNotification(
                        context = applicationContext,
                        chatGroup = group.data,
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
            printlnCK("handlerRequestAddRemoteMember clientId: $clientId  clientDomain: $clientDomain")
                groupRepository.getGroupFromAPIById(groupId, clientDomain, clientId)
            if (removedMember.isNotEmpty()) {
                groupRepository.removeGroupOnWorkSpace(groupId, clientDomain, removedMember)
                peopleRepository.deleteFriend(removedMember)
            }
            val updateGroupIntent = Intent(ACTION_ADD_REMOVE_MEMBER)
            updateGroupIntent.putExtra(EXTRA_GROUP_ID, groupId)
            sendBroadcast(updateGroupIntent)
        } catch (e: Exception) {
            printlnCK("handlerRequestAddRemoteMember Exception ${e.message}")
        }
    }

    private suspend fun handleCancelCall(remoteMessage: RemoteMessage) {
        val groupId = remoteMessage.data["group_id"]
        val clientId = remoteMessage.data["client_id"] ?: ""
        val clientDomain = remoteMessage.data["client_workspace_domain"] ?: ""
        if (groupId.isNullOrBlank()) {
            return
        }
        val group = groupRepository.getGroupByID(groupId.toLong(), clientDomain, clientId)
        if (group != null) {
            NotificationManagerCompat.from(applicationContext).cancel(null, INCOMING_NOTIFICATION_ID)
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
        val group = groupRepository.getGroupByID(groupId.toLong(), clientDomain, clientId)
        if (group != null) {
            val endIntent = Intent(ACTION_CALL_BUSY)
            endIntent.putExtra(EXTRA_CALL_CANCEL_GROUP_ID, groupId)
            sendBroadcast(endIntent)
        }
    }


    override fun onNewToken(token: String) {
        Log.w(TAG, "onNewToken: $token")

        printlnCK("onNewToken: $token")
        super.onNewToken(token)
    }

    companion object {
        private const val TAG = "MyFirebaseService"
    }
}