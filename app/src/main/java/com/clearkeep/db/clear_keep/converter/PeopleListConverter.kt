package com.clearkeep.db.clear_keep.converter

import androidx.room.TypeConverter
import com.clearkeep.db.clear_keep.model.People
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import java.lang.reflect.Type

class PeopleListConverter {
    var listType: Type = object : TypeToken<List<People>>() {}.type

    @TypeConverter
    fun restorePeopleList(listOfString: String): List<People> {
        return Gson().fromJson(listOfString, listType)
    }

    @TypeConverter
    fun savePeopleList(listOfString: List<People>): String {
        return Gson().toJson(listOfString, listType)
    }
}