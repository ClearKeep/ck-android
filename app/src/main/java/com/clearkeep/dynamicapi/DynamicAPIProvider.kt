package com.clearkeep.dynamicapi

import auth.AuthGrpc
import com.clearkeep.db.clear_keep.model.Server
import group.GroupGrpc
import message.MessageGrpc
import notification.NotifyGrpc
import notify_push.NotifyPushGrpc
import signal.SignalKeyDistributionGrpc
import user.UserGrpc
import video_call.VideoCallGrpc

interface DynamicAPIProvider {
    fun setUpDomain(server: Server)

    fun provideSignalKeyDistributionGrpc(): SignalKeyDistributionGrpc.SignalKeyDistributionStub

    fun provideSignalKeyDistributionBlockingStub(): SignalKeyDistributionGrpc.SignalKeyDistributionBlockingStub

    fun provideNotifyStub(): NotifyGrpc.NotifyStub

    fun provideNotifyBlockingStub(): NotifyGrpc.NotifyBlockingStub

    fun provideAuthBlockingStub(): AuthGrpc.AuthBlockingStub

    fun provideUserBlockingStub(): UserGrpc.UserBlockingStub

    fun provideGroupBlockingStub(): GroupGrpc.GroupBlockingStub

    fun provideMessageBlockingStub(): MessageGrpc.MessageBlockingStub

    fun provideMessageStub(): MessageGrpc.MessageStub

    fun provideNotifyPushBlockingStub(): NotifyPushGrpc.NotifyPushBlockingStub

    fun provideVideoCallBlockingStub(): VideoCallGrpc.VideoCallBlockingStub
}