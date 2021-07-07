package com.clearkeep.screen.chat.room

import android.app.NotificationManager
import android.content.Context
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
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.clearkeep.components.CKTheme
import com.clearkeep.db.clear_keep.model.ChatGroup
import com.clearkeep.db.clear_keep.model.User
import com.clearkeep.screen.videojanus.AppCall
import com.clearkeep.screen.chat.group_invite.InviteGroupViewModel
import com.clearkeep.screen.chat.room.room_detail.GroupMemberScreen
import com.clearkeep.screen.chat.room.room_detail.RoomInfoScreen
import com.clearkeep.utilities.network.Status
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.clearkeep.utilities.printlnCK


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

    private var roomId: Long = 0
    private lateinit var domain: String
    private lateinit var clientId: String

    @ExperimentalFoundationApi
    @ExperimentalComposeUiApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // To keep input text field above keyboard
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

        roomId = intent.getLongExtra(GROUP_ID, 0)
        domain = intent.getStringExtra(DOMAIN) ?: ""
        clientId = intent.getStringExtra(CLIENT_ID) ?: ""
        val friendId = intent.getStringExtra(FRIEND_ID) ?: ""

        if (roomId > 0) {
            val notificationManagerCompat = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManagerCompat.cancel(roomId.toInt())
        }

        roomViewModel.joinRoom(domain, clientId, roomId, friendId)

        setContent {
            CKTheme {
                val navController = rememberNavController()
                val selectedItem = remember { mutableStateListOf<User>() }
                NavHost(navController, startDestination = "room_screen") {
                    composable("room_screen") {
                        RoomScreen(
                            roomViewModel,
                            navController,
                            onFinishActivity = {
                                finish()
                            },
                            onCallingClick = { isPeer ->
                                AppCall.openCallAvailable(this@RoomActivity, isPeer)
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
                        /*InviteGroupScreenComingSoon(
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
                        )*/
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val newRoomId = intent.getLongExtra(GROUP_ID, 0)
        printlnCK("onNewIntent, $newRoomId")
        if (newRoomId > 0 && newRoomId != roomId) {
            finish()
            startActivity(intent)
        }
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

    private fun navigateToInComingCallActivity(group: ChatGroup, isAudioMode: Boolean) {
        val roomName = if (group.isGroup()) group.groupName else {
            group.clientList.firstOrNull { client ->
                client.id != clientId
            }?.userName ?: ""
        }
        AppCall.call(this, isAudioMode, null, group.groupId.toString(), group.groupType, roomName,  clientId, roomName, "", false)
    }

    companion object {
        const val GROUP_ID = "room_id"
        const val DOMAIN = "domain"
        const val CLIENT_ID = "client_id"
        const val FRIEND_ID = "remote_id"
    }
}