package com.clearkeep.screen.auth.login

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Stack
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.setContent
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.clearkeep.screen.auth.register.RegisterActivity
import com.clearkeep.screen.chat.home.HomeActivity
import com.clearkeep.components.lightThemeColors
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
        val onLoginPressed: (String, String) -> Unit = { userName, password -> loginViewModel.login(userName, password) }
        Stack() {
            LoginScreen(
                onLoginPressed = onLoginPressed,
                onRegisterPress = {
                    navigateToRegisterActivity()
                }
            )
        }
    }

    private fun navigateToHomeActivity() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    private fun navigateToRegisterActivity() {
        startActivity(Intent(this, RegisterActivity::class.java))
    }
}





