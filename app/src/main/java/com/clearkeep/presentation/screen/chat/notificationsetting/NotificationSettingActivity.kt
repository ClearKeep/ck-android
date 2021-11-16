package com.clearkeep.presentation.screen.chat.notificationsetting

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.clearkeep.presentation.components.CKSimpleTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NotificationSettingActivity : AppCompatActivity() {
    private val notificationSettingsViewModel: NotificationSettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CKSimpleTheme {
                NotificationSettingScreen(notificationSettingsViewModel) {
                    finish()
                }
            }
        }
    }
}