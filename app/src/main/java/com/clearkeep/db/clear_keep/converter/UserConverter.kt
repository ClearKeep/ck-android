package com.clearkeep.db.clear_keep.converter

import androidx.room.TypeConverter
import com.clearkeep.db.clear_keep.model.Profile
import com.google.gson.Gson

class UserConverter {
    @TypeConverter
    fun restoreList(userAsString: String?): Profile? {
        if (userAsString.isNullOrEmpty()) {
            return null
        }

        return Gson().fromJson(userAsString, Profile::class.java)
    }

    @TypeConverter
    fun saveList(message: Profile?): String? {
        if (message == null) {
            return null
        }

        return Gson().toJson(message)
    }
}