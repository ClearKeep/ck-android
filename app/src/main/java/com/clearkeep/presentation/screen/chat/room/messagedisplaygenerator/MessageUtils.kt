package com.clearkeep.presentation.screen.chat.room.messagedisplaygenerator

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.clearkeep.domain.model.Message
import com.clearkeep.domain.model.User
import com.clearkeep.utilities.printlnCK
import kotlin.collections.ArrayList


val roundSizeLarge = 18.dp
val roundSizeSmall = 4.dp

fun convertMessageList(
    messages: List<Message>,
    clients: List<User>,
    listAvatar: List<User>,
    myClientId: String,
    isGroup: Boolean,
): List<MessageDisplayInfo> {
    val groupedMessagesById = separateMessageList(messages)

    return groupedMessagesById.flatMap { subList ->
        val groupedSize = subList.size
        subList.mapIndexed { index, rawMessage ->
            val isOwner = myClientId == rawMessage.senderId
            val showAvatarAndName = (index == groupedSize - 1) && isGroup && !isOwner
            val showSpacerTop = index == groupedSize - 1
            val userName = clients.firstOrNull {
                it.userId == rawMessage.senderId
            }?.userName ?: rawMessage.senderId

            val avatar = listAvatar.firstOrNull {
                it.userId == rawMessage.senderId
            }?.avatar ?: ""

            val isForwardedMessage = rawMessage.message.startsWith(">>>")
            val isQuoteMessage = rawMessage.message.startsWith("```")

            var quotedUser = ""
            var quotedMessage = ""
            var quotedMessageTimestamp = 0L

            val message = when {
                isForwardedMessage -> {
                    val content = rawMessage.message.substring(3)
                    rawMessage.copy(message = content)
                }
                isQuoteMessage -> {
                    val parts = rawMessage.message.substring(3).split("|")
                    if (parts.size == 4) {
                        quotedUser = parts[0]
                        quotedMessage = parts[1]
                        quotedMessageTimestamp = parts[2].toLongOrNull() ?: 0L
                        val newMessage = parts[3]

                        printlnCK("convertMessageList quotedUser $quotedUser quotedMessage $quotedMessage quotedMessageTimestamp $quotedMessageTimestamp newMsg $newMessage")

                        rawMessage.copy(message = newMessage)
                    } else {
                        printlnCK("MessageUtils cannot parse quoted message")
                        rawMessage
                    }
                }
                else -> {
                    rawMessage
                }
            }

            MessageDisplayInfo(
                message, isOwner, showAvatarAndName, showSpacerTop, userName,
                if (isOwner) getOwnerShape(index, groupedSize) else getOtherShape(
                    index,
                    groupedSize
                ), avatar, isForwardedMessage, isQuoteMessage, quotedUser, quotedMessage, quotedMessageTimestamp
            )

        }
    }
}

fun separateMessageList(messages: List<Message>): List<List<Message>> {
    val result: MutableList<MutableList<Message>> = ArrayList()

    var cache: MutableList<Message> = ArrayList()
    var currentSenderId = ""
    for (message in messages) {
        if (currentSenderId.isEmpty() || currentSenderId != message.senderId) {
            currentSenderId = message.senderId
            if (cache.isNotEmpty()) {
                result.add(cache)
            }
            cache = ArrayList()
        }
        cache.add(message)
    }
    result.add(cache)

    return result
}

fun getOtherShape(index: Int, size: Int): RoundedCornerShape {
    if (size == 1) {
        return RoundedCornerShape(
            topStart = roundSizeSmall,
            topEnd = roundSizeLarge,
            bottomEnd = roundSizeLarge,
            bottomStart = roundSizeLarge
        )
    } else {
        return when (index) {
            size - 1 -> {
                RoundedCornerShape(
                    topStart = roundSizeLarge,
                    topEnd = roundSizeLarge,
                    bottomEnd = roundSizeLarge,
                    bottomStart = roundSizeSmall
                )
            }
            0 -> {
                RoundedCornerShape(
                    topStart = roundSizeSmall,
                    topEnd = roundSizeLarge,
                    bottomEnd = roundSizeLarge,
                    bottomStart = roundSizeLarge
                )
            }
            else -> {
                RoundedCornerShape(
                    topStart = roundSizeSmall,
                    topEnd = roundSizeLarge,
                    bottomEnd = roundSizeLarge,
                    bottomStart = roundSizeSmall
                )
            }
        }
    }
}

fun getOwnerShape(index: Int, size: Int): RoundedCornerShape {
    if (size == 1) {
        return RoundedCornerShape(
            topStart = roundSizeLarge,
            topEnd = roundSizeLarge,
            bottomEnd = roundSizeSmall,
            bottomStart = roundSizeLarge
        )
    } else {
        return when (index) {
            0 -> {
                RoundedCornerShape(
                    topStart = roundSizeLarge,
                    topEnd = roundSizeSmall,
                    bottomEnd = roundSizeLarge,
                    bottomStart = roundSizeLarge
                )
            }
            size - 1 -> {
                RoundedCornerShape(
                    topStart = roundSizeLarge,
                    topEnd = roundSizeLarge,
                    bottomEnd = roundSizeSmall,
                    bottomStart = roundSizeLarge
                )
            }
            else -> {
                RoundedCornerShape(
                    topStart = roundSizeLarge,
                    topEnd = roundSizeSmall,
                    bottomEnd = roundSizeSmall,
                    bottomStart = roundSizeLarge
                )
            }
        }
    }
}
