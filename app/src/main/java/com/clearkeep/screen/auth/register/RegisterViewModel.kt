package com.clearkeep.screen.auth.register

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import auth.AuthOuterClass
import com.clearkeep.screen.auth.AuthRepository
import com.clearkeep.utilities.network.Resource
import kotlinx.coroutines.launch
import javax.inject.Inject

class RegisterViewModel @Inject constructor(
    private val loginRepository: AuthRepository
): ViewModel() {
    private val _isLoading = MutableLiveData<Boolean>()

    val isLoading: LiveData<Boolean>
        get() = _isLoading

    suspend fun register(username: String, password: String, email: String): Resource<AuthOuterClass.RegisterRes> {
        _isLoading.value = true
        val res = loginRepository.register(username, password, email)
        _isLoading.value = false
        return res
    }
}