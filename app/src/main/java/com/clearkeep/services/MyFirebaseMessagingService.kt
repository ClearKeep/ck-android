package com.clearkeep.services

import android.util.Base64
import android.util.Log
import com.clearkeep.db.clear_keep.model.Owner
import com.clearkeep.screen.chat.repo.ChatRepository
import com.clearkeep.repo.MultiServerRepository
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
import kotlinx.coroutines.async
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
    lateinit var groupRepository: MultiServerRepository

    @Inject
    lateinit var messageRepository: MessageRepository

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        printlnCK("onMessageReceived: ${remoteMessage.data}")

        when (remoteMessage.data["notify_type"]) {
            "request_call" -> {
                handleRequestCall(remoteMessage)
            }
            "cancel_request_call" -> {
                val groupId = remoteMessage.data["group_id"]
                if (!groupId.isNullOrBlank()) {
                    handleCancelCall(groupId)
                }
            }
            "new_message" -> {
                handleNewMessage(remoteMessage)
            }
        }
    }

    private fun handleRequestCall(remoteMessage: RemoteMessage) {
        val toClientId = remoteMessage.data["client_id"]
        /*if (toClientId == userManager.getClientId()) {*/
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
                toClientId, fromClientName, avatar,
                turnUrl, turnUser, turnPass,
                webRtcGroupId, webRtcUrl,
                stunUrl)
        /*}*/
    }

    private fun handleNewMessage(remoteMessage: RemoteMessage) {
        val toClientId = remoteMessage.data["client_id"]
        val data: Map<String, String> = Gson().fromJson(
            remoteMessage.data["data"], object : TypeToken<HashMap<String, String>>() {}.type
        )
        /*if (toClientId == userManager.getClientId()) {*/
            /*val id = data["id"] ?: ""
            val clientId = data["client_id"] ?: ""
        val domain = data["domain"] ?: ""
            val createdAt = data["created_at"]?.toLong() ?: 0
            val fromClientID = data["from_client_id"] ?: ""
        val fromDomain = data["from_domain"] ?: ""
            val groupId = data["group_id"]?.toLong() ?: 0
            val groupType = data["group_type"] ?: ""
            val messageUTF8AsBytes = (data["message"] ?: "").toByteArray(StandardCharsets.UTF_8)
            val messageBase64 = Base64.decode(messageUTF8AsBytes, Base64.DEFAULT)
            val messageContent: ByteString = ByteString.copyFrom(messageBase64)
            GlobalScope.launch {
                try {
                    val decryptedMessage = messageRepository.decryptMessage(
                        id, groupId, groupType,
                        fromClientID, fromDomain,
                        clientId, createdAt, createdAt,
                        messageContent,
                        Owner(domain, clientId)
                    )

                    Log.i(TAG, "onMessageReceived: decrypted -> ${decryptedMessage.message}")

                    val groupAsyncRes = async { groupRepository.getGroupByID(groupId) }
                    val group = groupAsyncRes.await()
                    group?.let {
                        showMessagingStyleNotification(
                            context = applicationContext,
                            chatGroup = it,
                            decryptedMessage
                        )
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "onMessageReceived: error -> $e")
                }
            }*/
        /*}*/
    }

    private fun handleCancelCall(groupId: String) {
        /*GlobalScope.launch {
            val groupAsyncRes = async { groupRepository.getGroupByID(groupId.toLong()) }
            val group = groupAsyncRes.await()
            if (group != null ) {
                NotificationManagerCompat.from(applicationContext).cancel(null, INCOMING_NOTIFICATION_ID)
                val endIntent = Intent(ACTION_CALL_CANCEL)
                endIntent.putExtra(EXTRA_CALL_CANCEL_GROUP_ID, groupId)
                sendBroadcast(endIntent)
            }
        }*/
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