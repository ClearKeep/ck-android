package com.clearkeep.screen.chat.banned_users

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.clearkeep.components.CKSimpleTheme

class BannedUserActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CKSimpleTheme {
                BannedUserScreen {
                    finish()
                }
            }
        }
    }
}