package com.clearkeep.screen.chat.room

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.ui.platform.setContent
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.clearkeep.components.CKTheme
import com.clearkeep.db.clear_keep.model.ChatGroup
import com.clearkeep.screen.videojanus.AppCall
import com.clearkeep.screen.chat.group_invite.InviteGroupScreen
import com.clearkeep.screen.chat.group_invite.InviteGroupViewModel
import com.clearkeep.screen.chat.room.room_detail.GroupMemberScreen
import com.clearkeep.screen.chat.room.room_detail.RoomInfoScreen
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val roomId = intent.getLongExtra(GROUP_ID, 0)
        val friendId = intent.getStringExtra(FRIEND_ID) ?: ""

        roomViewModel.initRoom(roomId, friendId)

        setContent {
            CKTheme {
                val navController = rememberNavController()
                NavHost(navController, startDestination = "room_screen") {
                    composable("room_screen") {
                        RoomScreen(
                            roomViewModel,
                            navController,
                            onFinishActivity = {
                                finish()
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
                        InviteGroupScreen(
                                inviteGroupViewModel,
                                onFriendSelected = { friends ->
                                    if (!friends.isNullOrEmpty()) {
                                        roomViewModel.inviteToGroup(friends[0].id, roomId)
                                    }
                                },
                                onBackPressed = {
                                    navController.popBackStack()
                                }
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

    private fun subscriber() {
        roomViewModel.requestCallState.observe(this, Observer {
            if (it.status == Status.SUCCESS) {
                it.data?.let { it1 -> navigateToInComingCallActivity(it1) }
            }
        })
    }

    private fun navigateToInComingCallActivity(group: ChatGroup) {
        val roomName = if (group.isGroup()) group.groupName else {
            group.clientList.firstOrNull { client ->
                client.id != roomViewModel.getClientId()
            }?.userName ?: ""
        }
        AppCall.call(this, group.rtcToken, group.id, roomViewModel.getClientId(), roomName, "", false)
    }

    companion object {
        const val GROUP_ID = "room_id"
        const val FRIEND_ID = "remote_id"
    }
}