package com.clearkeep.screen.auth.forgot

import androidx.lifecycle.*
import auth.AuthOuterClass
import com.clearkeep.screen.auth.repo.AuthRepository
import com.clearkeep.utilities.network.Resource
import javax.inject.Inject

class ForgotViewModel @Inject constructor(
    private val loginRepository: AuthRepository
): ViewModel() {
    private val _isLoading = MutableLiveData<Boolean>()

    val isLoading: LiveData<Boolean>
        get() = _isLoading

    suspend fun recoverPassword(email: String): Resource<AuthOuterClass.BaseResponse> {
        _isLoading.value = true
        val res = loginRepository.recoverPassword(email.trim(), "")
        _isLoading.value = false
        return res
    }
}