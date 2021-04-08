
package com.clearkeep.screen.auth.login

import android.content.Intent
import android.os.Bundle
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


@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val loginViewModel: LoginViewModel by viewModels {
        viewModelFactory
    }

    var showErrorDiaLog: ((String) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApp()
        }
    }

    private val startForResultSignInGoogle =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data);
            onlSignInGoogleResult(task)
        }

    private fun onlSignInGoogleResult(completedTask: Task<GoogleSignInAccount>) {
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
                        showErrorDiaLog?.invoke(res.message ?: "unknown")
                    }
                    else -> {
                        showErrorDiaLog?.invoke("unknown")
                    }
            }}
        } catch (e: ApiException) {
            e.printStackTrace()
        }
    }

    private fun signInMicrosoft() {
        loginViewModel.firebaseAuth
            .startActivityForSignInWithProvider(this, loginViewModel.provider.build())
            .addOnSuccessListener {
            }
            .addOnFailureListener(
                OnFailureListener {
                    exception ->  exception.printStackTrace()
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
        loginViewModel.initGoogleSingIn(this)
        showErrorDiaLog = {
            setShowDialog(it)
        }
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

        fun signInGoogle() {
            lifecycleScope.launch {
                val signInIntent: Intent = loginViewModel.googleSignInClient.signInIntent
                startForResultSignInGoogle.launch(signInIntent)
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
                            signInMicrosoft()
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





