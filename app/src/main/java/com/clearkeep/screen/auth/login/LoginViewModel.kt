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

    suspend fun login(context: Context, email: String, password: String): Resource<AuthOuterClass.AuthRes> {
        _isLoading.value = true
        val result = if (!email.trim().isValidEmail()) {
            Resource.error(context.getString(R.string.email_invalid), null)
        } else {
            authRepo.login(email.trim(), password.trim())
        }
        /*delay(3000)
        val res = Resource.error("error", null)*/
        _isLoading.value = false
        return result
    }
}