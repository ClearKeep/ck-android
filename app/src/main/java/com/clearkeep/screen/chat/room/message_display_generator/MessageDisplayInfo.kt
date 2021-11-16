package com.clearkeep.screen.chat.room.message_display_generator

import androidx.compose.foundation.shape.RoundedCornerShape
import com.clearkeep.db.clearkeep.model.Message

data class MessageDisplayInfo(
    val message: Message,
    val isOwner: Boolean,
    val showAvatarAndName: Boolean,
    val showSpacer: Boolean,
    val userName: String,
    val cornerShape: RoundedCornerShape,
    val avatar: String,
    val isForwardedMessage: Boolean = false,
    val isQuoteMessage: Boolean = false,
    val quotedUser: String = "",
    val quotedMessage: String = "",
    val quotedTimestamp: Long = 0L
)