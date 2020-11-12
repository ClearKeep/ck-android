package com.clearkeep.chat.group

import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.navigation.NavType
import androidx.navigation.compose.*
import com.clearkeep.chat.group.compose.CreateGroupScreen
import com.clearkeep.chat.group.compose.RoomListScreen
import com.clearkeep.chat.group.compose.GroupRoomScreen

@Composable
fun GroupChatScreen(groupChatViewModel: GroupChatViewModel) {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "roomListScreen") {
        composable("roomListScreen") {
            groupChatViewModel.getGroupRoomList().observeAsState().value.let {
                it?.let { it1 ->
                    RoomListScreen(
                        navController,
                        clientId = groupChatViewModel.getClientId(),
                        it1,
                        onRoomSelected = { room ->
                            navController.navigate("groupChatRoom/${room.id}/${room.roomName}/${room.remoteId}")
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
            GroupRoomScreen(
                navController,
                roomId = roomId,
                roomName = roomName,
                groupId = groupId,
                groupChatViewModel = groupChatViewModel
            )
        }
        composable("createGroup") { CreateGroupScreen(navController, groupChatViewModel) }
    }
}