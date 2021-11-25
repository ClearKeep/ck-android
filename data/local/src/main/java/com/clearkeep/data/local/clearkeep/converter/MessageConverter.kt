package com.clearkeep.data.local.clearkeep.converter

import androidx.room.TypeConverter
import com.clearkeep.data.local.model.MessageLocal
import com.clearkeep.domain.model.Message
import com.google.gson.Gson

class MessageConverter {
    @TypeConverter
    fun restoreList(messageAsString: String?): MessageLocal? {
        if (messageAsString.isNullOrEmpty()) {
            return null
        }

        return Gson().fromJson(messageAsString, MessageLocal::class.java)
    }

    @TypeConverter
    fun saveList(message: MessageLocal?): String? {
        if (message == null) {
            return null
        }

        return Gson().toJson(message)
    }
}