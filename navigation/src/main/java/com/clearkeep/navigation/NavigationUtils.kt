package com.clearkeep.navigation

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity

object NavigationUtils {
    fun navigateToHomeActivity(context: Context) {
        val intent = Intent(context, Class.forName("com.clearkeep.presentation.screen.chat.home.MainActivity"))
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    fun navigateToStartActivity(context: Context) {
        val intent = Intent(context, Class.forName("com.clearkeep.presentation.screen.auth.login.LoginActivity"))
        context.startActivity(intent)
    }
}