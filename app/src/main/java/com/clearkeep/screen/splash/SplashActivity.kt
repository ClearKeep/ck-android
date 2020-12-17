package com.clearkeep.screen.splash

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.setContent
import com.clearkeep.screen.chat.home.HomeActivity
import com.clearkeep.screen.auth.login.LoginActivity
import com.clearkeep.components.CKTheme
import com.clearkeep.utilities.FIREBASE_TOKEN
import com.clearkeep.utilities.UserManager
import com.clearkeep.utilities.storage.Storage
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    private val splashViewModel: SplashViewModel by viewModels()

    @Inject
    lateinit var storage: Storage

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
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("Test", "Fetching FCM registration token failed", task.exception)
            }

            // Get new FCM registration token
            val token = task.result
            GlobalScope.launch(context = Dispatchers.Main) {
                if (!token.isNullOrEmpty()) {
                    storage.setString(FIREBASE_TOKEN, token)
                }
                delay(1000)
                if (splashViewModel.isUserRegistered) navigateToHomeActivity() else navigateToLoginActivity()
            }
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