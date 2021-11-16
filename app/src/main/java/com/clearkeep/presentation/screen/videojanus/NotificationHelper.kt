package com.clearkeep.presentation.screen.videojanus

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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC
import androidx.core.app.NotificationManagerCompat
import com.clearkeep.presentation.screen.chat.room.RoomActivity
import com.clearkeep.utilities.*
import androidx.core.app.Person
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.NotificationTarget
import com.clearkeep.R
import com.clearkeep.domain.model.ChatGroup
import com.clearkeep.domain.model.Message
import com.clearkeep.domain.model.User
import com.clearkeep.domain.model.UserPreference
import com.clearkeep.presentation.screen.chat.home.MainActivity

const val HEADS_UP_APPEAR_DURATION: Long = 3 * 1000

fun showMessagingStyleNotification(
    context: Context,
    chatGroup: ChatGroup,
    message: Message,
    preference: UserPreference,
    avatar: String? = ""
) {
    printlnCK("Notification showMessagingStyleNotification message $message")
    val sender = if (chatGroup.isGroup()) {
        chatGroup.groupName
    } else {
        chatGroup.clientList.find { it.userId == message.senderId }?.userName ?: "unknown"
    }

    val groupSender = if (chatGroup.isGroup()) {
        chatGroup.clientList.find { it.userId == message.senderId }?.userName ?: "unknown"
    } else {
        ""
    }

    val messageNotificationContent = when {
        isImageMessage(message.message) -> {
            val messageSender = if (groupSender.isNotBlank()) groupSender else sender
            getImageNotificationContent(
                context,
                message.message,
                messageSender,
                chatGroup.isGroup()
            )
        }
        isFileMessage(message.message) -> {
            val messageSender = if (groupSender.isNotBlank()) groupSender else sender
            getFileNotificationContent(context, message.message, messageSender, chatGroup.isGroup())
        }
        isForwardedMessage(message.message) -> {
            message.message.substring(3)
        }
        else -> {
            message.message
        }
    }

    showHeadsUpMessageWithNoAutoLaunch(
        context,
        sender,
        message.copy(message = messageNotificationContent),
        preference,
        avatar,
        groupSender
    )
}

