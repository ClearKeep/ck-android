package com.clearkeep.di

import android.app.Application
import androidx.room.Room
import com.clearkeep.db.*
import com.clearkeep.db.signal_key.dao.SignalIdentityKeyDAO
import com.clearkeep.db.signal_key.dao.SignalKeyDAO
import com.clearkeep.db.SignalKeyDatabase
import com.clearkeep.db.clear_keep.dao.*
import com.clearkeep.db.signal_key.dao.SignalPreKeyDAO
import com.clearkeep.screen.chat.signal_store.InMemorySenderKeyStore
import com.clearkeep.screen.chat.signal_store.InMemorySignalProtocolStore
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
    fun provideUserDao(db: ClearKeepDatabase): ProfileDao {
        return db.profileDao()
    }

    @Singleton
    @Provides
    fun provideMessageDAO(db: ClearKeepDatabase): MessageDAO {
        return db.messageDao()
    }

    @Singleton
    @Provides
    fun provideNoteDAO(db: ClearKeepDatabase): NoteDAO {
        return db.noteDao()
    }

    @Singleton
    @Provides
    fun provideGroupDAO(db: ClearKeepDatabase): GroupDAO {
        return db.groupDao()
    }

    @Singleton
    @Provides
    fun providePeopleDAO(db: ClearKeepDatabase): UserDao {
        return db.userDao()
    }

    @Singleton
    @Provides
    fun provideServerDao(db: ClearKeepDatabase): ServerDAO {
        return db.serverDao()
    }

    @Singleton
    @Provides
    fun provideUserPreferenceDao(db: ClearKeepDatabase): UserPreferenceDAO {
        return db.userPreferenceDao()
    }

    @Singleton
    @Provides
    fun provideSignalKeyDatabase(app: Application): SignalKeyDatabase {
        return Room
                .databaseBuilder(app, SignalKeyDatabase::class.java, "ck_signal_database.db")
                .fallbackToDestructiveMigration()
                .build()
    }

    @Singleton
    @Provides
    fun provideSignalKeyDAO(db: SignalKeyDatabase): SignalKeyDAO {
        return db.signalKeyDao()
    }

    @Singleton
    @Provides
    fun provideSignalPreKeyDAO(db: SignalKeyDatabase): SignalPreKeyDAO {
        return db.signalPreKeyDao()
    }

    @Singleton
    @Provides
    fun provideSignalIdentityKeyDAO(db: SignalKeyDatabase): SignalIdentityKeyDAO {
        return db.signalIdentityKeyDao()
    }

    @Singleton
    @Provides
    fun provideInMemorySignalProtocolStore(
        preKeyDAO: SignalPreKeyDAO,
        signalIdentityKeyDAO: SignalIdentityKeyDAO,
    ): InMemorySignalProtocolStore {
        return InMemorySignalProtocolStore(preKeyDAO, signalIdentityKeyDAO)
    }

    @Singleton
    @Provides
    fun provideInMemorySenderKeyStore(
        signalKeyDAO: SignalKeyDAO,
    ): InMemorySenderKeyStore {
        return InMemorySenderKeyStore(signalKeyDAO)
    }
}
