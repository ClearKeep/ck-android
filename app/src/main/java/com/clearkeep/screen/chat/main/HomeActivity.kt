package com.clearkeep.screen.chat.main

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.VectorAsset
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.KEY_ROUTE
import androidx.navigation.compose.currentBackStackEntryAsState
import com.clearkeep.R
import dagger.hilt.android.AndroidEntryPoint
import androidx.navigation.compose.*
import com.clearkeep.components.base.CKButton
import com.clearkeep.screen.chat.main.chat.ChatHistoryScreen
import com.clearkeep.screen.chat.main.chat.ChatViewModel
import com.clearkeep.screen.chat.main.people.PeopleScreen
import com.clearkeep.screen.chat.main.people.PeopleViewModel
import com.clearkeep.screen.chat.room.RoomActivity
import com.clearkeep.db.model.People
import com.clearkeep.db.model.ChatGroup
import com.clearkeep.screen.chat.create_group.CreateGroupActivity
import com.clearkeep.screen.chat.main.profile.ProfileScreen
import com.clearkeep.screen.chat.main.profile.ProfileViewModel
import javax.inject.Inject

val items = listOf(
        Screen.Chat,
        Screen.People,
        Screen.Profile,
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

    private val profileViewModel: ProfileViewModel by viewModels {
        viewModelFactory
    }

    private val homeViewModel: HomeViewModel by viewModels {
        viewModelFactory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            homeViewModel.loginState.observeAsState().value.let { prepareState ->
                when (prepareState) {
                    PrepareSuccess -> {
                        MainComposable()
                    }
                    PrepareProcessing -> {
                        LoadingComposable()
                    }
                    PrepareError -> {
                        ErrorComposable()
                    }
                }
            }
        }

        homeViewModel.prepareChat()
    }

    @Composable
    private fun LoadingComposable() {
        Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
        }
    }

    @Composable
    private fun ErrorComposable() {
        Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Please try again")
            Spacer(Modifier.preferredHeight(20.dp))
            CKButton(
                "Try again",
                onClick = {
                    homeViewModel.prepareChat()
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
            )
        }
    }

    @Composable
    private fun MainComposable() {
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
                                if (currentRoute != screen.route) {
                                    navController.popBackStack(navController.graph.startDestination, false)
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
                        },
                        onCreateGroup = {
                            navigateToCreateGroupScreen()
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
                composable(Screen.Profile.route) {
                    ProfileScreen(
                            profileViewModel,
                            onLogout = {}
                    )
                }
            }
        }
    }

    private fun navigateToRoomScreen(group: ChatGroup) {
        val intent = Intent(this, RoomActivity::class.java)
        intent.putExtra(RoomActivity.GROUP_ID, group.id)
        intent.putExtra(RoomActivity.GROUP_NAME, group.groupName)
        startActivity(intent)
    }

    private fun navigateToRoomScreen(friend: People) {
        val intent = Intent(this, RoomActivity::class.java)
        intent.putExtra(RoomActivity.FRIEND_ID, friend.id)
        intent.putExtra(RoomActivity.GROUP_NAME, friend.userName)
        startActivity(intent)
    }

    private fun navigateToCreateGroupScreen() {
        val intent = Intent(this, CreateGroupActivity::class.java)
        startActivity(intent)
    }
}

sealed class Screen(val route: String, @StringRes val resourceId: Int, val iconId: VectorAsset) {
    object Chat : Screen("single_chat", R.string.bottom_nav_single, Icons.Filled.Person)
    object People : Screen("group_chat", R.string.bottom_nav_group, Icons.Filled.Person)
    object Profile : Screen("profile", R.string.bottom_nav_profile, Icons.Filled.Person)
}