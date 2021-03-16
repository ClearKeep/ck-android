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
import io.grpc.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import signal.SignalKeyDistributionGrpc
import java.util.concurrent.Executor
import javax.inject.Singleton
import message.MessageGrpc
import notification.NotifyGrpc
import notify_push.NotifyPushGrpc
import user.UserGrpc
import video_call.VideoCallGrpc

@InstallIn(ApplicationComponent::class)
@Module(includes = [ViewModelModule::class, StorageModule::class, DatabaseModule::class])
class AppModule {
    @Singleton
    @Provides
    fun provideManagedChannel(): ManagedChannel {
        return ManagedChannelBuilder.forAddress(BASE_URL, PORT)
            .usePlaintext()
            .executor(Dispatchers.Default.asExecutor())
            .build()
    }

    @Singleton
    @Provides
    fun provideSignalKeyDistributionGrpc(managedChannel: ManagedChannel): SignalKeyDistributionGrpc.SignalKeyDistributionStub {
        return SignalKeyDistributionGrpc.newStub(managedChannel)
    }

    @Singleton
    @Provides
    fun provideSignalKeyDistributionBlockingStub(managedChannel: ManagedChannel): SignalKeyDistributionGrpc.SignalKeyDistributionBlockingStub {
        return SignalKeyDistributionGrpc.newBlockingStub(managedChannel)
    }

    @Singleton
    @Provides
    fun provideNotifyStub(managedChannel: ManagedChannel): NotifyGrpc.NotifyStub {
        return NotifyGrpc.newStub(managedChannel)
    }

    @Singleton
    @Provides
    fun provideNotifyBlockingStub(managedChannel: ManagedChannel): NotifyGrpc.NotifyBlockingStub {
        return NotifyGrpc.newBlockingStub(managedChannel)
    }

    @Singleton
    @Provides
    fun provideAuthBlockingStub(managedChannel: ManagedChannel): AuthGrpc.AuthBlockingStub {
        return AuthGrpc.newBlockingStub(managedChannel)
    }

    @Singleton
    @Provides
    fun provideUserBlockingStub(userManager: UserManager, managedChannel: ManagedChannel): UserGrpc.UserBlockingStub {
        return UserGrpc.newBlockingStub(managedChannel)
            .withCallCredentials(CallCredentialsImpl(userManager.getAccessKey(), userManager.getHashKey()))
    }

    @Singleton
    @Provides
    fun provideGroupBlockingStub(managedChannel: ManagedChannel): GroupGrpc.GroupBlockingStub {
        return GroupGrpc.newBlockingStub(managedChannel)
    }

    @Singleton
    @Provides
    fun provideMessageBlockingStub(managedChannel: ManagedChannel): MessageGrpc.MessageBlockingStub {
        return MessageGrpc.newBlockingStub(managedChannel)
    }

    @Singleton
    @Provides
    fun provideMessageStub(managedChannel: ManagedChannel): MessageGrpc.MessageStub {
        return MessageGrpc.newStub(managedChannel)
    }

    @Singleton
    @Provides
    fun provideNotifyPushBlockingStub(userManager: UserManager, managedChannel: ManagedChannel): NotifyPushGrpc.NotifyPushBlockingStub {
        return NotifyPushGrpc.newBlockingStub(managedChannel)
                .withCallCredentials(CallCredentialsImpl(userManager.getAccessKey(), userManager.getHashKey()))
    }

    @Singleton
    @Provides
    fun provideVideoCallBlockingStub(userManager: UserManager, managedChannel: ManagedChannel): VideoCallGrpc.VideoCallBlockingStub {
        return VideoCallGrpc.newBlockingStub(managedChannel)
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