package com.clearkeep.features.auth.presentation.register

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.clearkeep.common.utilities.isValidEmail
import com.clearkeep.common.utilities.isValidPassword
import com.clearkeep.domain.usecase.auth.RegisterUseCase
import com.clearkeep.common.utilities.network.Resource
import com.clearkeep.features.auth.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import java.util.regex.Pattern


@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val registerUseCase: RegisterUseCase,
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
    private val regexUppercase = "^[A-Z0-9]" //alpha uppercase


    suspend fun register(
        context: Context,
        email: String,
        displayName: String,
        password: String,
        confirmPassword: String
    ): Resource<Any>? {
        _isLoading.value = true

        _emailError.value = ""
        _passError.value = ""
        _confirmPassError.value = ""
        _displayNameError.value = ""

        var isValid = true
        if (isUpperCase(email)) {
            _emailError.value = context.getString(R.string.email_uppercase)
            isValid = false
        }
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
        } else if (password.length !in 8..64) {
            _passError.value = context.getString(R.string.password_length_invalid)
            isValid = false
        } else if(!password.isValidPassword()){
            _passError.value = context.getString(R.string.password_invalid)
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
            registerUseCase(displayName, password, email, domain)
        } else {
            null
        }

        _isLoading.value = false
        return result
    }
    private fun isUpperCase(str: String): Boolean {
        return Pattern.compile(regexUppercase).matcher(str).find()
    }

}