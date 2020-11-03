package com.clearkeep.main

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.setContent
import com.clearkeep.login.LoginActivity
import com.clearkeep.home.HomeActivity
import com.clearkeep.ui.lightThemeColors
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Splash()
        }

        handleNextNavigation()
    }

    private fun handleNextNavigation() {
        GlobalScope.launch(context = Dispatchers.Main) {
            delay(1000)
            if (mainViewModel.isUserRegistered) navigateToHomeActivity() else navigateToLoginActivity()
        }
    }

    @Composable
    fun Splash() {
        MaterialTheme(
            colors = lightThemeColors
        ) {
            SplashView()
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