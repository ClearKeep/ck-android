package com.clearkeep.services

import android.util.Log
import com.clearkeep.januswrapper.AppCall
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
        Log.w("Test", "onMessageReceived" + remoteMessage.data.toString())
        val groupId = remoteMessage.data["group_id"]
        val avatar = remoteMessage.data["from_client_avatar"]
        val fromClientId = remoteMessage.data["from_client_id"]
        val fromClientName = remoteMessage.data["from_client_name"]
        if (groupId != null) {
            AppCall.inComingCall(this, groupId.toLong(), userManager.getClientId(), fromClientName, avatar)
        }
    }

    override fun onNewToken(token: String) {
        Log.w("Test", "onNewToken" + token)
        super.onNewToken(token)
    }
}