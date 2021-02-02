package com.clearkeep.screen.auth.register

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import auth.AuthOuterClass
import com.clearkeep.screen.auth.AuthRepository
import com.clearkeep.utilities.network.Resource
import javax.inject.Inject

class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
): ViewModel() {
    private val _isLoading = MutableLiveData<Boolean>()

    val isLoading: LiveData<Boolean>
        get() = _isLoading

    suspend fun register(username: String, password: String, email: String): Resource<AuthOuterClass.RegisterRes> {
        _isLoading.value = true
        val res = authRepository.register(username, password, email)
        _isLoading.value = false
        return res
    }
}