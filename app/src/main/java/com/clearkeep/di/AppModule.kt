package com.clearkeep.di

import android.text.TextUtils
import auth.AuthGrpc
import com.clearkeep.chat.signal_store.InMemorySenderKeyStore
import com.clearkeep.chat.signal_store.InMemorySignalProtocolStore
import com.clearkeep.db.signal.SignalKeyDAO
import com.clearkeep.repository.UserRepository
import com.clearkeep.utilities.storage.Storage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import io.grpc.CallCredentials
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import signal.SignalKeyDistributionGrpc
import java.util.concurrent.Executor
import javax.inject.Singleton
import io.grpc.Metadata
import io.grpc.Status
import user.UserGrpc

@InstallIn(ApplicationComponent::class)
@Module(includes = [ViewModelModule::class, StorageModule::class, DatabaseModule::class])
class AppModule {
    @Singleton
    @Provides
    fun provideSignalKeyDistributionGrpc(): SignalKeyDistributionGrpc.SignalKeyDistributionStub {
        val channel = ManagedChannelBuilder.forAddress(BASE_URL, PORT)
            .usePlaintext()
            .executor(Dispatchers.Default.asExecutor())
            .build()

        return SignalKeyDistributionGrpc.newStub(channel)
    }

    @Singleton
    @Provides
    fun provideSignalKeyDistributionBlockingStub(): SignalKeyDistributionGrpc.SignalKeyDistributionBlockingStub {
        val channel = ManagedChannelBuilder.forAddress(BASE_URL, PORT)
            .usePlaintext()
            .executor(Dispatchers.Default.asExecutor())
            .build()

        return SignalKeyDistributionGrpc.newBlockingStub(channel)
    }

    @Singleton
    @Provides
    fun provideAuthStub(): AuthGrpc.AuthStub {
        val channel = ManagedChannelBuilder.forAddress(BASE_URL, PORT)
                .usePlaintext()
                .executor(Dispatchers.Default.asExecutor())
                .build()

        return AuthGrpc.newStub(channel)
    }


    @Singleton
    @Provides
    fun provideAuthBlockingStub(): AuthGrpc.AuthBlockingStub {
        val channel = ManagedChannelBuilder.forAddress(BASE_URL, PORT)
                .usePlaintext()
                .executor(Dispatchers.Default.asExecutor())
                .build()

        return AuthGrpc.newBlockingStub(channel)
    }

    @Singleton
    @Provides
    fun provideUserStub(userRepository: UserRepository): UserGrpc.UserStub {
        val channel = ManagedChannelBuilder.forAddress(BASE_URL, PORT)
            .usePlaintext()
            .executor(Dispatchers.Default.asExecutor())
            .build()

        return UserGrpc.newStub(channel)
            .withCallCredentials(CallCredentialsImpl(userRepository.getAccessKey(), userRepository.getHashKey()))
    }


    @Singleton
    @Provides
    fun provideUserBlockingStub(userRepository: UserRepository): UserGrpc.UserBlockingStub {
        val channel = ManagedChannelBuilder.forAddress(BASE_URL, PORT)
            .usePlaintext()
            .executor(Dispatchers.Default.asExecutor())
            .build()

        return UserGrpc.newBlockingStub(channel)
            .withCallCredentials(CallCredentialsImpl(userRepository.getAccessKey(), userRepository.getHashKey()))
    }

    @Singleton
    @Provides
    fun provideInMemorySignalProtocolStore(storage: Storage): InMemorySignalProtocolStore {
        return InMemorySignalProtocolStore(storage)
    }

    @Singleton
    @Provides
    fun provideInMemorySenderKeyStore(signalKeyDAO: SignalKeyDAO): InMemorySenderKeyStore {
        return InMemorySenderKeyStore(signalKeyDAO)
    }

    companion object {
        private const val BASE_URL = "172.16.0.216"
        //private const val BASE_URL = "172.16.1.41"
        private const val PORT = 5000
    }
}

class CallCredentialsImpl(
    private val accessKey: String,
    private val hashKey: String
) : CallCredentials() {
    override fun applyRequestMetadata(
        requestInfo: RequestInfo,
        appExecutor: Executor,
        applier: MetadataApplier
    ) {
        appExecutor.execute {
            try {
                val headers = Metadata()
                if (!TextUtils.isEmpty(accessKey)) {
                    val accessMetaKey: Metadata.Key<String> =
                        Metadata.Key.of(ACCESS_TOKEN, Metadata.ASCII_STRING_MARSHALLER)
                    headers.put(accessMetaKey, accessKey)
                }
                if (!TextUtils.isEmpty(hashKey)) {
                    val hashMetaKey: Metadata.Key<String> =
                        Metadata.Key.of(HASH_KEY, Metadata.ASCII_STRING_MARSHALLER)
                    headers.put(hashMetaKey, hashKey)
                }
                applier.apply(headers)
            } catch (e: Throwable) {
                applier.fail(Status.UNAUTHENTICATED.withCause(e))
            }
        }
    }

    override fun thisUsesUnstableApi() {
    }

    companion object {
        private const val ACCESS_TOKEN = "access_token"
        private const val HASH_KEY = "hash_key"
    }
}