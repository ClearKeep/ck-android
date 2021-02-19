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
import androidx.core.app.NotificationCompat
import com.clearkeep.R
import com.clearkeep.utilities.*


object AppCall {
    fun call(context: Context, token: String?, groupId: String?, ourClientId: String?, userName: String?, avatar: String?, isIncomingCall: Boolean,
             turnUrl: String = "", turnUser: String = "", turnPass: String = "",
             stunUrl: String = ""
    ) {
        printlnCK("token = $token, groupID = $groupId")
        val intent = Intent(context, InCallActivity::class.java)
        intent.putExtra(EXTRA_GROUP_ID, groupId)
        intent.putExtra(EXTRA_GROUP_TOKEN, token)
        intent.putExtra(EXTRA_OUR_CLIENT_ID, ourClientId)
        intent.putExtra(EXTRA_USER_NAME, userName)
        intent.putExtra(EXTRA_FROM_IN_COMING_CALL, isIncomingCall)
        intent.putExtra(EXTRA_AVATAR_USER_IN_CONVERSATION, avatar)
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

    fun inComingCall(context: Context, token: String, groupId: String?, ourClientId: String?,
                     userName: String?, avatar: String?,
                     turnUrl: String, turnUser: String, turnPass: String,
                     stunUrl: String) {
        printlnCK("token = $token, groupID = $groupId, turnURL= $turnUrl, turnUser=$turnUser, turnPass= $turnPass, stunUrl = $stunUrl")
        val intent = createIncomingCallIntent(context, token, groupId, ourClientId,
                userName, avatar, turnUrl, turnUser, turnPass, stunUrl, true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val attributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()
            val channelId = INCOMING_CHANNEL_ID
            val channelName = INCOMING_CHANNEL_NAME
            val notificationId = INCOMING_NOTIFICATION_ID
            var mChannel = notificationManager.getNotificationChannel(channelId)
            if (mChannel == null) {
                val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                mChannel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
                mChannel.setSound(notification, attributes)
                notificationManager.createNotificationChannel(mChannel)
            }
            val builder: NotificationCompat.Builder = NotificationCompat.Builder(context, channelId)
            val pendingIntent = PendingIntent.getActivity(context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT)

            val intentToInCall = createIncomingCallIntent(context, token, groupId, ourClientId,
                    userName, avatar, turnUrl, turnUser, turnPass, stunUrl, false)
            val pendingIntentToInCall = PendingIntent.getActivity(context, 0, intentToInCall,
                    PendingIntent.FLAG_UPDATE_CURRENT)

            val dismissIntent = Intent(context, DismissNotificationReceiver::class.java)
            val dismissPendingIntent = PendingIntent.getBroadcast(context, 0, dismissIntent, 0)

            builder.setSmallIcon(R.drawable.ic_logo)
                    .setContentTitle(context.getString(R.string.app_name))
                    .setContentText("$userName calling")
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_CALL)
                    /*.setContentIntent(pendingIntent)*/
                    .setFullScreenIntent(pendingIntent, true)
                    .setAutoCancel(true)
                    .setTimeoutAfter(20 * 1000)
                    .setOngoing(true)
                    .addAction(R.drawable.ic_string_ee_answer, "Answer", pendingIntentToInCall)
                    .addAction(R.drawable.ic_close_call, "End", dismissPendingIntent)
            val notification: Notification = builder.build()
            notificationManager.notify(notificationId, notification)
        } else {
            val newIntent = intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(newIntent)
        }
    }

    private fun createIncomingCallIntent(context: Context, token: String, groupId: String?, ourClientId: String?,
                                         userName: String?, avatar: String?,
                                         turnUrl: String, turnUser: String, turnPass: String,
                                         stunUrl: String, isWaitingScreen: Boolean): Intent {
        val intent = if (isWaitingScreen) {
            Intent(context, InComingCallActivity::class.java)
        } else {
            Intent(context, InCallActivity::class.java)
        }
        intent.putExtra(EXTRA_GROUP_ID, groupId)
        intent.putExtra(EXTRA_GROUP_TOKEN, token)
        intent.putExtra(EXTRA_OUR_CLIENT_ID, ourClientId)
        intent.putExtra(EXTRA_USER_NAME, userName)
        intent.putExtra(EXTRA_FROM_IN_COMING_CALL, true)
        intent.putExtra(EXTRA_AVATAR_USER_IN_CONVERSATION, avatar)
        intent.putExtra(EXTRA_TURN_URL, turnUrl)
        intent.putExtra(EXTRA_TURN_USER_NAME, turnUser)
        intent.putExtra(EXTRA_TURN_PASS, turnPass)
        intent.putExtra(EXTRA_STUN_URL, stunUrl)
        return intent
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