package com.clearkeep.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.clearkeep.db.model.*

@Database(entities = [
    User::class,
    Message::class,
    ChatGroup::class,
    People::class
], version = 1, exportSchema = false)
abstract class ClearKeepDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun messageDao(): MessageDAO
    abstract fun groupDao(): GroupDAO
    abstract fun peopleDao(): PeopleDao
}