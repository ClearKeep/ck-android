package com.clearkeep.screen.auth.login

import android.content.Context
import androidx.lifecycle.*
import auth.AuthOuterClass
import com.clearkeep.R
import com.clearkeep.repo.AuthRepository
import com.clearkeep.utilities.BASE_URL
import com.clearkeep.utilities.PORT
import com.clearkeep.utilities.isValidEmail
import com.clearkeep.utilities.network.Resource
import com.facebook.AccessToken
import com.facebook.login.LoginManager
import javax.inject.Inject
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.microsoft.identity.client.*
import com.microsoft.identity.client.exception.MsalException
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest
import com.facebook.appevents.AppEventsLogger;


class LoginViewModel @Inject constructor(
    private val authRepo: AuthRepository
): ViewModel() {
    private val _isLoading = MutableLiveData<Boolean>()

    lateinit var googleSignIn: GoogleSignInOptions
    lateinit var googleSignInClient: GoogleSignInClient
    val SCOPES_MICROSOFT = arrayOf("Files.Read","User.Read")
    var mSingleAccountApp: ISingleAccountPublicClientApplication? = null
    var loginFacebookManager=LoginManager.getInstance()

    var isCustomServer: Boolean = false
    var port: String = ""
    var url: String = ""

    val isLoading: LiveData<Boolean>
        get() = _isLoading

    private val _emailError = MutableLiveData<String>()

    val emailError: LiveData<String>
        get() = _emailError

    private val _passError = MutableLiveData<String>()

    val passError: LiveData<String>
        get() = _passError

    val loginErrorMess = MutableLiveData<LoginActivity.ErrorMessage>()

    val urlCustomServer= MutableLiveData<String>()

    fun initGoogleSingIn(context: Context){
        googleSignIn= GoogleSignInOptions
            .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestProfile()
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(context, googleSignIn)
    }

    fun initMicrosoftSignIn(context: Context,onSuccess: (()->Unit),onError: ((String?)->Unit)){
        PublicClientApplication.createSingleAccountPublicClientApplication(
            context, R.raw.auth_config_single_account, object :
                IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                override fun onCreated(application: ISingleAccountPublicClientApplication?) {
                    mSingleAccountApp = application
                    onSuccess.invoke()
                }

                override fun onError(exception: MsalException?) {
                    onError.invoke(exception?.message)
                }
            })
    }

    suspend fun loginByGoogle(token: String, userName: String? = ""): Resource<AuthOuterClass.AuthRes> {
        return authRepo.loginByGoogle(token,userName, getDomain())
    }

    suspend fun loginByFacebook(token: String, userName: String? = ""): Resource<AuthOuterClass.AuthRes> {
        return authRepo.loginByFacebook(token,userName, getDomain())
    }

    fun getFacebookProfile(accessToken: AccessToken, getName: (String) -> Unit) {
        val request = GraphRequest.newGraphPathRequest(
            accessToken, "me"
        ) {
            try {
                val name = it.jsonObject.get("name").toString()
                getName.invoke(name)
            } catch (e: Exception) {
                getName.invoke("")
            }
        }
        request.executeAsync()
    }


    suspend fun loginByMicrosoft(accessToken:String,userName: String?=""):Resource<AuthOuterClass.AuthRes>{
        return authRepo.loginByMicrosoft(accessToken,userName, getDomain())
    }
    suspend fun login(context: Context, email: String, password: String): Resource<AuthOuterClass.AuthRes>? {
        _emailError.value = ""
        _passError.value = ""
        _isLoading.value = true
        val result = if (email.isBlank()) {
            _emailError.value = context.getString(R.string.email_empty)
            loginErrorMess.postValue(
                LoginActivity.ErrorMessage(
                    context.getString(R.string.email_empty),
                    context.getString(R.string.pls_check_again)
                )
            )
            null
        } else if (!email.trim().isValidEmail()) {
            _emailError.value = context.getString(R.string.email_invalid)
            loginErrorMess.postValue(
                LoginActivity.ErrorMessage(
                    context.getString(R.string.email_invalid),
                    context.getString(R.string.pls_check_again)
                )
            )
            null
        } else if (password.isBlank()) {
            _passError.value = context.getString(R.string.password_empty)
            loginErrorMess.postValue(
                LoginActivity.ErrorMessage(
                    context.getString(R.string.password_empty),
                    context.getString(R.string.pls_check_again)
                )
            )
            null
        } else {
            authRepo.login(email.trim(), password.trim(), getDomain())
        }
        _isLoading.value = false
        return result
    }

    private fun getDomain(): String {
        return if (isCustomServer) "$url:$port" else "$BASE_URL:$PORT"
    }
}