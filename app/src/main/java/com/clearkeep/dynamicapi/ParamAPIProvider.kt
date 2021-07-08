package com.clearkeep.dynamicapi

import auth.AuthGrpc
import group.GroupGrpc
import message.MessageGrpc
import notification.NotifyGrpc
import notify_push.NotifyPushGrpc
import signal.SignalKeyDistributionGrpc
import user.UserGrpc
import video_call.VideoCallGrpc

interface ParamAPIProvider {
    fun provideSignalKeyDistributionGrpc(paramAPI: ParamAPI): SignalKeyDistributionGrpc.SignalKeyDistributionStub

    fun provideSignalKeyDistributionBlockingStub(paramAPI: ParamAPI): SignalKeyDistributionGrpc.SignalKeyDistributionBlockingStub

    fun provideNotifyStub(paramAPI: ParamAPI): NotifyGrpc.NotifyStub

    fun provideNotifyBlockingStub(paramAPI: ParamAPI): NotifyGrpc.NotifyBlockingStub

    fun provideAuthBlockingStub(paramAPI: ParamAPI): AuthGrpc.AuthBlockingStub

    fun provideUserBlockingStub(paramAPI: ParamAPI): UserGrpc.UserBlockingStub

    fun provideGroupBlockingStub(paramAPI: ParamAPI): GroupGrpc.GroupBlockingStub

    fun provideMessageBlockingStub(paramAPI: ParamAPI): MessageGrpc.MessageBlockingStub

    fun provideMessageStub(paramAPI: ParamAPI): MessageGrpc.MessageStub

    fun provideNotifyPushBlockingStub(paramAPI: ParamAPI): NotifyPushGrpc.NotifyPushBlockingStub

    fun provideVideoCallBlockingStub(paramAPI: ParamAPI): VideoCallGrpc.VideoCallBlockingStub
}

class ParamAPI(
    val serverDomain: String,
    val accessKey: String? = null,
    val hashKey: String? = null,
)