package com.clearkeep.login

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
import com.clearkeep.chat.group.GroupChatActivity
import com.clearkeep.ui.lightThemeColors
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class GroupLoginActivity : AppCompatActivity() {

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
                navigateToGroupActivity()
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
        val onRegisterPressed: (String) -> Unit = { loginViewModel.registerGroup(it) }
        Stack() {
            LoginMainView(
                onRegisterPressed = onRegisterPressed
            )
        }
    }

    private fun navigateToGroupActivity() {
        startActivity(Intent(this, GroupChatActivity::class.java))
        finish()
    }
}