package com.clearkeep.data.local.clearkeep

import android.provider.ContactsContract
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.clearkeep.data.local.clearkeep.converter.ProfileConverter
import com.clearkeep.data.local.clearkeep.dao.*
import com.clearkeep.data.local.model.*
import com.clearkeep.domain.model.*

@Database(
    entities = [
        ProfileLocal::class,
        MessageLocal::class,
        ChatGroupLocal::class,
        UserEntityLocal::class,
        ServerLocal::class,
        UserPreferenceLocal::class,
        UserKeyLocal::class
    ], version = 16, exportSchema = false
)
@TypeConverters(ProfileConverter::class)
abstract class ClearKeepDatabase : RoomDatabase() {
    abstract fun serverDao(): ServerDAO
    abstract fun messageDao(): MessageDAO
    abstract fun groupDao(): GroupDAO
    abstract fun userDao(): UserDAO
    abstract fun userPreferenceDao(): UserPreferenceDAO
    abstract fun userKeyDao(): UserKeyDAO
}