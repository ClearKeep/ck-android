package com.clearkeep.data.local.clearkeep.converter

import androidx.room.TypeConverter
import com.clearkeep.domain.model.Profile
import com.google.gson.Gson

class ProfileConverter {
    @TypeConverter
    fun restoreList(profileAsString: String?): Profile? {
        if (profileAsString.isNullOrEmpty()) {
            return null
        }

        return Gson().fromJson(profileAsString, Profile::class.java)
    }

    @TypeConverter
    fun saveList(profile: Profile?): String? {
        if (profile == null) {
            return null
        }

        return Gson().toJson(profile)
    }
}