package com.clearkeep.chat.room

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.ui.platform.setContent
import androidx.lifecycle.ViewModelProvider
import com.clearkeep.components.CKScaffold
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RoomActivity : AppCompatActivity() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val roomViewModel: RoomViewModel by viewModels {
        viewModelFactory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isFromHistory = intent.getBooleanExtra(IS_FROM_HISTORY, false)
        val roomName = intent.getStringExtra(ROOM_NAME) ?: ""
        val remoteId = intent.getStringExtra(REMOTE_ID) ?: ""
        val isGroup = intent.getBooleanExtra(IS_GROUP, false)

        val roomId = intent.getIntExtra(CHAT_HISTORY_ID, -1)
        setContent {
            CKScaffold {
                RoomScreen(
                    roomId,
                    roomName,
                    isGroup,
                    remoteId,
                    isFromHistory,
                    roomViewModel,
                    onFinishActivity = {
                        finish()
                    }
                )
            }
        }
    }

    companion object {
        const val CHAT_HISTORY_ID = "room_id"

        const val ROOM_NAME = "room_name"
        const val REMOTE_ID = "remote_id"
        const val IS_GROUP = "is_group"
        const val IS_FROM_HISTORY = "is_from_history"
    }
}