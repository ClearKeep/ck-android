package com.clearkeep.utilities

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearkeep.domain.model.Server
import com.clearkeep.domain.usecase.auth.LogoutUseCase
import com.clearkeep.domain.usecase.group.DeleteGroupUseCase
import com.clearkeep.domain.usecase.message.DeleteMessageUseCase
import com.clearkeep.domain.usecase.server.*
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
}