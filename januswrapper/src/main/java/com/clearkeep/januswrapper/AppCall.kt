package com.clearkeep.januswrapper

import android.content.Context
import android.content.Intent
import android.util.Log
import com.clearkeep.januswrapper.services.InCallForegroundService
import com.clearkeep.januswrapper.utils.Constants
import com.clearkeep.januswrapper.utils.Utils
import com.koushikdutta.async.Util

object AppCall {
    fun call(context: Context, token: String, groupId: Long?, ourClientId: String?, userName: String?, avatar: String?, isIncomingCall: Boolean) {
        Log.i("Test", "token = $token, groupID = $groupId")
        val intent = Intent(context, InCallActivity::class.java)
        intent.putExtra(Constants.EXTRA_GROUP_ID, groupId)
        intent.putExtra(Constants.EXTRA_GROUP_TOKEN, token)
        intent.putExtra(Constants.EXTRA_OUR_CLIENT_ID, ourClientId)
        intent.putExtra(Constants.EXTRA_USER_NAME, userName)
        intent.putExtra(Constants.EXTRA_FROM_IN_COMING_CALL, isIncomingCall)
        intent.putExtra(Constants.EXTRA_AVATAR_USER_IN_CONVERSATION, avatar)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    fun inComingCall(context: Context, token: String, groupId: Long?, ourClientId: String?, userName: String?, avatar: String?) {
        val intent = Intent(context, InComingCallActivity::class.java)
        intent.putExtra(Constants.EXTRA_GROUP_ID, groupId)
        intent.putExtra(Constants.EXTRA_GROUP_TOKEN, token)
        intent.putExtra(Constants.EXTRA_OUR_CLIENT_ID, ourClientId)
        intent.putExtra(Constants.EXTRA_USER_NAME, userName)
        intent.putExtra(Constants.EXTRA_AVATAR_USER_IN_CONVERSATION, avatar)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    fun isCallAvailable(context: Context): Boolean {
        return Utils.isServiceRunning(context, InCallForegroundService::class.java.name)
    }

    fun openCallAvailable(context: Context) {
        val intent = Intent(context, InCallActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
}