package com.clearkeep.chat

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.compose.foundation.Text
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.VectorAsset
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.KEY_ROUTE
import androidx.navigation.compose.currentBackStackEntryAsState
import com.clearkeep.R
import dagger.hilt.android.AndroidEntryPoint
import androidx.navigation.compose.*
import com.clearkeep.chat.main.chat.ChatHistoryScreen
import com.clearkeep.chat.main.chat.ChatViewModel
import com.clearkeep.chat.main.people.PeopleScreen
import com.clearkeep.chat.main.people.PeopleViewModel
import com.clearkeep.chat.room.RoomActivity
import com.clearkeep.db.model.People
import com.clearkeep.db.model.Room
import javax.inject.Inject

val items = listOf(
        Screen.Chat,
        Screen.People,
)

@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val peopleViewModel: PeopleViewModel by viewModels {
        viewModelFactory
    }

    private val chatViewModel: ChatViewModel by viewModels {
        viewModelFactory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            Scaffold (
                    bottomBar = {
                        BottomNavigation {
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val currentRoute = navBackStackEntry?.arguments?.getString(KEY_ROUTE)
                            items.forEach { screen ->
                                BottomNavigationItem(
                                        icon = { Icon(screen.iconId) },
                                        label = { Text(stringResource(screen.resourceId)) },
                                        selected = currentRoute == screen.route,
                                        onClick = {
                                            // This is the equivalent to popUpTo the start destination
                                            navController.popBackStack(navController.graph.startDestination, false)

                                            // This if check gives us a "singleTop" behavior where we do not create a
                                            // second instance of the composable if we are already on that destination
                                            if (currentRoute != screen.route) {
                                                navController.navigate(screen.route)
                                            }
                                        }
                                )
                            }
                        }
                    }
            ) {
                NavHost(navController, startDestination = Screen.Chat.route) {
                    composable(Screen.Chat.route) {
                        ChatHistoryScreen(
                                chatViewModel,
                                onRoomSelected = { room ->
                                    navigateToRoomScreen(room)
                                }
                        )
                    }
                    composable(Screen.People.route) {
                        PeopleScreen(
                                peopleViewModel,
                                onFriendSelected = { friend ->
                                    navigateToRoomScreen(friend)
                                }
                        )
                    }
                }
            }
        }
    }

    private fun navigateToRoomScreen(room: Room) {
        val intent = Intent(this, RoomActivity::class.java)
        intent.putExtra(RoomActivity.CHAT_HISTORY_ID, room.id)
        intent.putExtra(RoomActivity.ROOM_NAME, room.roomName)
        intent.putExtra(RoomActivity.REMOTE_ID, room.remoteId)
        intent.putExtra(RoomActivity.IS_GROUP, room.isGroup)
        intent.putExtra(RoomActivity.IS_FROM_HISTORY, true)
        startActivity(intent)
    }

    private fun navigateToRoomScreen(friend: People) {
        val intent = Intent(this, RoomActivity::class.java)
        intent.putExtra(RoomActivity.ROOM_NAME, friend.userName)
        intent.putExtra(RoomActivity.REMOTE_ID, friend.userName)
        intent.putExtra(RoomActivity.IS_GROUP, false)
        intent.putExtra(RoomActivity.IS_FROM_HISTORY, false)
        startActivity(intent)
    }
}

sealed class Screen(val route: String, @StringRes val resourceId: Int, val iconId: VectorAsset) {
    object Chat : Screen("single_chat", R.string.bottom_nav_single, Icons.Filled.Person)
    object People : Screen("group_chat", R.string.bottom_nav_group, Icons.Filled.Person)
}