package com.clearkeep.main

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.setContent
import com.clearkeep.chat.single.SingleChatActivity
import com.clearkeep.login.SingleLoginActivity
import com.clearkeep.login.GroupLoginActivity
import com.clearkeep.ui.lightThemeColors
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            OurView()
        }
    }

    @Composable
    fun OurView() {
        MaterialTheme(
            colors = lightThemeColors
        ) {
            MainScreen(
                onOpenSingleChat = {
                    handleNextNavigation(true)
                },
                onOpenGroupChat = {
                    handleNextNavigation(false)
                }
            )
        }
    }

    private fun handleNextNavigation(isSingle: Boolean) {
        GlobalScope.launch(context = Dispatchers.Main) {
            delay(1000)
            if (isSingle) {
                if (mainViewModel.isUserRegistered) navigateToHomeActivity() else navigateToLoginActivity()
            } else {
                //if (mainViewModel.isGroupUserRegistered) navigateToGroupActivity() else navigateToGroupLoginActivity()
                navigateToGroupLoginActivity()
            }
        }
    }

    private fun navigateToHomeActivity() {
        startActivity(Intent(this, SingleChatActivity::class.java))
        finish()
    }

    private fun navigateToLoginActivity() {
        startActivity(Intent(this, SingleLoginActivity::class.java))
        finish()
    }

    private fun navigateToGroupLoginActivity() {
        startActivity(Intent(this, GroupLoginActivity::class.java))
        finish()
    }
}