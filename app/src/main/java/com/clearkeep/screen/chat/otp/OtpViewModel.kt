package com.clearkeep.screen.chat.otp

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearkeep.db.clear_keep.model.Owner
import com.clearkeep.dynamicapi.Environment
import com.clearkeep.screen.chat.repo.ProfileRepository
import com.clearkeep.utilities.network.Resource
import com.clearkeep.utilities.network.Status
import kotlinx.coroutines.launch
import javax.inject.Inject

class OtpViewModel @Inject constructor(
    private val environment: Environment,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    val verifyPasswordResponse = MutableLiveData<Resource<String>>()

    fun verifyPassword(password: String) {
        if (password.isBlank()) {
            verifyPasswordResponse.value = Resource.error("Current Password must not be blank", null)
            return
        } else if (password.length !in 6..12) {
            verifyPasswordResponse.value = Resource.error("Password must be between 6-12 characters", null)
            return
        }

        viewModelScope.launch {
            val response = profileRepository.mfaValidatePassword(getOwner(), password)
            if (response.status == Status.ERROR) {
                verifyPasswordResponse.value = Resource.error("The password is incorrect. Try again", null)
            } else {
                verifyPasswordResponse.value = response
            }
        }
    }

    private fun getOwner() : Owner {
        val server = environment.getServer()
        return Owner(server.serverDomain, server.profile.userId)
    }
}