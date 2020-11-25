package com.clearkeep.screen.auth.register

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Stack
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.setContent
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.clearkeep.components.lightThemeColors
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RegisterActivity : AppCompatActivity() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val registerViewModel: RegisterViewModel by viewModels {
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
        registerViewModel.registerState.observe(this, Observer {
            if (it == RegisterSuccess) {
                finish()
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
        val onRegisterPressed: (String, String, String) -> Unit = { userName, password, email -> registerViewModel.register(userName, password, email) }
        Stack() {
            RegisterScreen(
                    onRegisterPressed = onRegisterPressed,
                    onBackPress = {
                        finish()
                    }
            )
        }
    }
}