@file:Suppress("DeferredResultUnused")

package com.clearkeep.features.chat.presentation.home

import android.util.Log
import androidx.lifecycle.*
import com.clearkeep.common.utilities.isValidServerUrl
import com.clearkeep.common.utilities.printlnCK
import com.clearkeep.domain.model.*
import com.clearkeep.domain.repository.*
import com.clearkeep.domain.usecase.auth.LogoutUseCase
import com.clearkeep.domain.usecase.group.FetchGroupsUseCase
import com.clearkeep.domain.usecase.group.GetAllPeerGroupByDomainUseCase
import com.clearkeep.domain.usecase.group.GetAllRoomsAsStateUseCase
import com.clearkeep.domain.usecase.message.ClearTempMessageUseCase
import com.clearkeep.domain.usecase.notification.RegisterTokenUseCase
import com.clearkeep.domain.usecase.notification.SetFirebaseTokenUseCase
import com.clearkeep.domain.usecase.people.*
import com.clearkeep.domain.usecase.server.*
import com.clearkeep.domain.usecase.workspace.GetWorkspaceInfoUseCase
import com.clearkeep.features.chat.presentation.utils.SingleLiveEvent
import com.clearkeep.features.chat.presentation.utils.getLinkFromPeople
import com.clearkeep.features.shared.presentation.BaseViewModel
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val environment: Environment,
    private val authRepository: AuthRepository,
    private val serverRepository: ServerRepository,
    private val setFirebaseTokenUseCase: SetFirebaseTokenUseCase,
    private val registerTokenUseCase: RegisterTokenUseCase,
    private val fetchGroupsUseCase: FetchGroupsUseCase,
    private val getAllPeerGroupByDomainUseCase: GetAllPeerGroupByDomainUseCase,
    private val clearTempMessageUseCase: ClearTempMessageUseCase,
    private val getServerByDomainUseCase: GetServerByDomainUseCase,

    private val getWorkspaceInfoUseCase: GetWorkspaceInfoUseCase,

    private val getListClientStatusUseCase: GetListClientStatusUseCase,
    private val updateStatusUseCase: UpdateStatusUseCase,
    private val getUserInfoUseCase: GetUserInfoUseCase,
    private val sendPingUseCase: SendPingUseCase,
    private val updateAvatarUserEntityUseCase: UpdateAvatarUserEntityUseCase,
    private val getListUserEntityUseCase: GetListUserEntityUseCase,
    private val signalKeyRepository: SignalKeyRepository,

    private val getDefaultServerProfileAsStateUseCase: GetDefaultServerProfileAsStateUseCase,
    getIsLogoutUseCase: GetIsLogoutUseCase,
    getAllRoomsAsStateUseCase: GetAllRoomsAsStateUseCase,

    getServersAsStateUseCase: GetServersAsStateUseCase,
    logoutUseCase: LogoutUseCase,
    getActiveServerUseCase: GetActiveServerUseCase,
    var senderKeyStore: SenderKeyStore,

    private val setActiveServerUseCase: SetActiveServerUseCase
) : BaseViewModel(logoutUseCase) {
    val currentServer = getActiveServerUseCase()
    val servers: LiveData<List<Server>> = getServersAsStateUseCase()

    var profile: LiveData<Profile> = getDefaultServerProfileAsStateUseCase()

    val isLogout = getIsLogoutUseCase()

    val selectingJoinServer = MutableLiveData(false)
//    val groupName = MutableLiveData<String>()
    private val _prepareState = MutableLiveData<PrepareViewState>()

    val prepareState: LiveData<PrepareViewState>
        get() = _prepareState

    val groups: LiveData<List<ChatGroup>> = getAllRoomsAsStateUseCase()

    val isRefreshing = MutableLiveData(false)

    private val _currentStatus = SingleLiveEvent<String>()
    val currentStatus: LiveData<String>
        get() = _currentStatus
    private val _listUserStatus = MutableLiveData<List<User>>()
    val listUserInfo: LiveData<List<User>>
        get() = _listUserStatus

    val serverUrlValidateResponse = MutableLiveData<String>()

    private val _isServerUrlValidateLoading = MutableLiveData<Boolean>()
    val isServerUrlValidateLoading: LiveData<Boolean>
        get() = _isServerUrlValidateLoading

    private var checkValidServerJob: Job? = null
    init {
        viewModelScope.launch {
            async { clearTempMessageUseCase.invoke() }.await()
//            val fetchGroupResponse = fetchGroupsUseCase() 
//            handleResponse(fetchGroupResponse)
            async { fetchGroupsUseCase.invoke() }.await()
            async { getStatusUserInDirectGroup() }
        }
        getAllSenderKey()
        sendPing()
    }
    fun getClientIdOfActiveServer() = environment.getServer().profile.userId

    fun getDomainOfActiveServer() = environment.getServer().serverDomain

    private fun getAllSenderKey(){
        viewModelScope.launch {
            launch(Dispatchers.IO){
                //senderKeyStore.getAllSenderKey()
            }
            Log.d("antx: ", "HomeViewModel getAllSenderKey line = 103: ")
        }

    }

    fun onPullToRefresh() {
        viewModelScope.launch {
            val newProfile = authRepository.getProfile(environment.getServer().serverDomain, environment.getServer().accessKey, environment.getServer().hashKey)
            if (newProfile != null) {
                serverRepository.updateServerProfile(environment.getServer().serverDomain, newProfile)
            }
        }
        isRefreshing.postValue(true)
        getAllSenderKey()
        viewModelScope.launch {
            val fetchGroupResponse = fetchGroupsUseCase()
            getStatusUserInDirectGroup()
            handleResponse(fetchGroupResponse)
            isRefreshing.postValue(false)
        }
    }


    val chatGroups = liveData<List<ChatGroup>> {
        val result = MediatorLiveData<List<ChatGroup>>()
        result.addSource(groups) { groupList ->
            val server = environment.getServer()
            result.value = groupList.filter {
                it.ownerDomain == server.serverDomain
                        && it.ownerClientId == server.profile.userId
                        && it.isGroup()
                        && it.clientList.firstOrNull { it.userId == profile.value?.userId }?.userState == com.clearkeep.domain.model.UserStateTypeInGroup.ACTIVE.value
            }
        }

        result.addSource(selectingJoinServer) { _ ->
            val server = environment.getServer()
            result.value = groups.value?.filter {
                it.ownerDomain == server.serverDomain
                        && it.ownerClientId == server.profile.userId
                        && it.isGroup()
                        && it.clientList.firstOrNull { it.userId == server.profile.userId }?.userState == com.clearkeep.domain.model.UserStateTypeInGroup.ACTIVE.value
            }
        }
        emitSource(result)
    }

    val directGroups = liveData<List<ChatGroup>> {
        val result = MediatorLiveData<List<ChatGroup>>()

        result.addSource(groups) { groupList ->
            val server = environment.getServer()
            result.value = groupList.filter {
                it.ownerDomain == server.serverDomain
                        && it.ownerClientId == server.profile.userId
                        && !it.isGroup()
            }
        }
        result.addSource(selectingJoinServer) { _ ->
            val server = environment.getServer()
            //getStatusUserInDirectGroup()
            result.value = groups.value?.filter {
                it.ownerDomain == server.serverDomain
                        && it.ownerClientId == server.profile.userId
                        && !it.isGroup()
            }
        }
        emitSource(result)
    }

    private suspend fun getStatusUserInDirectGroup() {
        try {
            val listUserRequest = arrayListOf<User>()
            getAllPeerGroupByDomainUseCase(
                owner = Owner(
                    getDomainOfActiveServer(),
                    getClientIdOfActiveServer()
                )
            )
                .forEach { group ->
                    if (!group.isGroup()) {
                        val user = group.clientList.firstOrNull { client ->
                            client.userId != getClientIdOfActiveServer()
                        }
                        if (user != null) {
                            listUserRequest.add(user)
                        }
                    }
                }
            val server = environment.getServer()
            listUserRequest.add(
                User(
                    server.profile.userId,
                    server.profile.userName ?: "",
                    server.serverDomain
                )
            )
            listUserRequest.addAll(getListUserEntityUseCase())
            val listClientStatus = getListClientStatusUseCase(listUserRequest)?.filter { !it.userName.isEmpty() }
            _listUserStatus.postValue(listClientStatus)
            val status =
                listClientStatus?.filter { it.userId == server.profile.userId && it.userName.isEmpty() }?.get(0)?.userStatus
            _currentStatus.postValue(status)
            listClientStatus?.forEach {
                currentServer.value?.serverDomain?.let { it1 ->
                    currentServer.value?.ownerClientId?.let { it2 ->
                        com.clearkeep.domain.model.Owner(it1, it2)
                    }
                }?.let { it2 -> updateAvatarUserEntityUseCase(it, owner = it2) }
            }
            delay(300 * 1000)
            getStatusUserInDirectGroup()

        } catch (e: Exception) {
            printlnCK("getStatusUserInDirectGroup error: ${e.message}")
        }
    }

    private fun sendPing() {
        viewModelScope.launch {
            delay(60 * 1000)
            sendPingUseCase()
            sendPing()
        }
    }

    fun setUserStatus(status: com.clearkeep.domain.model.UserStatus) {
        viewModelScope.launch {
            val result = updateStatusUseCase(status.value)
            if (result) _currentStatus.postValue(status.value)
        }
    }

    fun prepare() {
        viewModelScope.launch {
            _prepareState.value = PrepareProcessing
            updateFirebaseToken()
            _prepareState.value = PrepareSuccess
        }
    }

    fun selectChannel(server: Server) {
        viewModelScope.launch {
            setActiveServerUseCase(server)
            selectingJoinServer.value = false
        }
    }

    fun showJoinServer() {
        selectingJoinServer.value = true
    }

    private val _isLogOutProcessing = MutableLiveData<Boolean>()

    val isLogOutProcessing: LiveData<Boolean>
        get() = _isLogOutProcessing

    private fun updateFirebaseToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                printlnCK("Fetching FCM registration token failed, ${task.exception}")
            }

            // Get new FCM registration token
            val token = task.result
            if (!token.isNullOrEmpty()) {
                viewModelScope.launch(Dispatchers.IO) {
                    setFirebaseTokenUseCase(token)
                    pushFireBaseTokenToServer()
                }
            }
        }
    }

    private suspend fun pushFireBaseTokenToServer() = withContext(Dispatchers.IO) {
        registerTokenUseCase()
    }

    fun getProfileLink(): String {
        val server = environment.getServer()
        return getLinkFromPeople(
            User(
                userId = server.profile.userId,
                userName = server.profile.userName ?: "",
                domain = server.serverDomain
            )
        )
    }

    fun checkValidServerUrl(url: String) {
        _isServerUrlValidateLoading.value = true
        checkValidServerJob?.cancel()
        checkValidServerJob = viewModelScope.launch {
            if (!isValidServerUrl(url)) {
                printlnCK("checkValidServerUrl local validation invalid")
                serverUrlValidateResponse.value = ""
                _isServerUrlValidateLoading.value = false
                return@launch
            }

            if (getServerByDomainUseCase(url) != null) {
                printlnCK("checkValidServerUrl duplicate server")
                serverUrlValidateResponse.value = ""
                _isServerUrlValidateLoading.value = false
                return@launch
            }

            val server = environment.getServer()
            val workspaceInfoResponse =
                getWorkspaceInfoUseCase(server.serverDomain, url)
            _isServerUrlValidateLoading.value = false
            if (workspaceInfoResponse.status == com.clearkeep.common.utilities.network.Status.ERROR) {
                printlnCK("checkValidServerUrl invalid server from remote")
                serverUrlValidateResponse.value = ""
                return@launch
            }

            serverUrlValidateResponse.value = url
        }
    }

    fun cancelCheckValidServer() {
        _isServerUrlValidateLoading.value = false
        checkValidServerJob?.cancel()
    }
}

sealed class PrepareViewState
object PrepareSuccess : PrepareViewState()
object PrepareError : PrepareViewState()
object PrepareProcessing : PrepareViewState()
