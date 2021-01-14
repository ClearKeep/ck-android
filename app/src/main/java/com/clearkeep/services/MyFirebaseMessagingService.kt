package com.clearkeep.services

import android.util.Log
import com.clearkeep.screen.videojanus.AppCall
import com.clearkeep.utilities.UserManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var userManager: UserManager

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.w(TAG, "onMessageReceived" + remoteMessage.data.toString())
        val groupId = remoteMessage.data["group_id"]
        val avatar = remoteMessage.data["from_client_avatar"] ?: ""
        val fromClientId = remoteMessage.data["from_client_id"]
        val fromClientName = remoteMessage.data["from_client_name"]
        val rtcToken = remoteMessage.data["group_rtc_token"] ?: ""
        if (groupId != null && !rtcToken.isNullOrEmpty()) {
            AppCall.inComingCall(this, rtcToken, groupId.toLong(), userManager.getClientId(), fromClientName, "")
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