private fun showHeadsUpMessageWithNoAutoLaunch(
    context: Context,
    sender: String,
    message: Message,
    preference: UserPreference,
    avatar: String? = "",
    groupSender: String = "",
) {
    if (!preference.doNotDisturb) {
        val channelId = MESSAGE_HEADS_UP_CHANNEL_ID
        val channelName = MESSAGE_HEADS_UP_CHANNEL_NAME
        val notificationId = message.createdTime.toInt()

        val intent = Intent(context, RoomActivity::class.java)
        intent.putExtra(RoomActivity.GROUP_ID, message.groupId)
        intent.putExtra(RoomActivity.DOMAIN, message.ownerDomain)
        intent.putExtra(RoomActivity.CLIENT_ID, message.ownerClientId)
        intent.putExtra(RoomActivity.CLEAR_TEMP_MESSAGE, true)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val pendingIntent =
            PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val trickIntent = Intent()
        val trickPendingIntent =
            PendingIntent.getActivity(context, 0, trickIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val dismissIntent = Intent(context, DismissNotificationReceiver::class.java)
        dismissIntent.action = ACTION_MESSAGE_CANCEL
        dismissIntent.putExtra(MESSAGE_HEADS_UP_CANCEL_NOTIFICATION_ID, notificationId)
        val pendingDismissIntent =
            PendingIntent.getBroadcast(context, 0, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val showSummaryIntent = Intent(context, ShowSummaryNotificationReceiver::class.java)
        showSummaryIntent.putExtra(EXTRA_GROUP_ID, message.groupId)
        showSummaryIntent.putExtra(EXTRA_OWNER_CLIENT, message.ownerClientId)
        showSummaryIntent.putExtra(EXTRA_OWNER_DOMAIN, message.ownerDomain)
        val pendingShowSummaryIntent = PendingIntent.getBroadcast(
            context,
            0,
            showSummaryIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val smallLayout = RemoteViews(context.packageName, R.layout.notification_message_view_small)
        val headsUpLayout =
            RemoteViews(context.packageName, R.layout.notification_message_view_expand)
        val messageContent =
            if (preference.showNotificationPreview) message.message else context.getString(R.string.notification_content_no_preview)
        val messageFrom = context.getString(R.string.notification_sender_no_preview, sender)
        smallLayout.apply {
            setTextViewText(R.id.tvFrom, messageFrom)
            setTextViewText(R.id.tvMessage, messageContent)
            setViewVisibility(R.id.imageButton, View.VISIBLE)
        }
        headsUpLayout.apply {
            setTextViewText(R.id.tvFrom, messageFrom)
            setTextViewText(R.id.tvMessage, messageContent)
            if (groupSender.isNotBlank()) setViewVisibility(R.id.tvGroupSenderName, View.VISIBLE)
            setTextViewText(R.id.tvGroupSenderName, "$groupSender:")
            setViewVisibility(R.id.imageButton, View.VISIBLE)
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            var channel = notificationManager.getNotificationChannel(channelId)
                ?: NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_MAX)
            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            notificationManager.createNotificationChannel(channel)
        }

        val builder: NotificationCompat.Builder = NotificationCompat.Builder(context, channelId)
        val notification = builder.setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(smallLayout)
            .setCustomBigContentView(smallLayout)
            .setCustomHeadsUpContentView(headsUpLayout)
            .setDeleteIntent(pendingShowSummaryIntent)
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
            .setAutoCancel(true)
            .setTimeoutAfter(HEADS_UP_APPEAR_DURATION)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(VISIBILITY_PUBLIC)
            .setVibrate(longArrayOf(Notification.DEFAULT_VIBRATE.toLong()))
            .build()

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
            .into(target)

        notificationManager.notify(notificationId, notification)
    }
}

fun showMessageNotificationToSystemBar(
    context: Context,
    me: User,
    chatGroup: ChatGroup,
    messages: List<Message>,
    userPreference: UserPreference
) {
    if (!userPreference.doNotDisturb) {
        val channelId = MESSAGE_CHANNEL_ID
        val channelName = MESSAGE_CHANNEL_NAME
        val notificationId = chatGroup.groupId.toInt()
        val contentTitle = chatGroup.groupName
        val participants = chatGroup.clientList
        val userName = if (me.userName.isEmpty()) "me" else me.userName
        val messagingStyle: NotificationCompat.MessagingStyle = NotificationCompat.MessagingStyle(
            Person.Builder()
                .setName(userName)
                .setKey(me.userId)
                .build()
        )
            .setConversationTitle(contentTitle)
            .setGroupConversation(chatGroup.isGroup())

        for (message in messages) {
            val people = participants.find { it.userId == message.senderId }
            val messageContent =
                if (userPreference.showNotificationPreview) {
                    when {
                        isImageMessage(message.message) -> {
                            getImageNotificationContent(
                                context,
                                message.message,
                                people?.userName ?: "",
                                chatGroup.isGroup()
                            )
                        }
                        isFileMessage(message.message) -> {
                            getFileNotificationContent(
                                context,
                                message.message,
                                people?.userName ?: "",
                                chatGroup.isGroup()
                            )
                        }
                        isForwardedMessage(message.message) -> {
                            message.message.substring(3)
                        }
                        else -> {
                            message.message
                        }
                    }
                } else context.getString(R.string.notification_content_no_preview)
            val username = people?.userName
            messagingStyle.addMessage(
                NotificationCompat.MessagingStyle.Message(
                    messageContent,
                    message.createdTime,
                    Person.Builder().setName(username).setKey(message.senderId)
                        .build()
                )
            )
        }

        // 3. Set up main Intent for notification.
        val notifyIntent = Intent(context, RoomActivity::class.java)
        notifyIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        notifyIntent.putExtra(RoomActivity.GROUP_ID, chatGroup.groupId)
        notifyIntent.putExtra(RoomActivity.DOMAIN, chatGroup.ownerDomain)
        notifyIntent.putExtra(RoomActivity.CLIENT_ID, chatGroup.ownerClientId)

        val stackBuilder: TaskStackBuilder = TaskStackBuilder.create(context)
        // Adds the back stack
        stackBuilder.addParentStack(MainActivity::class.java)
        // Adds the Intent to the top of the stack
        stackBuilder.addNextIntent(notifyIntent)
        // Gets a PendingIntent containing the entire back stack
        val mainPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            notifyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            var mChannel = notificationManager.getNotificationChannel(channelId)
            if (mChannel == null) {
                val attributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()
                val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                mChannel =
                    NotificationChannel(
                        channelId,
                        channelName,
                        NotificationManager.IMPORTANCE_DEFAULT
                    )
                mChannel.setSound(notification, attributes)
                notificationManager.createNotificationChannel(mChannel)
            }
        }

        val builder: NotificationCompat.Builder = NotificationCompat.Builder(context, channelId)
        builder // MESSAGING_STYLE sets title and content for API 16 and above devices.
            .setStyle(messagingStyle) // Title for API < 16 devices.
            .setContentTitle(contentTitle) // Content for API < 16 devices.
            .setContentTitle(
                context.getString(
                    R.string.notification_new_messages_title,
                    messages.size,
                    me.userName
                )
            )
            .setContentText(context.getString(R.string.notification_new_messages_content))
            .setSmallIcon(R.drawable.ic_logo)
            .setContentIntent(mainPendingIntent)
            .setColor(
                ContextCompat.getColor(
                    context.applicationContext,
                    R.color.primaryDefault
                )
            )
            .setGroupSummary(chatGroup.isGroup())
            .setGroup(chatGroup.groupId.toString())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(Notification.CATEGORY_MESSAGE)

        val notification: Notification = builder.build()
        notificationManager.notify(notificationId, notification)
    }
}

fun createInCallNotification(context: Context, activityToLaunchOnClick: Class<*>) {
    val intent = Intent(context, activityToLaunchOnClick)
    intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
    val pendingIntent =
        PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

    val channelId = CALL_CHANNEL_ID
    val channelName = CALL_CHANNEL_NAME
    val notification = NotificationCompat.Builder(context, channelId)
        .setContentTitle(context.getString(R.string.notification_return_call_title))
        .setContentText(context.getString(R.string.notification_return_call_content))
        .setSmallIcon(R.drawable.ic_logo)
        .setContentIntent(pendingIntent)
        .setOngoing(true)
        .build()

    val notificationManager =
        context.getSystemService(AppCompatActivity.NOTIFICATION_SERVICE) as NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel =
            notificationManager.getNotificationChannel(channelId) ?: NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            )
        notificationManager.createNotificationChannel(channel)
    }

    notificationManager.notify(1, notification)
}

fun dismissInCallNotification(context: Context) {
    NotificationManagerCompat.from(context).cancel(null, CALL_NOTIFICATION_ID)
}

private fun getImageNotificationContent(
    context: Context,
    message: String,
    sender: String,
    isGroup: Boolean
): String {
    val imageCount = getImageUriStrings(message).size

    val pluralString = if (imageCount > 1) "s" else ""

    val messageContent =
        if (getMessageContent(message).isNotBlank()) "\n${getMessageContent(message)}" else ""
    val senderString = if (isGroup) "" else "$sender "

    return context.getString(
        R.string.notification_new_message_image_content,
        senderString,
        imageCount,
        pluralString,
        messageContent
    )
}

private fun getFileNotificationContent(
    context: Context,
    message: String,
    sender: String,
    isGroup: Boolean
): String {
    val fileCount = getFileUriStrings(message).size
    val pluralString = if (fileCount > 1) "s" else ""
    val senderString = if (isGroup) "" else "$sender "
    return context.getString(
        R.string.notification_new_message_file_content,
        senderString,
        fileCount,
        pluralString
    )
}