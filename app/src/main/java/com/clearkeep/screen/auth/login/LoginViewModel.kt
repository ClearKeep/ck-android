package com.clearkeep.screen.auth.login

import android.content.Context
import android.provider.Settings.Global.getString
import androidx.lifecycle.*
import auth.AuthOuterClass
import com.clearkeep.R
import com.clearkeep.repo.AuthRepository
import com.clearkeep.utilities.isValidEmail
import com.clearkeep.utilities.network.Resource
import javax.inject.Inject

import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase


class LoginViewModel @Inject constructor(
    private val authRepo: AuthRepository
): ViewModel() {
    private val _isLoading = MutableLiveData<Boolean>()
    lateinit var googleSignInClient: GoogleSignInClient


    lateinit var googleSignIn: GoogleSignInOptions
    lateinit var googleSignInClient: GoogleSignInClient


    val isLoading: LiveData<Boolean>
        get() = _isLoading

    private val _emailError = MutableLiveData<String>()

    val emailError: LiveData<String>
        get() = _emailError

    private val _passError = MutableLiveData<String>()

    val passError: LiveData<String>
        get() = _passError

    var provider = OAuthProvider.newBuilder("microsoft.com").apply {
        addCustomParameter("prompt", "consent");
        val scopes = arrayListOf("profile","openid")
        setScopes(scopes)

    }
    var firebaseAuth = Firebase.auth

    fun initGoogleSingIn(context: Context){
        googleSignIn= GoogleSignInOptions
            .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestProfile()
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(context, googleSignIn)
    }

    fun loginByMicrosoft(){
        val pendingResultTask = firebaseAuth.pendingAuthResult
        if (pendingResultTask != null) {
            pendingResultTask
                .addOnSuccessListener {
                    Log.e("antx","loginMicrosoft: ${it.user.providerData[0].uid}")
                }
                .addOnFailureListener {
                    Log.e("antx","loginMicrosoft: Error: $it")
                }
        } else {

        }
    }

    suspend fun loginByGoogle(token: String, userName: String? = ""): Resource<AuthOuterClass.AuthRes> {
        return authRepo.loginByGoogle(token,userName)
    }
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