package com.clearkeep.features.shared.presentation

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
import com.clearkeep.common.utilities.*
import com.clearkeep.features.shared.DismissNotificationReceiver
import com.clearkeep.features.shared.R
import com.clearkeep.navigation.NavigationUtils

object AppCall {
    var listenerCallingState = MutableLiveData<CallingStateData>()

    fun inComingCall(
        context: Context,
        isAudioMode: Boolean,
        token: String,
        groupId: String, groupType: String, groupName: String,
        ownerDomain: String, ownerClientId: String,
        userName: String?, avatar: String?,
        turnUrl: String, turnUser: String, turnPass: String,
        webRtcGroupId: String, webRtcUrl: String,
        stunUrl: String,
        currentUserName: String = "",
        currentUserAvatar: String = ""
    ) {
        printlnCK("token = $token, groupID = $groupId, turnURL= $turnUrl, turnUser=$turnUser, turnPass= $turnPass, stunUrl = $stunUrl")
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = INCOMING_CHANNEL_ID
        val channelName = INCOMING_CHANNEL_NAME
        val notificationId = INCOMING_NOTIFICATION_ID

        val dismissIntent = Intent(context, DismissNotificationReceiver::class.java)
        dismissIntent.action = ACTION_CALL_CANCEL
        dismissIntent.putExtra(EXTRA_CALL_CANCEL_GROUP_ID, groupId)
        dismissIntent.putExtra(EXTRA_CALL_CANCEL_GROUP_TYPE, groupType)
        dismissIntent.putExtra(EXTRA_OWNER_DOMAIN, ownerDomain)
        dismissIntent.putExtra(EXTRA_OWNER_CLIENT, ownerClientId)
        val dismissPendingIntent =
            PendingIntent.getBroadcast(context, 0, dismissIntent, PendingIntent.FLAG_ONE_SHOT)

        val waitIntent = createIncomingCallIntent(
            context,
            isAudioMode,
            token,
            groupId,
            groupType,
            groupName,
            ownerDomain,
            ownerClientId,
            userName,
            avatar,
            turnUrl,
            turnUser,
            turnPass,
            webRtcGroupId,
            webRtcUrl,
            stunUrl,
            true,
            currentUserName,
            currentUserAvatar
        )
        val pendingWaitIntent = PendingIntent.getActivity(
            context, groupId.toIntOrNull() ?: 0, waitIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val inCallIntent = createIncomingCallIntent(
            context,
            isAudioMode,
            token,
            groupId,
            groupType,
            groupName,
            ownerDomain,
            ownerClientId,
            userName,
            avatar,
            turnUrl,
            turnUser,
            turnPass,
            webRtcGroupId,
            webRtcUrl,
            stunUrl,
            false,
            currentUserName,
            currentUserAvatar
        )
        val pendingInCallIntent = PendingIntent.getActivity(
            context, groupId.toIntOrNull() ?: 0, inCallIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val titleCallResource = if (isGroup(groupType)) {
            if (isAudioMode) R.string.notification_incoming_group else R.string.notification_incoming_video_group
        } else {
            if (isAudioMode) R.string.notification_incoming_peer else R.string.notification_incoming_video_peer
        }
        val titleCall = context.resources.getString(titleCallResource)

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
                channel =
                    NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
                channel.setSound(notification, attributes)
                notificationManager.createNotificationChannel(channel)
            }
        }

        val builder: NotificationCompat.Builder = NotificationCompat.Builder(context, channelId)
        builder.setSmallIcon(R.drawable.ic_logo)
            .setCustomHeadsUpContentView(headsUpLayout)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText("$userName calling ${if (isAudioMode) "audio" else "video"}")
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
                .load(avatar)
                .placeholder(context.applicationContext.getDrawable(R.drawable.ic_logo))
                .into(target)
        }

        notificationManager.notify(notificationId, notification)
    }

    private fun createIncomingCallIntent(
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
        return if (isWaitingScreen) {
            printlnCK("createIncomingCallIntent")
            NavigationUtils.createIncomingCallIntent(
                context,
                isAudioMode,
                token,
                groupId,
                groupType,
                groupName,
                domain,
                ownerClientId,
                userName,
                avatar,
                turnUrl,
                turnUser,
                turnPass,
                webRtcGroupId,
                webRtcUrl,
                stunUrl,
                isWaitingScreen,
                currentUserName,
                currentUserAvatar
            )
        } else {
            if (isGroup(groupType)) {
                NavigationUtils.createInCallIntent(
                    context,
                    isAudioMode,
                    token,
                    groupId,
                    groupType,
                    groupName,
                    domain,
                    ownerClientId,
                    userName,
                    avatar,
                    turnUrl,
                    turnUser,
                    turnPass,
                    webRtcGroupId,
                    webRtcUrl,
                    stunUrl,
                    isWaitingScreen,
                    currentUserName,
                    currentUserAvatar
                )
            } else {
                NavigationUtils.createInCallPeerToPeerIntent(
                    context,
                    isAudioMode,
                    token,
                    groupId,
                    groupType,
                    groupName,
                    domain,
                    ownerClientId,
                    userName,
                    avatar,
                    turnUrl,
                    turnUser,
                    turnPass,
                    webRtcGroupId,
                    webRtcUrl,
                    stunUrl,
                    isWaitingScreen,
                    currentUserName,
                    currentUserAvatar
                )
            }
        }
    }

    fun isCallAvailable(context: Context): Boolean {
        return false
    }
}

data class CallingStateData(
    val isCalling: Boolean = false,
    val nameInComeCall: String? = "",
    val isCallPeer: Boolean = false,
    val timeStarted: Long
)