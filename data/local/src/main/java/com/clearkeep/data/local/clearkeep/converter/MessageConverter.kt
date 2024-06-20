package com.clearkeep.data.local.clearkeep.converter

import androidx.room.TypeConverter
import com.clearkeep.data.local.clearkeep.message.MessageEntity
import com.google.gson.Gson

class MessageConverter {
    @TypeConverter
    fun restoreList(messageAsString: String?): MessageEntity? {
        if (messageAsString.isNullOrEmpty()) {
            return null
        }

        return Gson().fromJson(messageAsString, MessageEntity::class.java)
    }

    @TypeConverter
    fun saveList(message: MessageEntity?): String? {
        if (message == null) {
            return null
        }

        return Gson().toJson(message)
    }
}