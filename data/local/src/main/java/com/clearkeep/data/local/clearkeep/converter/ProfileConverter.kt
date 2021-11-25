package com.clearkeep.data.local.clearkeep.converter

import androidx.room.TypeConverter
import com.clearkeep.data.local.model.ProfileLocal
import com.clearkeep.domain.model.Profile
import com.google.gson.Gson

class ProfileConverter {
    @TypeConverter
    fun restoreList(profileAsString: String?): ProfileLocal? {
        if (profileAsString.isNullOrEmpty()) {
            return null
        }

        return Gson().fromJson(profileAsString, ProfileLocal::class.java)
    }

    @TypeConverter
    fun saveList(profile: ProfileLocal?): String? {
        if (profile == null) {
            return null
        }

        return Gson().toJson(profile)
    }
}