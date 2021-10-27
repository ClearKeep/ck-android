package com.clearkeep.repo

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import com.clearkeep.db.clear_keep.model.Note
import com.clearkeep.screen.chat.signal_store.InMemorySenderKeyStore
import com.clearkeep.screen.chat.signal_store.InMemorySignalProtocolStore
import com.clearkeep.db.clear_keep.model.Owner
import com.clearkeep.db.signal_key.CKSignalProtocolAddress
import com.clearkeep.dynamicapi.DynamicAPIProvider
import com.clearkeep.dynamicapi.ParamAPI
import com.clearkeep.dynamicapi.ParamAPIProvider
import com.clearkeep.screen.chat.utils.*
import com.clearkeep.utilities.*
import com.clearkeep.utilities.network.Resource
import com.google.protobuf.ByteString
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.*
import message.MessageOuterClass
import net.gotev.uploadservice.data.UploadInfo
import net.gotev.uploadservice.network.ServerResponse
import net.gotev.uploadservice.observer.request.BaseRequestObserver
import net.gotev.uploadservice.observer.request.RequestObserver
import net.gotev.uploadservice.observer.request.RequestObserverDelegate
import net.gotev.uploadservice.protocols.binary.BinaryUploadRequest
import note.NoteOuterClass
import org.whispersystems.libsignal.SessionCipher
import org.whispersystems.libsignal.groups.GroupCipher
import org.whispersystems.libsignal.groups.SenderKeyName
import org.whispersystems.libsignal.protocol.CiphertextMessage
import upload_file.UploadFileOuterClass
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Singleton
class ChatRepository @Inject constructor(
    // network calls
    private val dynamicAPIProvider: DynamicAPIProvider,
    private val apiProvider: ParamAPIProvider,
    // data
    private val senderKeyStore: InMemorySenderKeyStore,
    private val signalProtocolStore: InMemorySignalProtocolStore,
    private val messageRepository: MessageRepository,
    private val serverRepository: ServerRepository,
    private val userManager: AppStorage
) {
    val scope: CoroutineScope = CoroutineScope(Job() + Dispatchers.IO)

    private var roomId: Long = -1

    fun setJoiningRoomId(roomId: Long) {
        this.roomId = roomId
    }

    fun getJoiningRoomId(): Long {
        return roomId
    }

    suspend fun sendMessageInPeer(
        senderId: String,
        ownerWorkSpace: String,
        receiverId: String,
        receiverWorkspaceDomain: String,
        groupId: Long,
        plainMessage: String,
        isForceProcessKey: Boolean = false,
        cachedMessageId: Int = 0
    ): Resource<Nothing> = withContext(Dispatchers.IO) {
        printlnCK("sendMessageInPeer: sender=$senderId + $ownerWorkSpace, receiver= $receiverId + $receiverWorkspaceDomain, groupId= $groupId")
        try {
            val signalProtocolAddress =
                CKSignalProtocolAddress(
                    Owner(receiverWorkspaceDomain, receiverId),
                    SENDER_DEVICE_ID
                )

            if (isForceProcessKey || !signalProtocolStore.containsSession(signalProtocolAddress)) {
                val processSuccess =
                    processPeerKey(receiverId, receiverWorkspaceDomain, senderId, ownerWorkSpace)
                if (!processSuccess) {
                    printlnCK("sendMessageInPeer, init session failed with message \"$plainMessage\"")
                    return@withContext Resource.error("init session failed", null)
                }
            }
            val signalProtocolAddressPublishRequest =
                CKSignalProtocolAddress(Owner(ownerWorkSpace, senderId), SENDER_DEVICE_ID)
            val sessionCipher = SessionCipher(signalProtocolStore, signalProtocolAddress)
            val message: CiphertextMessage =
                sessionCipher.encrypt(plainMessage.toByteArray(charset("UTF-8")))

            val sessionCipherSender =
                SessionCipher(signalProtocolStore, signalProtocolAddressPublishRequest)
            val messageSender: CiphertextMessage =
                sessionCipherSender.encrypt(plainMessage.toByteArray(charset("UTF-8")))

            val request = MessageOuterClass.PublishRequest.newBuilder()
                .setClientId(receiverId)
                .setFromClientDeviceId(userManager.getUniqueDeviceID())
                .setGroupId(groupId)
                .setMessage(ByteString.copyFrom(message.serialize()))
                .setSenderMessage(ByteString.copyFrom(messageSender.serialize()))
                .build()

            val server = serverRepository.getServerByOwner(Owner(ownerWorkSpace, senderId))
            if (server == null) {
                printlnCK("sendMessageInPeer: server must be not null")
                return@withContext Resource.error("", null)
            }

            val paramAPI = ParamAPI(server.serverDomain, server.accessKey, server.hashKey)
            val response = apiProvider.provideMessageBlockingStub(paramAPI).publish(request)
            val responseMessage = messageRepository.convertMessageResponse(
                response,
                plainMessage,
                Owner(ownerWorkSpace, senderId)
            )
            if (cachedMessageId == 0) {
                messageRepository.saveNewMessage(responseMessage)
            } else {
                messageRepository.updateMessage(responseMessage.copy(generateId = cachedMessageId))
            }
            printlnCK("send message success: $plainMessage")
            return@withContext Resource.success(null)
        } catch (e: StatusRuntimeException) {
            val parsedError = parseError(e)

            val message = when (parsedError.code) {
                1000, 1077 -> {
                    printlnCK("sendMessageInPeer token expire")
                    serverRepository.isLogout.postValue(true)
                    parsedError.message
                }
                else -> parsedError.message
            }
            return@withContext Resource.error(message, null, parsedError.code)
        } catch (e: java.lang.Exception) {
            printlnCK("sendMessage: $e")
            return@withContext Resource.error(e.toString(), null)
        }
    }

    suspend fun processPeerKey(
        receiverId: String,
        receiverWorkspaceDomain: String,
        senderId: String,
        ownerWorkSpace: String
    ): Boolean {
        val signalProtocolAddress =
            CKSignalProtocolAddress(Owner(receiverWorkspaceDomain, receiverId), SENDER_DEVICE_ID)
        val signalProtocolAddress2 =
            CKSignalProtocolAddress(Owner(ownerWorkSpace, senderId), SENDER_DEVICE_ID)
        messageRepository.initSessionUserPeer(
            signalProtocolAddress2,
            signalProtocolStore,
            owner = Owner(ownerWorkSpace, senderId)
        )
        return messageRepository.initSessionUserPeer(
            signalProtocolAddress,
            signalProtocolStore,
            owner = Owner(ownerWorkSpace, senderId)
        )
    }

    suspend fun sendMessageToGroup(
        senderId: String,
        ownerWorkSpace: String,
        groupId: Long,
        plainMessage: String,
        cachedMessageId: Int = 0
    ): Resource<Nothing> = withContext(Dispatchers.IO) {
        printlnCK("sendMessageToGroup: sender $senderId to group $groupId, ownerWorkSpace = $ownerWorkSpace")
        try {
            val senderAddress =
                CKSignalProtocolAddress(Owner(ownerWorkSpace, senderId), SENDER_DEVICE_ID)
            val groupSender = SenderKeyName(groupId.toString(), senderAddress)
            printlnCK("sendMessageToGroup: senderAddress : $senderAddress  groupSender: $groupSender")
            val aliceGroupCipher = GroupCipher(senderKeyStore, groupSender)
            val senderKeyStore = senderKeyStore.loadSenderKey(groupSender)
            printlnCK("send message iteration ${senderKeyStore.senderKeyState.senderChainKey.senderMessageKey.iteration}")
            val ciphertextFromAlice: ByteArray =
                aliceGroupCipher.encrypt(plainMessage.toByteArray(charset("UTF-8")))

            val request = MessageOuterClass.PublishRequest.newBuilder()
                .setGroupId(groupId)
                .setFromClientDeviceId(userManager.getUniqueDeviceID())
                .setMessage(ByteString.copyFrom(ciphertextFromAlice))
                .setSenderMessage(ByteString.copyFrom(ciphertextFromAlice))
                .build()

            val server = serverRepository.getServerByOwner(Owner(ownerWorkSpace, senderId))
            if (server == null) {
                printlnCK("sendMessageToGroup: server must be not null")
                return@withContext Resource.error("server must be not null", null)
            }

            val paramAPI = ParamAPI(server.serverDomain, server.accessKey, server.hashKey)
            val response = apiProvider.provideMessageBlockingStub(paramAPI).publish(request)
            val message = messageRepository.convertMessageResponse(
                response,
                plainMessage,
                Owner(ownerWorkSpace, senderId)
            )

            if (cachedMessageId == 0) {
                messageRepository.saveNewMessage(message)
            } else {
                messageRepository.updateMessage(message.copy(generateId = cachedMessageId))
            }

            printlnCK("send message success: $plainMessage")
            return@withContext Resource.success(null)
        } catch (e: StatusRuntimeException) {
            val parsedError = parseError(e)

            val message = when (parsedError.code) {
                1000, 1077 -> {
                    printlnCK("sendMessageToGroup token expired")
                    serverRepository.isLogout.postValue(true)
                    parsedError.message
                }
                else -> parsedError.message
            }
            return@withContext Resource.error(message, null, parsedError.code)
        } catch (e: Exception) {
            printlnCK("sendMessage: $e")
            return@withContext Resource.error(e.toString(), null)
        }
    }

    suspend fun sendNote(note: Note, cachedNoteId: Long = 0): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val request = NoteOuterClass.CreateNoteRequest.newBuilder()
                    .setTitle("")
                    .setContent(ByteString.copyFrom(note.content, Charsets.UTF_8))
                    .setNoteType("")
                    .build()
                val response = dynamicAPIProvider.provideNoteBlockingStub().createNote(request)
                if (cachedNoteId == 0L) {
                    messageRepository.saveNote(note.copy(createdTime = response.createdAt))
                } else {
                    messageRepository.updateNote(
                        note.copy(
                            generateId = cachedNoteId,
                            createdTime = response.createdAt
                        )
                    )
                }
                return@withContext true
            } catch (e: StatusRuntimeException) {

                val parsedError = parseError(e)

                val message = when (parsedError.code) {
                    1000, 1077 -> {
                        printlnCK("sendNote token expired")
                        serverRepository.isLogout.postValue(true)
                        parsedError.message
                    }
                    else -> parsedError.message
                }
            } catch (e: Exception) {
                printlnCK("create note $e")
            }

            return@withContext false
        }

    suspend fun uploadFile(
        context: Context,
        mimeType: String,
        fileName: String,
        fileUri: String
    ): Resource<String> {
        return withContext(Dispatchers.Main) {
            try {
                val uploadFileLink = getUploadedFileUrl(fileName, mimeType)
                printlnCK("uploadFile ${uploadFileLink.uploadedFileUrl} downloadFileUrl ${uploadFileLink.downloadFileUrl} object file path ${uploadFileLink.objectFilePath}")

                val uploadId =
                    BinaryUploadRequest(context, serverUrl = uploadFileLink.uploadedFileUrl)
                        .setMethod("PUT")
                        .setFileToUpload(fileUri)
                        .startUpload()

                val isSuccessful = suspendCoroutine<Boolean> { cont ->
                    val requestObserverDelegate = object : RequestObserverDelegate {
                        override fun onCompleted(context: Context, uploadInfo: UploadInfo) {
                            cont.resume(true)
                        }

                        override fun onCompletedWhileNotObserving() {}

                        override fun onError(
                            context: Context,
                            uploadInfo: UploadInfo,
                            exception: Throwable
                        ) {
                            cont.resume(false)
                        }

                        override fun onProgress(context: Context, uploadInfo: UploadInfo) {}

                        override fun onSuccess(
                            context: Context,
                            uploadInfo: UploadInfo,
                            serverResponse: ServerResponse
                        ) {
                        }
                    }

                    val requestObserver = BaseRequestObserver(context, requestObserverDelegate) {
                        it.uploadId == uploadId
                    }

                    requestObserver.register()
                }

                return@withContext if (isSuccessful) {
                    Resource.success(uploadFileLink.downloadFileUrl)
                } else {
                    Resource.error("Error uploading files!", null, 0)
                }
            } catch (e: StatusRuntimeException) {
                val parsedError = parseError(e)

                val message = when (parsedError.code) {
                    1000, 1077 -> {
                        printlnCK("uploadFile token expired")
                        serverRepository.isLogout.postValue(true)
                        parsedError.message
                    }
                    else -> parsedError.message
                }

                return@withContext Resource.error(message, null, parsedError.code)
            } catch (e: Exception) {
                printlnCK("uploadFileToGroup $e")
                return@withContext Resource.error(e.toString(), null)
            }
        }
    }

    fun downloadFile(context: Context, fileName: String, url: String) {
        val dmRequest = DownloadManager.Request(Uri.parse(url))
        dmRequest.setDescription("Downloading file")
        dmRequest.setTitle(fileName)
        dmRequest.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        dmRequest.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

        val manager = (context.getSystemService(Context.DOWNLOAD_SERVICE)) as DownloadManager
        val ref = manager.enqueue(dmRequest)
    }

    private suspend fun getUploadedFileUrl(
        fileName: String,
        mimeType: String
    ): UploadFileOuterClass.GetUploadFileLinkResponse = withContext(Dispatchers.Main) {
        val request = UploadFileOuterClass.GetUploadFileLinkRequest.newBuilder()
            .setFileName(fileName)
            .setFileContentType(mimeType)
            .setIsPublic(true)
            .build()

        return@withContext dynamicAPIProvider.provideUploadFileBlockingStub()
            .getUploadFileLink(request)
    }
}