package com.clearkeep.screen.auth.forgot

import androidx.lifecycle.*
import auth.AuthOuterClass
import com.clearkeep.screen.auth.AuthRepository
import com.clearkeep.utilities.network.Resource
import com.clearkeep.utilities.network.Status
import javax.inject.Inject

class ForgotViewModel @Inject constructor(
    private val loginRepository: AuthRepository
): ViewModel() {
    private val _isLoading = MutableLiveData<Boolean>()

    val isLoading: LiveData<Boolean>
        get() = _isLoading

    suspend fun recoverPassword(email: String): Resource<AuthOuterClass.BaseResponse> {
        _isLoading.value = true
        val res = loginRepository.recoverPassword(email)
        _isLoading.value = false
        return res
    }
}