package com.clearkeep.db.converter

import androidx.room.TypeConverter

class SortedStringListConverter {
    @TypeConverter
    fun restoreList(listOfString: String): List<String> {
        return listOfString.split(",")
    }

    @TypeConverter
    fun saveList(listOfString: List<String>): String {
        val newString = listOfString.sortedDescending()
        return newString.joinToString (separator = ",")
    }
}