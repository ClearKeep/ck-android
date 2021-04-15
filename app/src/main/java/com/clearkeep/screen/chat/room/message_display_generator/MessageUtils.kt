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
    return messages.mapIndexed { index, message ->
        val isOwner = myClientId == message.senderId
        val showAvatarAndName = index == messages.size - 1 && isGroup
        val userName = clients.firstOrNull {
            it.id == message.senderId
        }?.userName ?: message.senderId
        MessageDisplayInfo(message, isOwner, showAvatarAndName, userName,
            RoundedCornerShape(
                topStart = 0.dp,
                topEnd = roundSize,
                bottomEnd = roundSize,
                bottomStart = roundSize
            )
        )
    }
}