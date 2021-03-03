package com.clearkeep.screen.auth.login

import android.content.Context
import androidx.lifecycle.*
import auth.AuthOuterClass
import com.clearkeep.R
import com.clearkeep.screen.repo.AuthRepository
import com.clearkeep.utilities.isValidEmail
import com.clearkeep.utilities.network.Resource
import javax.inject.Inject

class LoginViewModel @Inject constructor(
    private val authRepo: AuthRepository
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

    suspend fun login(context: Context, email: String, password: String): Resource<AuthOuterClass.AuthRes>? {
        _emailError.value = ""
        _passError.value = ""
        _isLoading.value = true

        val result = if (email.isBlank()) {
            _emailError.value = context.getString(R.string.email_empty)
            null
        } else if (!email.trim().isValidEmail()) {
            _emailError.value = context.getString(R.string.email_invalid)
            null
        } else if (password.isBlank()) {
            _passError.value = context.getString(R.string.password_empty)
            null
        } else {
            authRepo.login(email.trim(), password.trim())
        }
        _isLoading.value = false
        return result
    }
}