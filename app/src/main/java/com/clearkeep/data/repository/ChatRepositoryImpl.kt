package com.clearkeep.data.repository

import android.content.Context
import android.text.TextUtils
import com.clearkeep.data.local.clearkeep.dao.MessageDAO
import com.clearkeep.data.local.clearkeep.dao.NoteDAO
import com.clearkeep.data.local.signal.CKSignalProtocolAddress
import com.clearkeep.domain.model.Note
import com.clearkeep.data.local.signal.store.InMemorySenderKeyStore
import com.clearkeep.data.local.signal.store.InMemorySignalProtocolStore
import com.clearkeep.data.remote.service.*
import com.clearkeep.domain.model.Message
import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.model.Server
import com.clearkeep.domain.repository.ChatRepository
import com.clearkeep.domain.repository.MessageRepository
import com.clearkeep.domain.repository.ServerRepository
import com.clearkeep.presentation.screen.chat.utils.*
import com.clearkeep.utilities.*
import com.clearkeep.utilities.network.Resource
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.*
import message.MessageOuterClass
import net.gotev.uploadservice.data.UploadInfo
import net.gotev.uploadservice.network.ServerResponse
import net.gotev.uploadservice.observer.request.BaseRequestObserver
import net.gotev.uploadservice.observer.request.RequestObserverDelegate
import net.gotev.uploadservice.protocols.binary.BinaryUploadRequest
import org.whispersystems.libsignal.IdentityKey
import org.whispersystems.libsignal.SessionBuilder
import org.whispersystems.libsignal.SessionCipher
import org.whispersystems.libsignal.groups.GroupCipher
import org.whispersystems.libsignal.groups.SenderKeyName
import org.whispersystems.libsignal.protocol.CiphertextMessage
import org.whispersystems.libsignal.state.PreKeyBundle
import org.whispersystems.libsignal.state.PreKeyRecord
import org.whispersystems.libsignal.state.SignedPreKeyRecord
import upload_file.UploadFileOuterClass
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ChatRepositoryImpl @Inject constructor(
    private val senderKeyStore: InMemorySenderKeyStore,
    private val signalProtocolStore: InMemorySignalProtocolStore,
    private val messageRepository: MessageRepository, //TODO: Clean
    private val serverRepository: ServerRepository, //TODO: Clean
    private val userManager: AppStorage,
    private val messageDAO: MessageDAO,
    private val noteDAO: NoteDAO,
    private val signalKeyDistributionService: SignalKeyDistributionService,
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
        server: Server,
        senderId: String,
        ownerWorkSpace: String,
        receiverId: String,
        receiverWorkspaceDomain: String,
        groupId: Long,
        plainMessage: String,
        isForceProcessKey: Boolean,
        cachedMessageId: Int
    ): Resource<Nothing> = withContext(Dispatchers.IO) {
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

            val response = messageService.sendMessagePeer(server, receiverId, userManager.getUniqueDeviceID(), groupId, message.serialize(), messageSender.serialize())
            val responseMessage = convertMessageResponse(
                response,
                plainMessage,
                Owner(ownerWorkSpace, senderId)
            ) //TODO: CLEAN ARCH Move logic to UseCase
            if (cachedMessageId == 0) {
                messageRepository.saveNewMessage(responseMessage) //TODO: CLEAN ARCH Move logic to UseCase
            } else {
                updateMessage(responseMessage.copy(generateId = cachedMessageId))
            }
            printlnCK("send message success: $plainMessage")
            return@withContext Resource.success(null)
        } catch (e: StatusRuntimeException) {
            val parsedError = parseError(e)

            val message = when (parsedError.code) {
                1000, 1077 -> {
                    printlnCK("sendMessageInPeer token expire")
                    serverRepository.isLogout.postValue(true) //TODO: CLEAN ARCH Move logic to UseCase
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
        initSessionUserPeer(
            signalProtocolAddress2,
            signalProtocolStore,
            owner = Owner(ownerWorkSpace, senderId)
        )
        return initSessionUserPeer(
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

            val server = serverRepository.getServerByOwner(Owner(ownerWorkSpace, senderId)) //TODO: CLEAN ARCH Move logic to UseCase
            if (server == null) {
                printlnCK("sendMessageToGroup: server must be not null")
                return@withContext Resource.error("server must be not null", null)
            }

            val response = messageService.sendMessageGroup(server, userManager.getUniqueDeviceID(), groupId, ciphertextFromAlice)
            
            val message = convertMessageResponse(
                response,
                plainMessage,
                Owner(ownerWorkSpace, senderId)
            ) //TODO: CLEAN ARCH Move logic to UseCase

            if (cachedMessageId == 0) {
                messageRepository.saveNewMessage(message) //TODO: CLEAN ARCH Move logic to UseCase
            } else {
                updateMessage(message.copy(generateId = cachedMessageId))
            }

            printlnCK("send message success: $plainMessage")
            return@withContext Resource.success(null)
        } catch (e: StatusRuntimeException) {
            val parsedError = parseError(e)

            val message = when (parsedError.code) {
                1000, 1077 -> {
                    printlnCK("sendMessageToGroup token expired")
                    serverRepository.isLogout.postValue(true) //TODO: CLEAN ARCH Move logic to UseCase
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
                    messageRepository.saveNote(note.copy(createdTime = response.createdAt)) //TODO: CLEAN ARCH Move logic to UseCase
                } else {
                    updateNote(
                        note.copy(
                            generateId = cachedNoteId,
                            createdTime = response.createdAt
                        )
                    ) //TODO: Move to notes repo
                }
                return@withContext true
            } catch (e: StatusRuntimeException) {

                val parsedError = parseError(e)

                val message = when (parsedError.code) {
                    1000, 1077 -> {
                        printlnCK("sendNote token expired")
                        serverRepository.isLogout.postValue(true) //TODO: CLEAN ARCH Move logic to UseCase
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
                        serverRepository.isLogout.postValue(true) //TODO: CLEAN ARCH Move logic to UseCase
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

    private fun convertMessageResponse(
        value: MessageOuterClass.MessageObjectResponse,
        decryptedMessage: String,
        owner: Owner
    ): Message {
        return Message(
            messageId = value.id,
            groupId = value.groupId,
            groupType = value.groupType,
            senderId = value.fromClientId,
            receiverId = value.clientId,
            message = decryptedMessage,
            createdTime = value.createdAt,
            updatedTime = value.updatedAt,
            ownerDomain = owner.domain,
            ownerClientId = owner.clientId
        )
    }

    private suspend fun initSessionUserPeer(
        signalProtocolAddress: CKSignalProtocolAddress,
        signalProtocolStore: InMemorySignalProtocolStore,
        owner: Owner
    ): Boolean = withContext(Dispatchers.IO) {
        val remoteClientId = signalProtocolAddress.owner.clientId
        printlnCK("initSessionUserPeer with $remoteClientId, domain = ${signalProtocolAddress.owner.domain}")
        if (TextUtils.isEmpty(remoteClientId)) {
            return@withContext false
        }
        try {
            val server = serverRepository.getServerByOwner(owner)
            if (server == null) {
                printlnCK("initSessionUserPeer: server must be not null")
                return@withContext false
            }

            val remoteKeyBundle = signalKeyDistributionService.getPeerClientKey(server, remoteClientId, signalProtocolAddress.owner.domain)

            val preKey = PreKeyRecord(remoteKeyBundle.preKey.toByteArray())
            val signedPreKey = SignedPreKeyRecord(remoteKeyBundle.signedPreKey.toByteArray())
            val identityKeyPublic = IdentityKey(remoteKeyBundle.identityKeyPublic.toByteArray(), 0)

            val retrievedPreKey = PreKeyBundle(
                remoteKeyBundle.registrationId,
                signalProtocolAddress.deviceId,
                preKey.id,
                preKey.keyPair.publicKey,
                remoteKeyBundle.signedPreKeyId,
                signedPreKey.keyPair.publicKey,
                signedPreKey.signature,
                identityKeyPublic
            )

            val sessionBuilder = SessionBuilder(signalProtocolStore, signalProtocolAddress)

            // Build a session with a PreKey retrieved from the server.
            sessionBuilder.process(retrievedPreKey)
            printlnCK("initSessionUserPeer: success")
            return@withContext true
        } catch (e: StatusRuntimeException) {
            val parsedError = parseError(e)

            val message = when (parsedError.code) {
                1000, 1077 -> {
                    printlnCK("initSessionUserPeer token expired")
                    serverRepository.isLogout.postValue(true)
                    parsedError.message
                }
                else -> parsedError.message
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            printlnCK("initSessionUserPeer: $e")
        }

        return@withContext false
    }

    private suspend fun updateNote(note: Note) {
        noteDAO.updateNotes(note)
    }

    private suspend fun updateMessage(message: Message) {
        messageDAO.updateMessage(message)
    }
}