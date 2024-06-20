package com.clearkeep.data.remote.dynamicapi

import android.text.TextUtils
import android.util.Log
import auth.AuthOuterClass
import io.grpc.CallCredentials
import io.grpc.Metadata
import io.grpc.Status
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.Executor

class CallCredentials(
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
                Log.d("antx: ", "CallCredentials applyRequestMetadata line = 41: ${e.message} " );
                applier.fail(Status.UNAUTHENTICATED.withCause(e))

            }
        }
    }

    override fun thisUsesUnstableApi() {
        Log.d("antx: ", "CallCredentials thisUsesUnstableApi line = 50: " );
        var req=AuthOuterClass.RefreshTokenReq.newBuilder()
            .setRefreshToken(accessKey)
            .build()

    }

    companion object {
        private const val ACCESS_TOKEN = "access_token"
        private const val HASH_KEY = "hash_key"
    }
}