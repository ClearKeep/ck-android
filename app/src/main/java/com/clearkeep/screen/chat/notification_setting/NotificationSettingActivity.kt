package com.clearkeep.screen.chat.notification_setting

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.clearkeep.presentation.components.CKSimpleTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

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