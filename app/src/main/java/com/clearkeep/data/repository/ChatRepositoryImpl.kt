package com.clearkeep.data.repository

import android.content.Context
import com.clearkeep.data.remote.DownloadService
import com.clearkeep.data.remote.MessageService
import com.clearkeep.data.remote.NoteService
import com.clearkeep.data.remote.UploadFileService
import com.clearkeep.db.clearkeep.model.Note
import com.clearkeep.data.local.signal.InMemorySenderKeyStore
import com.clearkeep.data.local.signal.InMemorySignalProtocolStore
import com.clearkeep.db.clearkeep.model.Owner
import com.clearkeep.db.signalkey.CKSignalProtocolAddress
import com.clearkeep.domain.repository.ChatRepository
import com.clearkeep.domain.repository.MessageRepository
import com.clearkeep.domain.repository.ServerRepository
import com.clearkeep.screen.chat.utils.*
import com.clearkeep.utilities.*
import com.clearkeep.utilities.network.Resource
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.*
import net.gotev.uploadservice.data.UploadInfo
import net.gotev.uploadservice.network.ServerResponse
import net.gotev.uploadservice.observer.request.BaseRequestObserver
import net.gotev.uploadservice.observer.request.RequestObserverDelegate
import net.gotev.uploadservice.protocols.binary.BinaryUploadRequest
import org.whispersystems.libsignal.SessionCipher
import org.whispersystems.libsignal.groups.GroupCipher
import org.whispersystems.libsignal.groups.SenderKeyName
import org.whispersystems.libsignal.protocol.CiphertextMessage
import upload_file.UploadFileOuterClass
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ChatRepositoryImpl @Inject constructor(
    private val senderKeyStore: InMemorySenderKeyStore,
    private val signalProtocolStore: InMemorySignalProtocolStore,
    private val messageRepository: MessageRepository,
    private val serverRepository: ServerRepository,
    private val userManager: AppStorage,
    private val downloadService: DownloadService,
    private val uploadFileService: UploadFileService,
    private val noteService: NoteService,
    private val messageService: MessageService
) : ChatRepository {
    private var roomId: Long = -1

    override fun setJoiningRoomId(roomId: Long) {
        this.roomId = roomId
    }

    override fun getJoiningRoomId(): Long {
        return roomId
    }

    override suspend fun sendMessageInPeer(
        senderId: String,
        ownerWorkSpace: String,
        receiverId: String,
        receiverWorkspaceDomain: String,
        groupId: Long,
        plainMessage: String,
        isForceProcessKey: Boolean,
        cachedMessageId: Int
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

            val server = serverRepository.getServerByOwner(Owner(ownerWorkSpace, senderId))
            if (server == null) {
                printlnCK("sendMessageInPeer: server must be not null")
                return@withContext Resource.error("", null)
            }
            
            val response = messageService.sendMessagePeer(server, receiverId, userManager.getUniqueDeviceID(), groupId, message.serialize(), messageSender.serialize())
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

    override suspend fun processPeerKey(
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

    override suspend fun sendMessageToGroup(
        senderId: String,
        ownerWorkSpace: String,
        groupId: Long,
        plainMessage: String,
        cachedMessageId: Int
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

            val server = serverRepository.getServerByOwner(Owner(ownerWorkSpace, senderId))
            if (server == null) {
                printlnCK("sendMessageToGroup: server must be not null")
                return@withContext Resource.error("server must be not null", null)
            }

            val response = messageService.sendMessageGroup(server, userManager.getUniqueDeviceID(), groupId, ciphertextFromAlice)
            
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

    override suspend fun sendNote(note: Note, cachedNoteId: Long): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val response = noteService.sendNote(note.content)
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

    override suspend fun uploadFile(
        context: Context,
        mimeType: String,
        fileName: String,
        fileUri: String
    ): Resource<String> {
        return withContext(Dispatchers.Main) {
            try {
                val uploadFileLink = getUploadedFileUrl(fileName, mimeType)
                val uploadId =
                    BinaryUploadRequest(context, serverUrl = uploadFileLink.uploadedFileUrl)
                        .setMethod("PUT")
                        .setFileToUpload(fileUri)
                        .startUpload()

                val isSuccessful = suspendCoroutine<Boolean> { cont ->
                    val requestObserverDelegate = object : RequestObserverDelegate {
                        override fun onCompleted(context: Context, uploadInfo: UploadInfo) {
                            printlnCK("uploadFile onCompleted")
                            try {
                                cont.resume(true)
                            } catch (e: IllegalStateException) {
                                printlnCK("uploadFile already resumed!")
                            }
                        }

                        override fun onCompletedWhileNotObserving() {
                            printlnCK("uploadFile onCompletedWhileNotObserving")
                        }

                        override fun onError(
                            context: Context,
                            uploadInfo: UploadInfo,
                            exception: Throwable
                        ) {
                            printlnCK("uploadFile onError")
                            cont.resume(false)
                        }

                        override fun onProgress(context: Context, uploadInfo: UploadInfo) {}

                        override fun onSuccess(
                            context: Context,
                            uploadInfo: UploadInfo,
                            serverResponse: ServerResponse
                        ) {
                            printlnCK("uploadFile onSuccess")
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

                printlnCK("uploadFile statusRuntime $e")

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
                printlnCK("uploadFile exception $e")
                return@withContext Resource.error(e.toString(), null)
            }
        }
    }

    override fun downloadFile(fileName: String, url: String) {
        downloadService.downloadFile(fileName, url)
    }

    private suspend fun getUploadedFileUrl(
        fileName: String,
        mimeType: String
    ): UploadFileOuterClass.GetUploadFileLinkResponse = withContext(Dispatchers.Main) {
        uploadFileService.getUploadFileUrl(fileName, mimeType)
    }
}