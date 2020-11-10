package com.clearkeep.chat.group

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.setContent
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavType
import androidx.navigation.compose.*
import com.clearkeep.chat.common_views.CreateGroupScreen
import com.clearkeep.chat.common_views.RoomChatScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.clearkeep.chat.common_views.RoomListScreen
import com.clearkeep.ui.CKScaffold

@AndroidEntryPoint
class GroupChatActivity : AppCompatActivity() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val groupChatViewModel: GroupChatViewModel by viewModels {
        viewModelFactory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CKScaffold {
                OurView()
            }
        }
    }

    @Composable
    fun OurView() {
        val navController = rememberNavController()
        Column {
            NavHost(navController, startDestination = "roomListScreen") {
                composable("roomListScreen") {
                    groupChatViewModel.getGroupRooms().observeAsState().value.let {
                        it?.let { it1 ->
                            RoomListScreen(
                                    navController,
                                    it1,
                                    onRoomSelected = { room ->
                                        navController.navigate("groupChatRoom/${room.id}/${room.roomName}/${room.remoteId}")
                                    },
                                    onFinishActivity = {
                                        finish()
                                    }
                            )
                        }
                    }
                }
                composable(
                    "groupChatRoom/{roomId}/{roomName}/{remoteId}",
                        arguments = listOf(
                                navArgument("roomId") { type = NavType.IntType },
                                navArgument("roomName") { type = NavType.StringType },
                                navArgument("remoteId") { type = NavType.StringType }
                        ),
                ) {backStackEntry ->
                    val roomId = backStackEntry.arguments!!.getInt("roomId")!!
                    val roomName = backStackEntry.arguments!!.getString("roomName")!!
                    val groupId = backStackEntry.arguments!!.getString("remoteId")!!
                    groupChatViewModel.getMessageList(roomId).observeAsState().value.let {
                        RoomChatScreen(
                                navController,
                                myClientId = groupChatViewModel.getMyClientId(),
                                roomName = roomName,
                                messageList = it ?: emptyList(),
                                onSendMessage = { message -> groupChatViewModel.sendMessage(roomId, groupId, message) }
                        )
                    }
                }
                composable("createGroup") { CreateGroupScreen(navController, groupChatViewModel) }
            }
        }
    }
}