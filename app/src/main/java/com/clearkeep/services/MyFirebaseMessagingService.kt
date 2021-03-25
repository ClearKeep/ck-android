package com.clearkeep.services

import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.clearkeep.screen.videojanus.AppCall
import com.clearkeep.utilities.ACTION_CALL_CANCEL
import com.clearkeep.utilities.EXTRA_CALL_CANCEL_GROUP_ID
import com.clearkeep.utilities.INCOMING_NOTIFICATION_ID
import com.clearkeep.utilities.UserManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import org.json.JSONObject
import javax.inject.Inject


@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var userManager: UserManager

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.w(TAG, "onMessageReceived" + remoteMessage.data.toString())

        when (remoteMessage.data["notify_type"]) {
            "request_call" -> {
                val groupId = remoteMessage.data["group_id"]
                val groupCallType = remoteMessage.data["call_type"]
                val avatar = remoteMessage.data["from_client_avatar"] ?: ""
                //val fromClientId = remoteMessage.data["from_client_id"]
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
                val isAudioMode = groupCallType == "audio"

                AppCall.inComingCall(this, isAudioMode, rtcToken, groupId!!, userManager.getClientId(), fromClientName, avatar,
                    turnUrl, turnUser, turnPass, stunUrl)
            }
            "cancel_request_call" -> {
                NotificationManagerCompat.from(this).cancel(null, INCOMING_NOTIFICATION_ID)
                val groupId = remoteMessage.data["group_id"]
                //val fromClientId = remoteMessage.data["from_client_id"]
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