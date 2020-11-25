package com.clearkeep.screen.chat.room

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.ui.platform.setContent
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.clearkeep.components.CKTheme
import com.clearkeep.screen.chat.invite_group.InviteGroupScreen
import com.clearkeep.screen.chat.invite_group.InviteGroupViewModel
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

        val roomId = intent.getStringExtra(GROUP_ID) ?: ""
        val friendId = intent.getStringExtra(FRIEND_ID) ?: ""

        setContent {
            CKTheme {
                val navController = rememberNavController()
                NavHost(navController, startDestination = "room_screen") {
                    composable("room_screen") {
                        RoomScreen(
                            roomId,
                            friendId,
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
                                        roomViewModel.inviteToGroup(friends[0].id, friendId)
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
    }

    companion object {
        const val GROUP_ID = "room_id"
        const val FRIEND_ID = "remote_id"
    }
}