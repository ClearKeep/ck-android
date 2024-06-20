package com.clearkeep.data.local.clearkeep.converter

import androidx.room.TypeConverter
import com.clearkeep.data.local.clearkeep.server.ProfileEntity
import com.google.gson.Gson

class ProfileConverter {
    @TypeConverter
    fun restoreList(profileAsString: String?): ProfileEntity? {
        if (profileAsString.isNullOrEmpty()) {
            return null
        }

        return Gson().fromJson(profileAsString, ProfileEntity::class.java)
    }

    @TypeConverter
    fun saveList(profile: ProfileEntity?): String? {
        if (profile == null) {
            return null
        }

        return Gson().toJson(profile)
    }
}