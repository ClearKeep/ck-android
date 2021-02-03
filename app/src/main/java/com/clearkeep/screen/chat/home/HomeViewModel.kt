package com.clearkeep.screen.chat.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearkeep.db.ClearKeepDatabase
import com.clearkeep.db.SignalKeyDatabase
import com.clearkeep.screen.repo.ProfileRepository
import com.clearkeep.screen.repo.ChatRepository
import com.clearkeep.screen.repo.SignalKeyRepository
import com.clearkeep.utilities.FIREBASE_TOKEN
import com.clearkeep.utilities.storage.Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class HomeViewModel @Inject constructor(
        private val signalKeyRepository: SignalKeyRepository,
        private val profileRepository: ProfileRepository,
        private val chatRepository: ChatRepository,
        private val storage: Storage,
        private val clearKeepDatabase: ClearKeepDatabase,
        private val signalKeyDatabase: SignalKeyDatabase
): ViewModel() {
    private val _isLogOutProcessing = MutableLiveData<Boolean>()

    val isLogOutProcessing: LiveData<Boolean>
        get() = _isLogOutProcessing

    private val _loginState = MutableLiveData<PrepareViewState>()

    val loginState: LiveData<PrepareViewState>
        get() = _loginState

    fun prepareChat() {
        viewModelScope.launch {
            _loginState.value = PrepareProcessing
            val profile = profileRepository.getProfile()
            if (profile == null) {
                _loginState.value = PrepareError
                return@launch
            }

            val isRegisterKeySuccess = if (signalKeyRepository.isPeerKeyRegistered()) {
                true
            } else {
                signalKeyRepository.peerRegisterClientKey(profile.id)
            }
            if (!isRegisterKeySuccess) {
                _loginState.value = PrepareError
                return@launch
            }

            chatRepository.initSubscriber()
            _loginState.value = PrepareSuccess

            pushFireBaseTokenToServer()
        }
    }

    fun logOut() {
        viewModelScope.launch {
            _isLogOutProcessing.value = true

            storage.clear()
            clearKeepDatabase.clearAllTables()
            signalKeyDatabase.clearAllTables()

            _isLogOutProcessing.value = false
        }
    }

    private suspend fun pushFireBaseTokenToServer() = withContext(Dispatchers.IO) {
        val token = storage.getString(FIREBASE_TOKEN)
        if (!token.isNullOrEmpty()) {
            profileRepository.registerToken(token)
        }
    }
}

sealed class PrepareViewState
object PrepareSuccess : PrepareViewState()
object PrepareError : PrepareViewState()
object PrepareProcessing : PrepareViewState()
