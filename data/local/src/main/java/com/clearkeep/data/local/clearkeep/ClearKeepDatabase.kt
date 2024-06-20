package com.clearkeep.data.local.clearkeep

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.clearkeep.data.local.clearkeep.converter.ProfileConverter
import com.clearkeep.data.local.clearkeep.group.ChatGroupEntity
import com.clearkeep.data.local.clearkeep.userkey.UserKeyDAO
import com.clearkeep.data.local.clearkeep.group.GroupDAO
import com.clearkeep.data.local.clearkeep.message.MessageDAO
import com.clearkeep.data.local.clearkeep.message.MessageEntity
import com.clearkeep.data.local.clearkeep.server.ProfileEntity
import com.clearkeep.data.local.clearkeep.server.ServerDAO
import com.clearkeep.data.local.clearkeep.server.ServerEntity
import com.clearkeep.data.local.clearkeep.userkey.UserKeyEntity
import com.clearkeep.data.local.clearkeep.user.UserDAO
import com.clearkeep.data.local.clearkeep.user.UserEntity
import com.clearkeep.data.local.clearkeep.userpreference.UserPreferenceDAO
import com.clearkeep.data.local.clearkeep.userpreference.UserPreferenceEntity

@Database(
    entities = [
        ProfileEntity::class,
        MessageEntity::class,
        ChatGroupEntity::class,
        UserEntity::class,
        ServerEntity::class,
        UserPreferenceEntity::class,
        UserKeyEntity::class
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