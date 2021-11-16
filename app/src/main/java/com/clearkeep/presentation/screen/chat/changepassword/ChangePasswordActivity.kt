package com.clearkeep.presentation.screen.chat.changepassword

import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.*
import androidx.lifecycle.*
import com.clearkeep.presentation.components.CKSimpleTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChangePasswordActivity : AppCompatActivity(), LifecycleObserver {
    private val changePasswordViewModel: ChangePasswordViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        val data: Uri? = intent?.data
        changePasswordViewModel.processDeepLinkUri(data)

        setContent {
            CKSimpleTheme {
                MainComposable()
            }
        }
    }

        @Composable
    private fun MainComposable() {
        ChangePasswordScreen(
            changePasswordViewModel,
            onBackPress = {
                finish()
            }
        )
    }
}