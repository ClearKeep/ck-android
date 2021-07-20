package com.clearkeep.screen.chat.room

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.text.TextUtils
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clearkeep.db.clear_keep.model.*
import com.clearkeep.dynamicapi.Environment
import com.clearkeep.repo.ServerRepository
import com.clearkeep.screen.chat.repo.*
import com.clearkeep.utilities.network.Resource
import com.clearkeep.utilities.printlnCK
import com.google.protobuf.ByteString
import kotlinx.coroutines.launch
import java.lang.IllegalArgumentException
import java.security.MessageDigest
import javax.inject.Inject
import java.math.BigInteger
import java.util.*

class RoomViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val groupRepository: GroupRepository,
    private val signalKeyRepository: SignalKeyRepository,

    private val peopleRepository: PeopleRepository,
    private val messageRepository: MessageRepository,
    private val serverRepository: ServerRepository,

    private val environment: Environment
): ViewModel() {
    private var roomId: Long? = null

    private var friendId: String? = null
    private var friendDomain: String? = null

    private var isLatestPeerSignalKeyProcessed = false

    private val _group = MutableLiveData<ChatGroup>()

    private val _requestCallState = MutableLiveData<Resource<RequestInfo>>()

    val requestCallState: LiveData<Resource<RequestInfo>>
        get() = _requestCallState

    val group: LiveData<ChatGroup>
        get() = _group

    var clientId: String = ""

    var domain: String = ""

    private val _imageUri = MutableLiveData<List<String>>()
    val imageUri: LiveData<List<String>>
        get() = _imageUri

    private val _imageUriSelected = MutableLiveData<List<String>>()
    val imageUriSelected: LiveData<List<String>>
        get() = _imageUriSelected

    val uploadFileResponse = MutableLiveData<Resource<String>>()

    fun joinRoom(
        ownerDomain: String,
        ownerClientId: String,
        roomId: Long?,
        friendId: String?,
        friendDomain: String?
    ) {
        if (ownerDomain.isNullOrBlank() || ownerClientId.isNullOrBlank()) {
            throw IllegalArgumentException("domain and clientId must be not NULL")
        }
        if (roomId == null && TextUtils.isEmpty(friendId)) {
            throw IllegalArgumentException("Can not load room with both groupId and friendId is empty")
        }
        this.domain = ownerDomain
        this.clientId = ownerClientId
        this.roomId = roomId
        this.friendId = friendId
        this.friendDomain = friendDomain

        viewModelScope.launch {
            val selectedServer = serverRepository.getServerByOwner(Owner(ownerDomain, ownerClientId))
            if (selectedServer == null) {
                printlnCK("default server must be not NULL")
                return@launch
            }
            environment.setUpDomain(selectedServer)
            serverRepository.setActiveServer(selectedServer)

            if (roomId != null && roomId != 0L) {
                updateGroupWithId(roomId)
            } else if (!friendId.isNullOrEmpty() && !friendDomain.isNullOrEmpty()) {
                updateGroupWithFriendId(friendId, friendDomain)
            }
        }
    }

    fun leaveRoom() {
        setJoiningRoomId(-1)
    }

    fun refreshRoom(){
        viewModelScope.launch {
            roomId?.let { updateGroupWithId(it) }
        }
    }

    fun setJoiningRoomId(roomId: Long) {
        chatRepository.setJoiningRoomId(roomId)
    }

    fun getMessages(groupId: Long, domain: String, clientId: String): LiveData<List<Message>> {
        printlnCK("getMessages: groupId $groupId")
        return messageRepository.getMessagesAsState(groupId, Owner(domain, clientId))
    }

    private suspend fun updateGroupWithId(groupId: Long) {
        printlnCK("updateGroupWithId: groupId $groupId")
        val ret = groupRepository.getGroupByID(groupId, domain, clientId)
        if (ret != null) {
            setJoiningGroup(ret)
            updateMessagesFromRemote(groupId, ret.lastMessageSyncTimestamp)
        }
    }

    private suspend fun updateGroupWithFriendId(friendId: String, friendDomain: String) {
        printlnCK("updateGroupWithFriendId: friendId $friendId")
        val friend = peopleRepository.getFriend(friendId, friendDomain, getOwner()) ?: User(userId = friendId, userName = "", domain = friendDomain)
        if (friend == null) {
            printlnCK("updateGroupWithFriendId: can not find friend with id $friendId")
            return
        }
        var existingGroup = groupRepository.getGroupPeerByClientId(friend, Owner(domain = domain, clientId = clientId))
        if (existingGroup == null) {
            existingGroup = groupRepository.getTemporaryGroupWithAFriend(getUser(), friend)
        } else {
            updateMessagesFromRemote(existingGroup.groupId, existingGroup.lastMessageSyncTimestamp)
        }
        setJoiningGroup(existingGroup)
    }

    private fun getOwner(): Owner {
        return Owner(domain, clientId)
    }

    private suspend fun updateMessagesFromRemote(groupId: Long, lastMessageAt: Long) {
        val server = environment.getServer()
        messageRepository.updateMessageFromAPI(groupId, Owner(server.serverDomain, server.profile.userId), lastMessageAt, 0)
    }

    fun sendMessageToUser(context: Context, receiverPeople: User, groupId: Long, message: String) {
        viewModelScope.launch {
            try {
                if (!_imageUriSelected.value.isNullOrEmpty()) {
                    uploadImage(context, groupId, message, null, receiverPeople)
                } else {
                    sendMessageToUser(receiverPeople, groupId, message)
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun sendMessageToUser(receiverPeople: User, groupId: Long, message: String, tempMessageId: Int = 0) {
        viewModelScope.launch {
            var lastGroupId: Long = groupId
            if (lastGroupId == GROUP_ID_TEMPO) {
                val user = environment.getServer().profile
                val group = groupRepository.createGroupFromAPI(
                        user.userId,
                        "${user.getDisplayName()},${receiverPeople.userName}",
                        mutableListOf(getUser(), receiverPeople),
                        false
                )
                if (group != null) {
                    setJoiningGroup(group)
                    lastGroupId = group.groupId
                }
            }

            if (lastGroupId != GROUP_ID_TEMPO) {
                if (!isLatestPeerSignalKeyProcessed) {
                    // work around: always load user signal key for first open room
                    val success = chatRepository.sendMessageInPeer(clientId, domain, receiverPeople.userId, receiverPeople.domain, lastGroupId, message, isForceProcessKey = true, cachedMessageId = tempMessageId)
                    isLatestPeerSignalKeyProcessed = success
                } else {
                    chatRepository.sendMessageInPeer(
                        clientId,
                        domain,
                        receiverPeople.userId,
                        receiverPeople.domain,
                        lastGroupId,
                        message,
                        cachedMessageId = tempMessageId
                    )
                }
            }
        }
    }

    fun sendMessageToGroup(
        context: Context,
        groupId: Long,
        message: String,
        isRegisteredGroup: Boolean
    ) {
        viewModelScope.launch {
            try {
                if (!_imageUriSelected.value.isNullOrEmpty()) {
                    uploadImage(context, groupId, message, isRegisteredGroup)
                } else {
                    sendMessageToGroup(groupId, message, isRegisteredGroup)
                }
            } catch (e: Exception) {
            }
        }
    }

    private suspend fun sendMessageToGroup(
        groupId: Long,
        message: String,
        isRegisteredGroup: Boolean,
        cachedMessageId: Int = 0
    ) {
        if (!isRegisteredGroup) {
            val result =
                signalKeyRepository.registerSenderKeyToGroup(groupId, clientId, domain)
            if (result) {
                _group.value = groupRepository.remarkGroupKeyRegistered(groupId)
                chatRepository.sendMessageToGroup(clientId, domain, groupId, message, cachedMessageId)
            }
        } else {
            chatRepository.sendMessageToGroup(clientId, domain, groupId, message, cachedMessageId)
        }
    }

    fun inviteToGroup(invitedUsers: List<User>, groupId: Long) {
        viewModelScope.launch {
            val inviteGroupSuccess =
                groupRepository.inviteToGroupFromAPIs(invitedUsers, groupId, getOwner())
            inviteGroupSuccess?.let {
                setJoiningGroup(inviteGroupSuccess)
            }
        }
    }

    fun removeMember(user: User,groupId:Long){
        viewModelScope.launch {
            val remoteMember=groupRepository.removeMemberInGroup(user,groupId,getOwner())
            remoteMember?.let {
                setJoiningGroup(remoteMember)
            }
        }
    }

    fun requestCall(groupId: Long, isAudioMode: Boolean) {
        viewModelScope.launch {
            _requestCallState.value = Resource.loading(null)

            var lastGroupId: Long = groupId
            if (lastGroupId == GROUP_ID_TEMPO) {
                val user = getUser()
                printlnCK("requestCall $domain")
                val friend = peopleRepository.getFriend(friendId!!, friendDomain!! , getOwner())!!
                val group = groupRepository.createGroupFromAPI(
                    user.userId,
                    "",
                    mutableListOf(user, friend),
                        false
                )
                if (group != null) {
                    setJoiningGroup(group)
                    lastGroupId = group.groupId
                }
            }

            if (lastGroupId != GROUP_ID_TEMPO) {
                _requestCallState.value = Resource.success(_group.value?.let { RequestInfo(it, isAudioMode) })
            } else {
                _requestCallState.value = Resource.error("error",
                    _group.value?.let { RequestInfo(it, isAudioMode) })
            }
        }
    }

    private fun setJoiningGroup(group: ChatGroup) {
        _group.value = group
        chatRepository.setJoiningRoomId(group.groupId)
    }

    private fun getUser(): User {
        val server = environment.getServer()
        return User(userId = server.profile.userId, userName = server.profile.getDisplayName(), domain = server.serverDomain)
    }

    fun setSelectedImages(uris: List<String>) {
        _imageUriSelected.value = uris
    }

    fun removeImage(uri: String) {
        val list = mutableListOf<String>()
        list.addAll(_imageUriSelected.value ?: emptyList())
        list.remove(uri)
        _imageUriSelected.value = list
    }

    fun addImage(uri: String) {
        val list = mutableListOf<String>()
        list.addAll(_imageUriSelected.value ?: emptyList())
        list.add(uri)
        _imageUriSelected.value = list
    }

    private suspend fun uploadImage(
        context: Context, groupId: Long,
        message: String,
        isRegisteredGroup: Boolean? = null,
        receiverPeople: User? = null
    ) {
        val imageUris = _imageUriSelected.value
        _imageUriSelected.postValue(emptyList())

        if (!imageUris.isNullOrEmpty()) {
            if (!isValidFileCount()) {
                uploadFileResponse.value = Resource.error("Failed to send message - Maximum number of attachments in a message reached (10)", null)
                return
            }

            if (!isValidFileSizes(context, imageUris)) {
                uploadFileResponse.value = Resource.error("Failed to send message - File is larger than 4 MB.", null)
                return
            }

            val tempMessageId = messageRepository.saveMessage(Message(null, "", groupId, getOwner().domain, getOwner().clientId, getOwner().clientId, imageUris.joinToString(" ") + " " + message, Calendar.getInstance().timeInMillis, Calendar.getInstance().timeInMillis, getOwner().domain, getOwner().clientId))
            val imageUrls = mutableListOf<String>()
            imageUris.forEach { uriString ->
                val uri = Uri.parse(uriString)
                val contentResolver = context.contentResolver
                val mimeType = getFileMimeType(context, uri)
                val fileName = getFileName(context, uri)
                printlnCK("MIME $mimeType")
                printlnCK("FILE NAME $fileName")
                val byteStrings = mutableListOf<ByteString>()
                val blockDigestStrings = mutableListOf<String>()
                val byteArray = ByteArray(FILE_UPLOAD_CHUNK_SIZE)
                val inputStream = contentResolver.openInputStream(uri)
                var fileSize = 0
                var size: Int
                size = inputStream?.read(byteArray) ?: 0
                val fileDigest = MessageDigest.getInstance("MD5")
                while (size > 0) {
                    val blockDigest = MessageDigest.getInstance("MD5")
                    blockDigest.update(byteArray, 0, size)
                    val blockDigestByteArray = blockDigest.digest()
                    val blockDigestString = byteArrayToMd5HashString(blockDigestByteArray)
                    blockDigestStrings.add(blockDigestString)
                    fileDigest.update(byteArray, 0, size)
                    byteStrings.add(ByteString.copyFrom(byteArray, 0, size))
                    fileSize += size
                    size = inputStream?.read(byteArray) ?: 0
                }
                printlnCK("File size from inputStream ${fileSize.toString()}")
                val fileHashByteArray = fileDigest.digest()
                val fileHashString = byteArrayToMd5HashString(fileHashByteArray)
                printlnCK(fileHashString)
                val url = chatRepository.uploadFile(
                    mimeType,
                    fileName,
                    byteStrings,
                    blockDigestStrings,
                    fileHashString
                )
                imageUrls.add(url)
            }
            if (isRegisteredGroup != null) {
                sendMessageToGroup(groupId, "${imageUrls.joinToString(" ")} $message", isRegisteredGroup, tempMessageId)
            } else {
                sendMessageToUser(receiverPeople!!, groupId, "${imageUrls.joinToString(" ")} $message", tempMessageId)
            }
        }
    }

    private fun isValidFileCount() =
        _imageUriSelected.value != null && _imageUriSelected.value!!.size <= FILE_MAX_COUNT

    private fun isValidFileSizes(context: Context, fileUriList: List<String>) : Boolean {
        fileUriList.forEach {
            val fileSize = getFileSize(context, Uri.parse(it))
            printlnCK("File size $fileSize")
            if (fileSize > FILE_MAX_SIZE) {
                return false
            }
        }
        return true
    }

    private fun getFileMimeType(context: Context, uri: Uri): String {
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri)
        return mimeType ?: ""
    }

    private fun getFileName(context: Context, uri: Uri): String {
        val contentResolver = context.contentResolver
        val cursor = contentResolver.query(uri, null, null, null, null, null)
        if (cursor != null && cursor.moveToFirst()) {
            return cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
        }
        return ""
    }

    private fun getFileSize(context: Context, uri: Uri): Long {
        val contentResolver = context.contentResolver
        val cursor = contentResolver.query(uri, null, null, null, null, null)
        if (cursor != null && cursor.moveToFirst()) {
            return cursor.getLong(cursor.getColumnIndex(OpenableColumns.SIZE))
        }
        return 0L
    }

    private fun byteArrayToMd5HashString(byteArray: ByteArray): String {
        val bigInt = BigInteger(1, byteArray)
        val hashString = bigInt.toString(16)
        return String.format("%32s", hashString).replace(' ', '0')
    }

    companion object {
        private const val FILE_UPLOAD_CHUNK_SIZE = 4_000_000 //4MB
        private const val FILE_MAX_COUNT = 10
        private const val FILE_MAX_SIZE = 4_000_000 //4MB
    }
}

class RequestInfo(val chatGroup: ChatGroup, val isAudioMode: Boolean)