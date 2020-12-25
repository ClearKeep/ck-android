package com.clearkeep.januswrapper

import android.content.Context
import android.content.Intent
import com.clearkeep.januswrapper.utils.Constants

object AppCall {
    fun call(context: Context, groupId: Long?, ourClientId: String?, userName: String?, avatar: String?, isIncomingCall: Boolean) {
        val intent = Intent(context, InCallActivity::class.java)
        intent.putExtra(Constants.EXTRA_GROUP_ID, groupId)
        intent.putExtra(Constants.EXTRA_OUR_CLIENT_ID, ourClientId)
        intent.putExtra(Constants.EXTRA_USER_NAME, userName)
        intent.putExtra(Constants.EXTRA_FROM_IN_COMING_CALL, isIncomingCall)
        intent.putExtra(Constants.EXTRA_AVATAR_USER_IN_CONVERSATION, avatar)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    fun inComingCall(context: Context, groupId: Long?, ourClientId: String?, userName: String?, avatar: String?) {
        val intent = Intent(context, InComingCallActivity::class.java)
        intent.putExtra(Constants.EXTRA_GROUP_ID, groupId)
        intent.putExtra(Constants.EXTRA_OUR_CLIENT_ID, ourClientId)
        intent.putExtra(Constants.EXTRA_USER_NAME, userName)
        intent.putExtra(Constants.EXTRA_AVATAR_USER_IN_CONVERSATION, avatar)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
}