package com.clearkeep.services.utils

interface ChannelSubscriber {
    suspend fun subscribeAndListen()
    suspend fun shutdown()
}