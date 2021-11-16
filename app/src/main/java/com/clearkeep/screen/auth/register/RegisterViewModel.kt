package com.clearkeep.screen.auth.register

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import auth.AuthOuterClass
import com.clearkeep.R
import com.clearkeep.screen.auth.repo.AuthRepository
import com.clearkeep.utilities.isValidEmail
import com.clearkeep.utilities.network.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _isLoading = MutableLiveData<Boolean>()

    val isLoading: LiveData<Boolean>
        get() = _isLoading

    private val _email = MutableLiveData<String>()
    val email: LiveData<String> get() = _email

    private val _emailError = MutableLiveData<String>()
    val emailError: LiveData<String>
        get() = _emailError

    private val _passError = MutableLiveData<String>()
    val passError: LiveData<String>
        get() = _passError

    private val _confirmPassError = MutableLiveData<String>()
    val confirmPassError: LiveData<String>
        get() = _confirmPassError

    private val _displayNameError = MutableLiveData<String>()
    val displayNameError: LiveData<String>
        get() = _displayNameError

    var domain: String = ""

    suspend fun register(
        context: Context,
        email: String,
        displayName: String,
        password: String,
        confirmPassword: String
    ): Resource<AuthOuterClass.RegisterSRPRes>? {
        _isLoading.value = true

        _emailError.value = ""
        _passError.value = ""
        _confirmPassError.value = ""
        _displayNameError.value = ""

        var isValid = true
        if (email.isBlank()) {
            _emailError.value = context.getString(R.string.email_empty)
            isValid = false
        } else if (!email.trim().isValidEmail()) {
            _emailError.value = context.getString(R.string.email_invalid)
            isValid = false
        }
        if (displayName.isBlank()) {
            _displayNameError.value = context.getString(R.string.display_empty)
            isValid = false
        }
        if (password.isBlank()) {
            _passError.value = context.getString(R.string.password_empty)
            isValid = false
        } else if (password.length !in 6..12) {
            _passError.value = context.getString(R.string.password_length_invalid)
            isValid = false
        }
        if (confirmPassword.isBlank()) {
            _confirmPassError.value = context.getString(R.string.confirm_password_empty)
            isValid = false
        } else if (confirmPassword != password) {
            _confirmPassError.value = context.getString(R.string.confirm_password_is_not_match)
            isValid = false
        }

        _email.value = email

        val result = if (isValid) {
            authRepository.register(displayName.trim(), password, email.trim(), domain)
        } else {
            null
        }

        _isLoading.value = false
        return result
    }
}