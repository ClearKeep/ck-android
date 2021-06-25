package com.clearkeep.di

import com.clearkeep.dynamicapi.DynamicAPIProvider
import com.clearkeep.dynamicapi.DynamicAPIProviderImpl
import com.clearkeep.dynamicapi.channel.ChannelSelector
import com.clearkeep.dynamicapi.subscriber.DynamicSubscriberAPIProvider
import com.clearkeep.dynamicapi.subscriber.DynamicSubscriberAPIProviderImpl
import com.clearkeep.utilities.UserManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import javax.inject.Singleton

@InstallIn(ApplicationComponent::class)
@Module(includes = [ViewModelModule::class, StorageModule::class, DatabaseModule::class])
class AppModule {
    @Singleton
    @Provides
    fun provideChannelSelector(): ChannelSelector {
        return ChannelSelector()
    }

    @Singleton
    @Provides
    fun provideDynamicAPIProvider(selector: ChannelSelector, userManager: UserManager): DynamicAPIProvider {
        return DynamicAPIProviderImpl(selector, userManager)
    }

    @Singleton
    @Provides
    fun provideDynamicSubscriberAPIProvider(): DynamicSubscriberAPIProvider {
        return DynamicSubscriberAPIProviderImpl()
    }
}