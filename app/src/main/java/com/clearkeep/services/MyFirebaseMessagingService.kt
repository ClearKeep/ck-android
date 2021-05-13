package com.clearkeep.services

import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.clearkeep.repo.GroupRepository
import com.clearkeep.screen.chat.utils.isGroup
import com.clearkeep.screen.videojanus.AppCall
import com.clearkeep.utilities.*
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject


@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var userManager: UserManager

    @Inject
    lateinit var groupRepository: GroupRepository

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.w(TAG, "onMessageReceived" + remoteMessage.data.toString())

        when (remoteMessage.data["notify_type"]) {
            "request_call" -> {
                val toClientId = remoteMessage.data["client_id"]
                if (toClientId == userManager.getClientId()) {
                    val groupId = remoteMessage.data["group_id"]
                    val groupType = remoteMessage.data["group_type"]?: ""
                    val groupName = remoteMessage.data["group_name"]?: ""
                    val groupCallType = remoteMessage.data["call_type"]
                    val avatar = remoteMessage.data["from_client_avatar"] ?: ""
                    val fromClientName = remoteMessage.data["from_client_name"]
                    val rtcToken = remoteMessage.data["group_rtc_token"] ?: ""

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
                    AppCall.inComingCall(this, isAudioMode, rtcToken, groupId!!, groupType, groupNameExactly ?:"", userManager.getClientId(), fromClientName, avatar,
                        turnUrl, turnUser, turnPass, stunUrl)
                }
            }
            "cancel_request_call" -> {
                val groupId = remoteMessage.data["group_id"]
                if (!groupId.isNullOrBlank()) {
                    handleCancelCall(groupId)
                }
            }
        }
    }

    private fun handleCancelCall(groupId: String) {
        GlobalScope.launch {
            val groupAsyncRes = async { groupRepository.getGroupByID(groupId.toLong()) }
            val group = groupAsyncRes.await()
            if (group != null && !group.isGroup()) {
                NotificationManagerCompat.from(applicationContext).cancel(null, INCOMING_NOTIFICATION_ID)
                val endIntent = Intent(ACTION_CALL_CANCEL)
                endIntent.putExtra(EXTRA_CALL_CANCEL_GROUP_ID, groupId)
                sendBroadcast(endIntent)
            }
        }
    }

    override fun onNewToken(token: String) {
        Log.w(TAG, "onNewToken: $token")
        super.onNewToken(token)
    }

    companion object {
        private const val TAG = "MyFirebaseService"
    }
}