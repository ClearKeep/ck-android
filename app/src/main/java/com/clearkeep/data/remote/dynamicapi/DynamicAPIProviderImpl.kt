package com.clearkeep.data.remote.dynamicapi

import auth.AuthGrpc
import com.clearkeep.domain.model.Server
import com.clearkeep.data.remote.dynamicapi.channel.ChannelSelector
import com.clearkeep.data.remote.utils.ServiceExceptionHandler
import com.clearkeep.utilities.printlnCK
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
import java.lang.IllegalArgumentException
import javax.inject.Inject

/*
* active server
* */
class DynamicAPIProviderImpl @Inject constructor(
    private val channelSelector: ChannelSelector,
) : DynamicAPIProvider {

    private var server: Server? = null

    override fun setUpDomain(server: Server) {
        printlnCK("setUpDomain, domain = ${server.serverDomain}")
        this.server = server
    }

    override fun provideSignalKeyDistributionGrpc(): SignalKeyDistributionGrpc.SignalKeyDistributionStub {
        if (server == null) {
            throw IllegalArgumentException("server must be not null")
        }
        val managedChannel = channelSelector.getChannel(server!!.serverDomain)
        return SignalKeyDistributionGrpc.newStub(managedChannel).withInterceptors(ServiceExceptionHandler())
    }

    override fun provideSignalKeyDistributionBlockingStub(): SignalKeyDistributionGrpc.SignalKeyDistributionBlockingStub {
        if (server == null) {
            throw IllegalArgumentException("server must be not null")
        }
        val managedChannel = channelSelector.getChannel(server!!.serverDomain)
        return SignalKeyDistributionGrpc.newBlockingStub(managedChannel).withCallCredentials(
            CallCredentials(
                server!!.accessKey,
                server!!.hashKey
            )
        ).withInterceptors(ServiceExceptionHandler())
    }

    override fun provideNotifyStub(): NotifyGrpc.NotifyStub {
        if (server == null) {
            throw IllegalArgumentException("server must be not null")
        }
        val managedChannel = channelSelector.getChannel(server!!.serverDomain)
        return NotifyGrpc.newStub(managedChannel).withInterceptors(ServiceExceptionHandler())
    }

    override fun provideNotifyBlockingStub(): NotifyGrpc.NotifyBlockingStub {
        if (server == null) {
            throw IllegalArgumentException("server must be not null")
        }
        val managedChannel = channelSelector.getChannel(server!!.serverDomain)
        return NotifyGrpc.newBlockingStub(managedChannel).withInterceptors(ServiceExceptionHandler())
    }

    override fun provideAuthBlockingStub(): AuthGrpc.AuthBlockingStub {
        if (server == null) {
            throw IllegalArgumentException("server must be not null")
        }
        val managedChannel = channelSelector.getChannel(server!!.serverDomain)
        return AuthGrpc.newBlockingStub(managedChannel).withInterceptors(ServiceExceptionHandler())
    }

    override fun provideUserBlockingStub(): UserGrpc.UserBlockingStub {
        if (server == null) {
            throw IllegalArgumentException("server must be not null")
        }
        val managedChannel = channelSelector.getChannel(server!!.serverDomain)
        return UserGrpc.newBlockingStub(managedChannel)
            .withCallCredentials(
                CallCredentials(
                    server!!.accessKey,
                    server!!.hashKey
                )
            ).withInterceptors(ServiceExceptionHandler())
    }

    override fun provideGroupBlockingStub(): GroupGrpc.GroupBlockingStub {
        if (server == null) {
            throw IllegalArgumentException("server must be not null")
        }
        val managedChannel = channelSelector.getChannel(server!!.serverDomain)
        return GroupGrpc.newBlockingStub(managedChannel)
            .withCallCredentials(
                CallCredentials(
                    server!!.accessKey,
                    server!!.hashKey
                )
            ).withInterceptors(ServiceExceptionHandler())
    }

    override fun provideMessageBlockingStub(): MessageGrpc.MessageBlockingStub {
        if (server == null) {
            throw IllegalArgumentException("server must be not null")
        }
        val managedChannel = channelSelector.getChannel(server!!.serverDomain)
        printlnCK("provideMessageBlockingStub: ${managedChannel.authority()}")
        return MessageGrpc.newBlockingStub(managedChannel).withInterceptors(ServiceExceptionHandler())
    }

    override fun provideMessageStub(): MessageGrpc.MessageStub {
        if (server == null) {
            throw IllegalArgumentException("server must be not null")
        }
        val managedChannel = channelSelector.getChannel(server!!.serverDomain)
        return MessageGrpc.newStub(managedChannel).withInterceptors(ServiceExceptionHandler())
    }

    override fun provideNoteBlockingStub(): NoteGrpc.NoteBlockingStub {
        if (server == null) {
            throw IllegalArgumentException("server must be not null")
        }
        val managedChannel = channelSelector.getChannel(server!!.serverDomain)
        printlnCK("provideNoteBlockingStub: ${managedChannel.authority()}")
        return NoteGrpc.newBlockingStub(managedChannel)
            .withCallCredentials(
                CallCredentials(
                    server!!.accessKey,
                    server!!.hashKey
                )
            ).withInterceptors(ServiceExceptionHandler())
    }

    override fun provideNotifyPushBlockingStub(): NotifyPushGrpc.NotifyPushBlockingStub {
        if (server == null) {
            throw IllegalArgumentException("server must be not null")
        }
        val managedChannel = channelSelector.getChannel(server!!.serverDomain)
        return NotifyPushGrpc.newBlockingStub(managedChannel)
            .withCallCredentials(
                CallCredentials(
                    server!!.accessKey,
                    server!!.hashKey
                )
            ).withInterceptors(ServiceExceptionHandler())
    }

    override fun provideVideoCallBlockingStub(): VideoCallGrpc.VideoCallBlockingStub {
        if (server == null) {
            throw IllegalArgumentException("server must be not null")
        }
        val managedChannel = channelSelector.getChannel(server!!.serverDomain)
        return VideoCallGrpc.newBlockingStub(managedChannel)
            .withCallCredentials(
                CallCredentials(
                    server!!.accessKey,
                    server!!.hashKey
                )
            ).withInterceptors(ServiceExceptionHandler())
    }

    override fun provideUploadFileBlockingStub(): UploadFileGrpc.UploadFileBlockingStub {
        if (server == null) {
            throw IllegalArgumentException("server must be not null")
        }
        val managedChannel = channelSelector.getChannel(server!!.serverDomain)
        return UploadFileGrpc.newBlockingStub(managedChannel)
            .withCallCredentials(
                CallCredentials(
                    server!!.accessKey,
                    server!!.hashKey
                )
            ).withInterceptors(ServiceExceptionHandler())
    }

    override fun provideUploadFileStub(): UploadFileGrpc.UploadFileStub {
        if (server == null) {
            throw IllegalArgumentException("server must be not null")
        }
        val managedChannel = channelSelector.getChannel(server!!.serverDomain)
        return UploadFileGrpc.newStub(managedChannel)
            .withCallCredentials(
                CallCredentials(
                    server!!.accessKey,
                    server!!.hashKey
                )
            ).withInterceptors(ServiceExceptionHandler())
    }

    override fun provideWorkSpaceBlockingStub(): WorkspaceGrpc.WorkspaceBlockingStub {
        if (server == null) {
            throw IllegalArgumentException("server must be not null")
        }
        val managedChannel = channelSelector.getChannel(server!!.serverDomain)
        return WorkspaceGrpc.newBlockingStub(managedChannel).withCallCredentials(
            CallCredentials(
                server!!.accessKey,
                server!!.hashKey
            )
        ).withInterceptors(ServiceExceptionHandler())
    }
}