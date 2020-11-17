package com.clearkeep.screen.auth.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearkeep.screen.auth.AuthRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

class LoginViewModel @Inject constructor(
    private val loginRepository: AuthRepository
): ViewModel() {
    private val _loginState = MutableLiveData<LoginViewState>()

    val loginState: LiveData<LoginViewState>
        get() = _loginState

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginProcessing
            if (loginRepository.login(username, password)) {
                _loginState.value = LoginSuccess
            } else {
                _loginState.value = LoginError
            }
        }
    }
}

sealed class LoginViewState
object LoginSuccess : LoginViewState()
object LoginError : LoginViewState()
object LoginProcessing : LoginViewState()