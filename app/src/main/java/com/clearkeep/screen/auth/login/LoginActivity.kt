package com.clearkeep.screen.auth.login

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.compose.*
import auth.AuthOuterClass
import com.clearkeep.R
import com.clearkeep.components.CKInsetTheme
import com.clearkeep.components.CKTheme
import com.clearkeep.components.base.CKAlertDialog
import com.clearkeep.components.base.CKCircularProgressIndicator
import com.clearkeep.screen.auth.advance_setting.CustomServerScreen
import com.clearkeep.screen.auth.forgot.ForgotActivity
import com.clearkeep.screen.auth.register.RegisterActivity
import com.clearkeep.screen.chat.otp.EnterOtpScreen
import com.clearkeep.screen.chat.social_login.ConfirmSocialLoginPhraseScreen
import com.clearkeep.screen.chat.social_login.EnterSocialLoginPhraseScreen
import com.clearkeep.screen.chat.social_login.SetSocialLoginPhraseScreen
import com.clearkeep.screen.splash.SplashActivity
import com.clearkeep.utilities.ERROR_CODE_TIMEOUT
import com.clearkeep.utilities.network.Resource
import com.clearkeep.utilities.network.Status
import com.clearkeep.utilities.printlnCK
import com.clearkeep.utilities.restartToRoot
import com.facebook.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.microsoft.identity.client.*
import com.microsoft.identity.client.exception.MsalException
import com.facebook.login.LoginResult
import com.google.android.gms.common.ConnectionResult.NETWORK_ERROR
import com.microsoft.identity.client.exception.MsalServiceException

