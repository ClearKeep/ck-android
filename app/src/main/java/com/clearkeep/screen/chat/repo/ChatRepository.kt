package com.clearkeep.screen.chat.repo

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
import com.clearkeep.screen.chat.utils.*
import com.clearkeep.utilities.*
import com.google.protobuf.ByteString
import kotlinx.coroutines.*
import message.MessageOuterClass
import note.NoteOuterClass
import org.whispersystems.libsignal.SessionCipher
import org.whispersystems.libsignal.groups.GroupCipher
import org.whispersystems.libsignal.groups.SenderKeyName
import org.whispersystems.libsignal.protocol.CiphertextMessage
import upload_file.UploadFileOuterClass
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    // network calls
    private val dynamicAPIProvider: DynamicAPIProvider,

    // data
    private val senderKeyStore: InMemorySenderKeyStore,
    private val signalProtocolStore: InMemorySignalProtocolStore,
    private val messageRepository: MessageRepository
) {
    val scope: CoroutineScope = CoroutineScope(Job() + Dispatchers.IO)

    private var roomId: Long = -1

    fun setJoiningRoomId(roomId: Long) {
        this.roomId = roomId
    }

    fun getJoiningRoomId() : Long {
        return roomId
    }

    suspend fun sendMessageInPeer(senderId: String, ownerWorkSpace: String, receiverId: String, receiverWorkspaceDomain: String, groupId: Long, plainMessage: String, isForceProcessKey: Boolean = false, cachedMessageId: Int = 0) : Boolean = withContext(Dispatchers.IO) {
        printlnCK("sendMessageInPeer: sender=$senderId + $ownerWorkSpace, receiver= $receiverId + $receiverWorkspaceDomain, groupId= $groupId")
        try {
            val signalProtocolAddress = CKSignalProtocolAddress(Owner(receiverWorkspaceDomain, receiverId), 111)

            if (isForceProcessKey || !signalProtocolStore.containsSession(signalProtocolAddress)) {
                val processSuccess = processPeerKey(receiverId, receiverWorkspaceDomain)
                if (!processSuccess) {
                    printlnCK("sendMessageInPeer, init session failed with message \"$plainMessage\"")
                    return@withContext false
                }
            }

            val sessionCipher = SessionCipher(signalProtocolStore, signalProtocolAddress)
            val message: CiphertextMessage =
                    sessionCipher.encrypt(plainMessage.toByteArray(charset("UTF-8")))

            val request = MessageOuterClass.PublishRequest.newBuilder()
                    .setClientId(receiverId)
                    .setFromClientId(senderId)
                    .setGroupId(groupId)
                    .setMessage(ByteString.copyFrom(message.serialize()))
                    .build()

            val response = dynamicAPIProvider.provideMessageBlockingStub().publish(request)
            val responseMessage = messageRepository.convertMessageResponse(response, plainMessage, Owner(ownerWorkSpace, senderId))
            if (cachedMessageId == 0) {
                messageRepository.saveNewMessage(responseMessage)
            } else {
                messageRepository.updateMessage(responseMessage.copy(generateId = cachedMessageId))
            }

            printlnCK("send message success: $plainMessage")
        } catch (e: java.lang.Exception) {
            printlnCK("sendMessage: $e")
            return@withContext false
        }

        return@withContext true
    }

    suspend fun processPeerKey(receiverId: String, receiverWorkspaceDomain: String): Boolean {
        val signalProtocolAddress = CKSignalProtocolAddress(Owner(receiverWorkspaceDomain, receiverId), 111)
        return messageRepository.initSessionUserPeer(
            signalProtocolAddress,
            dynamicAPIProvider.provideSignalKeyDistributionBlockingStub(),
            signalProtocolStore
        )
    }

    suspend fun sendMessageToGroup(senderId: String, ownerWorkSpace: String, groupId: Long, plainMessage: String, cachedMessageId: Int = 0) : Boolean = withContext(Dispatchers.IO) {
        printlnCK("sendMessageToGroup: sender $senderId to group $groupId, ownerWorkSpace = $ownerWorkSpace")
        try {
            val senderAddress = CKSignalProtocolAddress(Owner(ownerWorkSpace, senderId), 111)
            val groupSender  =  SenderKeyName(groupId.toString(), senderAddress)

            val aliceGroupCipher = GroupCipher(senderKeyStore, groupSender)
            val ciphertextFromAlice: ByteArray =
                    aliceGroupCipher.encrypt(plainMessage.toByteArray(charset("UTF-8")))

            val request = MessageOuterClass.PublishRequest.newBuilder()
                    .setGroupId(groupId)
                    .setFromClientId(senderAddress.owner.clientId)
                    .setMessage(ByteString.copyFrom(ciphertextFromAlice))
                    .build()
            val response = dynamicAPIProvider.provideMessageBlockingStub().publish(request)
            val message = messageRepository.convertMessageResponse(response, plainMessage, Owner(ownerWorkSpace, senderId))

            if (cachedMessageId == 0) {
                messageRepository.saveNewMessage(message)
            } else {
                messageRepository.updateMessage(message.copy(generateId = cachedMessageId))
            }

            printlnCK("send message success: $plainMessage")
            return@withContext true
        } catch (e: Exception) {
            printlnCK("sendMessage: $e")
        }

        return@withContext false
    }

    suspend fun sendNote(note: Note, cachedNoteId: Long = 0) : Boolean = withContext(Dispatchers.IO) {
        try {
            val request = NoteOuterClass.CreateNoteRequest.newBuilder()
                .setTitle("title")
                .setContent(ByteString.copyFrom(note.content, Charsets.UTF_8))
                .setNoteType("1")
                .build()
            val response = dynamicAPIProvider.provideNoteBlockingStub().createNote(request)
            printlnCK("create note success? ${response.success}")
            printlnCK("create note error? ${response.errors.message}")
            if (response.success) {
                if (cachedNoteId == 0L) {
                    messageRepository.saveNote(note)
                } else {
                    messageRepository.updateNote(note.copy(generateId = cachedNoteId))
                }
            }
            return@withContext true
        } catch (e: Exception) {
            printlnCK("create note $e")
        }

        return@withContext false
    }

    suspend fun uploadFile(
        mimeType: String,
        fileName: String,
        byteStrings: List<ByteString>,
        blockHash: List<String>,
        fileHash: String
    ): String {
//        val liveData = MutableLiveData<String>()
        return withContext(Dispatchers.IO) {
            try {
                if (byteStrings.isNotEmpty() && byteStrings.size == 1) {
                    val request = UploadFileOuterClass.FileUploadRequest.newBuilder()
                        .setFileName(fileName)
                        .setFileContentType(mimeType)
                        .setFileData(byteStrings[0])
                        .setFileHash(fileHash)
                        .build()

                    val response =
                        dynamicAPIProvider.provideUploadFileBlockingStub().uploadFile(request)

                    return@withContext response.fileUrl
                } else {
//                    val responseObserver =
//                        object : StreamObserver<UploadFileOuterClass.UploadFilesResponse> {
//                            override fun onNext(value: UploadFileOuterClass.UploadFilesResponse?) {
//                                printlnCK("onNext response" + value?.fileUrl)
//                                liveData.postValue(value?.fileUrl ?: "")
//                            }
//
//                            override fun onError(t: Throwable?) {
//                                printlnCK("onError $t")
//                            }
//
//                            override fun onCompleted() {
//                                printlnCK("onCompleted")
//                            }
//                        }
//                    val requestObserver = dynamicAPIProvider.provideUploadFileStub()
//                        .uploadChunkedFile(responseObserver)
//
//                    byteStrings.forEachIndexed { index, byteString ->
//                        val request = UploadFileOuterClass.FileDataBlockRequest.newBuilder()
//                            .setFileName(fileName)
//                            .setFileContentType(mimeType)
//                            .setFileDataBlock(byteString)
//                            .setFileDataBlockHash(blockHash[index])
//                            .setFileHash(fileHash)
//                            .build()
//
//                        requestObserver.onNext(request)
//                    }
//                    requestObserver.onCompleted()
                    return@withContext ""
                }
            } catch (e: Exception) {
                printlnCK("uploadFileToGroup $e")
                return@withContext ""
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
}