package com.clearkeep.features.splash.presentation

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import com.clearkeep.common.presentation.components.CKTheme
import com.clearkeep.navigation.NavigationUtils
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
            if (splashViewModel.isUserRegistered()) {
                splashViewModel.setupEnvironment()
                navigateToHomeActivity()
            } else navigateToLoginActivity()
        }
    }

    private fun navigateToHomeActivity() {
        NavigationUtils.navigateToHomeActivity(this)
        finish()
    }

    private fun navigateToLoginActivity() {
        NavigationUtils.navigateToStartActivity(this)
        finish()
    }
}