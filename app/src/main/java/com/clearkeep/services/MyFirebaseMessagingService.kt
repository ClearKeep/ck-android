package com.clearkeep.services

import android.content.Intent
import android.util.Base64
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.clearkeep.db.clear_keep.model.Owner
import com.clearkeep.repo.ServerRepository
import com.clearkeep.screen.chat.repo.ChatRepository
import com.clearkeep.screen.chat.repo.GroupRepository
import com.clearkeep.screen.chat.repo.MessageRepository
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

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        printlnCK("onMessageReceived: ${remoteMessage.data}")
        handleNotification(remoteMessage)
    }

    private fun handleNotification(remoteMessage: RemoteMessage) {
        val clientId = remoteMessage.data["client_id"] ?: ""
        val clientDomain = remoteMessage.data["client_workspace_domain"] ?: ""
        GlobalScope.launch {
            val server = serverRepository.getServer(clientDomain, clientId)
            if (server != null) {
                when (remoteMessage.data["notify_type"]) {
                    "request_call" -> {
                        handleRequestCall(remoteMessage)
                    }
                    "cancel_request_call" -> {
                        handleCancelCall(remoteMessage)
                    }
                    "new_message" -> {
                        handleNewMessage(remoteMessage)
                    }
                }
            }
        }
    }

    private fun handleRequestCall(remoteMessage: RemoteMessage) {
        val clientId = remoteMessage.data["client_id"] ?: ""
        val clientDomain = remoteMessage.data["client_workspace_domain"] ?: ""
        val groupId = remoteMessage.data["group_id"]
        val groupType = remoteMessage.data["group_type"]?: ""
        val groupName = remoteMessage.data["group_name"]?: ""
        val groupCallType = remoteMessage.data["call_type"]
        val avatar = remoteMessage.data["from_client_avatar"] ?: ""
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

        val groupNameExactly = if (isGroup(groupType)) groupName else fromClientName
        AppCall.inComingCall(this, isAudioMode, rtcToken, groupId!!, groupType, groupNameExactly ?:"",
            clientId, fromClientName, avatar,
            turnUrl, turnUser, turnPass,
            webRtcGroupId, webRtcUrl,
            stunUrl)
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
        val messageContent: ByteString = ByteString.copyFrom(messageBase64)
        try {
            val decryptedMessage = messageRepository.decryptMessage(
                messageId, groupId, groupType,
                fromClientID, fromDomain,
                createdAt, createdAt,
                messageContent,
                Owner(clientDomain, clientId)
            )

            val group = groupRepository.getGroupByID(groupId, clientDomain, clientId)
            if (group != null) {
                showMessagingStyleNotification(
                    context = applicationContext,
                    chatGroup = group,
                    decryptedMessage
                )
            }
        } catch (e: Exception) {
            printlnCK("onMessageReceived: error -> $e")
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

    override fun onNewToken(token: String) {
        Log.w(TAG, "onNewToken: $token")
        printlnCK("onNewToken: $token")
        super.onNewToken(token)
    }

    companion object {
        private const val TAG = "MyFirebaseService"
    }
}