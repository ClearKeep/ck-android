package com.clearkeep.screen.chat.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearkeep.dynamicapi.DynamicAPIProvider
import com.clearkeep.repo.*
import com.clearkeep.utilities.UserManager
import com.clearkeep.utilities.printlnCK
import kotlinx.coroutines.*
import javax.inject.Inject

class MainPreparingViewModel @Inject constructor(
    private val signalKeyRepository: SignalKeyRepository,
    private val profileRepository: ProfileRepository,
    // network calls
    private val dynamicAPIProvider: DynamicAPIProvider,
    private val userManager: UserManager
): ViewModel() {
    private val _prepareState = MutableLiveData<PrepareViewState>()

    val prepareState: LiveData<PrepareViewState>
        get() = _prepareState

    fun prepareChat() {
        viewModelScope.launch {
            dynamicAPIProvider.setUpDomain(userManager.getWorkspaceDomain())

            _prepareState.value = PrepareProcessing
            val profile = profileRepository.getProfile()
            if (profile == null) {
                _prepareState.value = PrepareError
                return@launch
            }

            printlnCK("client id = ${profile.id}")
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
