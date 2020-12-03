package com.clearkeep.screen.splash

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.setContent
import com.clearkeep.screen.chat.home.HomeActivity
import com.clearkeep.screen.auth.login.LoginActivity
import com.clearkeep.components.CKTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    private val splashViewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            OurView()
        }

        nextNavigation()
    }

    @Composable
    fun OurView() {
        CKTheme {
            SplashScreen()
        }
    }

    private fun nextNavigation() {
        GlobalScope.launch(context = Dispatchers.Main) {
            delay(1000)
            if (splashViewModel.isUserRegistered) navigateToHomeActivity() else navigateToLoginActivity()
        }
    }

    private fun navigateToHomeActivity() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    private fun navigateToLoginActivity() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}