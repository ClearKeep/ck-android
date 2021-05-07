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
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import auth.AuthOuterClass
import com.clearkeep.components.CKTheme
import com.clearkeep.components.base.CKAlertDialog
import com.clearkeep.components.base.CKCircularProgressIndicator
import com.clearkeep.screen.auth.forgot.ForgotActivity
import com.clearkeep.screen.auth.register.RegisterActivity
import com.clearkeep.screen.chat.main.MainPreparingActivity
import com.clearkeep.screen.videojanus.common.InCallServiceLiveData
import com.clearkeep.utilities.network.Resource
import com.clearkeep.utilities.network.Status
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

    @SuppressLint("WrongThread")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApp()
        }
        subscriberError()
    }

    @Composable
    fun MyApp() {
        CKTheme {
            AppContent()
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }

    @Composable
    fun AppContent() {
        val (messageError, setShowDialog) = remember { mutableStateOf<ErrorMessage?>(null) }
        loginViewModel.initGoogleSingIn(this)
        showErrorDiaLog = {
            setShowDialog.invoke(it)
        }
        val inCallServiceLiveData = InCallServiceLiveData(this).observeAsState()
        val onLoginPressed: (String, String) -> Unit = { email, password ->
            lifecycleScope.launch {
                val res = loginViewModel.login(this@LoginActivity, email, password)
                    ?: return@launch
                if (res.status == Status.SUCCESS) {
                    navigateToHomeActivity()
                } else if (res.status == Status.ERROR) {
                    setShowDialog(ErrorMessage(title = "Error",message = res.message.toString()))
                }
            }
        }

        val isLoadingState = loginViewModel.isLoading.observeAsState()
        Box() {
            Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
            ) {
                Row(/*modifier = Modifier.weight(1.0f, true)*/) {
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
                        onLoginFacebook={
                            loginFacebook()
                        },
                        isLoading = isLoadingState.value ?: false,
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

    private fun navigateToHomeActivity() {
        startActivity(Intent(this, MainPreparingActivity::class.java))
        finish()
    }

    private fun navigateToRegisterActivity() {
        startActivity(Intent(this, RegisterActivity::class.java))
        /*AppCall.call(this, "", 1234, "dai", "", "", false)*/
    }

    private fun navigateToForgotActivity() {
        startActivity(Intent(this, ForgotActivity::class.java))
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
                    loginViewModel.loginByGoogle(it, account.account?.name)
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
                        authenticationResult.account.username
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
                navigateToHomeActivity()
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
                                userName = name
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
}





