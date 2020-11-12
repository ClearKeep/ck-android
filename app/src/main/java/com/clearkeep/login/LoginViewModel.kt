package com.clearkeep.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class LoginViewModel @Inject constructor(
    private val loginRepository: LoginRepository
): ViewModel() {
    private val _loginState = MutableLiveData<LoginViewState>()

    val loginState: LiveData<LoginViewState>
        get() = _loginState

    fun register(username: String) {
        viewModelScope.launch {
            _loginState.value = LoginProcessing
            if (loginRepository.register(username)) {
                _loginState.value = LoginSuccess
            } else {
                _loginState.value = LoginError
            }
        }
    }
}
