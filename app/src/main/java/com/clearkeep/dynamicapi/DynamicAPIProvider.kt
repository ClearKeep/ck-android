package com.clearkeep.dynamicapi

import auth.AuthGrpc
import com.clearkeep.db.clearkeep.model.Server
import group.GroupGrpc
import message.MessageGrpc
import note.NoteGrpc
import notification.NotifyGrpc
import notify_push.NotifyPushGrpc
import signal.SignalKeyDistributionGrpc
import upload_file.UploadFileGrpc
import user.UserGrpc
import video_call.VideoCallGrpc
import workspace.WorkspaceGrpc

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

    fun provideNoteBlockingStub(): NoteGrpc.NoteBlockingStub

    fun provideNotifyPushBlockingStub(): NotifyPushGrpc.NotifyPushBlockingStub

    fun provideVideoCallBlockingStub(): VideoCallGrpc.VideoCallBlockingStub

    fun provideUploadFileBlockingStub(): UploadFileGrpc.UploadFileBlockingStub

    fun provideUploadFileStub(): UploadFileGrpc.UploadFileStub

    fun provideWorkSpaceBlockingStub(): WorkspaceGrpc.WorkspaceBlockingStub
}