package com.clearkeep.presentation.screen.chat.room

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import androidx.lifecycle.*
import com.clearkeep.R
import com.clearkeep.data.repository.*
import com.clearkeep.domain.repository.*
import com.clearkeep.data.remote.dynamicapi.Environment
import com.clearkeep.domain.model.*
import com.clearkeep.domain.usecase.auth.LogoutUseCase
import com.clearkeep.domain.usecase.chat.*
import com.clearkeep.domain.usecase.group.*
import com.clearkeep.domain.usecase.message.*
import com.clearkeep.domain.usecase.people.GetFriendUseCase
import com.clearkeep.domain.usecase.people.GetListClientStatusUseCase
import com.clearkeep.domain.usecase.people.UpdateAvatarUserEntityUseCase
import com.clearkeep.domain.usecase.server.*
import com.clearkeep.domain.usecase.signalkey.RegisterSenderKeyToGroupUseCase
import com.clearkeep.presentation.screen.chat.room.messagedisplaygenerator.MessageDisplayInfo
import com.clearkeep.utilities.*
import com.clearkeep.utilities.files.*
import com.clearkeep.utilities.network.Resource
import com.clearkeep.utilities.network.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import java.lang.IllegalArgumentException
import javax.inject.Inject
import java.util.*

@HiltViewModel
class RoomViewModel @Inject constructor(
    private val environment: Environment,
    getAllRoomsUseCase: GetAllRoomsUseCase,
    private val createGroupUseCase: CreateGroupUseCase,
    private val inviteToGroupUseCase: InviteToGroupUseCase,
    private val removeMemberUseCase: RemoveMemberUseCase,
    private val leaveGroupUseCase: LeaveGroupUseCase,
    private val getGroupByGroupIdUseCase: GetGroupByGroupIdUseCase,
    private val getTemporaryGroupUseCase: GetTemporaryGroupUseCase,
    private val getGroupByIdUseCase: GetGroupByIdUseCase,
    deleteGroupUseCase: DeleteGroupUseCase,
    deleteMessageUseCase: DeleteMessageUseCase,
    private val getGroupPeerByClientIdUseCase: GetGroupPeerByClientIdUseCase,
    private val remarkGroupKeyRegisteredUseCase: RemarkGroupKeyRegisteredUseCase,
    private val getAllPeerGroupByDomainUseCase: GetAllPeerGroupByDomainUseCase,
    private val downloadFileUseCase: DownloadFileUseCase,

    private val sendMessageUseCase: SendMessageUseCase,
    private val sendNoteUseCase: SendNoteUseCase,
    private val uploadFileUseCase: UploadFileUseCase,
    private val setJoiningRoomUseCase: SetJoiningRoomUseCase,

    private val getMessageAsStateUseCase: GetMessageAsStateUseCase,
    private val getNotesAsStateUseCase: GetNotesAsStateUseCase,
    private val saveMessageUseCase: SaveMessageUseCase,
    private val saveNoteUseCase: SaveNoteUseCase,
    private val clearTempNotesUseCase: ClearTempNotesUseCase,
    private val clearTempMessageUseCase: ClearTempMessageUseCase,
    private val updateNotesFromApiUseCase: UpdateNotesFromApiUseCase,
    private val updateMessageFromApiUseCase: UpdateMessageFromApiUseCase,
    private val getServerByOwnerUseCase: GetServerByOwnerUseCase,

    private val getListClientStatusUseCase: GetListClientStatusUseCase,
    private val updateAvatarUserEntityUseCase: UpdateAvatarUserEntityUseCase,
    private val getFriendUseCase: GetFriendUseCase,

    private val registerSenderKeyToGroupUseCase: RegisterSenderKeyToGroupUseCase,

    getDefaultServerProfileAsStateUseCase: GetDefaultServerProfileAsStateUseCase,
    setActiveServerUseCase: SetActiveServerUseCase,
    logoutUseCase: LogoutUseCase,
    deleteServerUseCase: DeleteServerUseCase,
    getIsLogoutUseCase: GetIsLogoutUseCase,
    getServersUseCase: GetServersUseCase,
    getServersAsStateUseCase: GetServersAsStateUseCase,
    getActiveServerUseCase: GetActiveServerUseCase,
) : BaseViewModel(deleteGroupUseCase, deleteMessageUseCase, logoutUseCase, deleteServerUseCase, setActiveServerUseCase, getServersUseCase, getServersAsStateUseCase, getActiveServerUseCase) {
    val isLogout = getIsLogoutUseCase()

    val profile = getDefaultServerProfileAsStateUseCase()

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

    val groups: LiveData<List<ChatGroup>> = getAllRoomsUseCase()

    private val _imageUriSelected = MutableLiveData<List<String>>()
    val imageUriSelected: LiveData<List<String>>
        get() = _imageUriSelected

    private val _fileUriStaged = MutableLiveData<Map<Uri, Boolean>>()
    val fileUriStaged: LiveData<Map<Uri, Boolean>>
        get() = _fileUriStaged

    val uploadFileResponse = MutableLiveData<Resource<String>>()

    private val _message = MutableLiveData<String>()
    val message: LiveData<String>
        get() = _message

    private val _isNote = MutableLiveData<Boolean>()
    val isNote: LiveData<Boolean>
        get() = _isNote

    private var _currentPhotoUri: Uri? = null

    private val _imageDetailList = MutableLiveData<List<String>>()
    val imageDetailList: LiveData<List<String>>
        get() = _imageDetailList

    private val _imageDetailSenderName = MutableLiveData<String>()
    val imageDetailSenderName: LiveData<String>
        get() = _imageDetailSenderName

    private val _listGroupUserStatus = MutableLiveData<List<User>>()
    val listGroupUserStatus: LiveData<List<User>>
        get() = _listGroupUserStatus
    
    private val _listUserStatus = MutableLiveData<List<User>>()
    val listUserStatus: LiveData<List<User>> get() = _listUserStatus

    val listPeerAvatars = MutableLiveData<List<String>>()

    private var _selectedMessage: MessageDisplayInfo? = null
    val selectedMessage: MessageDisplayInfo?
        get() = _selectedMessage

    val quotedMessage = MutableLiveData<MessageDisplayInfo>()

    val getGroupResponse = MutableLiveData<Resource<ChatGroup>>()
    val createGroupResponse = MutableLiveData<Resource<ChatGroup>>()
    val inviteToGroupResponse = MutableLiveData<Resource<ChatGroup>>()
    val sendMessageResponse = MutableLiveData<Resource<Any>>()
    val forwardMessageResponse = MutableLiveData<Long>()

    val isLoading = MutableLiveData(false)

    @Volatile
    private var endOfPaginationReached = false
    @Volatile
    private var lastLoadRequestTimestamp = 0L

    init {
        viewModelScope.launch {
            getStatusUserInDirectGroup()
        }
    }

    fun setQuoteMessage() {
        quotedMessage.value = selectedMessage
    }

    fun clearQuoteMessage() {
        quotedMessage.value = null
    }

    fun setMessage(message: String) {
        _message.value = message
    }

    fun clearTempMessage() {
        viewModelScope.launch {
            clearTempMessageUseCase()
            clearTempNotesUseCase()
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
            val selectedServer =
                getServerByOwnerUseCase(Owner(ownerDomain, ownerClientId))
            if (selectedServer == null) {
                printlnCK("default server must be not NULL")
                return@launch
            }
            environment.setUpDomain(selectedServer)
            setActiveServerUseCase(selectedServer)

            if (roomId != null && roomId != 0L) {
                isLoading.postValue(true)
                updateGroupWithId(roomId)
                isLoading.postValue(false)
                getStatusUserInGroup()
            } else if (!friendId.isNullOrEmpty() && !friendDomain.isNullOrEmpty()) {
                updateGroupWithFriendId(friendId, friendDomain)
            }
        }
    }

    private suspend fun getStatusUserInGroup() {
        group.value?.clientList?.let {
            val listClientStatus = it.let { it1 ->
                getListClientStatusUseCase(it1)
            }
            _listGroupUserStatus.postValue(listClientStatus)

            listClientStatus?.forEach {
                updateAvatarUserEntityUseCase(it, getOwner())
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

    private suspend fun getStatusUserInDirectGroup() {
        try {
            val currentUser = getUser()

            val listUserRequest = arrayListOf<User>()
            getAllPeerGroupByDomainUseCase(
                owner = Owner(
                    currentUser.domain,
                    currentUser.userId
                )
            )
                .forEach { group ->
                    if (!group.isGroup()) {
                        val user = group.clientList.firstOrNull { client ->
                            client.userId != currentUser.userId
                        }
                        if (user != null) {
                            listUserRequest.add(user)
                        }
                    }
                }
            val listClientStatus = getListClientStatusUseCase(listUserRequest)
            _listUserStatus.postValue(listClientStatus)
            listClientStatus?.forEach {
                currentServer.value?.serverDomain?.let { it1 ->
                    currentServer.value?.ownerClientId?.let { it2 ->
                        Owner(it1, it2)
                    }
                }?.let { it2 -> updateAvatarUserEntityUseCase(it, owner = it2) }
            }
            delay(60 * 1000)
            getStatusUserInDirectGroup()

        } catch (e: Exception) {
            printlnCK("getStatusUserInDirectGroup error: ${e.message}")
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

    fun refreshRoom() {
        printlnCK("refreshRoom")
        viewModelScope.launch {
            roomId?.let { updateGroupWithId(it) }
        }
    }

    fun setJoiningRoomId(roomId: Long) {
        setJoiningRoomUseCase(roomId)
    }

    fun getMessages(groupId: Long, domain: String, clientId: String): LiveData<List<Message>> {
        return getMessageAsStateUseCase(groupId, Owner(domain, clientId))
    }

    fun getNotes(): LiveData<List<Message>> {
        return getNotesAsStateUseCase(Owner(domain, clientId)).map { notes ->
            notes.map {
                Message(
                    it.generateId?.toInt(),
                    "",
                    0L,
                    "",
                    it.ownerClientId,
                    "",
                    it.content,
                    it.createdTime,
                    it.createdTime,
                    it.ownerDomain,
                    it.ownerClientId
                )
            }
        }
    }

    private suspend fun updateGroupWithId(groupId: Long) {
        printlnCK("updateGroupWithId: groupId $groupId")
        getGroupResponse.value = getGroupByIdUseCase(groupId, domain, clientId)
        getGroupResponse.value?.data?.let {
            setJoiningGroup(it)
            updateMessagesFromRemote(it.lastMessageSyncTimestamp)
        }
    }

    private suspend fun updateGroupWithFriendId(friendId: String, friendDomain: String) {
        printlnCK("updateGroupWithFriendId: friendId $friendId")
        val friend = getFriendUseCase(friendId, friendDomain, getOwner())
            ?: User(userId = friendId, userName = "", domain = friendDomain)
        if (friend == null) {
            printlnCK("updateGroupWithFriendId: can not find friend with id $friendId")
            return
        }
        var existingGroup = getGroupPeerByClientIdUseCase(
            friend,
            Owner(domain = domain, clientId = clientId)
        )
        if (existingGroup == null) {
            existingGroup = getTemporaryGroupUseCase(getUser(), friend)
        } else {
            updateMessagesFromRemote(existingGroup.lastMessageSyncTimestamp)
        }
        setJoiningGroup(existingGroup)
    }

    private fun getOwner(): Owner {
        return Owner(domain, clientId)
    }

    private fun updateMessagesFromRemote(lastMessageAt: Long) {
        onScrollChange(lastMessageAt, true)
    }

    private suspend fun updateNotesFromRemote() {
        val server = environment.getServer()
        updateNotesFromApiUseCase(Owner(server.serverDomain, server.profile.userId))
    }

    fun sendMessageToUser(context: Context, receiverPeople: User, groupId: Long, message: String, isForwardMessage: Boolean = false) {
        viewModelScope.launch {
            try {
                val quotedMessage = quotedMessage.value
                val encodedMessage = if (isForwardMessage) ">>>$message" else if (quotedMessage != null) "```${quotedMessage.userName}|${quotedMessage.message.message}|${quotedMessage.message.createdTime}|$message" else message
                this@RoomViewModel.quotedMessage.value = null

                if (!_imageUriSelected.value.isNullOrEmpty()) {
                    uploadImage(context, groupId, encodedMessage, null, receiverPeople)
                } else {
                    sendMessageToUser(receiverPeople, groupId, encodedMessage)
                }
                if (isForwardMessage) {
                    forwardMessageResponse.value = groupId
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun sendMessageToUser(
        receiverPeople: User,
        groupId: Long,
        message: String,
        tempMessageId: Int = 0
    ) {
        viewModelScope.launch {
            var lastGroupId: Long = groupId
            if (lastGroupId == GROUP_ID_TEMPO) {
                val user = environment.getServer().profile
                user.avatar = ""
                createGroupResponse.value = createGroupUseCase(
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
                    val response = sendMessageUseCase.toPeer(
                        clientId,
                        domain,
                        receiverPeople.userId,
                        receiverPeople.domain,
                        lastGroupId,
                        message,
                        isForceProcessKey = true,
                        cachedMessageId = tempMessageId
                    )
                    isLatestPeerSignalKeyProcessed = response.status == Status.SUCCESS
                    sendMessageResponse.value = response
                } else {
                    sendMessageResponse.value = sendMessageUseCase.toPeer(
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
        isRegisteredGroup: Boolean,
        isForwardMessage: Boolean = false
    ) {
        viewModelScope.launch {
            try {
                val quotedMessage = quotedMessage.value
                val encodedMessage = if (isForwardMessage) ">>>$message" else if (quotedMessage != null) "```${quotedMessage.userName}|${quotedMessage.message.message}|${quotedMessage.message.createdTime}|$message" else message
                this@RoomViewModel.quotedMessage.value = null

                if (!_imageUriSelected.value.isNullOrEmpty()) {
                    uploadImage(context, groupId, encodedMessage, isRegisteredGroup)
                } else {
                    sendMessageToGroup(groupId, encodedMessage)
                }

                if (isForwardMessage) {
                    forwardMessageResponse.value = groupId
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun sendMessageToGroup(
        groupId: Long,
        message: String,
        cachedMessageId: Int = 0
    ) {
        val group = getGroupByGroupIdUseCase(groupId)
        group?.let {
            if (!group.isJoined) {
                val result =
                    registerSenderKeyToGroupUseCase(groupId, clientId, domain)
                if (result) {
                    _group.value = remarkGroupKeyRegisteredUseCase(groupId)
                    sendMessageResponse.value = sendMessageUseCase.toGroup(
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
                sendMessageResponse.value = sendMessageUseCase.toGroup(
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
            sendNoteUseCase(
                Note(
                    null,
                    message,
                    Calendar.getInstance().timeInMillis,
                    domain,
                    clientId
                ), cachedNoteId
            )
        }
    }

    fun inviteToGroup(invitedUsers: List<User>, groupId: Long) {
        viewModelScope.launch {
            inviteToGroupResponse.value =
                inviteToGroupUseCase(invitedUsers, groupId, getOwner())
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
            val remoteMember = removeMemberUseCase(user, groupId, getOwner())
            if (remoteMember) {
                val ret = getGroupByIdUseCase(groupId, domain, clientId)
                ret?.data?.let {
                    setJoiningGroup(it)
                    updateMessagesFromRemote(it.lastMessageSyncTimestamp)
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
                val result = leaveGroupUseCase(it, getOwner())
                if (result) {
                    deleteMessageUseCase(
                        it,
                        getOwner().domain,
                        getOwner().clientId
                    )
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
                val friend = getFriendUseCase(friendId!!, friendDomain!!, getOwner())!!
                createGroupResponse.value = createGroupUseCase(
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
                requestCallState.value =
                    Resource.success(_group.value?.let { RequestInfo(it, isAudioMode) })
            } else {
                requestCallState.value = Resource.error("error",
                    _group.value?.let { RequestInfo(it, isAudioMode) })
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
        setJoiningRoomUseCase(group.groupId)
        endOfPaginationReached = false
        lastLoadRequestTimestamp = 0L
    }

    private fun getUser(): User {
        val server = environment.getServer()
        return User(
            userId = server.profile.userId,
            userName = server.profile.userName ?: "",
            domain = server.serverDomain
        )
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
            uploadFile(
                it,
                context,
                groupId,
                message,
                isRegisteredGroup,
                receiverPeople,
                persistablePermission = false
            )
        }
    }

    fun uploadFile(
        context: Context,
        groupId: Long,
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
        if (!urisList.isNullOrEmpty()) {
            if (!isValidFilesCount(urisList)) {
                uploadFileResponse.postValue(
                    Resource.error(
                        context.getString(R.string.upload_file_error_too_many),
                        null
                    )
                )
                return
            }

            if (!isValidFileSizes(context, urisList, persistablePermission)) {
                uploadFileResponse.value =
                    Resource.error(context.getString(R.string.upload_file_error_too_large), null)
                return
            }

            GlobalScope.launch {
                val tempMessageUris = urisList.joinToString(" ")
                val tempMessageContent =
                    if (message != null) "$tempMessageUris $message" else tempMessageUris
                val tempMessageId = if (isNote.value == true) {
                    saveNoteUseCase(
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
                    saveMessageUseCase(
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
                    val fileSize = uri.getFileSize(context, persistablePermission)
                    val contentResolver = context.contentResolver
                    if (persistablePermission) {
                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }
                    val mimeType = getFileMimeType(context, uri, persistablePermission)
                    val fileName = uri.getFileName(context, persistablePermission)
                    val urlResponse = uploadFileUseCase(
                        context,
                        mimeType,
                        fileName.replace(" ", "_"),
                        uriString
                    )
                    if (urlResponse.status == Status.SUCCESS) {
                        fileUrls.add(urlResponse.data ?: "")
                        filesSizeInBytes.add(fileSize)
                    } else {
                        sendMessageResponse.postValue(urlResponse)
                        return@forEach
                    }
                }
                val fileUrlsString = if (appendFileSize) {
                    fileUrls.filter { it.isNotBlank() }
                        .mapIndexed { index, url -> "$url|${filesSizeInBytes[index]}" }
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
                        if (isRegisteredGroup != null) {
                            sendMessageToGroup(
                                groupId,
                                messageContent,
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

    private fun isValidFileSizes(
        context: Context,
        fileUriList: List<String>,
        persistablePermission: Boolean
    ): Boolean {
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

    fun downloadFile(url: String) {
        downloadFileUseCase(url)
    }

    fun getPhotoUri(context: Context): Uri {
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

    fun getUserName(): String {
        return environment.getServer().profile.userName ?: ""
    }

    fun getUserAvatarUrl(): String {
        printlnCK("getUserAvatarUrl: ${listPeerAvatars.value?.get(0)}")
        return listPeerAvatars.value?.get(0) ?: ""
    }

    fun getSelfAvatarUrl(): String {
        return environment.getServer().profile.avatar ?: ""
    }

    fun setSelectedMessage(selectedMessage: MessageDisplayInfo) {
        _selectedMessage = selectedMessage
    }

    fun onScrollChange(lastMessageAt: Long, isRefresh: Boolean = false) {
        if (isRefresh || (isLoading.value == false && !endOfPaginationReached)) {
            viewModelScope.launch {
                isLoading.value = true
                val server = environment.getServer()
                val loadResponse = updateMessageFromApiUseCase(
                    group.value?.groupId ?: 0,
                    Owner(server.serverDomain, server.profile.userId),
                    if (isRefresh) 0 else lastMessageAt
                )
                val endOfPagination =  loadResponse.endOfPaginationReached
                endOfPaginationReached = endOfPagination
                var newestMessageTimestamp = loadResponse.newestMessageLoadedTimestamp
                while (isRefresh && newestMessageTimestamp > lastMessageAt) {
                    val response = updateMessageFromApiUseCase(
                        group.value?.groupId ?: 0,
                        Owner(server.serverDomain, server.profile.userId),
                        newestMessageTimestamp
                    )
                    newestMessageTimestamp = response.newestMessageLoadedTimestamp
                    endOfPaginationReached = response.endOfPaginationReached
                }

                while (!endOfPagination && lastLoadRequestTimestamp != 0L) {
                    //Execute queued load request
                    val temp = lastLoadRequestTimestamp
                    lastLoadRequestTimestamp = 0L
                    endOfPaginationReached = updateMessageFromApiUseCase(
                        group.value?.groupId ?: 0,
                        Owner(server.serverDomain, server.profile.userId),
                        temp
                    ).endOfPaginationReached
                }
                isLoading.value = false
            }
        } else if (!endOfPaginationReached || (lastMessageAt < lastLoadRequestTimestamp || lastLoadRequestTimestamp == 0L)) {
            //Queue up load request
            lastLoadRequestTimestamp = lastMessageAt
        }
    }

    companion object {
        private const val FILE_MAX_COUNT = 10
        private const val FILE_MAX_SIZE = 1_000_000_000 //1GB
    }
}

class RequestInfo(val chatGroup: ChatGroup, val isAudioMode: Boolean)