package com.clearkeep.utilities.storage

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SharedPreferencesStorage @Inject constructor(@ApplicationContext context: Context) : Storage {

    private val sharedPreferences = context.getSharedPreferences("CK_SharedPreference", Context.MODE_PRIVATE)

    override fun setString(key: String, value: String) {
        with(sharedPreferences.edit()) {
            putString(key, value)
            apply()
        }
    }

    override fun getString(key: String): String {
        return sharedPreferences.getString(key, "")!!
    }

    override fun setInt(key: String, value: Int) {
        with(sharedPreferences.edit()) {
            putInt(key, value)
            apply()
        }
    }

    override fun getInt(key: String): Int {
        return sharedPreferences.getInt(key, -1)
    }
}
