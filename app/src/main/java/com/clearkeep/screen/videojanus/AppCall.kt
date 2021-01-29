package com.clearkeep.screen.videojanus

import android.content.Context
import android.content.Intent
import android.util.Log
import com.clearkeep.utilities.*

object AppCall {
    fun call(context: Context, token: String, groupId: Long?, ourClientId: String?, userName: String?, avatar: String?, isIncomingCall: Boolean) {
        printlnCK("token = $token, groupID = $groupId")
        val intent = Intent(context, InCallActivity::class.java)
        intent.putExtra(EXTRA_GROUP_ID, groupId)
        intent.putExtra(EXTRA_GROUP_TOKEN, token)
        intent.putExtra(EXTRA_OUR_CLIENT_ID, ourClientId)
        intent.putExtra(EXTRA_USER_NAME, userName)
        intent.putExtra(EXTRA_FROM_IN_COMING_CALL, isIncomingCall)
        intent.putExtra(EXTRA_AVATAR_USER_IN_CONVERSATION, avatar)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    fun inComingCall(context: Context, token: String, groupId: Long?, ourClientId: String?, userName: String?, avatar: String?,
                     turnUser: String?, turnPass: String?) {
        printlnCK("token = $token, groupID = $groupId, turnUser=$turnUser, turnPass= $turnPass")
        val intent = Intent(context, InComingCallActivity::class.java)
        intent.putExtra(EXTRA_GROUP_ID, groupId)
        intent.putExtra(EXTRA_GROUP_TOKEN, token)
        intent.putExtra(EXTRA_OUR_CLIENT_ID, ourClientId)
        intent.putExtra(EXTRA_USER_NAME, userName)
        intent.putExtra(EXTRA_AVATAR_USER_IN_CONVERSATION, avatar)
        intent.putExtra(EXTRA_TURN_USER_NAME, turnUser)
        intent.putExtra(EXTRA_TURN_PASS, turnPass)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    fun isCallAvailable(context: Context): Boolean {
        /*return isServiceRunning(context, InCallForegroundService::class.java.name)*/
        return false
    }

    fun openCallAvailable(context: Context) {
        val intent = Intent(context, InCallActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
}