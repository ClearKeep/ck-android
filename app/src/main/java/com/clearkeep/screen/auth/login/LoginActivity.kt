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
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigate
import androidx.navigation.compose.rememberNavController
import auth.AuthOuterClass
import com.clearkeep.components.CKTheme
import com.clearkeep.components.base.CKAlertDialog
import com.clearkeep.components.base.CKCircularProgressIndicator
import com.clearkeep.screen.auth.advance_setting.CustomServerScreen
import com.clearkeep.screen.auth.forgot.ForgotActivity
import com.clearkeep.screen.auth.register.RegisterActivity
import com.clearkeep.screen.chat.otp.EnterOtpScreen
import com.clearkeep.screen.splash.SplashActivity
import com.clearkeep.utilities.network.Resource
import com.clearkeep.utilities.network.Status
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

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val loginViewModel: LoginViewModel by viewModels {
        viewModelFactory
    }

    var showErrorDiaLog: ((ErrorMessage) -> Unit)? = null
    val callbackManager = CallbackManager.Factory.create()

    private var isJoinServer: Boolean = false

    @SuppressLint("WrongThread")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isJoinServer = intent.getBooleanExtra(IS_JOIN_SERVER, false)

        if (isJoinServer) {
            val domain = intent.getStringExtra(SERVER_DOMAIN)
            if(domain.isNullOrBlank()) {
                throw IllegalArgumentException("join server with domain must be not null")
            }
            loginViewModel.isCustomServer = true
            loginViewModel.customDomain = domain
        }

        setContent {
            MyApp(isJoinServer)
        }

        subscriberError()
    }

    @Composable
    fun MyApp(isJoinServer: Boolean) {
        CKTheme {
            MainComposable(isJoinServer)
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }

    @Composable
    fun AppContent(navController: NavController, isjoinServer: Boolean) {
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
                        loginViewModel.setOtpLoginInfo(res.data?.otpHash ?: "", res.data?.sub ?: "", res.data?.hashKey ?: "")
                        navController.navigate("otp_confirm")
                    } else {
                        onLoginSuccess()
                    }
                } else if (res.status == Status.ERROR) {
                    setShowDialog(ErrorMessage(title = "Error",message = res.message.toString()))
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
                            signInGoogle()
                        },
                        onLoginMicrosoft = {
                            signInMicrosoft()
                        },
                        onLoginFacebook = {
                            loginFacebook()
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
    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    fun MainComposable(isJoinServer: Boolean){
        val navController = rememberNavController()
        Box(Modifier
            .fillMaxSize()) {
            NavHost(navController, startDestination = "login"){
                composable("login"){
                    AppContent(navController, isJoinServer)
                }
                composable("advance_setting") {
                    CustomServerScreen(
                        onBackPress = { isCustom, url ->
                            loginViewModel.isCustomServer = isCustom
                            loginViewModel.customDomain = url
                            onBackPressed()
                        },
                        loginViewModel.isCustomServer,
                        loginViewModel.customDomain
                    )
                }
                composable("otp_confirm") {
                    EnterOtpScreen(
                        otpResponse = loginViewModel.verifyOtpResponse,
                        onDismissMessage = { loginViewModel.verifyOtpResponse.value = null },
                        onClickResend = { loginViewModel.requestResendOtp() },
                        onClickSubmit = { loginViewModel.validateOtp(it) },
                        onBackPress = { navController.popBackStack() }) {
                        onLoginSuccess()
                    }
                }
            }
        }
    }

    private fun subscriberError(){
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

    private fun navigateToAdvanceSetting(navController: NavController){
        navController.navigate("advance_setting")
    }

    private fun signInGoogle() {
        lifecycleScope.launch {
            val signInIntent: Intent = loginViewModel.googleSignInClient.signInIntent
            startForResultSignInGoogle.launch(signInIntent)
        }
    }

    private fun signInMicrosoft() {
        loginViewModel.initMicrosoftSignIn(this,
            onSuccess = {
                loginViewModel.mSingleAccountApp?.signIn(
                    this, null, loginViewModel.SCOPES_MICROSOFT,
                    getAuthInteractiveCallback()
                )
            }, onError = {
                showErrorDiaLog?.invoke(ErrorMessage("Error",it.toString()))
            })
    }


    private fun onSignInGoogleResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            Log.e("antx","onSignInGoogleResult: ${account.toString()}")
            lifecycleScope.launch {
                account?.serverAuthCode
                val res = account?.idToken?.let {
                    loginViewModel.loginByGoogle(it)
                }
                onSignInResult(res)
            }
        } catch (e: ApiException) {
            e.printStackTrace()
        }
    }

    private val startForResultSignInGoogle =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            onSignInGoogleResult(task)
        }

    private fun getAuthInteractiveCallback(): AuthenticationCallback {
        return object : AuthenticationCallback {
            override fun onSuccess(authenticationResult: IAuthenticationResult) {
                lifecycleScope.launch{
                    val res = loginViewModel.loginByMicrosoft(
                        authenticationResult.accessToken,
                    )
                    onSignInResult(res)
                }
            }
            override fun onError(exception: MsalException) {
                showErrorDiaLog?.invoke(ErrorMessage("Error",exception.message ?: "unknown"))

            }

            override fun onCancel() {
            }
        }
    }

    fun onSignInResult(res: Resource<AuthOuterClass.AuthRes>?) {
        when (res?.status) {
            Status.SUCCESS -> {
                onLoginSuccess()
            }
            Status.ERROR -> {
                showErrorDiaLog?.invoke(ErrorMessage("Error",res.message ?: "unknown"))
            }
            else -> {
                showErrorDiaLog?.invoke(ErrorMessage("Error", "unknown"))
            }
        }
    }

    private fun loginFacebook() {
        loginViewModel.loginFacebookManager.logIn(this, arrayListOf("email", "public_profile"))
        loginViewModel.loginFacebookManager.registerCallback(callbackManager,
            object : FacebookCallback<LoginResult?> {
                override fun onSuccess(loginResult: LoginResult?) {
                    loginViewModel.getFacebookProfile(AccessToken.getCurrentAccessToken()) { name ->
                        lifecycleScope.launch {
                            val res = loginViewModel.loginByFacebook(
                                token = AccessToken.getCurrentAccessToken().token,
                            )
                            onSignInResult(res)
                        }
                    }
                }

                override fun onCancel() {
                }

                override fun onError(exception: FacebookException) {
                    showErrorDiaLog?.invoke(ErrorMessage("Error", exception.message ?: "unknown"))
                }
            })
    }

    data class ErrorMessage(val title:String, val message: String)

    companion object {
        const val IS_JOIN_SERVER = "is_join_server"
        const val SERVER_DOMAIN = "server_url_join"
    }
}





