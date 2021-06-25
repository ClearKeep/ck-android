package com.clearkeep.dynamicapi

import android.text.TextUtils
import auth.AuthGrpc
import com.clearkeep.dynamicapi.channel.ChannelSelector
import com.clearkeep.utilities.UserManager
import group.GroupGrpc
import io.grpc.CallCredentials
import io.grpc.Metadata
import io.grpc.Status
import message.MessageGrpc
import notification.NotifyGrpc
import notify_push.NotifyPushGrpc
import signal.SignalKeyDistributionGrpc
import user.UserGrpc
import video_call.VideoCallGrpc
import java.lang.IllegalArgumentException
import java.util.concurrent.Executor
import javax.inject.Inject

class DynamicAPIProviderImpl @Inject constructor(
    private val channelSelector: ChannelSelector,
    private val userManager: UserManager,
) : DynamicAPIProvider {

    private var domain: String = ""
    
    override fun setUpDomain(domain: String) {
        this.domain = domain
    }
    
    override fun provideSignalKeyDistributionGrpc(): SignalKeyDistributionGrpc.SignalKeyDistributionStub {
        if (domain.isBlank()) {
            throw IllegalArgumentException("domain must be not blank")
        }
        val managedChannel = channelSelector.getChannel(domain)
        return SignalKeyDistributionGrpc.newStub(managedChannel)
    }

    override fun provideSignalKeyDistributionBlockingStub(): SignalKeyDistributionGrpc.SignalKeyDistributionBlockingStub {
        if (domain.isBlank()) {
            throw IllegalArgumentException("domain must be not blank")
        }
        val managedChannel = channelSelector.getChannel(domain)
        return SignalKeyDistributionGrpc.newBlockingStub(managedChannel)
    }

    override fun provideNotifyStub(): NotifyGrpc.NotifyStub {
        if (domain.isBlank()) {
            throw IllegalArgumentException("domain must be not blank")
        }
        val managedChannel = channelSelector.getChannel(domain)
        return NotifyGrpc.newStub(managedChannel)
    }

    override fun provideNotifyBlockingStub(): NotifyGrpc.NotifyBlockingStub {
        if (domain.isBlank()) {
            throw IllegalArgumentException("domain must be not blank")
        }
        val managedChannel = channelSelector.getChannel(domain)
        return NotifyGrpc.newBlockingStub(managedChannel)
    }

    override fun provideAuthBlockingStub(): AuthGrpc.AuthBlockingStub {
        if (domain.isBlank()) {
            throw IllegalArgumentException("domain must be not blank")
        }
        val managedChannel = channelSelector.getChannel(domain)
        return AuthGrpc.newBlockingStub(managedChannel)
    }

    override fun provideUserBlockingStub(): UserGrpc.UserBlockingStub {
        if (domain.isBlank()) {
            throw IllegalArgumentException("domain must be not blank")
        }
        val managedChannel = channelSelector.getChannel(domain)
        return UserGrpc.newBlockingStub(managedChannel)
            .withCallCredentials(CallCredentialsImpl(userManager.getAccessKey(), userManager.getHashKey()))
    }

    override fun provideGroupBlockingStub(): GroupGrpc.GroupBlockingStub {
        if (domain.isBlank()) {
            throw IllegalArgumentException("domain must be not blank")
        }
        val managedChannel = channelSelector.getChannel(domain)
        return GroupGrpc.newBlockingStub(managedChannel)
    }

    override fun provideMessageBlockingStub(): MessageGrpc.MessageBlockingStub {
        if (domain.isBlank()) {
            throw IllegalArgumentException("domain must be not blank")
        }
        val managedChannel = channelSelector.getChannel(domain)
        return MessageGrpc.newBlockingStub(managedChannel)
    }

    override fun provideMessageStub(): MessageGrpc.MessageStub {
        if (domain.isBlank()) {
            throw IllegalArgumentException("domain must be not blank")
        }
        val managedChannel = channelSelector.getChannel(domain)
        return MessageGrpc.newStub(managedChannel)
    }

    override fun provideNotifyPushBlockingStub(): NotifyPushGrpc.NotifyPushBlockingStub {
        if (domain.isBlank()) {
            throw IllegalArgumentException("domain must be not blank")
        }
        val managedChannel = channelSelector.getChannel(domain)
        return NotifyPushGrpc.newBlockingStub(managedChannel)
            .withCallCredentials(CallCredentialsImpl(userManager.getAccessKey(), userManager.getHashKey()))
    }

    override fun provideVideoCallBlockingStub(): VideoCallGrpc.VideoCallBlockingStub {
        if (domain.isBlank()) {
            throw IllegalArgumentException("domain must be not blank")
        }
        val managedChannel = channelSelector.getChannel(domain)
        return VideoCallGrpc.newBlockingStub(managedChannel)
            .withCallCredentials(
                CallCredentialsImpl(
                    userManager.getAccessKey(),
                    userManager.getHashKey()
                )
            )
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