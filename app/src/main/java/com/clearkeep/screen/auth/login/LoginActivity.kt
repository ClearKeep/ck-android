package com.clearkeep.screen.auth.login

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.clearkeep.components.base.CKButton
import com.clearkeep.screen.chat.home.HomeActivity
import com.clearkeep.components.lightThemeColors
import com.clearkeep.januswrapper.AppCall
import com.clearkeep.januswrapper.common.InCallServiceLiveData
import com.clearkeep.screen.auth.register.RegisterActivity
import com.clearkeep.utilities.printlnCK
import dagger.hilt.android.AndroidEntryPoint
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

        subscribeUI()
    }

    private fun subscribeUI() {
        loginViewModel.loginState.observe(this, Observer {
            if (it == LoginSuccess) {
                navigateToHomeActivity()
            }
        })
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
        val inCallServiceLiveData = InCallServiceLiveData(this).observeAsState()
        val onLoginPressed: (String, String) -> Unit = { userName, password -> loginViewModel.login(userName, password) }
        Column {
            Row(modifier = Modifier.weight(1.0f, true)) {
                LoginScreen(
                        onLoginPressed = onLoginPressed,
                        onRegisterPress = {
                            navigateToRegisterActivity()
                        }
                )
            }
            /*inCallServiceLiveData.value?.let {
                printlnCK("is call available: $it")
                if (it) {
                    CKButton(
                            "Open Call available",
                            onClick = {
                                AppCall.openCallAvailable(this@LoginActivity)
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
                    )
                }
            }*/
        }
    }

    private fun navigateToHomeActivity() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    private fun navigateToRegisterActivity() {
        startActivity(Intent(this, RegisterActivity::class.java))
        /*AppCall.call(this, 1223L, "android", "dai", "", false)*/
    }
}





