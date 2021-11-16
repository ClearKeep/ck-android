package com.clearkeep.data.remote.dynamicapi.subscriber

import message.MessageGrpc
import notification.NotifyGrpc

interface DynamicSubscriberAPIProvider {
    fun provideNotifyStub(domain: String): NotifyGrpc.NotifyStub
    fun provideNotifyBlockingStub(domain: String): NotifyGrpc.NotifyBlockingStub
    fun provideMessageBlockingStub(domain: String): MessageGrpc.MessageBlockingStub
    fun provideMessageStub(domain: String): MessageGrpc.MessageStub
    fun shutDownAll()
}