package com.clearkeep.screen.chat.room.message_display_generator

import androidx.compose.foundation.shape.RoundedCornerShape
import com.clearkeep.db.clear_keep.model.Message

data class MessageDisplayInfo(
    val message: Message,
    val isOwner: Boolean,
    val showAvatarAndName: Boolean,
    val userName: String,
    val cornerShape: RoundedCornerShape
)