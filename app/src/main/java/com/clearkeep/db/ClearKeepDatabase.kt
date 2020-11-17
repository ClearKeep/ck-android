package com.clearkeep.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.clearkeep.db.model.Message
import com.clearkeep.db.model.People
import com.clearkeep.db.model.Room
import com.clearkeep.db.model.User

@Database(entities = [
    User::class,
    Message::class,
    Room::class,
    People::class
], version = 4, exportSchema = false)
abstract class ClearKeepDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun messageDao(): MessageDAO
    abstract fun roomDao(): RoomDAO
    abstract fun peopleDao(): PeopleDao
}