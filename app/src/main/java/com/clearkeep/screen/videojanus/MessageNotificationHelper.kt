package com.clearkeep.screen.videojanus

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.clearkeep.screen.chat.room.RoomActivity
import com.clearkeep.utilities.*
import android.os.Looper
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.NotificationTarget
import androidx.core.app.Person
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import com.clearkeep.R
import com.clearkeep.db.clear_keep.model.ChatGroup
import com.clearkeep.db.clear_keep.model.Message
import com.clearkeep.db.clear_keep.model.People
import com.clearkeep.screen.chat.home.MainActivity

fun generateMessagingStyleNotification(
    context: Context,
    me: People,
    chatGroup: ChatGroup,
    messages: List<Message>,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val channelId = MESSAGE_CHANNEL_ID
        val channelName = MESSAGE_CHANNEL_NAME
        val notificationId = MESSAGE_NOTIFICATION_ID

        val contentTitle = chatGroup.groupName
        val participants = chatGroup.clientList

        val messagingStyle: NotificationCompat.MessagingStyle = NotificationCompat.MessagingStyle(
            Person.Builder()
                .setName(me.userName)
                .setKey(me.id)
                .build()
        )
            .setConversationTitle(contentTitle)

        for (message in messages) {
            val people = participants.find { it.id == message.senderId }
            messagingStyle.addMessage(
                NotificationCompat.MessagingStyle.Message(
                    message.message,
                    message.createdTime,
                    Person.Builder().setName(people?.userName ?: "unknown").setKey(message.senderId).build()
                )
            )
        }
        messagingStyle.isGroupConversation = chatGroup.isGroup()

        // 3. Set up main Intent for notification.
        val notifyIntent = Intent(context, RoomActivity::class.java)
        val stackBuilder: TaskStackBuilder = TaskStackBuilder.create(context)
        // Adds the back stack
        stackBuilder.addParentStack(MainActivity::class.java)
        // Adds the Intent to the top of the stack
        stackBuilder.addNextIntent(notifyIntent)
        // Gets a PendingIntent containing the entire back stack
        val mainPendingIntent = PendingIntent.getActivity(
            context,
            0,
            notifyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        var mChannel = notificationManager.getNotificationChannel(channelId)
        if (mChannel == null) {
            val attributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()
            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            mChannel =
                NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_MAX)
            mChannel.setSound(notification, attributes)
            notificationManager.createNotificationChannel(mChannel)
        }


        val builder: NotificationCompat.Builder = NotificationCompat.Builder(context, channelId)
        builder // MESSAGING_STYLE sets title and content for API 16 and above devices.
            .setStyle(messagingStyle) // Title for API < 16 devices.
            .setContentTitle(contentTitle) // Content for API < 16 devices.
            .setContentText("Has new messages")
            .setSmallIcon(R.drawable.ic_logo)
            /*.setLargeIcon(
                BitmapFactory.decodeResource(
                    getResources(),
                    R.drawable.ic_person_black_48dp
                )
            )*/
            .setContentIntent(mainPendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setColor(
                ContextCompat.getColor(
                    context.applicationContext,
                    R.color.primaryDefault
                )
            )
            .setGroupSummary(true)
            .setGroup(chatGroup.id.toString())
            .setCategory(Notification.CATEGORY_MESSAGE) // Sets priority for 25 and below. For 26 and above, 'priority' is deprecated for
            .setPriority(NotificationCompat.PRIORITY_MAX) // Sets lock-screen visibility for 25 and below. For 26 and above, lock screen
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        val notification: Notification = builder.build()
        notificationManager.notify(notificationId, notification)
    }
}

@SuppressLint("RemoteViewLayout")
fun showNotification(
    context: Context,
    packageName: String,
    userName: String,
    message: String,
    groupId: Long
) {
    val channelId = MESSAGE_CHANNEL_ID
    val channelName = MESSAGE_CHANNEL_NAME
    val notificationId = MESSAGE_NOTIFICATION_ID

    val pendingIntent = PendingIntent.getActivity(
        context, notificationId, getIntentRoomActivity(context, groupId,packageName),
        PendingIntent.FLAG_UPDATE_CURRENT
    )

    val dismissIntent = Intent(context, DismissNotificationReceiver::class.java)
    dismissIntent.action = ACTION_MESSAGE_CANCEL
    val pendingDismissIntent = PendingIntent.getBroadcast(context, 100, dismissIntent, 0)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        var mChannel = notificationManager.getNotificationChannel(channelId)
        if (mChannel == null) {
            val attributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()
            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            mChannel =
                NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_MAX)
            mChannel.setSound(notification, attributes)
            notificationManager.createNotificationChannel(mChannel)
        }


        val builder: NotificationCompat.Builder = NotificationCompat.Builder(context, channelId)
        val contentView = RemoteViews(packageName, R.layout.notification_message_view)

        contentView.apply {
            setTextViewText(R.id.tvFrom,"New message from $userName" )
            setTextViewText(R.id.tvMessage, message)
            setOnClickPendingIntent(R.id.tvMute, pendingDismissIntent)
            setOnClickPendingIntent(R.id.tvReply, pendingIntent)
        }

        builder.setSmallIcon(R.drawable.ic_logo)
            .setContentTitle(context.getString(R.string.app_name))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setFullScreenIntent(pendingIntent, true)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setTimeoutAfter(5 * 1000)
            .setContent(contentView)
        val notification: Notification = builder.build()
        val target = NotificationTarget(
            context,
            R.id.imageButton,
            contentView,
            notification,
            MESSAGE_NOTIFICATION_ID)
        Handler(Looper.getMainLooper()).post(Runnable {
            Glide.with(context.applicationContext)
                .asBitmap()
                .circleCrop()
                .placeholder(R.drawable.ic_thumb_test)
                .load("data:image/jpeg;base64,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")
                .into(target)
        })
        notificationManager.notify(notificationId, notification)
    }
}

private fun getIntentRoomActivity(context: Context, groupId: Long,packageName: String): Intent {
    val intent = Intent(context, RoomActivity::class.java)
    intent.action=ACTION_MESSAGE_REPLY
    intent.putExtra(RoomActivity.GROUP_ID, groupId)
    intent.putExtra(packageName, MESSAGE_NOTIFICATION_ID)
    return intent
}