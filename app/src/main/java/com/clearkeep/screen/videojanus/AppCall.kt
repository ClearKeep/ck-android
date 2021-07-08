package com.clearkeep.screen.videojanus

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.NotificationTarget
import com.clearkeep.R
import com.clearkeep.screen.chat.utils.isGroup
import com.clearkeep.screen.videojanus.peer_to__peer.InCallPeerToPeerActivity
import com.clearkeep.utilities.*


object AppCall {
    var listenerCallingState = MutableLiveData<CallingStateData>()

    fun call(context: Context,
             isAudioMode: Boolean,
             token: String?,
             groupId: String?, groupType: String, groupName: String,
             domain: String, ownerClientId: String,
             userName: String?, avatar: String?,
             isIncomingCall: Boolean,
             turnUrl: String = "", turnUser: String = "", turnPass: String = "",
             webRtcGroupId: String = "", webRtcUrl: String = "",
             stunUrl: String = ""
    ) {
        val intent: Intent = if (isGroup(groupType)) {
            Intent(context, InCallActivity::class.java)
        } else {
            Intent(context, InCallPeerToPeerActivity::class.java)
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

    fun inComingCall(context: Context,
                     isAudioMode: Boolean,
                     token: String,
                     groupId: String, groupType: String, groupName: String,
                     ownerDomain: String, ownerClientId: String,
                     userName: String?, avatar: String?,
                     turnUrl: String, turnUser: String, turnPass: String,
                     webRtcGroupId: String, webRtcUrl: String,
                     stunUrl: String) {
        printlnCK("token = $token, groupID = $groupId, turnURL= $turnUrl, turnUser=$turnUser, turnPass= $turnPass, stunUrl = $stunUrl")
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = INCOMING_CHANNEL_ID
        val channelName = INCOMING_CHANNEL_NAME
        val notificationId = INCOMING_NOTIFICATION_ID

        val dismissIntent = Intent(context, DismissNotificationReceiver::class.java)
        dismissIntent.action=ACTION_CALL_CANCEL
        dismissIntent.putExtra(EXTRA_CALL_CANCEL_GROUP_ID, groupId)
        dismissIntent.putExtra(EXTRA_CALL_CANCEL_GROUP_TYPE, groupType)
        dismissIntent.putExtra(EXTRA_OWNER_DOMAIN, ownerDomain)
        dismissIntent.putExtra(EXTRA_OWNER_CLIENT, ownerClientId)
        val dismissPendingIntent = PendingIntent.getBroadcast(context, 0, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val waitIntent = createIncomingCallIntent(context, isAudioMode, token, groupId, groupType, groupName,
            ownerDomain, ownerClientId, userName, avatar, turnUrl, turnUser, turnPass, webRtcGroupId, webRtcUrl, stunUrl, true)
        val pendingWaitIntent = PendingIntent.getActivity(context, 0, waitIntent,
            PendingIntent.FLAG_UPDATE_CURRENT)

        val inCallIntent = createIncomingCallIntent(context, isAudioMode, token, groupId, groupType, groupName,
            ownerDomain, ownerClientId,
            userName, avatar, turnUrl, turnUser, turnPass, webRtcGroupId, webRtcUrl, stunUrl, false)
        val pendingInCallIntent = PendingIntent.getActivity(context, 0, inCallIntent,
            PendingIntent.FLAG_UPDATE_CURRENT)

        val titleCall = if (isGroup(groupType)) context.resources.getString(R.string.notification_incoming_group)
        else context.resources.getString(R.string.notification_incoming_peer)

        val headsUpLayout = RemoteViews(context.packageName, R.layout.notification_call)
        headsUpLayout.apply {
            setViewVisibility(R.id.imageButton, if (isGroup(groupType)) View.GONE else View.VISIBLE)
            setTextViewText(R.id.tvCallFrom, titleCall)
            setTextViewText(R.id.tvCallGroupName, groupName)
            setOnClickPendingIntent(R.id.tvDecline, dismissPendingIntent)
            setOnClickPendingIntent(R.id.tvAnswer, pendingInCallIntent)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            var channel = notificationManager.getNotificationChannel(channelId)
            if (channel == null) {
                val attributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()
                val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
                channel.setSound(notification, attributes)
                notificationManager.createNotificationChannel(channel)
            }
        }
        val builder: NotificationCompat.Builder = NotificationCompat.Builder(context, channelId)
        builder.setSmallIcon(R.drawable.ic_logo)
            /*.setCustomContentView(headsUpLayout)
            .setCustomBigContentView(headsUpLayout)*/
            .setCustomHeadsUpContentView(headsUpLayout)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText("$userName calling ${if(isAudioMode) "audio" else "video"}")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(pendingWaitIntent, true)
            .setAutoCancel(true)
            .setTimeoutAfter(60 * 1000)
            .setOngoing(true)
        val notification: Notification = builder.build()

        if (!isGroup(groupType)) {
            val target = NotificationTarget(
                context,
                R.id.imageButton,
                headsUpLayout,
                notification,
                notificationId
            )
            Glide.with(context.applicationContext)
                .asBitmap()
                .circleCrop()
                .load("https://i.ibb.co/WBKb3zf/Thumbnail.png")
                .into(target)
        }

        notificationManager.notify(notificationId, notification)
    }

    private fun createIncomingCallIntent(context: Context, isAudioMode: Boolean, token: String,
                                         groupId: String?, groupType: String, groupName: String,
                                         domain: String, ownerClientId: String,
                                         userName: String?, avatar: String?,
                                         turnUrl: String, turnUser: String, turnPass: String,
                                         webRtcGroupId: String, webRtcUrl: String,
                                         stunUrl: String, isWaitingScreen: Boolean): Intent {
        val intent = if (isWaitingScreen) {
            Intent(context, InComingCallActivity::class.java)
        } else {
             if (isGroup(groupType)) {
                 Intent(context, InCallActivity::class.java)
            } else {
                 Intent(context, InCallPeerToPeerActivity::class.java)
             }
        }
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
        return intent
    }

    fun isCallAvailable(context: Context): Boolean {
        /*return isServiceRunning(context, InCallForegroundService::class.java.name)*/
        return false
    }

    fun openCallAvailable(context: Context, isPeer: Boolean) {
        val intent = if (isPeer) {
            Intent(context, InCallPeerToPeerActivity::class.java)
        } else {
            Intent(context, InCallActivity::class.java)
        }
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
}
data class CallingStateData(
    val isCalling: Boolean,
    val nameInComeCall: String? = "",
    val isCallPeer: Boolean = false,
    val timeStarted: Long
)