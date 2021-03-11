package com.clearkeep.screen.chat.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearkeep.db.ClearKeepDatabase
import com.clearkeep.db.SignalKeyDatabase
import com.clearkeep.screen.chat.signal_store.InMemorySignalProtocolStore
import com.clearkeep.screen.repo.*
import com.clearkeep.utilities.FIREBASE_TOKEN
import com.clearkeep.utilities.printlnCK
import com.clearkeep.utilities.storage.UserPreferencesStorage
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.*
import javax.inject.Inject

class HomeViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    private val groupRepository: GroupRepository,
    private val messageRepository: MessageRepository,

    private val signalProtocolStore: InMemorySignalProtocolStore,

    private val storage: UserPreferencesStorage,
    private val clearKeepDatabase: ClearKeepDatabase,
    private val signalKeyDatabase: SignalKeyDatabase,
): ViewModel() {
    private var isSubscribed = false

    private val _isLogOutProcessing = MutableLiveData<Boolean>()

    val isLogOutProcessing: LiveData<Boolean>
        get() = _isLogOutProcessing

    private val _isLogOutCompleted = MutableLiveData(false)

    val isLogOutCompleted: LiveData<Boolean>
        get() = _isLogOutCompleted

    fun networkAvailable() {
        if (chatRepository.isNeedSubscribeAgain()) {
            viewModelScope.launch {
                chatRepository.reInitSubscribe()
                updateNewMessages()
            }
        }
    }

    fun appOnForeground() {
        viewModelScope.launch {
            if (!isSubscribed) {
                isSubscribed = true
                chatRepository.subscribe()
                return@launch
            }

            if (chatRepository.isNeedSubscribeAgain()) {
                chatRepository.reInitSubscribe()
                updateNewMessages()
            }
        }
    }

    fun appOnBackground() {
    }

    private suspend fun updateNewMessages() {
        groupRepository.fetchRoomsFromAPI()
        val roomId = chatRepository.getJoiningRoomId()
        if (roomId > 0) {
            val group = groupRepository.getGroupByID(roomId)!!
            messageRepository.updateMessageFromAPI(group.id, group.lastMessageSyncTimestamp)
        }
    }
    fun updateFirebaseToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                printlnCK("Fetching FCM registration token failed, ${task.exception}")
            }

            // Get new FCM registration token
            val token = task.result
            if (!token.isNullOrEmpty()) {
                viewModelScope.launch(Dispatchers.IO) {
                    storage.setString(FIREBASE_TOKEN, token)
                    pushFireBaseTokenToServer()
                }
            }
        }
    }

    fun logOut() {
        viewModelScope.launch {
            _isLogOutProcessing.value = true

            authRepository.logoutFromAPI()
            clearDatabase()
            _isLogOutCompleted.value = true
        }
    }

    private suspend fun clearDatabase() = withContext(Dispatchers.IO) {
        storage.clear()
        signalProtocolStore.clear()
        clearKeepDatabase.clearAllTables()
        signalKeyDatabase.clearAllTables()
    }

    private suspend fun pushFireBaseTokenToServer() = withContext(Dispatchers.IO) {
        val token = storage.getString(FIREBASE_TOKEN)
        if (!token.isNullOrEmpty()) {
            printlnCK("push token  = $token ")
            profileRepository.registerToken(token)
        }
    }
}
