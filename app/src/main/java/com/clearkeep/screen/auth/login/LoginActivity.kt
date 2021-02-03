package com.clearkeep.screen.auth.login

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.setContent
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.clearkeep.components.base.CKAlertDialog
import com.clearkeep.components.base.CKCircularProgressIndicator
import com.clearkeep.components.lightThemeColors
import com.clearkeep.screen.auth.forgot.ForgotActivity
import com.clearkeep.screen.auth.register.RegisterActivity
import com.clearkeep.screen.chat.home.HomeActivity
import com.clearkeep.screen.videojanus.AppCall
import com.clearkeep.screen.videojanus.common.InCallServiceLiveData
import com.clearkeep.utilities.network.Status
import com.clearkeep.utilities.printlnCK
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val loginViewModel: LoginViewModel by viewModels {
        viewModelFactory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApp()
        }
    }

    @Composable
    fun MyApp() {
        MaterialTheme(
                colors = lightThemeColors
        ) {
            AppContent()
        }
    }

    @Composable
    fun AppContent() {
        val (showDialog, setShowDialog) = remember { mutableStateOf("") }

        val inCallServiceLiveData = InCallServiceLiveData(this).observeAsState()
        val onLoginPressed: (String, String) -> Unit = { email, password ->
            lifecycleScope.launch {
                val res = loginViewModel.login(email, password)
                if (res.status == Status.SUCCESS) {
                    navigateToHomeActivity()
                } else if (res.status == Status.ERROR) {
                    setShowDialog(res.message ?: "unknown")
                    /*showLoginError(res.message ?: "unknown")*/
                }
            }
        }

        val isLoadingState = loginViewModel.isLoading.observeAsState()
        Box() {
            isLoadingState.value?.let { isLoading ->
                printlnCK("Test, loading $isLoading")
                if (isLoading) {
                    Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CKCircularProgressIndicator(
                                color = Color.Blue
                        )
                    }
                }
            }
            Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
            ) {
                Row(/*modifier = Modifier.weight(1.0f, true)*/) {
                    LoginScreen(
                            onLoginPressed = onLoginPressed,
                            onRegisterPress = {
                                navigateToRegisterActivity()
                            },
                            onForgotPasswordPress = {
                                navigateToForgotActivity()
                            },
                            isLoading = isLoadingState.value ?: false
                    )
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
        startActivity(Intent(this, HomeActivity::class.java))
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





