package com.clearkeep.presentation.screen.chat.bannedusers

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.clearkeep.common.presentation.components.CKSimpleTheme

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