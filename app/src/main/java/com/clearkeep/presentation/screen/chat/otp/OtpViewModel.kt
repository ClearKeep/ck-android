package com.clearkeep.presentation.screen.chat.otp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearkeep.data.remote.dynamicapi.Environment
import com.clearkeep.domain.usecase.profile.MfaResendOtpUseCase
import com.clearkeep.domain.usecase.profile.MfaValidateOtpUseCase
import com.clearkeep.domain.usecase.profile.MfaValidatePasswordUseCase
import com.clearkeep.common.utilities.network.Resource
import com.clearkeep.common.utilities.network.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OtpViewModel @Inject constructor(
    private val environment: Environment,
    private val mfaValidatePasswordUseCase: MfaValidatePasswordUseCase,
    private val mfaValidateOtpUseCase: MfaValidateOtpUseCase,
    private val mfaResendOtpUseCase: MfaResendOtpUseCase
) : ViewModel() {

    val verifyPasswordResponse = MutableLiveData<com.clearkeep.common.utilities.network.Resource<Pair<String, String>>?>()
    val verifyOtpResponse = MutableLiveData<com.clearkeep.common.utilities.network.Resource<String>>()

    private val _isAccountLocked = MutableLiveData<Boolean>()
    val isAccountLocked: LiveData<Boolean> get() = _isAccountLocked

    fun verifyPassword(password: String) {
        if (password.isBlank()) {
            verifyPasswordResponse.value =
                com.clearkeep.common.utilities.network.Resource.error("Current Password must not be blank", null)
            return
        } else if (password.length !in 6..12) {
            verifyPasswordResponse.value =
                com.clearkeep.common.utilities.network.Resource.error("Password must be between 6-12 characters", null)
            return
        }

        viewModelScope.launch {
            val response = mfaValidatePasswordUseCase(getOwner(), password)
            verifyPasswordResponse.value = response
        }
    }

    fun verifyOtp(otp: String) {
        if (otp.isBlank() || otp.length != 4) {
            verifyOtpResponse.value = com.clearkeep.common.utilities.network.Resource.error("Authentication failed. Please retry.", null)
            return
        }

        viewModelScope.launch {
            val response = mfaValidateOtpUseCase(getOwner(), otp)
            if (response.status == com.clearkeep.common.utilities.network.Status.ERROR) {
                verifyOtpResponse.value = com.clearkeep.common.utilities.network.Resource.error(
                    response.message ?: "The code you’ve entered is incorrect. Please try again",
                    null
                )
            } else {
                verifyOtpResponse.value = response
            }
        }
    }

    fun requestResendOtp() {
        viewModelScope.launch {
            val response = mfaResendOtpUseCase(getOwner())
            val errorCode = response.data?.first
            if (response.status == com.clearkeep.common.utilities.network.Status.ERROR) {
                verifyOtpResponse.value = com.clearkeep.common.utilities.network.Resource.error(
                    response.data?.second
                        ?: "The code you’ve entered is incorrect. Please try again", null
                )

                if (errorCode == 1069) {
                    _isAccountLocked.value = true
                }
            }
        }
    }

    fun resetAccountLock() {
        _isAccountLocked.value = false
    }

    private fun getOwner(): com.clearkeep.domain.model.Owner {
        val server = environment.getServer()
        return com.clearkeep.domain.model.Owner(server.serverDomain, server.profile.userId)
    }
}