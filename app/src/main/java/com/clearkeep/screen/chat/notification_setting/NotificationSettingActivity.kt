package com.clearkeep.screen.chat.notification_setting

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.clearkeep.components.CKSimpleTheme
import com.clearkeep.screen.chat.main.notification_setting.NotificationSettingScreen

class NotificationSettingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CKSimpleTheme {
                NotificationSettingScreen {
                    finish()
                }
            }
        }
    }
}