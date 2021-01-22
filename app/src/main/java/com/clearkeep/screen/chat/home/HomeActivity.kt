package com.clearkeep.screen.chat.home

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Message
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.KEY_ROUTE
import androidx.navigation.compose.currentBackStackEntryAsState
import com.clearkeep.R
import dagger.hilt.android.AndroidEntryPoint
import androidx.navigation.compose.*
import com.clearkeep.components.CKTheme
import com.clearkeep.components.base.CKButton
import com.clearkeep.components.base.CKCircularProgressIndicator
import com.clearkeep.screen.chat.home.chat_history.ChatHistoryScreen
import com.clearkeep.screen.chat.home.chat_history.ChatViewModel
import com.clearkeep.screen.chat.home.contact_list.PeopleScreen
import com.clearkeep.screen.chat.home.contact_list.PeopleViewModel
import com.clearkeep.screen.chat.room.RoomActivity
import com.clearkeep.db.clear_keep.model.People
import com.clearkeep.db.clear_keep.model.ChatGroup
import com.clearkeep.screen.chat.group_create.CreateGroupActivity
import com.clearkeep.screen.chat.home.profile.ProfileScreen
import com.clearkeep.screen.chat.home.profile.ProfileViewModel
import com.clearkeep.screen.chat.contact_search.SearchUserActivity
import javax.inject.Inject

val items = listOf(
        Screen.Chat,
        Screen.People,
        Screen.Profile,
)

private val BottomNavigationHeight = 56.dp

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
            CKTheme {
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
        }

        homeViewModel.prepareChat()
    }

    @Composable
    private fun LoadingComposable() {
        Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CKCircularProgressIndicator()
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
                modifier = Modifier.padding(vertical = 5.dp)
            )
        }
    }

    @Composable
    private fun MainComposable() {
        val navController = rememberNavController()
        Scaffold(
                bottomBar = {
                    BottomNavigation {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = navBackStackEntry?.arguments?.getString(KEY_ROUTE)
                        items.forEach { screen ->
                            BottomNavigationItem(
                                    selectedContentColor = MaterialTheme.colors.surface,
                                    unselectedContentColor = MaterialTheme.colors.onBackground,
                                    icon = { Icon(screen.iconId) },
                                    label = { Text(stringResource(screen.resourceId), style = MaterialTheme.typography.body2.copy(fontSize = 10.sp)) },
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
            Column {
                Row(modifier = Modifier.weight(1.0f, true)) {
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
                            peopleViewModel.updateContactList()
                            PeopleScreen(
                                peopleViewModel,
                                onFriendSelected = { friend ->
                                    navigateToRoomScreen(friend)
                                },
                                onNavigateToSearch = {
                                    navigateToSearchScreen()
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
                // TODO: work around issue of scaffold
                Spacer(modifier = Modifier.height(BottomNavigationHeight))
            }
        }
    }

    private fun navigateToRoomScreen(group: ChatGroup) {
        val intent = Intent(this, RoomActivity::class.java)
        intent.putExtra(RoomActivity.GROUP_ID, group.id)
        startActivity(intent)
    }

    private fun navigateToRoomScreen(friend: People) {
        val intent = Intent(this, RoomActivity::class.java)
        intent.putExtra(RoomActivity.FRIEND_ID, friend.id)
        startActivity(intent)
    }

    private fun navigateToCreateGroupScreen() {
        val intent = Intent(this, CreateGroupActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToSearchScreen() {
        val intent = Intent(this, SearchUserActivity::class.java)
        startActivity(intent)
    }
}

sealed class Screen(val route: String, @StringRes val resourceId: Int, val iconId: VectorAsset) {
    object Chat : Screen("chat_screen", R.string.bottom_nav_single, Icons.Filled.Message)
    object People : Screen("contact_screen", R.string.bottom_nav_group, Icons.Filled.Contacts)
    object Profile : Screen("profile_screen", R.string.bottom_nav_profile, Icons.Filled.Person)
}