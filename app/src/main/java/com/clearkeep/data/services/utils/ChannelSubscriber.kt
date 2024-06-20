package com.clearkeep.data.services.utils

interface ChannelSubscriber {
    suspend fun subscribeAndListen()
    suspend fun shutdown()
}