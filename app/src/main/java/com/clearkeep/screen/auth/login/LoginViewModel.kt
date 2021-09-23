package com.clearkeep.screen.auth.login

import android.content.Context
import androidx.lifecycle.*
import auth.AuthOuterClass
import com.clearkeep.R
import com.clearkeep.db.clear_keep.model.LoginResponse
import com.clearkeep.screen.auth.repo.AuthRepository
import com.clearkeep.screen.chat.repo.WorkSpaceRepository
import com.clearkeep.utilities.*
import com.clearkeep.utilities.network.Resource
import com.clearkeep.utilities.network.Status
import com.facebook.AccessToken
import com.facebook.login.LoginManager
import javax.inject.Inject
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.microsoft.identity.client.*
import com.microsoft.identity.client.exception.MsalException
import com.facebook.GraphRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch


class LoginViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val workSpaceRepository: WorkSpaceRepository
): ViewModel() {
    private val _isLoading = MutableLiveData<Boolean>()

    lateinit var googleSignIn: GoogleSignInOptions
    lateinit var googleSignInClient: GoogleSignInClient
    val SCOPES_MICROSOFT = arrayOf("Files.Read","User.Read")
    var mSingleAccountApp: ISingleAccountPublicClientApplication? = null
    var loginFacebookManager=LoginManager.getInstance()

    var isCustomServer: Boolean = false
    var customDomain: String = ""

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

    val verifyOtpResponse = MutableLiveData<Resource<String>>()

    val serverUrlValidateResponse = MutableLiveData<String>()

    private var checkValidServerJob: Job? = null

    private var otpHash: String = ""
    private var userId: String = ""
    private var hashKey: String = ""

    private var preAccessToken: String = ""

    private val _isAccountLocked = MutableLiveData<Boolean>()
    val isAccountLocked : LiveData<Boolean> get() = _isAccountLocked

    private val _securityPhrase = MutableLiveData<String>()
    private val _confirmSecurityPhrase = MutableLiveData<String>()

    private val _isSecurityPhraseValid = MutableLiveData<Boolean>()
    val isSecurityPhraseValid : LiveData<Boolean> get() = _isSecurityPhraseValid

    private val _isConfirmSecurityPhraseValid = MutableLiveData<Boolean>()
    val isConfirmSecurityPhraseValid : LiveData<Boolean> get() = _isConfirmSecurityPhraseValid

    val verifyPassphraseResponse = MutableLiveData<Resource<AuthOuterClass.AuthRes>>()
    val registerSocialPinResponse = MutableLiveData<Resource<AuthOuterClass.AuthRes>>()

    private var isResetPincode = false

    fun setOtpLoginInfo(otpHash: String, userId: String, hashKey: String) {
        this.otpHash = otpHash
        this.userId = userId
        this.hashKey = hashKey
    }

    fun setResetPincodeState(isResetPincode: Boolean) {
        this.isResetPincode = isResetPincode
    }

    fun setSecurityPhrase(phrase: String) {
        _securityPhrase.value = phrase
        _isSecurityPhraseValid.value = isSecurityPhraseValid(phrase)
    }

    fun setConfirmSecurityPhrase(phrase: String) {
        _confirmSecurityPhrase.value = phrase
        _isConfirmSecurityPhraseValid.value = isConfirmSecurityPhraseValid()
    }

    fun initGoogleSingIn(context: Context){
        googleSignIn= GoogleSignInOptions
            .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestProfile()
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(context, googleSignIn)
        googleSignInClient.signOut()
    }

    fun initMicrosoftSignIn(context: Context,onSuccess: (()->Unit),onError: ((String?)->Unit)){
        PublicClientApplication.createSingleAccountPublicClientApplication(
            context, R.raw.auth_config_single_account, object :
                IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                override fun onCreated(application: ISingleAccountPublicClientApplication?) {
                    mSingleAccountApp = application
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            mSingleAccountApp?.signOut()
                        } catch (e:Exception){
                            printlnCK(e.message.toString())
                        }
                        onSuccess.invoke()
                    }
                }

                override fun onError(exception: MsalException?) {
                    onError.invoke(exception?.message)
                }
            })
    }

    suspend fun loginByGoogle(token: String): Resource<AuthOuterClass.AuthRes> {
        return authRepo.loginByGoogle(token, getDomain()).also {
            if (it.status == Status.SUCCESS) {
                preAccessToken = it.data?.preAccessToken ?: ""
                userId = it.data?.sub ?: ""
            }
        }
    }

    suspend fun loginByFacebook(token: String): Resource<AuthOuterClass.AuthRes> {
        return authRepo.loginByFacebook(token, getDomain()).also {
            if (it.status == Status.SUCCESS) {
                preAccessToken = it.data?.preAccessToken ?: ""
                userId = it.data?.sub ?: ""
            }
        }
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

    suspend fun loginByMicrosoft(accessToken:String):Resource<AuthOuterClass.AuthRes>{
        return authRepo.loginByMicrosoft(accessToken, getDomain()).also {
            if (it.status == Status.SUCCESS) {
                preAccessToken = it.data?.preAccessToken ?: ""
                userId = it.data?.sub ?: ""
            }
        }
    }

    suspend fun login(context: Context, email: String, password: String): Resource<LoginResponse>? {
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

    fun onSubmitNewPin() {
        viewModelScope.launch {
            val pin = _confirmSecurityPhrase.value ?: ""
            registerSocialPinResponse.value = if (isResetPincode) {
                authRepo.resetSocialPin(getDomain(), pin, userId, preAccessToken)
            } else {
                authRepo.registerSocialPin(getDomain(), pin, userId, preAccessToken)
            }
        }
    }

    fun verifySocialPin() {
        viewModelScope.launch {
            val pin = _securityPhrase.value ?: ""
            verifyPassphraseResponse.value = authRepo.verifySocialPin(getDomain(), pin, userId, preAccessToken)
        }
    }

    fun validateOtp(otp: String) {
        if (otp.isBlank() || otp.length != 4) {
            verifyOtpResponse.value = Resource.error("The code you’ve entered is incorrect. Please try again", null)
            return
        }

        viewModelScope.launch {
            val response = authRepo.validateOtp(getDomain(), otp, otpHash, userId, hashKey)
            if (response.status == Status.ERROR) {
                verifyOtpResponse.value = Resource.error(response.message ?: "The code you’ve entered is incorrect. Please try again", null)
            } else {
                verifyOtpResponse.value = Resource.success(null)
            }
        }
    }

    fun requestResendOtp() {
        viewModelScope.launch {
            val response = authRepo.mfaResendOtp(getDomain(), otpHash, userId)
            val errorCode = response.data?.first

            if (response.status == Status.ERROR) {
                verifyOtpResponse.value = Resource.error(response.data?.second ?: "The code you’ve entered is incorrect. Please try again", null)

                if (errorCode == 1069) {
                    _isAccountLocked.value = true
                }
            } else {
                otpHash = response.data?.second ?: ""
            }
        }
    }

    fun resetAccountLock() {
        _isAccountLocked.value = false
    }

    fun checkValidServerUrl(url: String) {
        _isLoading.value = true
        checkValidServerJob?.cancel()
        checkValidServerJob = viewModelScope.launch {
            if (!isValidServerUrl(url)) {
                printlnCK("checkValidServerUrl local validation invalid")
                serverUrlValidateResponse.value = ""
                _isLoading.value = false
                return@launch
            }

            val workspaceInfoResponse = workSpaceRepository.getWorkspaceInfo(domain = url)
            _isLoading.value = false
            if (workspaceInfoResponse.status == Status.ERROR) {
                printlnCK("checkValidServerUrl invalid server from remote")
                serverUrlValidateResponse.value = ""
                return@launch
            }

            serverUrlValidateResponse.value = url
        }
    }

    fun clearLoading() {
        _isLoading.value = false
    }

    fun cancelCheckValidServer() {
        checkValidServerJob?.cancel()
    }

    fun getDomain(): String {
        return if (isCustomServer) customDomain else "$BASE_URL:$PORT"
    }

    private fun isSecurityPhraseValid(phrase: String): Boolean {
        val words = phrase.split("\\s+".toRegex())
        return words.size >= 3 && words.sumBy { it.length } >= 15
    }

    private fun isConfirmSecurityPhraseValid(): Boolean {
        return _confirmSecurityPhrase.value?.trim() == _securityPhrase.value?.trim()
    }
}