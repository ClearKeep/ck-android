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

    private val _emailError = MutableLiveData<String>()

    val emailError: LiveData<String>
        get() = _emailError

    private val _passError = MutableLiveData<String>()

    val passError: LiveData<String>
        get() = _passError

    private val _displayNameError = MutableLiveData<String>()

    val displayNameError: LiveData<String>
        get() = _displayNameError

    suspend fun register(context: Context, displayName: String, password: String, email: String): Resource<AuthOuterClass.RegisterRes>? {
        _emailError.value = ""
        _passError.value = ""
        _displayNameError.value = ""
        _isLoading.value = true

        val result = if (email.isNullOrEmpty()) {
            _emailError.value = context.getString(R.string.email_empty)
            null
        } else if (!email.trim().isValidEmail()) {
            _emailError.value = context.getString(R.string.email_invalid)
            null
        } else if (displayName.isNullOrEmpty()) {
            _displayNameError.value = context.getString(R.string.display_empty)
            null
        } else if (password.isNullOrEmpty()) {
            _passError.value = context.getString(R.string.password_empty)
            null
        } else {
            authRepository.register(displayName.trim(), password.trim(), email.trim())
        }

        _isLoading.value = false
        return result
    }
}