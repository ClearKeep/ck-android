package com.clearkeep.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.clearkeep.model.User

@Database(
    entities = [User::class],
    version = UserDatabase.DATABASE_VERSION
)
abstract class UserDatabase : RoomDatabase() {
    abstract fun userDAO(): UserDAO?

    companion object {
        private var sUserDatabase: UserDatabase? = null
        const val DATABASE_VERSION = 1
        const val DATABASE_NAME = "Room-database"
        fun getInstance(context: Context): UserDatabase? {
            if (sUserDatabase == null) {
                sUserDatabase =
                    Room.databaseBuilder(
                        context,
                        UserDatabase::class.java,
                        DATABASE_NAME
                    )
                        .fallbackToDestructiveMigration()
//                        .allowMainThreadQueries()
                        .build()
            }
            return sUserDatabase
        }
    }
}