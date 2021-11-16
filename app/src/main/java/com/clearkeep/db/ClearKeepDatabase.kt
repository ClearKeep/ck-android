package com.clearkeep.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.clearkeep.db.clearkeep.converter.ProfileConverter
import com.clearkeep.db.clearkeep.dao.*
import com.clearkeep.db.clearkeep.model.*

@Database(
    entities = [
        Profile::class,
        Message::class,
        ChatGroup::class,
        UserEntity::class,
        Server::class,
        Note::class,
        UserPreference::class,
        UserKey::class
    ], version = 15, exportSchema = false
)
@TypeConverters(ProfileConverter::class)
abstract class ClearKeepDatabase : RoomDatabase() {
    abstract fun serverDao(): ServerDAO
    abstract fun messageDao(): MessageDAO
    abstract fun noteDao(): NoteDAO
    abstract fun groupDao(): GroupDAO
    abstract fun userDao(): UserDAO
    abstract fun userPreferenceDao(): UserPreferenceDAO
    abstract fun userKeyDao(): UserKeyDAO
}