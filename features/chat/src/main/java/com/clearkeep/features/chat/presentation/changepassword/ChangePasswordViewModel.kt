package com.clearkeep.features.chat.presentation.changepassword

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearkeep.common.utilities.DecryptsPBKDF2
import com.clearkeep.common.utilities.SENDER_DEVICE_ID
import com.clearkeep.common.utilities.network.Resource
import com.clearkeep.common.utilities.network.Status
import com.clearkeep.domain.model.CKSignalProtocolAddress
import com.clearkeep.domain.model.Owner
import com.clearkeep.domain.model.response.AuthRes
import com.clearkeep.domain.repository.Environment
import com.clearkeep.domain.usecase.auth.LoginUseCase
import com.clearkeep.domain.usecase.group.GetAllGroupsByDomainUseCase
import com.clearkeep.domain.usecase.profile.ChangePasswordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.security.interfaces.ECPrivateKey
import javax.inject.Inject

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val environment: Environment,
    private val changePasswordUseCase: ChangePasswordUseCase,
    private val loginUseCase: LoginUseCase,
    private val getGroupsByDomainUseCase: GetAllGroupsByDomainUseCase
) : ViewModel() {
    private val _oldPassword = MutableLiveData<String>()
    private val _oldPasswordError = MutableLiveData<String>()
    val oldPasswordError: LiveData<String> get() = _oldPasswordError

    private val _newPassword = MutableLiveData<String>()
    private val _newPasswordError = MutableLiveData<String>()
    val newPasswordError: LiveData<String> get() = _newPasswordError

    private val _newPasswordConfirm = MutableLiveData<String>()
    private val _newPasswordConfirmError = MutableLiveData<String>()
    val newPasswordConfirmError: LiveData<String> get() = _newPasswordConfirmError

    val changePasswordResponse = MutableLiveData<Resource<Any>>()

    private val _isResetPassword = MutableLiveData<Boolean>()
    val isResetPassword: LiveData<Boolean> get() = _isResetPassword

    private var preAccessToken = ""
    private var userId = ""
    private var serverDomain = ""

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

    fun onClickConfirm() {
        if (isResetPassword.value == true) {
            resetPassword()
        } else {
            changePassword()
        }
    }

    fun processDeepLinkUri(uri: Uri?) {
        var isResetPassword = true
        if (uri == null || uri.toString().isBlank()) {
            isResetPassword = false
        }
        _isResetPassword.value = isResetPassword

        if (isResetPassword) {
            val stringUri = uri.toString()
            val dataString = stringUri.replace(DEEP_LINK_URI_PREFIX, "")
            val data: Map<String, String> = dataString.split("&").map {
                val keyValuePair = it.split("=")
                val key = keyValuePair[0]
                val value = keyValuePair[1]
                key to value
            }.toMap()

            preAccessToken = data["pre_access_token"] ?: ""
            userId = data["user_name"] ?: ""
            serverDomain = data["server_domain"] ?: ""
        }
    }

    private fun changePassword() {
        val oldPassword = _oldPassword.value ?: ""
        val newPassword = _newPassword.value ?: ""
        val server = environment.getServer()
        val owner = Owner(server.serverDomain, server.profile.userId)

        viewModelScope.launch {
            val response = changePasswordUseCase(
                owner,
                server.profile.email ?: "",
                oldPassword,
                newPassword
            )
            if (response.status == Status.ERROR) {
                _oldPasswordError.value = response.message
            } else {
                updateSenderKeyGroup()
                changePasswordResponse.value = response
            }
        }
    }

    private fun resetPassword() {
        val newPassword = _newPassword.value ?: ""

        viewModelScope.launch {
            changePasswordResponse.value =
                loginUseCase.resetPassword(preAccessToken, userId, serverDomain, newPassword)
        }
    }

    private fun getOldPasswordError(password: String): String? {
        return when {
            password.isEmpty() -> {
                null
            }
            password.length !in 6..12 && password.length > 1 -> {
                "Password must be between 6–12 characters"
            }
            else -> {
                null
            }
        }
    }

    private fun getPasswordError(oldPassword: String, newPassword: String): String? {
        return when {
            newPassword.isEmpty() -> {
                null
            }
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
            confirmPassword.isEmpty() -> {
                null
            }
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

    private fun updateSenderKeyGroup(){
        viewModelScope.launch {
            val server = environment.getServer()
            val listGroup = getGroupsByDomainUseCase.invoke(ownerDomain = server.serverDomain, ownerClientId = server.ownerClientId)
            val listSenderKeyNeedUpdate= arrayListOf<Pair<Long,ByteArray>>()
            listGroup.forEach {
                val senderAddress = CKSignalProtocolAddress(
                    Owner(
                        server.serverDomain,
                        server.ownerClientId
                    ), it.groupId, SENDER_DEVICE_ID
                )
                val senderKey = changePasswordUseCase.loadSenderKey(senderAddress,it.groupId)

                senderKey?.let {
                    listSenderKeyNeedUpdate.add(senderKey)
                }
            }
           changePasswordUseCase.updateKey(server,listSenderKeyNeedUpdate)
        }

    }

    companion object {
        const val DEEP_LINK_URI_PREFIX = "clearkeep://?"
    }
}