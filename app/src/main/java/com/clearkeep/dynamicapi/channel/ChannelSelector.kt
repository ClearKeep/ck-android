package com.clearkeep.dynamicapi.channel

import io.grpc.Channel
import java.util.HashMap
import javax.inject.Inject

class ChannelSelector @Inject constructor() {
    private val mChannelMap = HashMap<String, Channel>()

    fun getChannel(domain: String) : Channel {
        var channel = mChannelMap[domain]
        if (channel == null) {
            channel = ManagedChannelImpl().createManagedChannel(domain)
            mChannelMap[domain] = channel
        }
        return channel
    }
}