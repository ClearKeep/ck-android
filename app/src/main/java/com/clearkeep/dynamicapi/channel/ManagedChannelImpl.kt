package com.clearkeep.dynamicapi.channel

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

class ManagedChannelImpl : ChannelFactory {
    override fun createManagedChannel(domain: String): ManagedChannel {
        return ManagedChannelBuilder.forTarget(domain)
            .usePlaintext()
            .executor(Dispatchers.Default.asExecutor())
            .build()
    }
}