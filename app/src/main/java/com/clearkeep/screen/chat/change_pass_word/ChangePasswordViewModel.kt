package com.clearkeep.screen.chat.change_pass_word

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearkeep.db.clear_keep.model.Owner
import com.clearkeep.dynamicapi.Environment
import com.clearkeep.screen.chat.repo.ProfileRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

class ChangePasswordViewModel @Inject constructor(
    private val environment: Environment,
    private val profileRepository: ProfileRepository,
): ViewModel() {
    private val _oldPassword = MutableLiveData<String>()
    val oldPassword : LiveData<String> get() = _oldPassword

    private val _newPassword = MutableLiveData<String>()
    val newPassword : LiveData<String> get() = _newPassword

    private val _newPasswordConfirm = MutableLiveData<String>()
    val newPasswordConfirm : LiveData<String> get() = _newPasswordConfirm

    fun setOldPassword(password: String) {
        _oldPassword.value = password
    }

    fun setNewPasswordConfirm(password: String) {
        _newPasswordConfirm.value = password
    }

    fun setNewPassword(password: String) {
        _newPassword.value = password
    }

    fun changePassword() {
        val oldPassword = oldPassword.value ?: ""
        val newPassword = newPassword.value ?: ""
        val server = environment.getServer()
        val owner = Owner(server.serverDomain, server.profile.userId)

        viewModelScope.launch {
            profileRepository.changePassword(owner, oldPassword, newPassword)
        }
    }
}