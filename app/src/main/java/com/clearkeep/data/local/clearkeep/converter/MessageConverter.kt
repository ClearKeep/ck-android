package com.clearkeep.data.local.clearkeep.converter

import androidx.room.TypeConverter
import com.clearkeep.domain.model.Message
import com.google.gson.Gson

class MessageConverter {
    @TypeConverter
    fun restoreList(messageAsString: String?): Message? {
        if (messageAsString.isNullOrEmpty()) {
            return null
        }

        return Gson().fromJson(messageAsString, Message::class.java)
    }

    @TypeConverter
    fun saveList(message: Message?): String? {
        if (message == null) {
            return null
        }

        return Gson().toJson(message)
    }
}