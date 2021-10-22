package com.clearkeep.screen.chat.room

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import androidx.lifecycle.*
import com.clearkeep.db.clear_keep.model.*
import com.clearkeep.dynamicapi.Environment
import com.clearkeep.repo.ServerRepository
import com.clearkeep.screen.auth.repo.AuthRepository
import com.clearkeep.screen.chat.repo.*
import com.clearkeep.screen.chat.room.message_display_generator.MessageDisplayInfo
import com.clearkeep.utilities.*
import com.clearkeep.utilities.files.*
import com.clearkeep.utilities.network.Resource
import com.clearkeep.utilities.network.Status
import com.google.protobuf.ByteString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.IllegalArgumentException
import java.security.MessageDigest
import javax.inject.Inject
import java.util.*

class RoomViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val groupRepository: GroupRepository,
    private val signalKeyRepository: SignalKeyRepository,
    authRepository: AuthRepository,
    serverRepository: ServerRepository,
    messageRepository: MessageRepository,
    private val peopleRepository: PeopleRepository,
    private val environment: Environment,
): BaseViewModel(authRepository, groupRepository, serverRepository, messageRepository) {
    val isLogout = serverRepository.isLogout

    private var roomId: Long? = null

    private var friendId: String? = null
    private var friendDomain: String? = null

    private var isLatestPeerSignalKeyProcessed = false

    private val _group = MutableLiveData<ChatGroup>()

    val requestCallState = MutableLiveData<Resource<RequestInfo>>()

    val group: LiveData<ChatGroup>
        get() = _group

    var clientId: String = ""

    var domain: String = ""

    val groups: LiveData<List<ChatGroup>> = groupRepository.getAllRooms()

    private val _imageUriSelected = MutableLiveData<List<String>>()
    val imageUriSelected: LiveData<List<String>>
        get() = _imageUriSelected

    private val _fileUriStaged = MutableLiveData<Map<Uri, Boolean>>()
    val fileUriStaged: LiveData<Map<Uri, Boolean>>
        get() = _fileUriStaged

    val uploadFileResponse = MutableLiveData<Resource<String>>()

    private val _message = MutableLiveData<String>()
    val message : LiveData<String>
        get() = _message

    private val _isNote = MutableLiveData<Boolean>()
    val isNote : LiveData<Boolean>
        get() = _isNote

    private var _currentPhotoUri : Uri? = null

    private val _imageDetailList = MutableLiveData<List<String>>()
    val imageDetailList : LiveData<List<String>>
        get() = _imageDetailList

    private val _imageDetailSenderName = MutableLiveData<String>()
    val imageDetailSenderName : LiveData<String>
        get() = _imageDetailSenderName

    private val _listUserStatus = MutableLiveData<List<User>>()
    val listUserStatus: LiveData<List<User>>
        get() = _listUserStatus

    val listPeerAvatars = MutableLiveData<List<String>>()

    private var _selectedMessage : MessageDisplayInfo? = null
    val selectedMessage : MessageDisplayInfo?
        get() = _selectedMessage

    val getGroupResponse = MutableLiveData<Resource<ChatGroup>>()
    val createGroupResponse = MutableLiveData<Resource<ChatGroup>>()
    val inviteToGroupResponse = MutableLiveData<Resource<ChatGroup>>()
    val sendMessageResponse = MutableLiveData<Resource<Any>>()

    init {
        //getStatusUserInGroup()
    }

    fun setMessage(message: String) {
        _message.value = message
    }

    fun clearTempMessage() {
        viewModelScope.launch {
            messageRepository.clearTempMessage()
            messageRepository.clearTempNotes()
        }
    }

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
                getStatusUserInGroup()
            } else if (!friendId.isNullOrEmpty() && !friendDomain.isNullOrEmpty()) {
                updateGroupWithFriendId(friendId, friendDomain)
            }

        }

    }

    private suspend fun getStatusUserInGroup() {
        group.value?.clientList?.let {
            val listClientStatus = it.let { it1 ->
                peopleRepository.getListClientStatus(it1)
            }
            _listUserStatus.postValue(listClientStatus)

            listClientStatus?.forEach {
                peopleRepository.updateAvatarUserEntity(it, getOwner())
            }
            if (group.value?.isGroup() == false) {
                val avatars = arrayListOf<String>()
                listClientStatus?.forEach {
                    if (it.userId != getUser().userId)
                        it.avatar?.let { it1 -> avatars.add(it1) }
                }
                listPeerAvatars.postValue(avatars)
            }
        }
    }

    fun initNotes(
        ownerDomain: String,
        ownerClientId: String,
    ) {
        if (ownerDomain.isBlank() || ownerClientId.isBlank()) {
            throw IllegalArgumentException("domain and clientId must be not NULL")
        }
        this.domain = ownerDomain
        this.clientId = ownerClientId
        _isNote.value = true
        viewModelScope.launch {
            updateNotesFromRemote()
        }
    }

    fun leaveRoom() {
        setJoiningRoomId(-1)
    }

    fun refreshRoom(){
        printlnCK("refreshRoom")
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

    fun getNotes() : LiveData<List<Message>> {
        return messageRepository.getNotesAsState(Owner(domain, clientId)).map { notes -> notes.map { Message(it.generateId?.toInt(), "", 0L, "", it.ownerClientId, "", it.content, it.createdTime, it.createdTime, it.ownerDomain, it.ownerClientId) } }
    }

    private suspend fun updateGroupWithId(groupId: Long) {
        printlnCK("updateGroupWithId: groupId $groupId")
        getGroupResponse.value = groupRepository.getGroupByID(groupId, domain, clientId)
        getGroupResponse.value?.data?.let {
            setJoiningGroup(it)
            updateMessagesFromRemote(groupId, it.lastMessageSyncTimestamp)
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

    private suspend fun updateNotesFromRemote() {
        val server = environment.getServer()
        messageRepository.updateNotesFromAPI(Owner(server.serverDomain, server.profile.userId))
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
                e.printStackTrace()
            }
        }
    }

    private fun sendMessageToUser(receiverPeople: User, groupId: Long, message: String, tempMessageId: Int = 0) {
        viewModelScope.launch {
            var lastGroupId: Long = groupId
            if (lastGroupId == GROUP_ID_TEMPO) {
                val user = environment.getServer().profile
                user.avatar = ""
                createGroupResponse.value = groupRepository.createGroupFromAPI(
                    user.userId,
                    "$user,${receiverPeople.userName}",
                    mutableListOf(getUser(), receiverPeople),
                    false
                )
                createGroupResponse.value?.data?.let {
                    setJoiningGroup(it)
                    lastGroupId = it.groupId
                }
            }

            if (lastGroupId != GROUP_ID_TEMPO) {
                if (!isLatestPeerSignalKeyProcessed) {
                    // work around: always load user signal key for first open room
                    val response = chatRepository.sendMessageInPeer(clientId, domain, receiverPeople.userId, receiverPeople.domain, lastGroupId, message, isForceProcessKey = true, cachedMessageId = tempMessageId)
                    isLatestPeerSignalKeyProcessed = response.status == Status.SUCCESS
                    sendMessageResponse.value = response
                } else {
                    sendMessageResponse.value = chatRepository.sendMessageInPeer(
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
                e.printStackTrace()
            }
        }
    }

    private suspend fun sendMessageToGroup(
        groupId: Long,
        message: String,
        isRegisteredGroup: Boolean,
        cachedMessageId: Int = 0
    ) {
         val group = groupRepository.getGroupByGroupId(groupId)
        group?.let {
            if (!group.isJoined) {
                val result =
                    signalKeyRepository.registerSenderKeyToGroup(groupId, clientId, domain)
                if (result) {
                    _group.value = groupRepository.remarkGroupKeyRegistered(groupId)
                    sendMessageResponse.value = chatRepository.sendMessageToGroup(
                        clientId,
                        domain,
                        groupId,
                        message,
                        cachedMessageId
                    )
                } else {
                    sendMessageResponse.value = Resource.error("", null, ERROR_CODE_TIMEOUT)
                }
            } else {
                sendMessageResponse.value = chatRepository.sendMessageToGroup(
                    clientId,
                    domain,
                    groupId,
                    message,
                    cachedMessageId
                )
            }
        }
    }

    fun sendNote(context: Context) {
        viewModelScope.launch {
            try {
                if (!_imageUriSelected.value.isNullOrEmpty()) {
                    uploadImage(context, message = _message.value ?: "")
                } else {
                    sendNote(_message.value ?: "")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun sendNote(message: String, cachedNoteId: Long = 0) {
        viewModelScope.launch {
            chatRepository.sendNote(Note(null, message, Calendar.getInstance().timeInMillis, domain, clientId), cachedNoteId)
        }
    }

    fun inviteToGroup(invitedUsers: List<User>, groupId: Long) {
        viewModelScope.launch {
            inviteToGroupResponse.value =
                groupRepository.inviteToGroupFromAPIs(invitedUsers, groupId, getOwner())
            inviteToGroupResponse.value!!.data?.let {
                setJoiningGroup(it)
            }
        }
    }

    fun removeMember(
        user: User,
        groupId: Long,
        onSuccess: (() -> Unit)? = null,
        onError: (() -> Unit)?
    ) {
        viewModelScope.launch {
            val remoteMember = groupRepository.removeMemberInGroup(user, groupId, getOwner())
            if (remoteMember) {
                val ret = groupRepository.getGroupFromAPIById(groupId, domain, clientId)
                ret?.data?.let {
                    setJoiningGroup(it)
                    updateMessagesFromRemote(groupId, it.lastMessageSyncTimestamp)
                    onSuccess?.invoke()
                }
            } else {
                onError?.invoke()
            }
        }
    }

    fun leaveGroup(onSuccess: (() -> Unit)? = null, onError: (() -> Unit)? = null) {
        roomId?.let {
            viewModelScope.launch {
                val result = groupRepository.leaveGroup(it, getOwner())

                if (result) {
                    onSuccess?.invoke()
                } else {
                    onError?.invoke()
                }
            }
        }
    }

    fun requestCall(groupId: Long, isAudioMode: Boolean) {
        viewModelScope.launch {
            requestCallState.value = Resource.loading(null)

            var lastGroupId: Long = groupId
            if (lastGroupId == GROUP_ID_TEMPO) {
                val user = getUser()
                printlnCK("requestCall $domain")
                val friend = peopleRepository.getFriend(friendId!!, friendDomain!! , getOwner())!!
                createGroupResponse.value = groupRepository.createGroupFromAPI(
                    user.userId,
                    "",
                    mutableListOf(user, friend),
                        false
                )
                createGroupResponse.value?.data?.let {
                    setJoiningGroup(it)
                    lastGroupId = it.groupId
                }
            }

            if (lastGroupId != GROUP_ID_TEMPO && lastGroupId != 0L) {
                requestCallState.value = Resource.success(_group.value?.let { RequestInfo(it, isAudioMode) })
            } else {
                requestCallState.value = Resource.error("error",
                    _group.value?.let { RequestInfo(it, isAudioMode) })
            }
        }
    }

    fun deleteSelectedMessage() {
        viewModelScope.launch {
            if (isNote.value == true) {
                messageRepository.deleteNote((selectedMessage?.message?.generateId ?: 0).toLong())
            }
        }
    }

    fun copySelectedMessage(context: Context) {
        val clipboard: ClipboardManager =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip =
            ClipData.newPlainText("", getMessageContent(selectedMessage?.message?.message ?: ""))
        clipboard.setPrimaryClip(clip)
    }

    private fun setJoiningGroup(group: ChatGroup) {
        _group.value = group
        chatRepository.setJoiningRoomId(group.groupId)
    }

    private fun getUser(): User {
        val server = environment.getServer()
        return User(userId = server.profile.userId, userName = server.profile.userName ?: "", domain = server.serverDomain)
    }

    fun getCurrentUser(): User {
        return getUser()
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

    fun addImage() {
        val list = mutableListOf<String>()
        list.addAll(_imageUriSelected.value ?: emptyList())
        list.add(_currentPhotoUri.toString())
        _currentPhotoUri = null
        _imageUriSelected.value = list
    }

    private fun uploadImage(
        context: Context,
        groupId: Long = 0L,
        message: String,
        isRegisteredGroup: Boolean? = null,
        receiverPeople: User? = null
    ) {
        val imageUris = _imageUriSelected.value
        _imageUriSelected.value = emptyList()
        imageUris?.let {
            uploadFile(it, context, groupId, message, isRegisteredGroup, receiverPeople, persistablePermission = false)
        }
    }

    fun uploadFile(
        context: Context, groupId: Long,
        isRegisteredGroup: Boolean? = null,
        receiverPeople: User? = null
    ) {
        val files = _fileUriStaged.value?.filter { it.value }?.keys?.map { it.toString() }?.toList()
        _fileUriStaged.value = emptyMap()
        files?.let {
            uploadFile(
                it,
                context,
                groupId,
                null,
                isRegisteredGroup,
                receiverPeople,
                appendFileSize = true
            )
        }
    }

    private fun uploadFile(
        urisList: List<String>,
        context: Context,
        groupId: Long,
        message: String?,
        isRegisteredGroup: Boolean?,
        receiverPeople: User?,
        appendFileSize: Boolean = false,
        persistablePermission: Boolean = true
    ) {
        printlnCK("upload files uri list $urisList")
        if (!urisList.isNullOrEmpty()) {
            if (!isValidFilesCount(urisList)) {
                uploadFileResponse.postValue(
                    Resource.error(
                        "Maximum number of attachments in a message reached (10)",
                        null
                    )
                )
                return
            }

            if (!isValidFileSizes(context, urisList, persistablePermission)) {
                uploadFileResponse.value =
                    Resource.error("Your file cannot be larger than 200MB.", null)
                return
            }

            GlobalScope.launch {
                val tempMessageUris = urisList.joinToString(" ")
                val tempMessageContent =
                    if (message != null) "$tempMessageUris $message" else tempMessageUris
                println("take photo tempMessageContent $tempMessageContent")
                val tempMessageId = if (isNote.value == true) {
                    messageRepository.saveNote(
                        Note(
                            null,
                            tempMessageContent,
                            Calendar.getInstance().timeInMillis,
                            getOwner().domain,
                            getOwner().clientId,
                            true
                        )
                    )
                } else {
                    messageRepository.saveMessage(
                        Message(
                            null,
                            "",
                            groupId,
                            getOwner().domain,
                            getOwner().clientId,
                            getOwner().clientId,
                            tempMessageContent,
                            Calendar.getInstance().timeInMillis,
                            Calendar.getInstance().timeInMillis,
                            getOwner().domain,
                            getOwner().clientId
                        )
                    )
                }
                val fileUrls = mutableListOf<String>()
                val filesSizeInBytes = mutableListOf<Long>()
                urisList.forEach { uriString ->
                    val uri = Uri.parse(uriString)
                    val contentResolver = context.contentResolver
                    if (persistablePermission) {
                        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    val mimeType = getFileMimeType(context, uri, persistablePermission)
                    val fileName = uri.getFileName(context, persistablePermission)
                    val byteStrings = mutableListOf<ByteString>()
                    val blockDigestStrings = mutableListOf<String>()
                    val byteArray = ByteArray(FILE_UPLOAD_CHUNK_SIZE)
                    val inputStream = contentResolver.openInputStream(uri)
                    var fileSize = 0L
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
                    val fileHashByteArray = fileDigest.digest()
                    val fileHashString = byteArrayToMd5HashString(fileHashByteArray)
                    val urlResponse = chatRepository.uploadFile(
                        mimeType,
                        fileName.replace(" ", "_"),
                        byteStrings,
                        blockDigestStrings,
                        fileHashString
                    )
                    if (urlResponse.status == Status.SUCCESS) {
                        fileUrls.add(urlResponse.data ?: "")
                        filesSizeInBytes.add(fileSize)
                    } else {
                        sendMessageResponse.value = urlResponse
                        return@forEach
                    }
                }
                val fileUrlsString = if (appendFileSize) {
                    fileUrls.filter{ it.isNotBlank() }.mapIndexed { index, url -> "$url|${filesSizeInBytes[index]}" }
                        .joinToString(" ")
                } else {
                    fileUrls.joinToString(" ")
                }
                val messageContent =
                    if (message != null) "$fileUrlsString $message" else fileUrlsString
                withContext(Dispatchers.Main) {
                    if (isNote.value == true) {
                        sendNote(messageContent, tempMessageId.toLong())
                    } else {
                        println("take photo upload success messageContent $messageContent")
                        if (isRegisteredGroup != null) {
                            sendMessageToGroup(
                                groupId,
                                messageContent,
                                isRegisteredGroup,
                                tempMessageId.toInt()
                            )
                        } else {
                            sendMessageToUser(
                                receiverPeople!!,
                                groupId,
                                messageContent,
                                tempMessageId.toInt()
                            )
                        }
                    }
                }
            }
        }
    }

    private fun isValidFilesCount(fileUriList: List<String>?) =
        fileUriList != null && fileUriList.size <= FILE_MAX_COUNT

    private fun isValidFileSizes(context: Context, fileUriList: List<String>, persistablePermission: Boolean): Boolean {
        var totalFileSize = 0L
        fileUriList.forEach {
            val uri = Uri.parse(it)
            totalFileSize += uri.getFileSize(context, persistablePermission)
        }
        if (totalFileSize > FILE_MAX_SIZE) {
            return false
        }
        return true
    }

    fun addStagedFileUri(uri: Uri) {
        val stagedList = mutableMapOf<Uri, Boolean>()
        stagedList.putAll(_fileUriStaged.value ?: emptyMap())
        stagedList[uri] = true
        _fileUriStaged.value = stagedList
    }

    fun toggleSelectedFile(uri: Uri) {
        val selectedList = mutableMapOf<Uri, Boolean>()
        selectedList.putAll(_fileUriStaged.value ?: emptyMap())
        if (selectedList.contains(uri)) {
            val oldValue = selectedList[uri]
            selectedList[uri] = !oldValue!!
        }
        _fileUriStaged.value = selectedList
    }

    fun downloadFile(context: Context, url: String) {
        chatRepository.downloadFile(context, getFileNameFromUrl(url), getFileUrl(url))
    }

    fun getPhotoUri(context: Context) : Uri {
        if (_currentPhotoUri == null) {
            _currentPhotoUri = generatePhotoUri(context)
        }
        return _currentPhotoUri!!
    }

    fun setImageDetailList(list: List<String>) {
        _imageDetailList.value = list
    }

    fun setImageDetailSenderName(senderName: String) {
        _imageDetailSenderName.value = senderName
    }

    fun getUserName() : String {
        return environment.getServer().profile.userName ?: ""
    }

    fun getUserAvatarUrl() : String {
        printlnCK("getUserAvatarUrl: ${listPeerAvatars.value?.get(0)}")
        return listPeerAvatars.value?.get(0) ?: ""
    }

    fun setSelectedMessage(selectedMessage: MessageDisplayInfo) {
        _selectedMessage = selectedMessage
    }

    override fun onCleared() {
        super.onCleared()
        printlnCK("Share file cancel onCleared")
    }

    companion object {
        private const val FILE_UPLOAD_CHUNK_SIZE = 4_000_000 //4MB
        private const val FILE_MAX_COUNT = 10
        private const val FILE_MAX_SIZE = 200_000_000 //200MB
    }
}

class RequestInfo(val chatGroup: ChatGroup, val isAudioMode: Boolean)