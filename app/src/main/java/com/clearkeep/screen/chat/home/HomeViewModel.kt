package com.clearkeep.screen.chat.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearkeep.screen.repo.ProfileRepository
import com.clearkeep.screen.repo.ChatRepository
import com.clearkeep.screen.repo.SignalKeyRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

class HomeViewModel @Inject constructor(
        private val signalKeyRepository: SignalKeyRepository,
        private val profileRepository: ProfileRepository,
        private val chatRepository: ChatRepository,
): ViewModel() {
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
        }
    }
}

sealed class PrepareViewState
object PrepareSuccess : PrepareViewState()
object PrepareError : PrepareViewState()
object PrepareProcessing : PrepareViewState()
