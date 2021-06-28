package com.clearkeep.screen.chat.change_pass_word

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.*
import androidx.lifecycle.*
import com.clearkeep.components.CKSimpleTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChangePasswordActivity : AppCompatActivity(), LifecycleObserver {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        setContent {
            CKSimpleTheme {
                MainComposable()
            }
        }
    }

    @Composable
    private fun MainComposable() {
        ChangePassWordScreen(
            onBackPress = {
                finish()
            })
    }

}