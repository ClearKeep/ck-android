package com.clearkeep.dynamicapi

import auth.AuthGrpc
import com.clearkeep.dynamicapi.channel.ChannelSelector
import group.GroupGrpc
import message.MessageGrpc
import note.NoteGrpc
import notification.NotifyGrpc
import notify_push.NotifyPushGrpc
import signal.SignalKeyDistributionGrpc
import user.UserGrpc
import video_call.VideoCallGrpc
import workspace.WorkspaceGrpc
import javax.inject.Inject

/*
* runtime server
* */
class ParamAPIProviderImpl @Inject constructor(
    private val channelSelector: ChannelSelector,
) : ParamAPIProvider {

    override fun provideSignalKeyDistributionGrpc(paramAPI: ParamAPI): SignalKeyDistributionGrpc.SignalKeyDistributionStub {
        val managedChannel = channelSelector.getChannel(paramAPI.serverDomain)
        return SignalKeyDistributionGrpc.newStub(managedChannel)
    }

    override fun provideSignalKeyDistributionBlockingStub(paramAPI: ParamAPI): SignalKeyDistributionGrpc.SignalKeyDistributionBlockingStub {
        if (paramAPI.accessKey == null || paramAPI.hashKey == null) {
            throw IllegalArgumentException("provideNotifyPushBlockingStub: access and hash key must not null")
        }
        val managedChannel = channelSelector.getChannel(paramAPI.serverDomain)
        return SignalKeyDistributionGrpc.newBlockingStub(managedChannel).withCallCredentials(
            CallCredentials(paramAPI.accessKey, paramAPI.hashKey)
        )
    }

    override fun provideNotifyStub(paramAPI: ParamAPI): NotifyGrpc.NotifyStub {
        if (paramAPI.accessKey == null || paramAPI.hashKey == null) {
            throw IllegalArgumentException("provideNotifyStub: access and hash key must not null")
        }
        val managedChannel = channelSelector.getChannel(paramAPI.serverDomain)
        return NotifyGrpc.newStub(managedChannel)
            .withCallCredentials(
                CallCredentials(
                    paramAPI.accessKey,
                    paramAPI.hashKey
                )
            )
    }

    override fun provideNotifyBlockingStub(paramAPI: ParamAPI): NotifyGrpc.NotifyBlockingStub {
        if (paramAPI.accessKey == null || paramAPI.hashKey == null) {
            throw IllegalArgumentException("provideNotifyBlockingStub: access and hash key must not null")
        }
        val managedChannel = channelSelector.getChannel(paramAPI.serverDomain)
        return NotifyGrpc.newBlockingStub(managedChannel)
            .withCallCredentials(
                CallCredentials(
                    paramAPI.accessKey,
                    paramAPI.hashKey
                )
            )
    }

    override fun provideAuthBlockingStub(paramAPI: ParamAPI): AuthGrpc.AuthBlockingStub {
        val managedChannel = channelSelector.getChannel(paramAPI.serverDomain)
        return AuthGrpc.newBlockingStub(managedChannel)
    }

    override fun provideUserBlockingStub(paramAPI: ParamAPI): UserGrpc.UserBlockingStub {
        if (paramAPI.accessKey == null || paramAPI.hashKey == null) {
            throw IllegalArgumentException("provideNotifyPushBlockingStub: access and hash key must not null")
        }
        val managedChannel = channelSelector.getChannel(paramAPI.serverDomain)
        return UserGrpc.newBlockingStub(managedChannel)
            .withCallCredentials(
                CallCredentials(
                    paramAPI.accessKey,
                    paramAPI.hashKey
                )
            )
    }

    override fun provideGroupBlockingStub(paramAPI: ParamAPI): GroupGrpc.GroupBlockingStub {
        val managedChannel = channelSelector.getChannel(paramAPI.serverDomain)
        if (paramAPI.accessKey == null || paramAPI.hashKey == null) {
            throw NullPointerException("provideNotifyPushBlockingStub: access and hash key must not null")
        }
        return GroupGrpc.newBlockingStub(managedChannel)
            .withCallCredentials(
                CallCredentials(
                    paramAPI.accessKey,
                    paramAPI.hashKey
                )
            )
    }

    override fun provideMessageBlockingStub(paramAPI: ParamAPI): MessageGrpc.MessageBlockingStub {
        if (paramAPI.accessKey == null || paramAPI.hashKey == null) {
            throw IllegalArgumentException("provideNotifyPushBlockingStub: access and hash key must not null")
        }
        val managedChannel = channelSelector.getChannel(paramAPI.serverDomain)
        return MessageGrpc.newBlockingStub(managedChannel)
            .withCallCredentials(
                CallCredentials(
                    paramAPI.accessKey,
                    paramAPI.hashKey
                )
            )
    }

    override fun provideNotesBlockingStub(paramAPI: ParamAPI): NoteGrpc.NoteBlockingStub {
        if (paramAPI.accessKey == null || paramAPI.hashKey == null) {
            throw IllegalArgumentException("provideNotifyPushBlockingStub: access and hash key must not null")
        }
        val managedChannel = channelSelector.getChannel(paramAPI.serverDomain)
        return NoteGrpc.newBlockingStub(managedChannel)
            .withCallCredentials(
                CallCredentials(
                    paramAPI.accessKey,
                    paramAPI.hashKey
                )
            )
    }

    override fun provideMessageStub(paramAPI: ParamAPI): MessageGrpc.MessageStub {
        if (paramAPI.accessKey == null || paramAPI.hashKey == null) {
            throw IllegalArgumentException("provideNotifyPushBlockingStub: access and hash key must not null")
        }
        val managedChannel = channelSelector.getChannel(paramAPI.serverDomain)
        return MessageGrpc.newStub(managedChannel)
            .withCallCredentials(
                CallCredentials(
                    paramAPI.accessKey,
                    paramAPI.hashKey
                )
            )
    }

    override fun provideNotifyPushBlockingStub(paramAPI: ParamAPI): NotifyPushGrpc.NotifyPushBlockingStub {
        if (paramAPI.accessKey == null || paramAPI.hashKey == null) {
            throw IllegalArgumentException("provideNotifyPushBlockingStub: access and hasl key must not null")
        }
        val managedChannel = channelSelector.getChannel(paramAPI.serverDomain)
        return NotifyPushGrpc.newBlockingStub(managedChannel)
            .withCallCredentials(
                CallCredentials(
                    paramAPI.accessKey,
                    paramAPI.hashKey
                )
            )
    }

    override fun provideVideoCallBlockingStub(paramAPI: ParamAPI): VideoCallGrpc.VideoCallBlockingStub {
        if (paramAPI.accessKey == null || paramAPI.hashKey == null) {
            throw IllegalArgumentException("provideVideoCallBlockingStub: access and hasl key must not null")
        }
        val managedChannel = channelSelector.getChannel(paramAPI.serverDomain)
        return VideoCallGrpc.newBlockingStub(managedChannel)
            .withCallCredentials(
                CallCredentials(
                    paramAPI.accessKey,
                    paramAPI.hashKey
                )
            )
    }

    override fun provideWorkspaceBlockingStub(paramAPI: ParamAPI): WorkspaceGrpc.WorkspaceBlockingStub {
        val managedChannel = channelSelector.getChannel(paramAPI.serverDomain)
        return WorkspaceGrpc.newBlockingStub(managedChannel)
    }
}