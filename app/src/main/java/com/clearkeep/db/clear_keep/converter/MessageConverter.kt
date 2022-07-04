package com.clearkeep.db.clear_keep.converter

import androidx.room.TypeConverter
import com.clearkeep.db.clear_keep.model.Message
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