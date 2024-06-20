package com.clearkeep.data.di

import android.app.Application
import androidx.room.Room
import com.clearkeep.data.local.clearkeep.ClearKeepDatabase
import com.clearkeep.data.local.clearkeep.userkey.UserKeyDAO
import com.clearkeep.data.local.clearkeep.group.GroupDAO
import com.clearkeep.data.local.clearkeep.message.MessageDAO
import com.clearkeep.data.local.clearkeep.server.ServerDAO
import com.clearkeep.data.local.clearkeep.user.UserDAO
import com.clearkeep.data.local.signal.identitykey.SignalIdentityKeyDAO
import com.clearkeep.data.local.signal.senderkey.SignalKeyDAO
import com.clearkeep.data.local.signal.SignalKeyDatabase
import com.clearkeep.data.local.signal.prekey.SignalPreKeyDAO
import com.clearkeep.data.local.signal.store.InMemorySenderKeyStore
import com.clearkeep.data.remote.dynamicapi.Environment
import com.clearkeep.data.local.signal.store.InMemorySignalProtocolStore
import com.clearkeep.data.local.clearkeep.userpreference.UserPreferenceDAO
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
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
    fun provideMessageDAO(db: ClearKeepDatabase): MessageDAO {
        return db.messageDao()
    }

    @Singleton
    @Provides
    fun provideGroupDAO(db: ClearKeepDatabase): GroupDAO {
        return db.groupDao()
    }

    @Singleton
    @Provides
    fun providePeopleDAO(db: ClearKeepDatabase): UserDAO {
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
    fun provideUserKeyDao(db: ClearKeepDatabase): UserKeyDAO {
        return db.userKeyDao()
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
        environment: Environment,
    ): InMemorySignalProtocolStore {
        return InMemorySignalProtocolStore(preKeyDAO, signalIdentityKeyDAO, environment)
    }

    @Singleton
    @Provides
    fun provideInMemorySenderKeyStore(
        signalKeyDAO: SignalKeyDAO,
    ): InMemorySenderKeyStore {
        return InMemorySenderKeyStore(signalKeyDAO)
    }
}
