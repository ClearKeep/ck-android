package com.clearkeep.screen.chat.change_pass_word

import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.*
import androidx.lifecycle.*
import com.clearkeep.components.CKSimpleTheme
import com.clearkeep.utilities.printlnCK
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ChangePasswordActivity : AppCompatActivity(), LifecycleObserver {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val changePasswordViewModel: ChangePasswordViewModel by viewModels {
        viewModelFactory
    }

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