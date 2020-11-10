package com.clearkeep.chat.single

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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navArgument
import androidx.navigation.compose.rememberNavController
import com.clearkeep.R
import com.clearkeep.chat.common_views.EnterReceiverScreen
import com.clearkeep.ui.lightThemeColors
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.navigation.compose.navigate
import com.clearkeep.chat.common_views.RoomChatScreen
import com.clearkeep.chat.common_views.RoomListScreen

@AndroidEntryPoint
class SingleChatActivity : AppCompatActivity() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val singleChatViewModel: SingleChatViewModel by viewModels {
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
                    Text(text = "Single Chat 1:1")
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
                        navController.navigate("enterReceiver")
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
                    singleChatViewModel.getSingleRooms().observeAsState().value.let {
                        it?.let { it1 ->
                            RoomListScreen(
                                    navController,
                                    it1,
                                    onRoomSelected = { room ->
                                        navController.navigate("singleChatRoom/${room.id}/${room.roomName}/${room.remoteId}")
                                    },
                                    onFinishActivity = {
                                        finish()
                                    }
                            )
                        }
                    }
                }
                composable(
                    "singleChatRoom/{roomId}/{roomName}/{remoteId}",
                        arguments = listOf(
                                navArgument("roomId") { type = NavType.IntType },
                                navArgument("roomName") { type = NavType.StringType },
                                navArgument("remoteId") { type = NavType.StringType }
                        ),
                ) { backStackEntry ->
                    val roomId = backStackEntry.arguments!!.getInt("roomId")!!
                    val roomName = backStackEntry.arguments!!.getString("roomName")!!
                    val receiverId = backStackEntry.arguments!!.getString("remoteId")!!
                    singleChatViewModel.getMessageList(roomId).observeAsState().value.let {
                        RoomChatScreen(
                                navController,
                                myClientId = singleChatViewModel.getMyClientId(),
                                roomName = roomName,
                                messageList = it ?: emptyList(),
                                onSendMessage = { message -> singleChatViewModel.sendMessage(receiverId, message)}
                        )
                    }
                }
                composable("enterReceiver") { EnterReceiverScreen(navController, singleChatViewModel) }
            }
        }
    }
}