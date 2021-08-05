package com.clearkeep.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.clearkeep.db.clear_keep.dao.*
import com.clearkeep.db.clear_keep.model.*

@Database(entities = [
    Profile::class,
    Message::class,
    ChatGroup::class,
    UserEntity::class,
    Server::class,
    Note::class,
    UserPreference::class
], version = 10, exportSchema = false)
abstract class ClearKeepDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun serverDao(): ServerDAO
    abstract fun messageDao(): MessageDAO
    abstract fun noteDao(): NoteDAO
    abstract fun groupDao(): GroupDAO
    abstract fun userDao(): UserDao
    abstract fun userPreferenceDao(): UserPreferenceDAO
}