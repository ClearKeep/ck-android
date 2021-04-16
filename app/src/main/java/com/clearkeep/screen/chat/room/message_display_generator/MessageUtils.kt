package com.clearkeep.screen.chat.room.message_display_generator

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.clearkeep.db.clear_keep.model.Message
import com.clearkeep.db.clear_keep.model.People
import kotlin.collections.ArrayList


val roundSizeLarge = 18.dp
val roundSizeSmall = 4.dp

fun convertMessageList(
    messages: List<Message>,
    clients: List<People>,
    myClientId: String,
    isGroup: Boolean,
): List<MessageDisplayInfo> {
    val groupedMessagesById = separateMessageList(messages)

    return groupedMessagesById.flatMap { subList ->
        val groupedSize = subList.size
        subList.mapIndexed { index, message ->
            val isOwner = myClientId == message.senderId
            val showAvatarAndName = (index == groupedSize - 1) && isGroup && !isOwner
            val showSpacerTop = index == groupedSize - 1
            val userName = clients.firstOrNull {
                it.id == message.senderId
            }?.userName ?: message.senderId
            MessageDisplayInfo(
                message, isOwner, showAvatarAndName, showSpacerTop, userName,
                if (isOwner) getOwnerShape(index, groupedSize) else getOtherShape(index, groupedSize)
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