@ExperimentalComposeUiApi
@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val loginViewModel: LoginViewModel by viewModels {
        viewModelFactory
    }

    var showErrorDiaLog: ((ErrorMessage) -> Unit)? = null
    private val callbackManager = CallbackManager.Factory.create()

    private var isJoinServer: Boolean = false

    private var navController: NavController? = null

    private val startForResultSignInGoogle =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            navController?.let {
                printlnCK("onlogingoogle startForResultSignInGoogle controller $navController")
                onSignInGoogleResult(it, task)
            }
        }

    @SuppressLint("WrongThread")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isJoinServer = intent.getBooleanExtra(IS_JOIN_SERVER, false)

        if (isJoinServer) {
            val domain = intent.getStringExtra(SERVER_DOMAIN)
            if (domain.isNullOrBlank()) {
                throw IllegalArgumentException("join server with domain must be not null")
            }
            loginViewModel.isCustomServer = true
            loginViewModel.customDomain = domain
        }

        setContent {
            MyApp()
        }

        subscriberError()
    }

    @Composable
    fun MyApp() {
        CKTheme {
            MainComposable()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }

    @Composable
    fun AppContent(navController: NavController) {
        val (messageError, setShowDialog) = remember { mutableStateOf<ErrorMessage?>(null) }

        loginViewModel.initGoogleSingIn(this)
        showErrorDiaLog = {
            setShowDialog.invoke(it)
        }
        val onLoginPressed: (String, String) -> Unit = { email, password ->
            lifecycleScope.launch {
                val res = loginViewModel.login(this@LoginActivity, email, password)
                    ?: return@launch
                if (res.status == Status.SUCCESS) {
                    val shouldRequireOtp = res.data?.accessToken.isNullOrBlank()
                    if (shouldRequireOtp) {
                        loginViewModel.setOtpLoginInfo(
                            res.data?.otpHash ?: "",
                            res.data?.sub ?: "",
                            res.data?.hashKey ?: ""
                        )
                        navController.navigate("otp_confirm")
                    } else {
                        onLoginSuccess()
                    }
                } else if (res.status == Status.ERROR) {
                    val title = when (res.data?.errorCode) {
                        1001, 1079 -> getString(R.string.login_info_incorrect)
                        1026 -> getString(R.string.error)
                        1069 -> getString(R.string.mfa_account_locked)
                        ERROR_CODE_TIMEOUT -> getString(R.string.network_error_dialog_title)
                        else -> getString(R.string.error)
                    }
                    val dismissButtonTitle = when (res.data?.errorCode) {
                        1001, 1026 -> getString(R.string.ok)
                        1069 -> getString(R.string.close)
                        else -> getString(R.string.ok)
                    }
                    setShowDialog(
                        ErrorMessage(
                            title = title,
                            message = res.message ?: "",
                            dismissButtonText = dismissButtonTitle
                        )
                    )
                }
            }
        }

        val isLoadingState = loginViewModel.isLoading.observeAsState()
        Box {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row {
                    LoginScreen(
                        loginViewModel,
                        onLoginPressed = onLoginPressed,
                        onRegisterPress = {
                            navigateToRegisterActivity()
                        },
                        onForgotPasswordPress = {
                            navigateToForgotActivity()
                        },
                        onLoginGoogle = {
                            this@LoginActivity.navController = navController
                            signInGoogle()
                        },
                        onLoginMicrosoft = {
                            signInMicrosoft(navController)
                        },
                        onLoginFacebook = {
                            loginFacebook(navController)
                        },
                        advanceSetting = {
                            navigateToAdvanceSetting(navController)
                        },
                        isLoading = isLoadingState.value ?: false,
                        isShowAdvanceSetting = !isJoinServer,
                        isJoinServer = isJoinServer,
                        onNavigateBack = { finish() }
                    )
                }
            }

            isLoadingState.value?.let { isLoading ->
                if (isLoading) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CKCircularProgressIndicator()
                    }
                }
            }
            ErrorDialog(messageError, setShowDialog)
        }

    }

    @ExperimentalComposeUiApi
    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    fun MainComposable() {
        val navController = rememberNavController()
        Box(
            Modifier
                .fillMaxSize()
        ) {
            NavHost(navController, startDestination = "login") {
                composable("login") {
                    AppContent(navController)
                }
                composable("advance_setting") {
                    CustomServerScreen(
                        loginViewModel,
                        onBackPress = {
                            navController.popBackStack()
                        },
                        loginViewModel.isCustomServer,
                        loginViewModel.customDomain
                    )
                }
                composable("otp_confirm") {
                    EnterOtpScreen(
                        otpResponse = loginViewModel.verifyOtpResponse,
                        onDismissMessage = {
                            loginViewModel.verifyOtpResponse.value = null

                            if (loginViewModel.isAccountLocked.value == true) {
                                loginViewModel.resetAccountLock()
                                navController.popBackStack()
                            }
                        },
                        onClickResend = { loginViewModel.requestResendOtp() },
                        onClickSubmit = { loginViewModel.validateOtp(it) },
                        onBackPress = { navController.popBackStack() }) {
                        onLoginSuccess()
                    }
                }
                composable("set_security_phrase") {
                    SetSocialLoginPhraseScreen(
                        loginViewModel,
                        onBackPress = { navController.popBackStack() }) {
                        navController.navigate("confirm_security_phrase")
                    }
                }
                composable("confirm_security_phrase") {
                    ConfirmSocialLoginPhraseScreen(
                        loginViewModel,
                        onBackPress = {
                            navController.popBackStack()
                            loginViewModel.clearSecurityPhraseInput()
                        }) {
                        onLoginSuccess()
                    }
                }
                composable("enter_security_phrase") {
                    EnterSocialLoginPhraseScreen(
                        loginViewModel,
                        onBackPress = { navController.popBackStack() },
                        onVerifySuccess = { onLoginSuccess() }) {
                        loginViewModel.resetSecurityPhraseErrors()
                        loginViewModel.setResetPincodeState(true)
                        navController.navigate("set_security_phrase") {
                            popUpTo(route = "login") {

                            }
                        }
                    }
                }
            }
        }
    }

    private fun subscriberError() {
        loginViewModel.loginErrorMess.observe(this, {
            showErrorDiaLog?.invoke(it)
        })
    }

    @Composable
    fun ErrorDialog(errorMessage: ErrorMessage?, setShowDialog: (ErrorMessage?) -> Unit) {
        errorMessage?.let {
            CKAlertDialog(
                title = it.title,
                text = it.message,
                onDismissButtonClick = {
                    // Change the state to close the dialog
                    setShowDialog(null)
                },
                dismissTitle = it.dismissButtonText
            )
        }
    }

    private fun onLoginSuccess() {
        if (isJoinServer) {
            restartToRoot(this)
        } else {
            navigateToHomeActivity()
        }
    }

    private fun onSocialLoginSuccess(navController: NavController, requireAction: String) {
        when (requireAction) {
            "verify_pincode" -> {
                loginViewModel.resetSecurityPhraseErrors()
                loginViewModel.clearSecurityPhraseInput()
                loginViewModel.setResetPincodeState(false)
                navController.navigate("enter_security_phrase")
            }
            "register_pincode" -> {
                navController.navigate("set_security_phrase")
                loginViewModel.setResetPincodeState(false)
            }
        }
    }

    private fun navigateToHomeActivity() {
        val intent = Intent(this, SplashActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private fun navigateToRegisterActivity() {
        val intent = Intent(this, RegisterActivity::class.java)
        intent.putExtra(RegisterActivity.DOMAIN, loginViewModel.getDomain())
        startActivity(intent)
    }

    private fun navigateToForgotActivity() {
        val intent = Intent(this, ForgotActivity::class.java)
        intent.putExtra(ForgotActivity.EXTRA_DOMAIN, loginViewModel.getDomain())
        startActivity(intent)
    }

    private fun navigateToAdvanceSetting(navController: NavController) {
        navController.navigate("advance_setting")
    }

    private fun signInGoogle() {
        lifecycleScope.launch {
            val signInIntent: Intent = loginViewModel.googleSignInClient.signInIntent
            startForResultSignInGoogle.launch(signInIntent)
        }
    }

    private fun signInMicrosoft(navController: NavController) {
        loginViewModel.initMicrosoftSignIn(this,
            onSuccess = {
                loginViewModel.mSingleAccountApp?.signIn(
                    this, null, LoginViewModel.SCOPES_MICROSOFT,
                    getAuthInteractiveCallback(navController)
                )
            }, onError = {
                showErrorDiaLog?.invoke(ErrorMessage(getString(R.string.error), it.toString()))
            })
    }

    private fun onSignInGoogleResult(
        navController: NavController,
        completedTask: Task<GoogleSignInAccount>
    ) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            Log.e("antx", "onSignInGoogleResult: ${account.toString()}")
            lifecycleScope.launch {
                account?.serverAuthCode
                val res = account?.idToken?.let {
                    loginViewModel.loginByGoogle(it)
                }
                onSignInResult(navController, res)
            }
        } catch (e: ApiException) {
            e.printStackTrace()
            //unknown error
            if (e.statusCode == 12501) return
            val (title, text) = if (e.statusCode == NETWORK_ERROR) {
                getString(R.string.network_error_dialog_title) to getString(R.string.network_error_dialog_text)
            } else {
                getString(R.string.error) to (e.message ?: "unknown")
            }
            showErrorDiaLog?.invoke(ErrorMessage(title, text))
        }
    }

    private fun getAuthInteractiveCallback(navController: NavController): AuthenticationCallback {
        return object : AuthenticationCallback {
            override fun onSuccess(authenticationResult: IAuthenticationResult) {
                lifecycleScope.launch {
                    val res = loginViewModel.loginByMicrosoft(
                        authenticationResult.accessToken,
                    )
                    onSignInResult(navController, res)
                }
            }

            override fun onError(exception: MsalException) {
                val isLoginCancelled =
                    exception is MsalServiceException && exception.httpStatusCode == MsalServiceException.DEFAULT_STATUS_CODE
                if (!isLoginCancelled) {
                    val (title, text) = if (exception.errorCode == "device_network_not_available") {
                        getString(R.string.network_error_dialog_title) to getString(R.string.network_error_dialog_text)
                    } else {
                        getString(R.string.error) to (exception.message ?: "unknown")
                    }
                    showErrorDiaLog?.invoke(ErrorMessage(title, text))
                }
            }

            override fun onCancel() {
            }
        }
    }

    fun onSignInResult(
        navController: NavController,
        res: Resource<AuthOuterClass.SocialLoginRes>?
    ) {
        //Third party result
        when (res?.status) {
            Status.SUCCESS -> {
                onSocialLoginSuccess(navController, res.data?.requireAction ?: "")
            }
            Status.ERROR -> {
                showErrorDiaLog?.invoke(
                    ErrorMessage(
                        getString(R.string.error),
                        res.message ?: "unknown"
                    )
                )
            }
            else -> {
                showErrorDiaLog?.invoke(ErrorMessage(getString(R.string.error), "unknown"))
            }
        }
    }

    private fun loginFacebook(navController: NavController) {
        loginViewModel.loginFacebookManager.logIn(this, arrayListOf("email", "public_profile"))
        loginViewModel.loginFacebookManager.registerCallback(callbackManager,
            object : FacebookCallback<LoginResult?> {
                override fun onSuccess(loginResult: LoginResult?) {
                    loginViewModel.getFacebookProfile(AccessToken.getCurrentAccessToken()) { name ->
                        lifecycleScope.launch {
                            val res = loginViewModel.loginByFacebook(
                                token = AccessToken.getCurrentAccessToken().token,
                            )
                            loginViewModel.loginFacebookManager.logOut()
                            onSignInResult(navController, res)
                        }
                    }
                }

                override fun onCancel() {
                }

                override fun onError(exception: FacebookException) {
                    printlnCK("login with FB error $exception")
                    val (title, text) = if (exception.message != null && exception.message!!.contains(
                            "CONNECTION_FAILURE"
                        )
                    ) {
                        getString(R.string.network_error_dialog_title) to getString(R.string.network_error_dialog_text)
                    } else {
                        getString(R.string.error) to (exception.message ?: "unknown")
                    }
                    showErrorDiaLog?.invoke(ErrorMessage(title, text))
                }
            })
    }

    data class ErrorMessage(
        val title: String,
        val message: String,
        val dismissButtonText: String = "OK"
    )

    companion object {
        const val IS_JOIN_SERVER = "is_join_server"
        const val SERVER_DOMAIN = "server_url_join"
    }
}