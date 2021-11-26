package com.clearkeep.navigation

import android.content.Context
import android.content.Intent
import com.clearkeep.common.utilities.*
import kotlin.system.exitProcess

object NavigationUtils {
    fun navigateToHomeActivity(context: Context) {
        val intent = Intent(
            context,
            Class.forName("com.clearkeep.features.chat.presentation.home.MainActivity")
        )
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    fun navigateToStartActivity(context: Context) {
        val intent = Intent(
            context,
            Class.forName("com.clearkeep.features.auth.presentation.login.LoginActivity")
        )
        context.startActivity(intent)
    }

    fun navigateToSplashActivity(context: Context) {
        val splashActivity =
            Class.forName("com.clearkeep.features.splash.presentation.SplashActivity")
        val intent = Intent(context, splashActivity)
        intent.flags =
            Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    fun restartToRoot(context: Context) {
        val splashActivity =
            Class.forName("com.clearkeep.features.splash.presentation.SplashActivity")
        val intent = Intent(context, splashActivity)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
        exitProcess(2)
    }

    fun navigateToAvailableCall(context: Context, isPeer: Boolean) {
        val peerCallActivity =
            Class.forName("com.clearkeep.features.calls.presentation.peertopeer.InCallPeerToPeerActivity")
        val groupCallActivity =
            Class.forName("com.clearkeep.features.calls.presentation.InCallActivity")

        val intent = if (isPeer) {
            println("AppCall openCallAvailable inCallPeerToPeer opened")
            Intent(
                context,
                peerCallActivity
            )
        } else {
            Intent(context, groupCallActivity)
        }
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    fun navigateToJoinServer(context: Context, domain: String) {
        val loginActivity =
            Class.forName("com.clearkeep.presentation.screen.auth.login.LoginActivity")

        val intent = Intent(context, loginActivity)
        intent.putExtra(NAVIGATE_LOGIN_ACTIVITY_IS_JOIN_SERVER, true)
        intent.putExtra(NAVIGATE_LOGIN_ACTIVITY_SERVER_DOMAIN, domain)
    }

    fun startChatService(context: Context) {
        val chatService = getChatServiceRef()

        Intent(context, chatService).also { intent ->
            context.startService(intent)
        }
    }

    fun stopChatService(context: Context) {
        val chatService = getChatServiceRef()

        Intent(context, chatService).also { intent ->
            context.stopService(intent)
        }
    }

    fun getChatServiceRef(): Class<*> {
        return Class.forName("com.clearkeep.data.services.ChatService")
    }

    fun navigateToCall(
        context: Context,
        isAudioMode: Boolean,
        token: String?,
        groupId: String?, groupType: String, groupName: String,
        domain: String, ownerClientId: String,
        userName: String?, avatar: String?,
        isIncomingCall: Boolean,
        turnUrl: String = "", turnUser: String = "", turnPass: String = "",
        webRtcGroupId: String = "", webRtcUrl: String = "",
        stunUrl: String = "",
        currentUserName: String = "",
        currentUserAvatar: String = ""
    ) {
        val peerCallActivity =
            Class.forName("com.clearkeep.features.calls.presentation.peertopeer.InCallPeerToPeerActivity")
        val groupCallActivity =
            Class.forName("com.clearkeep.features.calls.presentation.InCallActivity")

        val intent: Intent = if (isGroup(groupType)) {
            Intent(context, groupCallActivity)
        } else {
            println("AppCall call  inCallPeerToPeer opened")
            Intent(
                context,
                peerCallActivity
            )
        }
        intent.putExtra(EXTRA_GROUP_ID, groupId)
        intent.putExtra(EXTRA_GROUP_TOKEN, token)
        intent.putExtra(EXTRA_GROUP_NAME, groupName)
        intent.putExtra(EXTRA_GROUP_TYPE, groupType)
        intent.putExtra(EXTRA_IS_AUDIO_MODE, isAudioMode)
        intent.putExtra(EXTRA_OWNER_DOMAIN, domain)
        intent.putExtra(EXTRA_OWNER_CLIENT, ownerClientId)
        intent.putExtra(EXTRA_USER_NAME, userName)
        intent.putExtra(EXTRA_FROM_IN_COMING_CALL, isIncomingCall)
        intent.putExtra(EXTRA_AVATAR_USER_IN_CONVERSATION, avatar)
        intent.putExtra(EXTRA_WEB_RTC_GROUP_ID, webRtcGroupId)
        intent.putExtra(EXTRA_WEB_RTC_URL, webRtcUrl)
        intent.putExtra(EXTRA_CURRENT_USERNAME, currentUserName)
        intent.putExtra(EXTRA_CURRENT_USER_AVATAR, currentUserAvatar)
        if (turnUrl.isNotEmpty()) {
            intent.putExtra(EXTRA_TURN_URL, turnUrl)
            intent.putExtra(EXTRA_TURN_USER_NAME, turnUser)
            intent.putExtra(EXTRA_TURN_PASS, turnPass)
        }
        if (stunUrl.isNotEmpty()) {
            intent.putExtra(EXTRA_STUN_URL, stunUrl)
        }
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    fun createIncomingCallIntent(
        context: Context,
        isAudioMode: Boolean,
        token: String,
        groupId: String?,
        groupType: String,
        groupName: String,
        domain: String,
        ownerClientId: String,
        userName: String?,
        avatar: String?,
        turnUrl: String,
        turnUser: String,
        turnPass: String,
        webRtcGroupId: String,
        webRtcUrl: String,
        stunUrl: String,
        isWaitingScreen: Boolean,
        currentUserName: String,
        currentUserAvatar: String
    ): Intent {
        val incomingCallActivity =
            Class.forName("com.clearkeep.features.calls.presentation.InComingCallActivity")
        val intent = Intent(context, incomingCallActivity)
        intent.putExtra(EXTRA_GROUP_ID, groupId)
        intent.putExtra(EXTRA_GROUP_NAME, groupName)
        intent.putExtra(EXTRA_GROUP_TYPE, groupType)
        intent.putExtra(EXTRA_GROUP_TOKEN, token)
        intent.putExtra(EXTRA_IS_AUDIO_MODE, isAudioMode)
        intent.putExtra(EXTRA_OWNER_DOMAIN, domain)
        intent.putExtra(EXTRA_OWNER_CLIENT, ownerClientId)
        intent.putExtra(EXTRA_USER_NAME, userName)
        intent.putExtra(EXTRA_FROM_IN_COMING_CALL, true)
        intent.putExtra(EXTRA_AVATAR_USER_IN_CONVERSATION, avatar)
        intent.putExtra(EXTRA_TURN_URL, turnUrl)
        intent.putExtra(EXTRA_TURN_USER_NAME, turnUser)
        intent.putExtra(EXTRA_TURN_PASS, turnPass)
        intent.putExtra(EXTRA_WEB_RTC_GROUP_ID, webRtcGroupId)
        intent.putExtra(EXTRA_WEB_RTC_URL, webRtcUrl)
        intent.putExtra(EXTRA_STUN_URL, stunUrl)
        intent.putExtra(EXTRA_CURRENT_USERNAME, currentUserName)
        intent.putExtra(EXTRA_CURRENT_USER_AVATAR, currentUserAvatar)
        return intent
    }

    fun createInCallIntent(
        context: Context,
        isAudioMode: Boolean,
        token: String,
        groupId: String?,
        groupType: String,
        groupName: String,
        domain: String,
        ownerClientId: String,
        userName: String?,
        avatar: String?,
        turnUrl: String,
        turnUser: String,
        turnPass: String,
        webRtcGroupId: String,
        webRtcUrl: String,
        stunUrl: String,
        isWaitingScreen: Boolean,
        currentUserName: String,
        currentUserAvatar: String
    ): Intent {
        val incomingCallActivity =
            Class.forName("com.clearkeep.features.calls.presentation.InCallActivity")
        val intent = Intent(context, incomingCallActivity)
        intent.putExtra(EXTRA_GROUP_ID, groupId)
        intent.putExtra(EXTRA_GROUP_NAME, groupName)
        intent.putExtra(EXTRA_GROUP_TYPE, groupType)
        intent.putExtra(EXTRA_GROUP_TOKEN, token)
        intent.putExtra(EXTRA_IS_AUDIO_MODE, isAudioMode)
        intent.putExtra(EXTRA_OWNER_DOMAIN, domain)
        intent.putExtra(EXTRA_OWNER_CLIENT, ownerClientId)
        intent.putExtra(EXTRA_USER_NAME, userName)
        intent.putExtra(EXTRA_FROM_IN_COMING_CALL, true)
        intent.putExtra(EXTRA_AVATAR_USER_IN_CONVERSATION, avatar)
        intent.putExtra(EXTRA_TURN_URL, turnUrl)
        intent.putExtra(EXTRA_TURN_USER_NAME, turnUser)
        intent.putExtra(EXTRA_TURN_PASS, turnPass)
        intent.putExtra(EXTRA_WEB_RTC_GROUP_ID, webRtcGroupId)
        intent.putExtra(EXTRA_WEB_RTC_URL, webRtcUrl)
        intent.putExtra(EXTRA_STUN_URL, stunUrl)
        intent.putExtra(EXTRA_CURRENT_USERNAME, currentUserName)
        intent.putExtra(EXTRA_CURRENT_USER_AVATAR, currentUserAvatar)
        return intent
    }

    fun createInCallPeerToPeerIntent(
        context: Context,
        isAudioMode: Boolean,
        token: String,
        groupId: String?,
        groupType: String,
        groupName: String,
        domain: String,
        ownerClientId: String,
        userName: String?,
        avatar: String?,
        turnUrl: String,
        turnUser: String,
        turnPass: String,
        webRtcGroupId: String,
        webRtcUrl: String,
        stunUrl: String,
        isWaitingScreen: Boolean,
        currentUserName: String,
        currentUserAvatar: String
    ): Intent {
        val incomingCallActivity =
            Class.forName("com.clearkeep.features.calls.presentation.peertopeer.InCallPeerToPeerActivity")
        val intent = Intent(context, incomingCallActivity)
        intent.putExtra(EXTRA_GROUP_ID, groupId)
        intent.putExtra(EXTRA_GROUP_NAME, groupName)
        intent.putExtra(EXTRA_GROUP_TYPE, groupType)
        intent.putExtra(EXTRA_GROUP_TOKEN, token)
        intent.putExtra(EXTRA_IS_AUDIO_MODE, isAudioMode)
        intent.putExtra(EXTRA_OWNER_DOMAIN, domain)
        intent.putExtra(EXTRA_OWNER_CLIENT, ownerClientId)
        intent.putExtra(EXTRA_USER_NAME, userName)
        intent.putExtra(EXTRA_FROM_IN_COMING_CALL, true)
        intent.putExtra(EXTRA_AVATAR_USER_IN_CONVERSATION, avatar)
        intent.putExtra(EXTRA_TURN_URL, turnUrl)
        intent.putExtra(EXTRA_TURN_USER_NAME, turnUser)
        intent.putExtra(EXTRA_TURN_PASS, turnPass)
        intent.putExtra(EXTRA_WEB_RTC_GROUP_ID, webRtcGroupId)
        intent.putExtra(EXTRA_WEB_RTC_URL, webRtcUrl)
        intent.putExtra(EXTRA_STUN_URL, stunUrl)
        intent.putExtra(EXTRA_CURRENT_USERNAME, currentUserName)
        intent.putExtra(EXTRA_CURRENT_USER_AVATAR, currentUserAvatar)
        return intent
    }

    fun getNotificationClickIntent(
        context: Context,
        groupId: Long,
        ownerDomain: String,
        ownerClientId: String,
        clearTempMessage: Boolean
    ): Intent {
        val targetActivity =
            Class.forName("com.clearkeep.presentation.screen.chat.room.RoomActivity")
        val intent = Intent(context, targetActivity)
        intent.putExtra(NAVIGATE_ROOM_ACTIVITY_GROUP_ID, groupId)
        intent.putExtra(NAVIGATE_ROOM_ACTIVITY_DOMAIN, ownerDomain)
        intent.putExtra(NAVIGATE_ROOM_ACTIVITY_CLIENT_ID, ownerClientId)
        intent.putExtra(NAVIGATE_ROOM_ACTIVITY_CLEAR_TEMP_MESSAGE, clearTempMessage)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        return intent
    }

    fun getMainActivityRef(): Class<*> {
        return Class.forName("com.clearkeep.presentation.screen.chat.home.MainActivity")
    }

    const val NAVIGATE_ROOM_ACTIVITY_GROUP_ID = "room_id"
    const val NAVIGATE_ROOM_ACTIVITY_DOMAIN = "domain"
    const val NAVIGATE_ROOM_ACTIVITY_CLIENT_ID = "client_id"
    const val NAVIGATE_ROOM_ACTIVITY_FRIEND_ID = "remote_id"
    const val NAVIGATE_ROOM_ACTIVITY_FRIEND_DOMAIN = "remote_domain"
    const val NAVIGATE_ROOM_ACTIVITY_IS_NOTE = "is_note"
    const val NAVIGATE_ROOM_ACTIVITY_CLEAR_TEMP_MESSAGE = "clear_temp_message"

    const val NAVIGATE_LOGIN_ACTIVITY_IS_JOIN_SERVER = "is_join_server"
    const val NAVIGATE_LOGIN_ACTIVITY_SERVER_DOMAIN = "server_url_join"
}