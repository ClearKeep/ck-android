package com.clearkeep.di

import com.clearkeep.dynamicapi.*
import com.clearkeep.dynamicapi.channel.ChannelSelector
import com.clearkeep.dynamicapi.subscriber.DynamicSubscriberAPIProvider
import com.clearkeep.dynamicapi.subscriber.DynamicSubscriberAPIProviderImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module(includes = [ViewModelModule::class, StorageModule::class, DatabaseModule::class])
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

    @Singleton
    @Provides
    fun provideEnvironment(dynamicAPIProvider: DynamicAPIProvider): Environment {
        return Environment(dynamicAPIProvider)
    }
}