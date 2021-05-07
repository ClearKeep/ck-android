package com.clearkeep.screen.splash

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import com.clearkeep.components.CKTheme
import com.clearkeep.screen.auth.login.LoginActivity
import com.clearkeep.screen.chat.main.MainPreparingActivity
import com.clearkeep.utilities.storage.UserPreferencesStorage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject


@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    private val splashViewModel: SplashViewModel by viewModels()

    @Inject
    lateinit var storage: UserPreferencesStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()

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
        startActivity(Intent(this, MainPreparingActivity::class.java))
        finish()
    }

    private fun navigateToLoginActivity() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}