package com.clearkeep.data.local.clearkeep

import android.provider.ContactsContract
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.clearkeep.data.local.clearkeep.converter.ProfileConverter
import com.clearkeep.data.local.clearkeep.dao.*
import com.clearkeep.domain.model.*

@Database(
    entities = [
        ContactsContract.Profile::class,
        Message::class,
        ChatGroup::class,
        UserEntity::class,
        Server::class,
        ContactsContract.CommonDataKinds.Note::class,
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