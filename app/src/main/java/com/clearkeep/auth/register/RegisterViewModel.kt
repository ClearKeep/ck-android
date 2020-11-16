package com.clearkeep.auth.register

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearkeep.auth.AuthRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

class RegisterViewModel @Inject constructor(
    private val loginRepository: AuthRepository
): ViewModel() {
    private val _registerState = MutableLiveData<RegisterViewState>()

    val registerState: LiveData<RegisterViewState>
        get() = _registerState

    fun register(username: String, password: String, email: String) {
        viewModelScope.launch {
            _registerState.value = RegisterProcessing
            if (loginRepository.register(username, password, email)) {
                _registerState.value = RegisterSuccess
            } else {
                _registerState.value = RegisterError
            }
        }
    }
}

sealed class RegisterViewState
object RegisterSuccess : RegisterViewState()
object RegisterError : RegisterViewState()
object RegisterProcessing : RegisterViewState()