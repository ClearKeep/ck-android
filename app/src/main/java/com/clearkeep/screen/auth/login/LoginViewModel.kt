package com.clearkeep.screen.auth.login

import androidx.lifecycle.*
import auth.AuthOuterClass
import com.clearkeep.screen.auth.AuthRepository
import com.clearkeep.utilities.network.Resource
import com.clearkeep.utilities.network.Status
import javax.inject.Inject

class LoginViewModel @Inject constructor(
    private val loginRepository: AuthRepository
): ViewModel() {
    private val _isLoading = MutableLiveData<Boolean>()

    val isLoading: LiveData<Boolean>
        get() = _isLoading

    suspend fun login(username: String, password: String): Resource<AuthOuterClass.AuthRes> {
        _isLoading.value = true
        val res = loginRepository.login(username, password)
        _isLoading.value = false
        return res
    }
}