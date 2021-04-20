package com.clearkeep.screen.chat.room

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.clearkeep.R
import com.clearkeep.components.CKTheme
import com.clearkeep.db.clear_keep.model.ChatGroup
import com.clearkeep.db.clear_keep.model.People
import com.clearkeep.screen.videojanus.AppCall
import com.clearkeep.screen.chat.group_invite.InviteGroupScreenComingSoon
import com.clearkeep.screen.chat.group_invite.InviteGroupViewModel
import com.clearkeep.screen.chat.room.room_detail.GroupMemberScreen
import com.clearkeep.screen.chat.room.room_detail.RoomInfoScreen
import com.clearkeep.utilities.ACTION_MESSAGE_REPLY
import com.clearkeep.utilities.MESSAGE_NOTIFICATION_ID
import com.clearkeep.utilities.network.Status
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RoomActivity : AppCompatActivity() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val roomViewModel: RoomViewModel by viewModels {
        viewModelFactory
    }

    private val inviteGroupViewModel: InviteGroupViewModel by viewModels {
        viewModelFactory
    }

    @ExperimentalFoundationApi
    @ExperimentalComposeUiApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // To keep input text field above keyboard
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ContextCompat.getColor(this, R.color.backgroundGradientStart)

        val roomId = intent.getLongExtra(GROUP_ID, 0)
        val friendId = intent.getStringExtra(FRIEND_ID) ?: ""

        roomViewModel.joinRoom(roomId, friendId)

        setContent {
            CKTheme {
                val navController = rememberNavController()
                val selectedItem = remember { mutableStateListOf<People>() }
                NavHost(navController, startDestination = "room_screen") {
                    composable("room_screen") {
                        RoomScreen(
                            roomViewModel,
                            navController,
                            onFinishActivity = {
                                finish()
                            },
                            onCallingClick = {
                                AppCall.openCallAvailable(this@RoomActivity)
                            }
                        )
                    }
                    composable("room_info_screen") {
                        RoomInfoScreen(
                                roomViewModel,
                            navController
                        )
                    }
                    composable("invite_group_screen") {
                        InviteGroupScreenComingSoon(
                                inviteGroupViewModel,
                                onFriendSelected = { friends ->
                                    if (!friends.isNullOrEmpty()) {
                                        roomViewModel.inviteToGroup(friends[0].id, roomId)
                                    }
                                },
                                onBackPressed = {
                                    navController.popBackStack()
                                },
                            selectedItem = selectedItem
                        )
                    }
                    composable("member_group_screen") {
                        GroupMemberScreen(
                                roomViewModel,
                                navController
                        )
                    }
                }
            }
        }

        subscriber()
    }

    override fun onDestroy() {
        roomViewModel.leaveRoom()
        super.onDestroy()
    }

    private fun subscriber() {
        roomViewModel.requestCallState.observe(this, Observer {
            if (it.status == Status.SUCCESS) {
                it.data?.let { requestInfo -> navigateToInComingCallActivity(requestInfo.chatGroup, requestInfo.isAudioMode) }
            }
        })
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.action == ACTION_MESSAGE_REPLY) {
            NotificationManagerCompat.from(this).cancel(null, MESSAGE_NOTIFICATION_ID)
            finish()
            startActivity(intent)
        }
    }

    private fun navigateToInComingCallActivity(group: ChatGroup, isAudioMode: Boolean) {
        val roomName = if (group.isGroup()) group.groupName else {
            group.clientList.firstOrNull { client ->
                client.id != roomViewModel.getClientId()
            }?.userName ?: ""
        }
        AppCall.call(this, isAudioMode, null, group.id.toString(), group.groupType, roomName, roomViewModel.getClientId(), roomName, "", false)
    }

    companion object {
        const val GROUP_ID = "room_id"
        const val FRIEND_ID = "remote_id"
    }
}