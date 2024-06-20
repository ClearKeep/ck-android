package com.clearkeep.data.local.preference

interface Storage {
    fun setString(key: String, value: String)
    fun getString(key: String): String
    fun setInt(key: String, value: Int)
    fun getInt(key: String): Int
    fun setLong(key: String, value: Long)
    fun getLong(key: String): Long
    fun setBoolean(key: String, value: Boolean)
    fun getBoolean(key: String): Boolean
    fun clear()
}
