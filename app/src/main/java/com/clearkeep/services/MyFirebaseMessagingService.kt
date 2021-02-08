package com.clearkeep.services

import android.util.Log
import com.clearkeep.screen.videojanus.AppCall
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
        val notifyType = remoteMessage.data["notify_type"] ?: ""

        if (!notifyType.isNullOrBlank() && notifyType == "request_call") {
            val groupId = remoteMessage.data["group_id"]
            val avatar = remoteMessage.data["from_client_avatar"] ?: ""
            val fromClientId = remoteMessage.data["from_client_id"]
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

            AppCall.inComingCall(this, rtcToken, groupId!!, userManager.getClientId(), fromClientName, "",
                    turnUrl, turnUser, turnPass, stunUrl)
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