package com.clearkeep.screen.chat.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearkeep.repo.*
import kotlinx.coroutines.*
import javax.inject.Inject

class MainPreparingViewModel @Inject constructor(
    private val signalKeyRepository: SignalKeyRepository,
    private val profileRepository: ProfileRepository,
): ViewModel() {
    private val _prepareState = MutableLiveData<PrepareViewState>()

    val prepareState: LiveData<PrepareViewState>
        get() = _prepareState

    fun prepareChat() {
        viewModelScope.launch {
            _prepareState.value = PrepareProcessing
            val profile = profileRepository.getProfile()
            if (profile == null) {
                _prepareState.value = PrepareError
                return@launch
            }

            val isRegisterKeySuccess = if (signalKeyRepository.isPeerKeyRegistered()) {
                true
            } else {
                signalKeyRepository.peerRegisterClientKey(profile.id)
            }
            if (!isRegisterKeySuccess) {
                _prepareState.value = PrepareError
                return@launch
            }

            _prepareState.value = PrepareSuccess
        }
    }
}

sealed class PrepareViewState
object PrepareSuccess : PrepareViewState()
object PrepareError : PrepareViewState()
object PrepareProcessing : PrepareViewState()
