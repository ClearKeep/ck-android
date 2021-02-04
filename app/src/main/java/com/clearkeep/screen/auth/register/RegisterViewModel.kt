package com.clearkeep.screen.auth.register

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import auth.AuthOuterClass
import com.clearkeep.R
import com.clearkeep.screen.repo.AuthRepository
import com.clearkeep.utilities.isValidEmail
import com.clearkeep.utilities.network.Resource
import javax.inject.Inject

class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
): ViewModel() {
    private val _isLoading = MutableLiveData<Boolean>()

    val isLoading: LiveData<Boolean>
        get() = _isLoading

    suspend fun register(context: Context, username: String, password: String, email: String): Resource<AuthOuterClass.RegisterRes> {
        _isLoading.value = true

        val result = if (!email.trim().isValidEmail()) {
            Resource.error(context.getString(R.string.email_invalid), null)
        } else {
            authRepository.register(username.trim(), password.trim(), email.trim())
        }
        _isLoading.value = false
        return result
    }
}