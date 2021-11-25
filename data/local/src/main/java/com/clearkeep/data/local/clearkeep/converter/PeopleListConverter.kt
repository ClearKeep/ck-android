package com.clearkeep.data.local.clearkeep.converter

import androidx.room.TypeConverter
import com.clearkeep.domain.model.User
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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