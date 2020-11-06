package com.clearkeep.chat.group

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.res.loadVectorResource
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavType
import androidx.navigation.compose.*
import com.clearkeep.R
import com.clearkeep.chat.common_views.CreateGroupScreen
import com.clearkeep.chat.common_views.RoomChatScreen
import com.clearkeep.ui.lightThemeColors
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.clearkeep.chat.common_views.RoomListScreen

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
            MaterialTheme(
                colors = lightThemeColors
            ) {
                OurView()
            }
        }
    }

    @Composable
    fun OurView() {
        val navController = rememberNavController()
        Column {
            TopAppBar(
                title = {
                    Text(text = "Group Chatting")
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            val popped = navController.popBackStack(navController.graph.startDestination, false)
                            if (!popped) {
                                finish()
                            }
                        }
                    ) {
                        Icon(asset = Icons.Filled.ArrowBack)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        navController.navigate("createGroup")
                    }) {
                        val vectorAsset = loadVectorResource(R.drawable.ic_add_white_24dp)
                        vectorAsset.resource.resource?.let {
                            Image(
                                asset = it
                            )
                        }
                    }
                }
            )
            NavHost(navController, startDestination = "roomListScreen") {
                composable("roomListScreen") {
                    groupChatViewModel.getGroupRooms().observeAsState().value.let {
                        it?.let { it1 ->
                            RoomListScreen(
                                onRoomSelected = { roomId, remoteId ->
                                    navController.navigate("groupChatRoom/${roomId}/${remoteId}")
                                },
                                it1
                            )
                        }
                    }
                }
                composable(
                    "groupChatRoom/{roomId}/{remoteId}",
                        arguments = listOf(
                                navArgument("roomId") { type = NavType.IntType },
                                navArgument("remoteId") { type = NavType.StringType }
                        ),
                ) {backStackEntry ->
                    val roomId = backStackEntry.arguments!!.getInt("roomId")!!
                    val groupId = backStackEntry.arguments!!.getString("remoteId")!!
                    groupChatViewModel.getMessageList(roomId).observeAsState().value.let {
                        RoomChatScreen(
                            myClientId = groupChatViewModel.getMyClientId(),
                            messageList = it ?: emptyList(),
                            onSendMessage = { message -> groupChatViewModel.sendMessage(roomId, groupId, message)}
                        )
                    }
                }
                composable("createGroup") { CreateGroupScreen(navController, groupChatViewModel) }
            }
        }
    }
}