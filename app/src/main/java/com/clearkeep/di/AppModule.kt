package com.clearkeep.di

import com.clearkeep.chat.signal_store.InMemorySenderKeyStore
import com.clearkeep.chat.signal_store.InMemorySignalProtocolStore
import com.clearkeep.db.signal.SignalKeyDAO
import com.clearkeep.utilities.storage.Storage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import signalc.SignalKeyDistributionGrpc
import signalc_group.GroupSenderKeyDistributionGrpc
import javax.inject.Singleton

@InstallIn(ApplicationComponent::class)
@Module(includes = [ViewModelModule::class, StorageModule::class, DatabaseModule::class])
class AppModule {
    @Singleton
    @Provides
    fun provideSignalKeyDistributionGrpc(): SignalKeyDistributionGrpc.SignalKeyDistributionStub {
        val channel = ManagedChannelBuilder.forAddress(BASE_URL, 50051)
            .usePlaintext()
            .executor(Dispatchers.Default.asExecutor())
            .build()

        return SignalKeyDistributionGrpc.newStub(channel)
    }

    @Singleton
    @Provides
    fun provideSignalKeyDistributionBlockingStub(): SignalKeyDistributionGrpc.SignalKeyDistributionBlockingStub {
        val channel = ManagedChannelBuilder.forAddress(BASE_URL, 50051)
            .usePlaintext()
            .executor(Dispatchers.Default.asExecutor())
            .build()

        return SignalKeyDistributionGrpc.newBlockingStub(channel)
    }

    @Singleton
    @Provides
    fun provideInMemorySignalProtocolStore(storage: Storage): InMemorySignalProtocolStore {
        return InMemorySignalProtocolStore(storage)
    }

    @Singleton
    @Provides
    fun provideGroupSenderKeyDistributionStub(): GroupSenderKeyDistributionGrpc.GroupSenderKeyDistributionStub {
        val channel = ManagedChannelBuilder.forAddress(BASE_URL_GROUP, 50052)
            .usePlaintext()
            .executor(Dispatchers.Default.asExecutor())
            .build()

        return GroupSenderKeyDistributionGrpc.newStub(channel)
    }

    @Singleton
    @Provides
    fun provideGroupSenderKeyDistributionBlockingStub(): GroupSenderKeyDistributionGrpc.GroupSenderKeyDistributionBlockingStub {
        val channel = ManagedChannelBuilder.forAddress(BASE_URL_GROUP, 50052)
            .usePlaintext()
            .executor(Dispatchers.Default.asExecutor())
            .build()

        return GroupSenderKeyDistributionGrpc.newBlockingStub(channel)
    }

    @Singleton
    @Provides
    fun provideInMemorySenderKeyStore(signalKeyDAO: SignalKeyDAO): InMemorySenderKeyStore {
        return InMemorySenderKeyStore(signalKeyDAO)
    }

    companion object {
        private const val BASE_URL = "172.16.1.41"
        private const val BASE_URL_GROUP = "172.16.1.41"
    }
}
