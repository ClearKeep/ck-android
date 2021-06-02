package com.clearkeep.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.clearkeep.db.clear_keep.dao.GroupDAO
import com.clearkeep.db.clear_keep.dao.MessageDAO
import com.clearkeep.db.clear_keep.dao.PeopleDao
import com.clearkeep.db.clear_keep.dao.UserDao
import com.clearkeep.db.clear_keep.model.*

@Database(entities = [
    User::class,
    Message::class,
    ChatGroup::class,
    People::class
], version = 6, exportSchema = false)
abstract class ClearKeepDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun messageDao(): MessageDAO
    abstract fun groupDao(): GroupDAO
    abstract fun peopleDao(): PeopleDao
}