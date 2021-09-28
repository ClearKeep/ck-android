package com.clearkeep.screen.chat.change_pass_word

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearkeep.R
import com.clearkeep.db.clear_keep.model.Owner
import com.clearkeep.dynamicapi.Environment
import com.clearkeep.screen.chat.repo.ProfileRepository
import com.clearkeep.utilities.network.Resource
import com.clearkeep.utilities.network.Status
import kotlinx.coroutines.launch
import javax.inject.Inject

class ChangePasswordViewModel @Inject constructor(
    private val environment: Environment,
    private val profileRepository: ProfileRepository,
): ViewModel() {
    private val _oldPassword = MutableLiveData<String>()
    private val _oldPasswordError = MutableLiveData<String>()
    val oldPasswordError: LiveData<String> get() = _oldPasswordError

    private val _newPassword = MutableLiveData<String>()
    private val _newPasswordError = MutableLiveData<String>()
    val newPasswordError: LiveData<String> get() = _newPasswordError

    private val _newPasswordConfirm = MutableLiveData<String>()
    private val _newPasswordConfirmError = MutableLiveData<String>()
    val newPasswordConfirmError: LiveData<String> get() = _newPasswordConfirmError

    val changePasswordResponse = MutableLiveData<Resource<String>>()

    fun setOldPassword(password: String) {
        _oldPassword.value = password
        val newPassword = _newPassword.value ?: ""
        _oldPasswordError.value = getOldPasswordError(password)
        _newPasswordError.value = getPasswordError(password, newPassword)
    }

    fun setNewPassword(password: String) {
        val oldPassword = _oldPassword.value ?: ""
        val confirmPassword = _newPasswordConfirm.value ?: ""
        _newPassword.value = password
        _newPasswordConfirmError.value = getConfirmPasswordError(password, confirmPassword)
        _newPasswordError.value = getPasswordError(oldPassword, password)
    }

    fun setNewPasswordConfirm(confirmPassword: String) {
        val password = _newPassword.value ?: ""
        _newPasswordConfirm.value = confirmPassword
        _newPasswordConfirmError.value = getConfirmPasswordError(password, confirmPassword)
    }

    fun changePassword() {
        val oldPassword = _oldPassword.value ?: ""
        val newPassword = _newPassword.value ?: ""
        val server = environment.getServer()
        val owner = Owner(server.serverDomain, server.profile.userId)

        viewModelScope.launch {
            val response = profileRepository.changePassword(owner, oldPassword, newPassword)
            if (response.status == Status.ERROR) {
                _oldPasswordError.value = response.message
            } else {
                changePasswordResponse.value = response
            }
        }
    }

    private fun getOldPasswordError(password: String): String? {
        return when (password.length) {
            !in 6..12 -> {
                "Password must be between 6–12 characters"
            }
            else -> {
                null
            }
        }
    }

    private fun getPasswordError(oldPassword: String, newPassword: String): String? {
        return when {
            newPassword.length !in 6..12 -> {
                "Password must be between 6–12 characters"
            }
            oldPassword.trim() == newPassword.trim() -> {
                "The new password must be different from the previous one!"
            }
            else -> {
                null
            }
        }
    }

    private fun getConfirmPasswordError(password: String, confirmPassword: String): String? {
        return when {
            confirmPassword.length !in 6..12 -> {
                "Password must be between 6–12 characters"
            }
            password.trim() != confirmPassword.trim() -> {
                "New Password and Confirm password do not match. Please try again!"
            }
            else -> {
                null
            }
        }
    }
}