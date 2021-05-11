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
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.clearkeep.screen.chat.room.RoomActivity
import com.clearkeep.utilities.*
import androidx.core.app.Person
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.NotificationTarget
import com.clearkeep.R
import com.clearkeep.db.clear_keep.model.ChatGroup
import com.clearkeep.db.clear_keep.model.Message
import com.clearkeep.db.clear_keep.model.People
import com.clearkeep.screen.chat.main.MainActivity

const val HEADS_UP_APPEAR_DURATION: Long = 3 * 1000

fun showMessagingStyleNotification(
    context: Context,
    me: People,
    chatGroup: ChatGroup,
    messageHistory: List<Message>,
) {
    val message = messageHistory.first()
    val sender = chatGroup.clientList.find { it.id == message.senderId } ?: People("", "unknown")
    showHeadsUpMessageWithNoAutoLaunch(context, sender, message)
}

private fun showHeadsUpMessageWithNoAutoLaunch(
    context: Context,
    sender: People,
    message: Message
) {
    val channelId = MESSAGE_HEADS_UP_CHANNEL_ID
    val channelName = MESSAGE_HEADS_UP_CHANNEL_NAME
    val notificationId = message.createdTime.toInt()

    val intent = Intent(context, RoomActivity::class.java)
    intent.putExtra(RoomActivity.GROUP_ID, message.groupId)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
    val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

    val trickIntent = Intent()
    val trickPendingIntent = PendingIntent.getActivity(context, 0, trickIntent, PendingIntent.FLAG_UPDATE_CURRENT)

    val dismissIntent = Intent(context, DismissNotificationReceiver::class.java)
    dismissIntent.action = ACTION_MESSAGE_CANCEL
    val pendingDismissIntent = PendingIntent.getBroadcast(context, 0, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT)

    val smallLayout = RemoteViews(context.packageName, R.layout.notification_message_view_small)
    val headsUpLayout = RemoteViews(context.packageName, R.layout.notification_message_view_expand)
    headsUpLayout.apply {
        setTextViewText(R.id.tvFrom,"New message from ${sender.userName}" )
        setTextViewText(R.id.tvMessage, message.message)
        setOnClickPendingIntent(R.id.tvMute, pendingDismissIntent)
        setOnClickPendingIntent(R.id.tvReply, pendingIntent)
    }

    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        var mChannel = notificationManager.getNotificationChannel(channelId)
            ?: NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_MAX)
        notificationManager.createNotificationChannel(mChannel)
    }

    val builder: NotificationCompat.Builder = NotificationCompat.Builder(context, channelId)
    val notification = builder.setStyle(NotificationCompat.DecoratedCustomViewStyle())
        .setCustomContentView(smallLayout)
        .setCustomBigContentView(smallLayout)
        .setCustomHeadsUpContentView(headsUpLayout)
        .setSmallIcon(R.drawable.ic_logo)
        .setColor(
            ContextCompat.getColor(
                context.applicationContext,
                R.color.primaryDefault
            )
        )
        .setFullScreenIntent(trickPendingIntent, true) // trick here to not auto launch activity
        .setContentIntent(pendingIntent)
        .setOngoing(true)
        /*.setAutoCancel(true)
        .setTimeoutAfter(HEADS_UP_APPEAR_DURATION)*/
        .setGroup(message.groupId.toString())
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setVibrate(longArrayOf(Notification.DEFAULT_VIBRATE.toLong()))
        .build()

    val target = NotificationTarget(
        context,
        R.id.imageButton,
        headsUpLayout,
        notification,
        notificationId)
    Glide.with(context.applicationContext)
        .asBitmap()
        .circleCrop()
        .load("https://i.ibb.co/WBKb3zf/Thumbnail.png")
        .into(target)

    notificationManager.notify(notificationId, notification)
}

fun showMessageNotificationToSystemBar(
    context: Context,
    me: People,
    chatGroup: ChatGroup,
    messages: List<Message>,
) {
    val channelId = MESSAGE_CHANNEL_ID
    val channelName = MESSAGE_CHANNEL_NAME
    val notificationId = chatGroup.id.toInt()

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
    notifyIntent.putExtra(RoomActivity.GROUP_ID, chatGroup.id)

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
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        var mChannel = notificationManager.getNotificationChannel(channelId)
        if (mChannel == null) {
            val attributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()
            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            mChannel =
                NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            mChannel.setSound(notification, attributes)
            notificationManager.createNotificationChannel(mChannel)
        }
    }

    val builder: NotificationCompat.Builder = NotificationCompat.Builder(context, channelId)
    builder // MESSAGING_STYLE sets title and content for API 16 and above devices.
        .setStyle(messagingStyle) // Title for API < 16 devices.
        .setContentTitle(contentTitle) // Content for API < 16 devices.
        .setContentText("Has new messages")
        .setSmallIcon(R.drawable.ic_logo)
        .setContentIntent(mainPendingIntent)
        .setColor(
            ContextCompat.getColor(
                context.applicationContext,
                R.color.primaryDefault
            )
        )
        .setGroupSummary(true)
        .setGroup(chatGroup.id.toString())
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(Notification.CATEGORY_MESSAGE)

    val notification: Notification = builder.build()
    notificationManager.notify(notificationId, notification)
}
