package com.clearkeep.data.di

import com.clearkeep.data.remote.dynamicapi.*
import com.clearkeep.data.remote.dynamicapi.channel.ChannelSelector
import com.clearkeep.data.remote.dynamicapi.subscriber.DynamicSubscriberAPIProvider
import com.clearkeep.data.remote.dynamicapi.subscriber.DynamicSubscriberAPIProviderImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module(includes = [StorageModule::class, DatabaseModule::class])
class AppModule {
    @Singleton
    @Provides
    fun provideChannelSelector(): ChannelSelector {
        return ChannelSelector()
    }

    @Singleton
    @Provides
    fun provideDynamicAPIProvider(selector: ChannelSelector): DynamicAPIProvider {
        return DynamicAPIProviderImpl(selector)
    }

    @Singleton
    @Provides
    fun provideParamAPIProvider(selector: ChannelSelector): ParamAPIProvider {
        return ParamAPIProviderImpl(selector)
    }

    @Singleton
    @Provides
    fun provideDynamicSubscriberAPIProvider(): DynamicSubscriberAPIProvider {
        return DynamicSubscriberAPIProviderImpl()
    }
}