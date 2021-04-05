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
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.clearkeep.components.CKTheme
import com.clearkeep.components.base.CKAlertDialog
import com.clearkeep.components.base.CKCircularProgressIndicator
import com.clearkeep.screen.auth.forgot.ForgotActivity
import com.clearkeep.screen.auth.register.RegisterActivity
import com.clearkeep.screen.chat.home.HomePreparingActivity
import com.clearkeep.screen.videojanus.common.InCallServiceLiveData
import com.clearkeep.utilities.network.Status
import com.google.android.gms.auth.api.signin.GoogleSignIn
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.OnFailureListener
import com.microsoft.identity.client.*
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.common.exception.ClientException


@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val SCOPES = arrayOf("Files.Read","User.Read.All")

    /* Azure AD v2 Configs */
    val AUTHORITY = "https://login.microsoftonline.com/common"
    private var mSingleAccountApp: ISingleAccountPublicClientApplication? = null


    private val loginViewModel: LoginViewModel by viewModels {
        viewModelFactory
    }

    var showErrorDiaLog: ((String) -> Unit)? = null

    @SuppressLint("WrongThread")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApp()
        }

        PublicClientApplication.createSingleAccountPublicClientApplication(
            applicationContext, com.clearkeep.R.raw.auth_config_single_account, object :
                IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                override fun onCreated(application: ISingleAccountPublicClientApplication?) {
                    mSingleAccountApp = application;
                    loadAccount();
                }

                override fun onError(exception: MsalException?) {
                    Log.e("antx", "exception: ${exception?.message}")
                }
            })

        Log.e(
            "antx", "mSingleAccountApp==null ====== ${
                mSingleAccountApp?.currentAccount?.priorAccount?.idToken
            }")
    }

        private fun loadAccount() {
        if (mSingleAccountApp == null) {
            Log.e("antx", "mSingleAccountApp==null ======")
            return
        }


        mSingleAccountApp!!.getCurrentAccountAsync(object :
            ISingleAccountPublicClientApplication.CurrentAccountCallback {
            override fun onAccountLoaded(activeAccount: IAccount?) {
                Log.e("antx", "onAccountLoaded: ${activeAccount?.idToken} \nusername: ${activeAccount?.username} \nactiveAccount ${activeAccount}")
            }

            override fun onAccountChanged(priorAccount: IAccount?, currentAccount: IAccount?) {
                Log.e("antx", "onAccountChanged: ${priorAccount?.idToken}")

            }

            override fun onError(exception: MsalException) {
                Log.e("antx", "exception: ${exception?.message}")

            }


        })
    }

    private fun loginMicrosoft() {
        Log.e("antx", "loginMicrosoft click mSingleAccountApp: $mSingleAccountApp")
        mSingleAccountApp?.signIn(this, null, SCOPES, getAuthInteractiveCallback())

    }

    private fun getAuthInteractiveCallback(): AuthenticationCallback {
        return object : AuthenticationCallback {
            override fun onSuccess(authenticationResult: IAuthenticationResult) {
                Log.d("antx", "Successfully authenticated ${authenticationResult.accessToken} ${authenticationResult.account.toString()} ")
            }

            override fun onError(exception: MsalException) {
                Log.e("antx","getAuthInteractiveCallback: onError ${exception.message}")

            }

            override fun onCancel() {
                Log.e("antx","getAuthInteractiveCallback: onCancel")


            }
        }
    }

    private fun callGraphAPI(authenticationResult: IAuthenticationResult) {
        val accessToken = authenticationResult.accessToken

    }


    private val startForResultSignInGoogle =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data);
            onlSignInGoogleResult(task)
        }

    private fun onlSignInGoogleResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            lifecycleScope.launch {
                account?.serverAuthCode
                val res = account?.idToken?.let {
                    loginViewModel.loginByGoogle(it, account?.account?.name)
                }
                when (res?.status) {
                    Status.SUCCESS -> {
                        navigateToHomeActivity()
                    }
                    Status.ERROR -> {
                        showErrorDiaLog?.invoke(res.message ?: "unknown")
                    }
                    else -> {
                        showErrorDiaLog?.invoke("unknown")
                    }
                }
            }
        } catch (e: ApiException) {
            e.printStackTrace()
        }
    }

    private fun signInMicrosoft() {
        loginViewModel.firebaseAuth
            .startActivityForSignInWithProvider(this, loginViewModel.provider.build())
            .addOnSuccessListener {
                Log.e("antx", "signInMicrosoft: ${it.user}")
            }
            .addOnFailureListener(
                OnFailureListener { exception ->
                    exception.printStackTrace()
                })
    }

    private fun signInGoogle() {
        loginViewModel.googleSignInClient = GoogleSignIn.getClient(this, loginViewModel.googleSignIn)
        val signInIntent: Intent = loginViewModel.googleSignInClient.signInIntent
        startForResultSignInGoogle.launch(signInIntent)
    }
    private val startForResultSignInGoogle =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data);
            handleSignInResult(task)
        }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            lifecycleScope.launch{
                account?.serverAuthCode
                val res= account?.idToken?.let {
                    loginViewModel.loginByGoogle(it,account?.account?.name)
                }
                when (res?.status) {
                    Status.SUCCESS -> {
                        navigateToHomeActivity()
                    }
                    Status.ERROR -> {

                    }
                    else -> {

                    }
                }
            }
        } catch (e: ApiException) {
            e.printStackTrace()
        }
    }

    private fun signInMicrosoft() {
        Log.e("antx", "signInMicrosoft: ${loginViewModel.firebaseAuth}")
        loginViewModel.firebaseAuth
            .startActivityForSignInWithProvider(this, loginViewModel.provider.build())
            .addOnSuccessListener {
                Log.e("antx", "signInMicrosoft")
            }
            .addOnFailureListener(
                OnFailureListener {
                    Log.e(
                        "antx",
                        "signInMicrosoft:addOnFailureListener message:  $it \n message: ${it.message}"
                    )
                })
    }

    @Composable
    fun MyApp() {
        CKTheme {
            AppContent()
        }
    }

    @Composable
    fun AppContent() {
        val (showDialog, setShowDialog) = remember { mutableStateOf("") }

        val inCallServiceLiveData = InCallServiceLiveData(this).observeAsState()
        val onLoginPressed: (String, String) -> Unit = { email, password ->
            lifecycleScope.launch {
                val res = loginViewModel.login(this@LoginActivity, email, password)
                    ?: return@launch
                if (res.status == Status.SUCCESS) {
                    navigateToHomeActivity()
                } else if (res.status == Status.ERROR) {
                    setShowDialog(res.message ?: "unknown")
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
                        isLoading = isLoadingState.value ?: false,
                        onLoginGoogle = {
                            signInGoogle()
                        },
                        onLoginMicrosoft = {
                            Log.e("antx", "onLoginMicrosoft click")
                            loginMicrosoft()
                        }
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
            ErrorDialog(showDialog, setShowDialog)
        }
    }

    @Composable
    fun ErrorDialog(showDialog: String, setShowDialog: (String) -> Unit) {
        if (showDialog.isNotEmpty()) {
            CKAlertDialog(
                    title = {
                        Text("Error")
                    },
                    text = {
                        Text(showDialog)
                    },
                    dismissButton = {
                        Button(
                                onClick = {
                                    // Change the state to close the dialog
                                    setShowDialog("")
                                },
                        ) {
                            Text("OK")
                        }
                    },
            )
        }
    }

    private fun navigateToHomeActivity() {
        startActivity(Intent(this, HomePreparingActivity::class.java))
        finish()
    }

    private fun navigateToRegisterActivity() {
        startActivity(Intent(this, RegisterActivity::class.java))
        /*AppCall.call(this, "", 1234, "dai", "", "", false)*/
    }

    private fun navigateToForgotActivity() {
        startActivity(Intent(this, ForgotActivity::class.java))
    }
}





