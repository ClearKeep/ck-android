package com.clearkeep.screen.chat.room.message_display_generator

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.clearkeep.db.clear_keep.model.Message
import com.clearkeep.db.clear_keep.model.People

val roundSize = 24.dp

fun convertMessageList(
    messages: List<Message>,
    clients: List<People>,
    myClientId: String,
    isGroup: Boolean,
): List<MessageDisplayInfo> {
    val groupedMessagesById = messages.groupBy { it.senderId }

    return groupedMessagesById.flatMap { entry ->
        val groupedSize = entry.value.size
        entry.value.mapIndexed { index, message ->
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

fun getOtherShape(index: Int, size: Int): RoundedCornerShape {
    return if (index == size - 1 && size >= 2) {
        RoundedCornerShape(
            topStart = roundSize,
            topEnd = roundSize,
            bottomEnd = roundSize,
            bottomStart = 0.dp
        )
    } else {
        RoundedCornerShape(
            topStart = 0.dp,
            topEnd = roundSize,
            bottomEnd = roundSize,
            bottomStart = roundSize
        )
    }
}

fun getOwnerShape(index: Int, size: Int): RoundedCornerShape {
    return if (index == 0 && size >= 2) {
        RoundedCornerShape(
            topStart = roundSize,
            topEnd = 0.dp,
            bottomEnd = roundSize,
            bottomStart = roundSize
        )
    } else {
        RoundedCornerShape(
            topStart = roundSize,
            topEnd = roundSize,
            bottomEnd = 0.dp,
            bottomStart = roundSize
        )
    }
}
