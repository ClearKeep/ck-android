package com.clearkeep.di

import android.text.TextUtils
import auth.AuthGrpc
import com.clearkeep.db.signal_key.dao.SignalIdentityKeyDAO
import com.clearkeep.screen.chat.signal_store.InMemorySenderKeyStore
import com.clearkeep.screen.chat.signal_store.InMemorySignalProtocolStore
import com.clearkeep.db.signal_key.dao.SignalKeyDAO
import com.clearkeep.db.signal_key.dao.SignalPreKeyDAO
import com.clearkeep.utilities.BASE_URL
import com.clearkeep.utilities.PORT
import com.clearkeep.utilities.UserManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import group.GroupGrpc
import io.grpc.CallCredentials
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import signal.SignalKeyDistributionGrpc
import java.util.concurrent.Executor
import javax.inject.Singleton
import io.grpc.Metadata
import io.grpc.Status
import message.MessageGrpc
import notification.NotifyGrpc
import notify_push.NotifyPushGrpc
import user.UserGrpc
import video_call.VideoCallGrpc
import java.util.concurrent.TimeUnit

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
            .withDeadlineAfter(REQUEST_TIME_OUT, TimeUnit.MILLISECONDS)
    }

    @Singleton
    @Provides
    fun provideSignalKeyDistributionBlockingStub(): SignalKeyDistributionGrpc.SignalKeyDistributionBlockingStub {
        val channel = ManagedChannelBuilder.forAddress(BASE_URL, PORT)
            .usePlaintext()
            .executor(Dispatchers.Default.asExecutor())
            .build()

        return SignalKeyDistributionGrpc.newBlockingStub(channel)
            .withDeadlineAfter(REQUEST_TIME_OUT, TimeUnit.MILLISECONDS)
    }

    @Singleton
    @Provides
    fun provideNotifyStub(): NotifyGrpc.NotifyStub {
        val channel = ManagedChannelBuilder.forAddress(BASE_URL, PORT)
                .usePlaintext()
                .executor(Dispatchers.Default.asExecutor())
                .build()

        return NotifyGrpc.newStub(channel)
            .withDeadlineAfter(REQUEST_TIME_OUT, TimeUnit.MILLISECONDS)
    }

    @Singleton
    @Provides
    fun provideAuthBlockingStub(): AuthGrpc.AuthBlockingStub {
        val channel = ManagedChannelBuilder.forAddress(BASE_URL, PORT)
                .usePlaintext()
                .executor(Dispatchers.Default.asExecutor())
                .build()

        return AuthGrpc.newBlockingStub(channel)
            .withDeadlineAfter(REQUEST_TIME_OUT, TimeUnit.MILLISECONDS)
    }

    @Singleton
    @Provides
    fun provideUserBlockingStub(userManager: UserManager): UserGrpc.UserBlockingStub {
        val channel = ManagedChannelBuilder.forAddress(BASE_URL, PORT)
            .usePlaintext()
            .executor(Dispatchers.Default.asExecutor())
            .build()

        return UserGrpc.newBlockingStub(channel)
            .withCallCredentials(CallCredentialsImpl(userManager.getAccessKey(), userManager.getHashKey()))
            .withDeadlineAfter(REQUEST_TIME_OUT, TimeUnit.MILLISECONDS)
    }

    @Singleton
    @Provides
    fun provideGroupBlockingStub(): GroupGrpc.GroupBlockingStub {
        val channel = ManagedChannelBuilder.forAddress(BASE_URL, PORT)
                .usePlaintext()
                .executor(Dispatchers.Default.asExecutor())
                .build()

        return GroupGrpc.newBlockingStub(channel)
            .withDeadlineAfter(REQUEST_TIME_OUT, TimeUnit.MILLISECONDS)
    }

    @Singleton
    @Provides
    fun provideMessageBlockingStub(): MessageGrpc.MessageBlockingStub {
        val channel = ManagedChannelBuilder.forAddress(BASE_URL, PORT)
                .usePlaintext()
                .executor(Dispatchers.Default.asExecutor())
                .build()

        return MessageGrpc.newBlockingStub(channel)
            .withDeadlineAfter(REQUEST_TIME_OUT, TimeUnit.MILLISECONDS)
    }

    @Singleton
    @Provides
    fun provideMessageStub(): MessageGrpc.MessageStub {
        val channel = ManagedChannelBuilder.forAddress(BASE_URL, PORT)
            .usePlaintext()
            .executor(Dispatchers.Default.asExecutor())
            .build()

        return MessageGrpc.newStub(channel)
            .withDeadlineAfter(REQUEST_TIME_OUT, TimeUnit.MILLISECONDS)
    }

    @Singleton
    @Provides
    fun provideNotifyPushBlockingStub(userManager: UserManager): NotifyPushGrpc.NotifyPushBlockingStub {
        val channel = ManagedChannelBuilder.forAddress(BASE_URL, PORT)
                .usePlaintext()
                .executor(Dispatchers.Default.asExecutor())
                .build()

        return NotifyPushGrpc.newBlockingStub(channel)
                .withCallCredentials(CallCredentialsImpl(userManager.getAccessKey(), userManager.getHashKey()))
            .withDeadlineAfter(REQUEST_TIME_OUT, TimeUnit.MILLISECONDS)
    }

    @Singleton
    @Provides
    fun provideVideoCallBlockingStub(userManager: UserManager): VideoCallGrpc.VideoCallBlockingStub {
        val channel = ManagedChannelBuilder.forAddress(BASE_URL, PORT)
                .usePlaintext()
                .executor(Dispatchers.Default.asExecutor())
                .build()

        return VideoCallGrpc.newBlockingStub(channel)
            .withDeadlineAfter(REQUEST_TIME_OUT, TimeUnit.MILLISECONDS)
            .withCallCredentials(
                CallCredentialsImpl(
                    userManager.getAccessKey(),
                    userManager.getHashKey()
                )
            )
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

    companion object {
        private const val REQUEST_TIME_OUT = 30 * 1000L
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

                val domainMetaKey: Metadata.Key<String> =
                    Metadata.Key.of("domain", Metadata.ASCII_STRING_MARSHALLER)
                headers.put(domainMetaKey, "localhost")
                val ipAddressMetaKey: Metadata.Key<String> =
                    Metadata.Key.of("ip_address", Metadata.ASCII_STRING_MARSHALLER)
                headers.put(ipAddressMetaKey, "0.0.0.0")

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