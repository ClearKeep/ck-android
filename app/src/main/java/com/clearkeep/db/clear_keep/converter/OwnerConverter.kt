package com.clearkeep.db.clear_keep.converter

import androidx.room.TypeConverter
import com.clearkeep.db.clear_keep.model.Owner
import com.google.gson.Gson

class OwnerConverter {
    @TypeConverter
    fun restoreList(ownerAsString: String): Owner {
        return Gson().fromJson(ownerAsString, Owner::class.java)
    }

    @TypeConverter
    fun saveList(owner: Owner): String {
        return "${owner.domain}_${owner.clientId}"
    }
}