package com.clearkeep.dynamicapi.channel

import io.grpc.ManagedChannel

interface ChannelFactory {
    fun createManagedChannel(domain: String): ManagedChannel
}