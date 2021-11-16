package com.clearkeep.db.clearkeep.converter

import androidx.room.TypeConverter
import com.clearkeep.db.clearkeep.model.User
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import java.lang.reflect.Type

class PeopleListConverter {
    var listType: Type = object : TypeToken<List<User>>() {}.type

    @TypeConverter
    fun restorePeopleList(listOfString: String): List<User> {
        return Gson().fromJson(listOfString, listType)
    }

    @TypeConverter
    fun savePeopleList(listOfString: List<User>): String {
        return Gson().toJson(listOfString, listType)
    }
}