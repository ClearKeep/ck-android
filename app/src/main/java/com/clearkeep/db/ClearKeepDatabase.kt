package com.clearkeep.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.clearkeep.db.clear_keep.dao.*
import com.clearkeep.db.clear_keep.model.*

@Database(entities = [
    Profile::class,
    Message::class,
    ChatGroup::class,
    User::class,
    Server::class
], version = 6, exportSchema = false)
abstract class ClearKeepDatabase : RoomDatabase() {
    abstract fun userDao(): ProfileDao
    abstract fun serverDao(): ServerDAO
    abstract fun messageDao(): MessageDAO
    abstract fun groupDao(): GroupDAO
    abstract fun peopleDao(): UserDao
}