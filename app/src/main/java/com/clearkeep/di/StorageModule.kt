package com.clearkeep.di

import com.clearkeep.utilities.storage.PersistPreferencesStorage
import com.clearkeep.utilities.storage.Storage
import com.clearkeep.utilities.storage.UserPreferencesStorage
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.multibindings.IntoMap

@InstallIn(ApplicationComponent::class)
@Module
abstract class StorageModule {
    @Binds
    @IntoMap
    @StorageKey(UserPreferencesStorage::class)
    abstract fun provideStorageByUser(storage: UserPreferencesStorage): Storage

    @Binds
    @IntoMap
    @StorageKey(PersistPreferencesStorage::class)
    abstract fun providePersistAppStorage(storage: PersistPreferencesStorage): Storage
}
