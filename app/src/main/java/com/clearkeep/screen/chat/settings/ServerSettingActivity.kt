package com.clearkeep.screen.chat.settings

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.*
import androidx.lifecycle.*
import com.clearkeep.presentation.components.CKSimpleTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.livedata.observeAsState
import com.clearkeep.R


@AndroidEntryPoint
class ServerSettingActivity : AppCompatActivity(), LifecycleObserver {
    private val serverSettingViewModel: ServerSettingViewModel by viewModels()

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
        val server = serverSettingViewModel.server.observeAsState()
        server?.value?.let { server ->
            ServerSettingScreen(
                server = server,
                onCopiedServerDomain = {
                    copyProfileLinkToClipBoard("server domain", it)
                },
                onBackPressed = {
                    finish()
                }
            )
        }
    }

    private fun copyProfileLinkToClipBoard(label: String, text: String) {
        val clipboard: ClipboardManager =
            getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(applicationContext, getString(R.string.copied), Toast.LENGTH_SHORT).show()
    }
}