package com.clearkeep.features.chat.presentation.otp

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearkeep.common.utilities.network.Resource
import com.clearkeep.common.utilities.network.Status
import com.clearkeep.common.utilities.printlnCK
import com.clearkeep.domain.repository.Environment
import com.clearkeep.domain.usecase.profile.MfaResendOtpUseCase
import com.clearkeep.domain.usecase.profile.MfaValidateOtpUseCase
import com.clearkeep.domain.usecase.profile.MfaValidatePasswordUseCase
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

    val verifyPasswordResponse = MutableLiveData<Resource<Pair<String, String>>?>()
    val verifyOtpResponse = MutableLiveData<Resource<String>>()

    private val _isAccountLocked = MutableLiveData<Boolean>()
    val isAccountLocked: LiveData<Boolean> get() = _isAccountLocked

    fun verifyPassword(password: String) {
        printlnCK("OtpViewModel verifyPassword line = 33: " );
        if (password.isBlank()) {
            verifyPasswordResponse.value =
                Resource.error("Current Password must not be blank", null)
            return
        } else if (password.length !in 6..12) {
            verifyPasswordResponse.value =
                Resource.error("Password must be between 6-12 characters", null)
            return
        }

        viewModelScope.launch {
            printlnCK("OtpViewModel verifyPassword line = 45: " );
            val response = mfaValidatePasswordUseCase(getOwner(), password)
            verifyPasswordResponse.value = response
        }
    }

    fun verifyOtp(otp: String) {
        if (otp.isBlank()) {
            verifyOtpResponse.value = Resource.error("Authentication failed. Please retry.", null)
            return
        }

        viewModelScope.launch {
            printlnCK("OtpViewModel verifyOtp line = 60: " );
            val response = mfaValidateOtpUseCase(getOwner(), otp)
            if (response.status == Status.ERROR) {
                verifyOtpResponse.value = Resource.error(
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
            printlnCK("OtpViewModel requestResendOtp line = 69: " );
            val response = mfaResendOtpUseCase(getOwner())
            val errorCode = response.data?.first
            if (response.status == Status.ERROR) {
                verifyOtpResponse.value = Resource.error(
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