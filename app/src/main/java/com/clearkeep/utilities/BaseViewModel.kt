package com.clearkeep.utilities

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearkeep.data.remote.utils.TokenExpiredException
import com.clearkeep.domain.usecase.auth.LogoutUseCase
import com.clearkeep.utilities.network.Resource
import com.clearkeep.utilities.network.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
open class BaseViewModel @Inject constructor(
    protected val logoutUseCase: LogoutUseCase,
) : ViewModel() {
    private val _shouldReLogin = MutableLiveData(false)
    val shouldReLogin: LiveData<Boolean>
        get() = _shouldReLogin

    fun signOut() {
        viewModelScope.launch {
            _shouldReLogin.value = logoutUseCase()
        }
    }

    fun handleResponse(response: Resource<Any>?) {
        if (response?.status == Status.ERROR && response.error is TokenExpiredException) {
            signOut()
        }
    }
}