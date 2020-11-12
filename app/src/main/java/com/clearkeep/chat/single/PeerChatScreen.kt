package com.clearkeep.chat.single

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.navigation.NavType
import androidx.navigation.compose.*
import com.clearkeep.chat.single.compose.EnterReceiverScreen
import com.clearkeep.chat.single.compose.PeerChatScreen
import com.clearkeep.chat.single.compose.PeerRoomListScreen

@Composable
fun SingleChatScreen(singleChatViewModel: PeerChatViewModel) {
    val navController = rememberNavController()
    Column {
        NavHost(navController, startDestination = "roomListScreen") {
            composable("roomListScreen") {
                singleChatViewModel.getRooms().observeAsState().value.let {
                    it?.let { it1 ->
                        PeerRoomListScreen(
                            navController,
                            clientId = singleChatViewModel.getMyClientId(),
                            it1,
                            onRoomSelected = { room ->
                                navController.navigate("singleChatRoom/${room.id}/${room.roomName}/${room.remoteId}")
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
                PeerChatScreen(
                        navController,
                        myClientId = singleChatViewModel.getMyClientId(),
                        roomName = roomName,
                        roomId = roomId,
                        receiverId = receiverId,
                        singleChatViewModel = singleChatViewModel
                )
            }
            composable("enterReceiver") { EnterReceiverScreen(navController, singleChatViewModel) }
        }
    }
}