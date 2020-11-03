package com.clearkeep.di

import android.app.Application
import androidx.room.Room
import com.clearkeep.db.MessageDAO
import com.clearkeep.db.UserDao
import com.clearkeep.db.ClearKeepDatabase
import com.clearkeep.db.RoomDAO
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import javax.inject.Singleton

@InstallIn(ApplicationComponent::class)
@Module
class DatabaseModule {
    @Singleton
    @Provides
    fun provideUserDatabase(app: Application): ClearKeepDatabase {
        return Room
            .databaseBuilder(app, ClearKeepDatabase::class.java, "ck_database.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Singleton
    @Provides
    fun provideUserDao(db: ClearKeepDatabase): UserDao {
        return db.userDao()
    }

    @Singleton
    @Provides
    fun provideMessageDAO(db: ClearKeepDatabase): MessageDAO {
        return db.messageDao()
    }

    @Singleton
    @Provides
    fun provideRoomDAO(db: ClearKeepDatabase): RoomDAO {
        return db.roomDao()
    }
}
